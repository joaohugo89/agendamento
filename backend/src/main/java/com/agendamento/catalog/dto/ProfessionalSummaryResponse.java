package com.agendamento.catalog.dto;

import java.util.List;
import java.util.UUID;

import com.agendamento.catalog.Professional;

/**
 * A professional as the public page sees them.
 *
 * @param workingDays weekdays the professional works, 1 = Monday .. 7 = Sunday; empty means
 *                    they follow the business hours, so the calendar only needs the opening
 *                    days to decide what to enable
 */
public record ProfessionalSummaryResponse(UUID id, String name, List<Integer> workingDays) {

    public static ProfessionalSummaryResponse from(Professional professional,
            List<Integer> workingDays) {
        return new ProfessionalSummaryResponse(professional.getId(), professional.getName(),
                workingDays);
    }
}
