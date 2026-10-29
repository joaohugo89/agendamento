package com.agendamento.catalog;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One working period of one weekday for a single professional, expressed in the business
 * timezone. Several rows for the same weekday mean several periods.
 *
 * <p>A professional with no rows at all follows the business hours; this is the default and
 * what every professional does until a schedule of their own is saved.
 */
@Entity
@Table(name = "professional_hours")
public class ProfessionalHour {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    /** 1 = Monday .. 7 = Sunday, matching {@link DayOfWeek#getValue()}. */
    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected ProfessionalHour() {
    }

    public ProfessionalHour(UUID businessId, UUID professionalId, DayOfWeek dayOfWeek,
            LocalTime startTime, LocalTime endTime) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.professionalId = professionalId;
        this.dayOfWeek = (short) dayOfWeek.getValue();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getProfessionalId() {
        return professionalId;
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.of(dayOfWeek);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
