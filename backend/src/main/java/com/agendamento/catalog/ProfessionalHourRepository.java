package com.agendamento.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalHourRepository extends JpaRepository<ProfessionalHour, UUID> {

    List<ProfessionalHour> findByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(UUID professionalId);

    List<ProfessionalHour> findByBusinessIdOrderByDayOfWeekAscStartTimeAsc(UUID businessId);

    void deleteByProfessionalId(UUID professionalId);
}
