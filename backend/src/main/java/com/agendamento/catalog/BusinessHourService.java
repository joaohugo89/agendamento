package com.agendamento.catalog;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.catalog.dto.BusinessHourResponse;
import com.agendamento.catalog.dto.BusinessHoursRequest;

@Service
public class BusinessHourService {

    private final BusinessHourRepository repository;

    public BusinessHourService(BusinessHourRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BusinessHourResponse> list(UUID businessId) {
        return repository.findByBusinessIdOrderByDayOfWeekAscStartTimeAsc(businessId).stream()
                .map(BusinessHourResponse::from)
                .toList();
    }

    /** The schedule is replaced as a whole: it is a small, always fully edited set of rows. */
    @Transactional
    public List<BusinessHourResponse> replace(UUID businessId, BusinessHoursRequest request) {
        SchedulePeriods.validate(request.entries().stream()
                .map(entry -> new SchedulePeriods.Period(entry.dayOfWeek(), entry.startTime(),
                        entry.endTime()))
                .toList());
        repository.deleteByBusinessId(businessId);
        repository.flush();
        List<BusinessHour> hours = request.entries().stream()
                .map(entry -> new BusinessHour(businessId, DayOfWeek.of(entry.dayOfWeek()),
                        entry.startTime(), entry.endTime()))
                .toList();
        return repository.saveAll(hours).stream()
                .sorted(Comparator.comparing(BusinessHour::getDayOfWeek)
                        .thenComparing(BusinessHour::getStartTime))
                .map(BusinessHourResponse::from)
                .toList();
    }
}
