package com.agendamento.appointment.dto;

import com.agendamento.appointment.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull AppointmentStatus status) {
}
