package com.agendamento.catalog.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProfessionalRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull Boolean active,
        @NotNull List<UUID> serviceIds) {
}
