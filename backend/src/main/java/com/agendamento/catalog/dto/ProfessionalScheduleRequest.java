package com.agendamento.catalog.dto;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Replaces the whole schedule of one professional. With {@code followsBusinessHours} the
 * entries are ignored and the professional goes back to working the business hours; without
 * it, at least one entry is required and a weekday with no entry means the professional does
 * not work that day.
 */
public record ProfessionalScheduleRequest(
        @NotNull Boolean followsBusinessHours,
        @NotNull @Valid List<Entry> entries) {

    public record Entry(
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
    }
}
