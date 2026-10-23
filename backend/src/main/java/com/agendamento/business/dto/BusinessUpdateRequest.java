package com.agendamento.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 255) String address,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 60) String timezone) {
}
