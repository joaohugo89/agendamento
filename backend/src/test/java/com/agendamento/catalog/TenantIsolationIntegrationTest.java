package com.agendamento.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.agendamento.catalog.dto.ServiceRequest;
import com.agendamento.support.IntegrationTest;
import com.agendamento.support.TenantFixture;

/** Tenant A must not see or touch anything of tenant B, even knowing the exact ids. */
class TenantIsolationIntegrationTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantFixture tenantFixture;

    @Test
    void readingAnotherTenantsServiceReturnsNotFound() {
        TenantFixture.Tenant tenantA = tenantFixture.create("tenant-a");
        TenantFixture.Tenant tenantB = tenantFixture.create("tenant-b");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/services/" + tenantB.serviceId(), HttpMethod.PUT,
                new HttpEntity<>(new ServiceRequest("Invadido", null, 1000, 30, true),
                        authHeaders(tenantA.token())),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletingAnotherTenantsProfessionalReturnsNotFound() {
        TenantFixture.Tenant tenantA = tenantFixture.create("tenant-c");
        TenantFixture.Tenant tenantB = tenantFixture.create("tenant-d");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/professionals/" + tenantB.professionalId(), HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(tenantA.token())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listingServicesOnlyReturnsOwnRows() {
        TenantFixture.Tenant tenantA = tenantFixture.create("tenant-e");
        tenantFixture.create("tenant-f");

        ResponseEntity<ServiceSummary[]> response = restTemplate.exchange(
                "/api/services", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tenantA.token())), ServiceSummary[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].id()).isEqualTo(tenantA.serviceId().toString());
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private record ServiceSummary(String id, String name) {
    }
}
