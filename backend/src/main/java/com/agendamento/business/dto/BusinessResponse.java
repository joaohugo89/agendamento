package com.agendamento.business.dto;

import java.util.UUID;

import com.agendamento.business.Business;

public record BusinessResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String address,
        String phone,
        String timezone) {

    public static BusinessResponse from(Business business) {
        return new BusinessResponse(business.getId(), business.getName(), business.getSlug(),
                business.getDescription(), business.getAddress(), business.getPhone(),
                business.getTimezone());
    }
}
