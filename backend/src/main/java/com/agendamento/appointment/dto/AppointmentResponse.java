package com.agendamento.appointment.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import com.agendamento.appointment.Appointment;
import com.agendamento.appointment.AppointmentStatus;
import com.agendamento.availability.AvailabilityService;
import com.fasterxml.jackson.annotation.JsonFormat;

public record AppointmentResponse(
        UUID id,
        Instant startsAt,
        Instant endsAt,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        AppointmentStatus status,
        String customerName,
        String customerWhatsapp,
        String notes,
        UUID serviceId,
        String serviceName,
        UUID professionalId,
        String professionalName,
        int priceCents) {

    public static AppointmentResponse from(Appointment appointment, String serviceName,
            String professionalName, ZoneId zone) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                AvailabilityService.localTime(appointment.getStartsAt(), zone),
                AvailabilityService.localTime(appointment.getEndsAt(), zone),
                appointment.getStatus(),
                appointment.getCustomerName(),
                appointment.getCustomerWhatsapp(),
                appointment.getNotes(),
                appointment.getServiceId(),
                serviceName,
                appointment.getProfessionalId(),
                professionalName,
                appointment.getPriceCents());
    }
}
