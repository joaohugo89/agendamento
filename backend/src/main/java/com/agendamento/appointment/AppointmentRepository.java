package com.agendamento.appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("select a from Appointment a where a.businessId = :businessId "
            + "and a.startsAt >= :dayStart and a.startsAt < :dayEnd "
            + "order by a.startsAt asc")
    List<Appointment> findForDay(@Param("businessId") UUID businessId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);

    @Query("select a from Appointment a where a.businessId = :businessId "
            + "and a.professionalId = :professionalId "
            + "and a.startsAt >= :dayStart and a.startsAt < :dayEnd "
            + "order by a.startsAt asc")
    List<Appointment> findForDayAndProfessional(@Param("businessId") UUID businessId,
            @Param("professionalId") UUID professionalId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);

    @Query("select a from Appointment a where a.businessId = :businessId "
            + "and a.professionalId = :professionalId "
            + "and a.status <> com.agendamento.appointment.AppointmentStatus.CANCELLED "
            + "and a.startsAt < :windowEnd and a.endsAt > :windowStart")
    List<Appointment> findActiveOverlapping(@Param("businessId") UUID businessId,
            @Param("professionalId") UUID professionalId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);
}
