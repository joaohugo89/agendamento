package com.agendamento.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.catalog.dto.ServiceRequest;
import com.agendamento.catalog.dto.ServiceResponse;
import com.agendamento.common.NotFoundException;

@Service
public class ServiceCatalogService {

    private final ServiceOfferingRepository repository;

    public ServiceCatalogService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> list(UUID businessId) {
        return repository.findByBusinessIdOrderByNameAsc(businessId).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional
    public ServiceResponse create(UUID businessId, ServiceRequest request) {
        ServiceOffering service = new ServiceOffering(businessId, request.name(), request.description(),
                request.priceCents(), request.durationMinutes(), request.active());
        return ServiceResponse.from(repository.save(service));
    }

    @Transactional
    public ServiceResponse update(UUID businessId, UUID id, ServiceRequest request) {
        ServiceOffering service = require(businessId, id);
        service.update(request.name(), request.description(), request.priceCents(),
                request.durationMinutes(), request.active());
        return ServiceResponse.from(repository.save(service));
    }

    @Transactional
    public void delete(UUID businessId, UUID id) {
        repository.delete(require(businessId, id));
    }

    private ServiceOffering require(UUID businessId, UUID id) {
        return repository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Servico nao encontrado."));
    }
}
