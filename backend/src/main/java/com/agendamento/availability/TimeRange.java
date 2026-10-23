package com.agendamento.availability;

import java.time.Instant;

/** Half-open interval [start, end): touching ranges do not overlap. */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
