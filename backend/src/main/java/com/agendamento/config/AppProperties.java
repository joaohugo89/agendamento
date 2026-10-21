package com.agendamento.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Booking booking, Cors cors) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Booking(int slotIntervalMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
