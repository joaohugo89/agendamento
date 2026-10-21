package com.agendamento.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every lookup is scoped by businessId: a tenant must never be able to read another
 * tenant's rows, even when it knows the id.
 */
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    List<ServiceOffering> findByBusinessIdOrderByNameAsc(UUID businessId);

    List<ServiceOffering> findByBusinessIdAndActiveTrueOrderByNameAsc(UUID businessId);

    Optional<ServiceOffering> findByIdAndBusinessId(UUID id, UUID businessId);

    List<ServiceOffering> findByBusinessIdAndIdIn(UUID businessId, List<UUID> ids);
}
