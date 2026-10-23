package com.agendamento.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Everything {@link AvailabilityEngine} needs, already loaded by the caller.
 *
 * @param date            the day being offered, in the business calendar
 * @param zone            the business timezone, never the server one
 * @param serviceDuration how long the chosen service takes
 * @param slotInterval    gap between two offered start times
 * @param openingPeriods  the periods the business is open on that weekday (empty means closed)
 * @param busy            appointments and blocks that make time unavailable
 * @param now             current instant, so past slots can be dropped
 */
public record AvailabilityQuery(
        LocalDate date,
        ZoneId zone,
        Duration serviceDuration,
        Duration slotInterval,
        List<OpeningPeriod> openingPeriods,
        List<TimeRange> busy,
        Instant now) {
}
