package com.agendamento.business;

import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.business.dto.BusinessResponse;
import com.agendamento.business.dto.BusinessUpdateRequest;
import com.agendamento.common.BusinessRuleException;
import com.agendamento.common.NotFoundException;

@Service
public class BusinessService {

    private final BusinessRepository repository;

    public BusinessService(BusinessRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Business require(UUID businessId) {
        return repository.findById(businessId)
                .orElseThrow(() -> new NotFoundException("Estabelecimento nao encontrado."));
    }

    @Transactional(readOnly = true)
    public Business requireBySlug(String slug) {
        return repository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Estabelecimento nao encontrado."));
    }

    @Transactional(readOnly = true)
    public BusinessResponse get(UUID businessId) {
        return BusinessResponse.from(require(businessId));
    }

    /** The slug is not editable in this version: public links already in the wild must keep working. */
    @Transactional
    public BusinessResponse update(UUID businessId, BusinessUpdateRequest request) {
        Business business = require(businessId);
        business.setName(request.name());
        business.setDescription(request.description());
        business.setAddress(request.address());
        business.setPhone(request.phone());
        business.setTimezone(requireValidTimezone(request.timezone()));
        return BusinessResponse.from(repository.save(business));
    }

    private String requireValidTimezone(String timezone) {
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new BusinessRuleException("Fuso horario invalido.");
        }
        return timezone;
    }
}
