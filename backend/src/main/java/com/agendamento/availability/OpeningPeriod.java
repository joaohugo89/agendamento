package com.agendamento.availability;

import java.time.LocalTime;

/** An opening period expressed in the business local time, such as 08:00-12:00. */
public record OpeningPeriod(LocalTime start, LocalTime end) {

    public OpeningPeriod {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }
}
