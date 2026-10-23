package com.agendamento.appointment.dto;

import java.time.Instant;
import java.util.UUID;

/** Returned to the customer right after booking; it drives the success page and the .ics file. */
public record BookingConfirmationResponse(
        UUID id,
        String businessName,
        String businessAddress,
        String serviceName,
        String professionalName,
        String customerName,
        Instant startsAt,
        Instant endsAt,
        int priceCents,
        String timezone) {
}
