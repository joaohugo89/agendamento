package com.agendamento.catalog.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A null professionalId blocks the whole business. */
public record BlockRequest(
        UUID professionalId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Size(max = 255) String reason) {
}
