package com.agendamento.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Everything {@link AvailabilityEngine} needs, already loaded by the caller.
 *
 * @param date                the day being offered, in the business calendar
 * @param zone                the business timezone, never the server one
 * @param serviceDuration     how long the chosen service takes
 * @param slotInterval        gap between two offered start times
 * @param openingPeriods      the periods the business is open on that weekday (empty means closed)
 * @param professionalPeriods the periods the professional works on that weekday; empty optional
 *                            means the professional follows the business hours, while a present
 *                            but empty list means the professional does not work that day
 * @param busy                appointments and blocks that make time unavailable
 * @param now                 current instant, so past slots can be dropped
 */
public record AvailabilityQuery(
        LocalDate date,
        ZoneId zone,
        Duration serviceDuration,
        Duration slotInterval,
        List<OpeningPeriod> openingPeriods,
        Optional<List<OpeningPeriod>> professionalPeriods,
        List<TimeRange> busy,
        Instant now) {

    /** A professional with no schedule of their own: they work the business hours. */
    public AvailabilityQuery(LocalDate date, ZoneId zone, Duration serviceDuration,
            Duration slotInterval, List<OpeningPeriod> openingPeriods, List<TimeRange> busy,
            Instant now) {
        this(date, zone, serviceDuration, slotInterval, openingPeriods, Optional.empty(), busy, now);
    }
}
