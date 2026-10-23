package com.agendamento.catalog;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.catalog.dto.BusinessHourResponse;
import com.agendamento.catalog.dto.BusinessHoursRequest;
import com.agendamento.common.BusinessRuleException;

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
        validate(request.entries());
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

    private void validate(List<BusinessHoursRequest.Entry> entries) {
        for (BusinessHoursRequest.Entry entry : entries) {
            if (!entry.startTime().isBefore(entry.endTime())) {
                throw new BusinessRuleException("O inicio do periodo deve ser antes do fim.");
            }
        }
        for (int day = 1; day <= 7; day++) {
            int weekday = day;
            List<BusinessHoursRequest.Entry> ofDay = new ArrayList<>(entries.stream()
                    .filter(entry -> entry.dayOfWeek() == weekday)
                    .sorted(Comparator.comparing(BusinessHoursRequest.Entry::startTime))
                    .toList());
            for (int i = 1; i < ofDay.size(); i++) {
                if (ofDay.get(i).startTime().isBefore(ofDay.get(i - 1).endTime())) {
                    throw new BusinessRuleException("Os periodos de um mesmo dia nao podem se sobrepor.");
                }
            }
        }
    }
}
