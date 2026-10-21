package com.agendamento.appointment;

import java.util.Set;

public enum AppointmentStatus {

    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Set<AppointmentStatus> FROM_CONFIRMED =
            Set.of(COMPLETED, CANCELLED, NO_SHOW);

    /** Appointments are born CONFIRMED and only move out of it, never back. */
    public boolean canTransitionTo(AppointmentStatus target) {
        return this == CONFIRMED && FROM_CONFIRMED.contains(target);
    }

    public boolean blocksSlot() {
        return this != CANCELLED;
    }
}
