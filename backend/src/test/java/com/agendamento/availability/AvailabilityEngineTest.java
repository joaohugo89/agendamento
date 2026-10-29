package com.agendamento.availability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvailabilityEngineTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);
    private static final Duration THIRTY_MINUTES = Duration.ofMinutes(30);
    private static final Duration SLOT_INTERVAL = Duration.ofMinutes(15);

    @Test
    @DisplayName("a closed day offers no slots")
    void closedDay() {
        AvailabilityQuery query = query(List.of(), List.of(), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query)).isEmpty();
    }

    @Test
    @DisplayName("slots are offered every interval inside a single period")
    void singlePeriod() {
        AvailabilityQuery query = query(List.of(period(9, 0, 10, 0)), List.of(), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(9, 0), at(9, 15), at(9, 30));
    }

    @Test
    @DisplayName("several periods on the same day are honoured and the lunch break is skipped")
    void severalPeriods() {
        AvailabilityQuery query = query(
                List.of(period(8, 0, 9, 0), period(13, 0, 14, 0)), List.of(), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(8, 0), at(8, 15), at(8, 30),
                        at(13, 0), at(13, 15), at(13, 30));
    }

    @Test
    @DisplayName("a service that does not fit before closing time is not offered")
    void serviceMustFitInThePeriod() {
        AvailabilityQuery query = new AvailabilityQuery(DATE, ZONE, Duration.ofMinutes(60),
                SLOT_INTERVAL, List.of(period(9, 0, 10, 0)), List.of(), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query)).containsExactly(at(9, 0));
    }

    @Test
    @DisplayName("an existing appointment removes every overlapping slot")
    void existingAppointmentBlocksSlots() {
        AvailabilityQuery query = query(List.of(period(9, 0, 11, 0)),
                List.of(new TimeRange(at(9, 30), at(10, 0))), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(9, 0), at(10, 0), at(10, 15), at(10, 30));
    }

    @Test
    @DisplayName("a cancelled appointment is not passed in as busy, so its slot is free again")
    void cancelledAppointmentFreesTheSlot() {
        AvailabilityQuery withAppointment = query(List.of(period(9, 0, 10, 0)),
                List.of(new TimeRange(at(9, 0), at(9, 30))), at(0, 0));
        AvailabilityQuery afterCancelling = query(List.of(period(9, 0, 10, 0)), List.of(), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(withAppointment)).containsExactly(at(9, 30));
        assertThat(AvailabilityEngine.availableSlots(afterCancelling))
                .containsExactly(at(9, 0), at(9, 15), at(9, 30));
    }

    @Test
    @DisplayName("a block covering the whole period leaves no slots")
    void blockCoveringThePeriod() {
        AvailabilityQuery query = query(List.of(period(9, 0, 10, 0)),
                List.of(new TimeRange(at(8, 0), at(12, 0))), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query)).isEmpty();
    }

    @Test
    @DisplayName("past slots of the current day are dropped")
    void pastSlotsAreDropped() {
        AvailabilityQuery query = query(List.of(period(9, 0, 10, 0)), List.of(), at(9, 20));

        assertThat(AvailabilityEngine.availableSlots(query)).containsExactly(at(9, 30));
    }

    @Test
    @DisplayName("a slot ending exactly when a busy range starts is still offered")
    void touchingRangesDoNotOverlap() {
        AvailabilityQuery query = query(List.of(period(9, 0, 10, 0)),
                List.of(new TimeRange(at(9, 30), at(10, 0))), at(0, 0));

        assertThat(AvailabilityEngine.availableSlots(query)).contains(at(9, 0));
    }

    @Test
    @DisplayName("opening times are resolved in the business timezone, not the server one")
    void opensInTheBusinessTimezone() {
        AvailabilityQuery query = query(List.of(period(9, 0, 9, 30)), List.of(), at(0, 0));

        List<Instant> slots = AvailabilityEngine.availableSlots(query);

        assertThat(slots).hasSize(1);
        assertThat(AvailabilityService.localTime(slots.getFirst(), ZONE)).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.getFirst()).isEqualTo(Instant.parse("2026-03-10T12:00:00Z"));
    }

    @Test
    @DisplayName("a professional schedule narrows the opening hours to the shift")
    void professionalShiftNarrowsTheDay() {
        AvailabilityQuery query = withShift(List.of(period(9, 0, 12, 0)), List.of(period(10, 0, 11, 0)));

        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(10, 0), at(10, 15), at(10, 30));
    }

    @Test
    @DisplayName("a professional never works outside the opening hours either")
    void professionalShiftIsClippedByTheOpeningHours() {
        AvailabilityQuery query = withShift(List.of(period(9, 0, 10, 0)), List.of(period(8, 0, 18, 0)));

        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(9, 0), at(9, 15), at(9, 30));
    }

    @Test
    @DisplayName("a day off for the professional offers no slots even with the business open")
    void professionalDayOff() {
        AvailabilityQuery query = withShift(List.of(period(9, 0, 18, 0)), List.of());

        assertThat(AvailabilityEngine.availableSlots(query)).isEmpty();
    }

    @Test
    @DisplayName("a shift with no overlap with the opening hours offers no slots")
    void shiftOutsideTheOpeningHours() {
        AvailabilityQuery query = withShift(List.of(period(9, 0, 12, 0)), List.of(period(13, 0, 18, 0)));

        assertThat(AvailabilityEngine.availableSlots(query)).isEmpty();
    }

    @Test
    @DisplayName("both schedules can have several periods, and only the overlaps are offered")
    void severalPeriodsOnBothSides() {
        AvailabilityQuery query = withShift(
                List.of(period(9, 0, 12, 0), period(13, 0, 18, 0)),
                List.of(period(11, 0, 14, 0), period(17, 0, 18, 0)));

        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(11, 0), at(11, 15), at(11, 30),
                        at(13, 0), at(13, 15), at(13, 30),
                        at(17, 0), at(17, 15), at(17, 30));
    }

    @Test
    @DisplayName("no schedule of their own means the professional works the whole opening hours")
    void noProfessionalScheduleFollowsTheBusiness() {
        AvailabilityQuery query = query(List.of(period(9, 0, 10, 0)), List.of(), at(0, 0));

        assertThat(query.professionalPeriods()).isEmpty();
        assertThat(AvailabilityEngine.availableSlots(query))
                .containsExactly(at(9, 0), at(9, 15), at(9, 30));
    }

    @Test
    @DisplayName("isSlotAvailable agrees with the offered list")
    void slotAvailability() {
        AvailabilityQuery query = query(List.of(period(9, 0, 10, 0)),
                List.of(new TimeRange(at(9, 30), at(10, 0))), at(0, 0));

        assertThat(AvailabilityEngine.isSlotAvailable(query, at(9, 0))).isTrue();
        assertThat(AvailabilityEngine.isSlotAvailable(query, at(9, 30))).isFalse();
        assertThat(AvailabilityEngine.isSlotAvailable(query, at(9, 7))).isFalse();
    }

    private AvailabilityQuery query(List<OpeningPeriod> periods, List<TimeRange> busy, Instant now) {
        return new AvailabilityQuery(DATE, ZONE, THIRTY_MINUTES, SLOT_INTERVAL, periods, busy, now);
    }

    private AvailabilityQuery withShift(List<OpeningPeriod> opening, List<OpeningPeriod> shift) {
        return new AvailabilityQuery(DATE, ZONE, THIRTY_MINUTES, SLOT_INTERVAL, opening,
                Optional.of(shift), List.of(), at(0, 0));
    }

    private OpeningPeriod period(int startHour, int startMinute, int endHour, int endMinute) {
        return new OpeningPeriod(LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    private static Instant at(int hour, int minute) {
        return ZonedDateTime.of(DATE, LocalTime.of(hour, minute), ZONE).toInstant();
    }
}
