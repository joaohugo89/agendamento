package com.agendamento.availability;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.appointment.AppointmentRepository;
import com.agendamento.business.Business;
import com.agendamento.catalog.BusinessHourRepository;
import com.agendamento.catalog.BlockRepository;
import com.agendamento.catalog.ServiceOffering;
import com.agendamento.config.AppProperties;

/** Loads what the engine needs and hands it over; all the rules live in the engine. */
@Service
public class AvailabilityService {

    private final BusinessHourRepository businessHourRepository;
    private final BlockRepository blockRepository;
    private final AppointmentRepository appointmentRepository;
    private final Duration slotInterval;
    private final Clock clock;

    public AvailabilityService(BusinessHourRepository businessHourRepository,
            BlockRepository blockRepository, AppointmentRepository appointmentRepository,
            AppProperties properties, Clock clock) {
        this.businessHourRepository = businessHourRepository;
        this.blockRepository = blockRepository;
        this.appointmentRepository = appointmentRepository;
        this.slotInterval = Duration.ofMinutes(properties.booking().slotIntervalMinutes());
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Instant> availableSlots(Business business, ServiceOffering service,
            UUID professionalId, LocalDate date) {
        return AvailabilityEngine.availableSlots(buildQuery(business, service, professionalId, date));
    }

    @Transactional(readOnly = true)
    public boolean isSlotAvailable(Business business, ServiceOffering service, UUID professionalId,
            Instant start) {
        LocalDate date = ZonedDateTime.ofInstant(start, business.zoneId()).toLocalDate();
        return AvailabilityEngine.isSlotAvailable(buildQuery(business, service, professionalId, date), start);
    }

    private AvailabilityQuery buildQuery(Business business, ServiceOffering service,
            UUID professionalId, LocalDate date) {
        ZoneId zone = business.zoneId();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        List<OpeningPeriod> openingPeriods = businessHourRepository
                .findByBusinessIdOrderByDayOfWeekAscStartTimeAsc(business.getId()).stream()
                .filter(hour -> hour.getDayOfWeek() == date.getDayOfWeek())
                .map(hour -> new OpeningPeriod(hour.getStartTime(), hour.getEndTime()))
                .toList();

        List<TimeRange> busy = new ArrayList<>();
        appointmentRepository
                .findActiveOverlapping(business.getId(), professionalId, dayStart, dayEnd)
                .forEach(appointment -> busy.add(
                        new TimeRange(appointment.getStartsAt(), appointment.getEndsAt())));
        blockRepository.findOverlapping(business.getId(), professionalId, dayStart, dayEnd)
                .forEach(block -> busy.add(new TimeRange(block.getStartsAt(), block.getEndsAt())));

        return new AvailabilityQuery(date, zone, service.duration(), slotInterval,
                openingPeriods, List.copyOf(busy), clock.instant());
    }

    /** Local time of an instant in the business zone, for display and for API payloads. */
    public static LocalTime localTime(Instant instant, ZoneId zone) {
        return ZonedDateTime.ofInstant(instant, zone).toLocalTime();
    }
}
