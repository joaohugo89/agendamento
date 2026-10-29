package com.agendamento.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.agendamento.appointment.dto.CreateAppointmentRequest;
import com.agendamento.appointment.dto.UpdateStatusRequest;
import com.agendamento.catalog.dto.ProfessionalRequest;
import com.agendamento.support.IntegrationTest;
import com.agendamento.support.TenantFixture;

/** The day agenda: what it lists, how it filters and which status changes it accepts. */
class AgendaIntegrationTest extends IntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATE = LocalDate.now(ZONE).plusMonths(1);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantFixture tenantFixture;

    @Test
    void listsTheDayAndFiltersByProfessional() {
        TenantFixture.Tenant tenant = tenantFixture.create("agenda-filter");
        UUID other = createProfessional(tenant, "Carlos");

        book(tenant, tenant.professionalId(), LocalTime.of(9, 0), "Rafael");
        book(tenant, tenant.professionalId(), LocalTime.of(10, 0), "Bruno");
        book(tenant, other, LocalTime.of(9, 0), "Diego");

        List<AgendaRow> wholeDay = agenda(tenant, null);
        assertThat(wholeDay).hasSize(3);
        assertThat(wholeDay).extracting(AgendaRow::startTime).containsExactly("09:00", "09:00", "10:00");

        List<AgendaRow> onlyOther = agenda(tenant, other);
        assertThat(onlyOther).hasSize(1);
        assertThat(onlyOther.get(0).customerName()).isEqualTo("Diego");
    }

    @Test
    void anotherDayDoesNotLeakIntoTheAgenda() {
        TenantFixture.Tenant tenant = tenantFixture.create("agenda-day");
        book(tenant, tenant.professionalId(), LocalTime.of(9, 0), "Rafael");

        ResponseEntity<AgendaRow[]> response = restTemplate.exchange(
                "/api/appointments?date=" + DATE.plusDays(1), HttpMethod.GET,
                new HttpEntity<>(authHeaders(tenant.token())), AgendaRow[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void anAppointmentOnlyLeavesConfirmedOnce() {
        TenantFixture.Tenant tenant = tenantFixture.create("agenda-status");
        book(tenant, tenant.professionalId(), LocalTime.of(9, 0), "Rafael");
        AgendaRow row = agenda(tenant, null).get(0);
        assertThat(row.status()).isEqualTo("CONFIRMED");

        ResponseEntity<AgendaRow> completed = changeStatus(tenant, row.id(), AppointmentStatus.COMPLETED);
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody().status()).isEqualTo("COMPLETED");

        ResponseEntity<AgendaRow> again = changeStatus(tenant, row.id(), AppointmentStatus.CANCELLED);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private UUID createProfessional(TenantFixture.Tenant tenant, String name) {
        ResponseEntity<Created> response = restTemplate.exchange("/api/professionals", HttpMethod.POST,
                new HttpEntity<>(new ProfessionalRequest(name, true, List.of(tenant.serviceId())),
                        authHeaders(tenant.token())),
                Created.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(response.getBody().id());
    }

    private void book(TenantFixture.Tenant tenant, UUID professionalId, LocalTime time, String customer) {
        Instant startsAt = ZonedDateTime.of(DATE, time, ZONE).toInstant();
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/public/businesses/" + tenant.slug() + "/appointments",
                new CreateAppointmentRequest(tenant.serviceId(), professionalId, startsAt, customer,
                        "11988887777", null),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private List<AgendaRow> agenda(TenantFixture.Tenant tenant, UUID professionalId) {
        String url = "/api/appointments?date=" + DATE
                + (professionalId == null ? "" : "&professionalId=" + professionalId);
        ResponseEntity<AgendaRow[]> response = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders(tenant.token())), AgendaRow[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return List.of(response.getBody());
    }

    private ResponseEntity<AgendaRow> changeStatus(TenantFixture.Tenant tenant, String id,
            AppointmentStatus status) {
        return restTemplate.exchange("/api/appointments/" + id + "/status", HttpMethod.PATCH,
                new HttpEntity<>(new UpdateStatusRequest(status), authHeaders(tenant.token())),
                AgendaRow.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private record Created(String id) {
    }

    private record AgendaRow(String id, String startTime, String status, String customerName,
            String professionalName) {
    }
}
