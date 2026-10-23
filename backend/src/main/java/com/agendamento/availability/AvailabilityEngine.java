package com.agendamento.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure domain logic, with no access to the database or to the system clock, so it can be
 * tested in isolation.
 *
 * <p>Available slots = opening periods of the day, minus the professional's active
 * appointments, minus the professional's and the business' blocks, minus anything already in
 * the past, always taking the service duration into account: a slot is only offered when the
 * whole service fits before the period ends.
 */
public final class AvailabilityEngine {

    private AvailabilityEngine() {
    }

    public static List<Instant> availableSlots(AvailabilityQuery query) {
        List<Instant> slots = new ArrayList<>();
        Duration interval = query.slotInterval();
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("slotInterval must be positive");
        }

        for (OpeningPeriod period : query.openingPeriods()) {
            // The local opening time is resolved in the business zone, so the same 09:00 maps
            // to a different instant depending on the date and on daylight saving time.
            Instant periodStart = ZonedDateTime.of(query.date(), period.start(), query.zone()).toInstant();
            Instant periodEnd = ZonedDateTime.of(query.date(), period.end(), query.zone()).toInstant();

            for (Instant candidate = periodStart;
                    !candidate.plus(query.serviceDuration()).isAfter(periodEnd);
                    candidate = candidate.plus(interval)) {
                if (candidate.isBefore(query.now())) {
                    continue;
                }
                TimeRange slot = new TimeRange(candidate, candidate.plus(query.serviceDuration()));
                if (isFree(slot, query.busy())) {
                    slots.add(candidate);
                }
            }
        }
        return List.copyOf(slots);
    }

    public static boolean isSlotAvailable(AvailabilityQuery query, Instant start) {
        return availableSlots(query).contains(start);
    }

    private static boolean isFree(TimeRange slot, List<TimeRange> busy) {
        return busy.stream().noneMatch(slot::overlaps);
    }
}
