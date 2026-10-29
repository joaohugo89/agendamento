package com.agendamento.catalog.dto;

import java.time.LocalTime;
import java.util.UUID;

import com.agendamento.catalog.ProfessionalHour;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ProfessionalHourResponse(
        UUID id,
        int dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

    public static ProfessionalHourResponse from(ProfessionalHour hour) {
        return new ProfessionalHourResponse(hour.getId(), hour.getDayOfWeek().getValue(),
                hour.getStartTime(), hour.getEndTime());
    }
}
