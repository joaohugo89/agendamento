package com.agendamento.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import com.agendamento.appointment.dto.CreateAppointmentRequest;
import com.agendamento.support.IntegrationTest;
import com.agendamento.support.TenantFixture;

/**
 * Several customers trying the very same slot at the very same time: only one may win.
 * The guarantee comes from the exclusion constraint, so this needs a real PostgreSQL.
 */
class DoubleBookingIntegrationTest extends IntegrationTest {

    private static final int CONCURRENT_REQUESTS = 8;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantFixture tenantFixture;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void onlyOneOfManyConcurrentRequestsForTheSameSlotSucceeds() throws Exception {
        TenantFixture.Tenant tenant = tenantFixture.create("concorrencia");
        Instant slot = nextDayAt(10, 0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(tenant.serviceId(),
                tenant.professionalId(), slot, "Cliente", "11999999999", null);
        String url = "/api/public/businesses/" + tenant.slug() + "/appointments";

        CountDownLatch startLine = new CountDownLatch(1);
        List<Callable<HttpStatusCode>> calls = java.util.stream.IntStream.range(0, CONCURRENT_REQUESTS)
                .<Callable<HttpStatusCode>>mapToObj(index -> () -> {
                    startLine.await();
                    ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                    return response.getStatusCode();
                })
                .toList();

        List<HttpStatusCode> statuses;
        try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)) {
            List<Future<HttpStatusCode>> futures = calls.stream().map(executor::submit).toList();
            startLine.countDown();
            statuses = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }).toList();
        }

        assertThat(statuses).filteredOn(status -> status.isSameCodeAs(HttpStatus.CREATED)).hasSize(1);
        assertThat(statuses).filteredOn(status -> status.isSameCodeAs(HttpStatus.CONFLICT))
                .hasSize(CONCURRENT_REQUESTS - 1);
        assertThat(appointmentRepository.findActiveOverlapping(tenant.businessId(),
                tenant.professionalId(), slot, slot.plusSeconds(1))).hasSize(1);
    }

    private Instant nextDayAt(int hour, int minute) {
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        LocalDate tomorrow = LocalDate.now(zone).plusDays(1);
        return ZonedDateTime.of(tomorrow, LocalTime.of(hour, minute), zone).toInstant();
    }
}
