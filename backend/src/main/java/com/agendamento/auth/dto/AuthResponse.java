package com.agendamento.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        long expiresInSeconds,
        UUID userId,
        String userName,
        UUID businessId,
        String businessName,
        String businessSlug) {
}
