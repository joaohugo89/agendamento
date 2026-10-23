package com.agendamento.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.agendamento.auth.dto.AuthResponse;
import com.agendamento.auth.dto.LoginRequest;
import com.agendamento.auth.dto.RegisterRequest;
import com.agendamento.support.IntegrationTest;

class AuthenticationIntegrationTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/services",
        "/api/professionals",
        "/api/business",
        "/api/business-hours",
        "/api/blocks"
    })
    void protectedRoutesRejectRequestsWithoutAToken(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void agendaRejectsRequestsWithoutAToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/appointments?date=" + LocalDate.now(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedRoutesRejectAMalformedToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-jwt");

        ResponseEntity<String> response = restTemplate.exchange("/api/services", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void signingUpCreatesTheBusinessAndReturnsAUsableToken() {
        ResponseEntity<AuthResponse> registered = restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("Maria", "Studio Maria Unhas", "maria@example.com", "senha-forte-1"),
                AuthResponse.class);

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse body = registered.getBody();
        assertThat(body).isNotNull();
        assertThat(body.businessSlug()).isEqualTo("studio-maria-unhas");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(body.token());
        ResponseEntity<String> services = restTemplate.exchange("/api/services", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(services.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void loginWithTheWrongPasswordIsRejected() {
        restTemplate.postForEntity("/api/auth/register",
                new RegisterRequest("Carlos", "Barbearia Carlos", "carlos@example.com", "senha-forte-1"),
                AuthResponse.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("carlos@example.com", "senha-errada"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
