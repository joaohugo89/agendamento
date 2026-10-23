package com.agendamento.appointment.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAppointmentRequest(
        @NotNull UUID serviceId,
        @NotNull UUID professionalId,
        @NotNull Instant startsAt,
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Size(min = 10, max = 20)
        @Pattern(regexp = "[0-9()+\\-\\s]+", message = "informe apenas numeros")
        String customerWhatsapp,
        @Size(max = 500) String notes) {
}
