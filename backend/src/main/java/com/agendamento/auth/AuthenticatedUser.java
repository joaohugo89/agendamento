package com.agendamento.auth;

import java.util.UUID;

/**
 * Principal placed in the security context by {@link JwtAuthenticationFilter}.
 * The {@code businessId} always comes from the token, never from the request body or path,
 * which is what keeps tenants isolated.
 */
public record AuthenticatedUser(UUID userId, UUID businessId, String email) {
}
