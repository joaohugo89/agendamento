package com.agendamento.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    @Query("select distinct p from Professional p left join fetch p.services "
            + "where p.businessId = :businessId order by p.name asc")
    List<Professional> findAllWithServices(@Param("businessId") UUID businessId);

    @Query("select distinct p from Professional p left join fetch p.services "
            + "where p.id = :id and p.businessId = :businessId")
    Optional<Professional> findByIdAndBusinessIdWithServices(@Param("id") UUID id,
            @Param("businessId") UUID businessId);

    Optional<Professional> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("select p from Professional p join p.services s "
            + "where p.businessId = :businessId and s.id = :serviceId and p.active = true "
            + "order by p.name asc")
    List<Professional> findActiveByService(@Param("businessId") UUID businessId,
            @Param("serviceId") UUID serviceId);

    @Query("select count(p) > 0 from Professional p join p.services s "
            + "where p.id = :professionalId and s.id = :serviceId")
    boolean performsService(@Param("professionalId") UUID professionalId,
            @Param("serviceId") UUID serviceId);
}
