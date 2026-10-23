package com.agendamento.catalog.dto;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.agendamento.catalog.Professional;

public record ProfessionalResponse(
        UUID id,
        String name,
        boolean active,
        List<ServiceSummaryResponse> services) {

    public static ProfessionalResponse from(Professional professional) {
        List<ServiceSummaryResponse> services = professional.getServices().stream()
                .map(service -> new ServiceSummaryResponse(service.getId(), service.getName()))
                .sorted(Comparator.comparing(ServiceSummaryResponse::name))
                .toList();
        return new ProfessionalResponse(professional.getId(), professional.getName(),
                professional.isActive(), services);
    }
}
