package com.agendamento.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Block, UUID> {

    List<Block> findByBusinessIdOrderByStartsAtDesc(UUID businessId);

    Optional<Block> findByIdAndBusinessId(UUID id, UUID businessId);

    /** Blocks overlapping the window, both business-wide ones and the professional's own. */
    @Query("select b from Block b where b.businessId = :businessId "
            + "and b.startsAt < :windowEnd and b.endsAt > :windowStart "
            + "and (b.professionalId is null or b.professionalId = :professionalId)")
    List<Block> findOverlapping(@Param("businessId") UUID businessId,
            @Param("professionalId") UUID professionalId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);
}
