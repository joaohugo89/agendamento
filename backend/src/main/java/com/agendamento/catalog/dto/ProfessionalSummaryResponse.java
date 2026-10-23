package com.agendamento.catalog.dto;

import java.util.UUID;

import com.agendamento.catalog.Professional;

public record ProfessionalSummaryResponse(UUID id, String name) {

    public static ProfessionalSummaryResponse from(Professional professional) {
        return new ProfessionalSummaryResponse(professional.getId(), professional.getName());
    }
}
