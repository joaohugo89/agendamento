package com.agendamento.appointment.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import com.agendamento.availability.AvailabilityService;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A slot carries both the exact instant, used when booking, and its local time in the
 * business timezone, so the browser never has to guess the timezone to label it.
 */
public record SlotResponse(
        Instant startsAt,
        @JsonFormat(pattern = "HH:mm") LocalTime time) {

    public static SlotResponse from(Instant startsAt, ZoneId zone) {
        return new SlotResponse(startsAt, AvailabilityService.localTime(startsAt, zone));
    }
}
