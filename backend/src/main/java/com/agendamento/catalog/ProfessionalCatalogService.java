package com.agendamento.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.catalog.dto.ProfessionalRequest;
import com.agendamento.catalog.dto.ProfessionalResponse;
import com.agendamento.common.BusinessRuleException;
import com.agendamento.common.NotFoundException;

@Service
public class ProfessionalCatalogService {

    private final ProfessionalRepository repository;
    private final ServiceOfferingRepository serviceRepository;

    public ProfessionalCatalogService(ProfessionalRepository repository,
            ServiceOfferingRepository serviceRepository) {
        this.repository = repository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> list(UUID businessId) {
        return repository.findAllWithServices(businessId).stream()
                .map(ProfessionalResponse::from)
                .toList();
    }

    @Transactional
    public ProfessionalResponse create(UUID businessId, ProfessionalRequest request) {
        Professional professional = new Professional(businessId, request.name(), request.active());
        professional.update(request.name(), request.active(), resolveServices(businessId, request.serviceIds()));
        return ProfessionalResponse.from(repository.save(professional));
    }

    @Transactional
    public ProfessionalResponse update(UUID businessId, UUID id, ProfessionalRequest request) {
        Professional professional = repository.findByIdAndBusinessIdWithServices(id, businessId)
                .orElseThrow(() -> new NotFoundException("Profissional nao encontrado."));
        professional.update(request.name(), request.active(), resolveServices(businessId, request.serviceIds()));
        return ProfessionalResponse.from(repository.save(professional));
    }

    @Transactional
    public void delete(UUID businessId, UUID id) {
        Professional professional = repository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Profissional nao encontrado."));
        repository.delete(professional);
    }

    /** Services are resolved inside the tenant, so ids from another business simply do not exist. */
    private Set<ServiceOffering> resolveServices(UUID businessId, List<UUID> serviceIds) {
        if (serviceIds.isEmpty()) {
            return Set.of();
        }
        List<ServiceOffering> services = serviceRepository.findByBusinessIdAndIdIn(businessId, serviceIds);
        if (services.size() != new HashSet<>(serviceIds).size()) {
            throw new BusinessRuleException("Um ou mais servicos informados nao existem.");
        }
        return new HashSet<>(services);
    }
}
