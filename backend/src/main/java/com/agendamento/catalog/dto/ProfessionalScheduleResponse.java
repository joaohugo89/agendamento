package com.agendamento.catalog.dto;

import java.util.List;

/**
 * When {@code followsBusinessHours} is true the professional is available whenever the business
 * is open and {@code entries} is empty. Otherwise the entries are the only times the
 * professional works, and they still never go beyond the business hours.
 */
public record ProfessionalScheduleResponse(
        boolean followsBusinessHours,
        List<ProfessionalHourResponse> entries) {
}
