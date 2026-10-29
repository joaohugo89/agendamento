package com.agendamento.catalog;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import com.agendamento.common.BusinessRuleException;

/**
 * Rules shared by the business schedule and by each professional schedule: a period ends
 * after it starts, and two periods of the same weekday never overlap.
 */
final class SchedulePeriods {

    record Period(int dayOfWeek, LocalTime start, LocalTime end) {
    }

    private SchedulePeriods() {
    }

    static void validate(List<Period> periods) {
        for (Period period : periods) {
            if (!period.start().isBefore(period.end())) {
                throw new BusinessRuleException("O inicio do periodo deve ser antes do fim.");
            }
        }
        for (int day = 1; day <= 7; day++) {
            int weekday = day;
            List<Period> ofDay = periods.stream()
                    .filter(period -> period.dayOfWeek() == weekday)
                    .sorted(Comparator.comparing(Period::start))
                    .toList();
            for (int i = 1; i < ofDay.size(); i++) {
                if (ofDay.get(i).start().isBefore(ofDay.get(i - 1).end())) {
                    throw new BusinessRuleException("Os periodos de um mesmo dia nao podem se sobrepor.");
                }
            }
        }
    }
}
