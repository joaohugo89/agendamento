package com.agendamento.catalog.dto;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Replaces the whole opening schedule. A weekday with no entry is closed;
 * several entries for the same weekday are several periods.
 */
public record BusinessHoursRequest(@NotNull @Valid List<Entry> entries) {

    public record Entry(
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
    }
}
