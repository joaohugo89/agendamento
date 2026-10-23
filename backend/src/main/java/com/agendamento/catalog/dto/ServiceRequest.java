package com.agendamento.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull @Min(0) @Max(10_000_000) Integer priceCents,
        @NotNull @Min(5) @Max(600) Integer durationMinutes,
        @NotNull Boolean active) {
}
