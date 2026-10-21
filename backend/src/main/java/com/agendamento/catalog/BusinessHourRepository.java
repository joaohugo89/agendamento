package com.agendamento.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessHourRepository extends JpaRepository<BusinessHour, UUID> {

    List<BusinessHour> findByBusinessIdOrderByDayOfWeekAscStartTimeAsc(UUID businessId);

    void deleteByBusinessId(UUID businessId);
}
