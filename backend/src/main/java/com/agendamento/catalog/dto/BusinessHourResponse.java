package com.agendamento.catalog.dto;

import java.time.LocalTime;
import java.util.UUID;

import com.agendamento.catalog.BusinessHour;
import com.fasterxml.jackson.annotation.JsonFormat;

public record BusinessHourResponse(
        UUID id,
        int dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

    public static BusinessHourResponse from(BusinessHour hour) {
        return new BusinessHourResponse(hour.getId(), hour.getDayOfWeek().getValue(),
                hour.getStartTime(), hour.getEndTime());
    }
}
