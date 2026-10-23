package com.agendamento.catalog.dto;

import java.util.UUID;

import com.agendamento.catalog.ServiceOffering;

public record ServiceResponse(
        UUID id,
        String name,
        String description,
        int priceCents,
        int durationMinutes,
        boolean active) {

    public static ServiceResponse from(ServiceOffering service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPriceCents(),
                service.getDurationMinutes(),
                service.isActive());
    }
}
