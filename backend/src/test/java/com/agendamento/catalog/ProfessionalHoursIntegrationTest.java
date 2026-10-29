package com.agendamento.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.agendamento.catalog.dto.ProfessionalScheduleRequest;
import com.agendamento.support.IntegrationTest;
import com.agendamento.support.TenantFixture;

/**
 * The fixture tenant opens 08:00-18:00 every day, so whatever the public page offers for a
 * professional comes from that professional's own schedule.
 */
class ProfessionalHoursIntegrationTest extends IntegrationTest {

    /** Far enough ahead that no slot of the day is in the past. */
    private static final LocalDate DATE = LocalDate.now().plusMonths(1);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantFixture tenantFixture;

    @Test
    void aProfessionalWithoutASchedulePublishesTheWholeOpeningHours() {
        TenantFixture.Tenant tenant = tenantFixture.create("hours-default");

        ResponseEntity<Schedule> schedule = restTemplate.exchange(
                "/api/professionals/" + tenant.professionalId() + "/hours", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tenant.token())), Schedule.class);

        assertThat(schedule.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(schedule.getBody().followsBusinessHours()).isTrue();
        assertThat(schedule.getBody().entries()).isEmpty();
        assertThat(slots(tenant)).startsWith("08:00").endsWith("17:30");
    }

    @Test
    void savingAShiftRestrictsTheOfferedSlots() {
        TenantFixture.Tenant tenant = tenantFixture.create("hours-shift");

        ResponseEntity<Schedule> saved = putSchedule(tenant, new ProfessionalScheduleRequest(false,
                List.of(entry(LocalTime.of(10, 0), LocalTime.of(12, 0)))));

        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saved.getBody().followsBusinessHours()).isFalse();
        assertThat(saved.getBody().entries()).hasSize(1);
        assertThat(slots(tenant)).startsWith("10:00").endsWith("11:30");
    }

    @Test
    void aDayOffForTheProfessionalLeavesNoSlots() {
        TenantFixture.Tenant tenant = tenantFixture.create("hours-day-off");

        // A shift on the day after the queried one: the queried day becomes a day off.
        int otherDay = DATE.plusDays(1).getDayOfWeek().getValue();
        putSchedule(tenant, new ProfessionalScheduleRequest(false,
                List.of(new ProfessionalScheduleRequest.Entry(otherDay, LocalTime.of(10, 0),
                        LocalTime.of(12, 0)))));

        assertThat(slots(tenant)).isEmpty();
    }

    @Test
    void goingBackToTheBusinessHoursRestoresEveryHour() {
        TenantFixture.Tenant tenant = tenantFixture.create("hours-restore");
        putSchedule(tenant, new ProfessionalScheduleRequest(false,
                List.of(entry(LocalTime.of(10, 0), LocalTime.of(12, 0)))));

        putSchedule(tenant, new ProfessionalScheduleRequest(true, List.of()));

        assertThat(slots(tenant)).startsWith("08:00").endsWith("17:30");
    }

    @Test
    void anEmptyScheduleIsRejectedInsteadOfHidingTheProfessional() {
        TenantFixture.Tenant tenant = tenantFixture.create("hours-empty");

        ResponseEntity<Schedule> response = putSchedule(tenant,
                new ProfessionalScheduleRequest(false, List.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void overlappingPeriodsOfTheSameDayAreRejected() {
        TenantFixture.Tenant tenant = tenantFixture.create("hours-overlap");

        ResponseEntity<Schedule> response = putSchedule(tenant, new ProfessionalScheduleRequest(false,
                List.of(entry(LocalTime.of(10, 0), LocalTime.of(14, 0)),
                        entry(LocalTime.of(13, 0), LocalTime.of(18, 0)))));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void readingTheHoursOfAnotherTenantsProfessionalReturnsNotFound() {
        TenantFixture.Tenant tenantA = tenantFixture.create("hours-tenant-a");
        TenantFixture.Tenant tenantB = tenantFixture.create("hours-tenant-b");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/professionals/" + tenantB.professionalId() + "/hours", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tenantA.token())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Schedule> putSchedule(TenantFixture.Tenant tenant,
            ProfessionalScheduleRequest request) {
        return restTemplate.exchange("/api/professionals/" + tenant.professionalId() + "/hours",
                HttpMethod.PUT, new HttpEntity<>(request, authHeaders(tenant.token())), Schedule.class);
    }

    private ProfessionalScheduleRequest.Entry entry(LocalTime start, LocalTime end) {
        return new ProfessionalScheduleRequest.Entry(DATE.getDayOfWeek().getValue(), start, end);
    }

    private List<String> slots(TenantFixture.Tenant tenant) {
        ResponseEntity<Slot[]> response = restTemplate.getForEntity(
                "/api/public/businesses/" + tenant.slug() + "/availability?serviceId="
                        + tenant.serviceId() + "&professionalId=" + tenant.professionalId()
                        + "&date=" + DATE,
                Slot[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return List.of(response.getBody()).stream().map(Slot::time).toList();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private record Slot(String startsAt, String time) {
    }

    private record Schedule(boolean followsBusinessHours, List<Entry> entries) {

        private record Entry(String id, int dayOfWeek, String startTime, String endTime) {
        }
    }
}
