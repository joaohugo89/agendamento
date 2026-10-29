package com.agendamento.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure domain logic, with no access to the database or to the system clock, so it can be
 * tested in isolation.
 *
 * <p>Available slots = the periods the professional can be booked in, minus the
 * professional's active appointments, minus the professional's and the business' blocks,
 * minus anything already in the past, always taking the service duration into account: a slot
 * is only offered when the whole service fits before the period ends.
 *
 * <p>Those bookable periods are the business opening periods when the professional has no
 * schedule of their own, and the intersection of both schedules when they do — a professional
 * is never offered outside the business hours, nor outside their own.
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

        for (OpeningPeriod period : bookablePeriods(query)) {
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

    private static List<OpeningPeriod> bookablePeriods(AvailabilityQuery query) {
        return query.professionalPeriods()
                .map(periods -> intersect(query.openingPeriods(), periods))
                .orElseGet(query::openingPeriods);
    }

    /** Overlapping parts of the two schedules, so neither one is ever exceeded. */
    private static List<OpeningPeriod> intersect(List<OpeningPeriod> opening,
            List<OpeningPeriod> working) {
        List<OpeningPeriod> result = new ArrayList<>();
        for (OpeningPeriod open : opening) {
            for (OpeningPeriod shift : working) {
                LocalTime start = latest(open.start(), shift.start());
                LocalTime end = earliest(open.end(), shift.end());
                if (start.isBefore(end)) {
                    result.add(new OpeningPeriod(start, end));
                }
            }
        }
        result.sort(Comparator.comparing(OpeningPeriod::start));
        return List.copyOf(result);
    }

    private static LocalTime latest(LocalTime left, LocalTime right) {
        return left.isAfter(right) ? left : right;
    }

    private static LocalTime earliest(LocalTime left, LocalTime right) {
        return left.isBefore(right) ? left : right;
    }

    private static boolean isFree(TimeRange slot, List<TimeRange> busy) {
        return busy.stream().noneMatch(slot::overlaps);
    }
}
