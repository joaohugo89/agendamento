package com.agendamento.catalog.dto;

import java.time.Instant;
import java.util.UUID;

import com.agendamento.catalog.Block;

public record BlockResponse(
        UUID id,
        UUID professionalId,
        String professionalName,
        Instant startsAt,
        Instant endsAt,
        String reason) {

    public static BlockResponse from(Block block, String professionalName) {
        return new BlockResponse(block.getId(), block.getProfessionalId(), professionalName,
                block.getStartsAt(), block.getEndsAt(), block.getReason());
    }
}
