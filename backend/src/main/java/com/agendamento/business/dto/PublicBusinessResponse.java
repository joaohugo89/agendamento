package com.agendamento.business.dto;

import java.util.List;

import com.agendamento.business.Business;
import com.agendamento.catalog.dto.BusinessHourResponse;
import com.agendamento.catalog.dto.ServiceResponse;

/** What the public booking page needs. Deliberately leaves out ids and owner data. */
public record PublicBusinessResponse(
        String name,
        String slug,
        String description,
        String address,
        String phone,
        String timezone,
        List<ServiceResponse> services,
        List<BusinessHourResponse> openingHours) {

    public static PublicBusinessResponse from(Business business, List<ServiceResponse> services,
            List<BusinessHourResponse> openingHours) {
        return new PublicBusinessResponse(business.getName(), business.getSlug(),
                business.getDescription(), business.getAddress(), business.getPhone(),
                business.getTimezone(), services, openingHours);
    }
}
