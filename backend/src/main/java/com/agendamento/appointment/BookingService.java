package com.agendamento.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.appointment.dto.BookingConfirmationResponse;
import com.agendamento.appointment.dto.CreateAppointmentRequest;
import com.agendamento.appointment.dto.SlotResponse;
import com.agendamento.availability.AvailabilityService;
import com.agendamento.business.Business;
import com.agendamento.business.BusinessService;
import com.agendamento.business.dto.PublicBusinessResponse;
import com.agendamento.catalog.Professional;
import com.agendamento.catalog.ProfessionalHour;
import com.agendamento.catalog.ProfessionalHourRepository;
import com.agendamento.catalog.ProfessionalRepository;
import com.agendamento.catalog.ServiceOffering;
import com.agendamento.catalog.ServiceOfferingRepository;
import com.agendamento.catalog.BusinessHourRepository;
import com.agendamento.catalog.dto.BusinessHourResponse;
import com.agendamento.catalog.dto.ProfessionalSummaryResponse;
import com.agendamento.catalog.dto.ServiceResponse;
import com.agendamento.common.ConflictException;
import com.agendamento.common.NotFoundException;

/** The public side of booking: resolve the business by slug, offer slots, create appointments. */
@Service
public class BookingService {

    private final BusinessService businessService;
    private final ServiceOfferingRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final ProfessionalHourRepository professionalHourRepository;
    private final BusinessHourRepository businessHourRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final BookingLock bookingLock;

    public BookingService(BusinessService businessService, ServiceOfferingRepository serviceRepository,
            ProfessionalRepository professionalRepository,
            ProfessionalHourRepository professionalHourRepository,
            BusinessHourRepository businessHourRepository,
            AppointmentRepository appointmentRepository, AvailabilityService availabilityService,
            BookingLock bookingLock) {
        this.businessService = businessService;
        this.serviceRepository = serviceRepository;
        this.professionalRepository = professionalRepository;
        this.professionalHourRepository = professionalHourRepository;
        this.businessHourRepository = businessHourRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.bookingLock = bookingLock;
    }

    @Transactional(readOnly = true)
    public PublicBusinessResponse publicPage(String slug) {
        Business business = businessService.requireBySlug(slug);
        List<ServiceResponse> services = serviceRepository
                .findByBusinessIdAndActiveTrueOrderByNameAsc(business.getId()).stream()
                .map(ServiceResponse::from)
                .toList();
        List<BusinessHourResponse> openingHours = businessHourRepository
                .findByBusinessIdOrderByDayOfWeekAscStartTimeAsc(business.getId()).stream()
                .map(BusinessHourResponse::from)
                .toList();
        return PublicBusinessResponse.from(business, services, openingHours);
    }

    /** Only active professionals that actually perform the chosen service. */
    @Transactional(readOnly = true)
    public List<ProfessionalSummaryResponse> professionalsForService(String slug, UUID serviceId) {
        Business business = businessService.requireBySlug(slug);
        requireActiveService(business, serviceId);
        Map<UUID, List<Integer>> workingDays = workingDaysByProfessional(business.getId());
        return professionalRepository.findActiveByService(business.getId(), serviceId).stream()
                .map(professional -> ProfessionalSummaryResponse.from(professional,
                        workingDays.getOrDefault(professional.getId(), List.of())))
                .toList();
    }

    /**
     * Weekdays each professional works, for the professionals that have a schedule of their
     * own. The ones missing from the map follow the business hours.
     */
    private Map<UUID, List<Integer>> workingDaysByProfessional(UUID businessId) {
        return professionalHourRepository.findByBusinessIdOrderByDayOfWeekAscStartTimeAsc(businessId)
                .stream()
                .collect(Collectors.groupingBy(ProfessionalHour::getProfessionalId,
                        Collectors.mapping(hour -> hour.getDayOfWeek().getValue(),
                                Collectors.toUnmodifiableSet())))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().toList()));
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> slots(String slug, UUID serviceId, UUID professionalId, LocalDate date) {
        Business business = businessService.requireBySlug(slug);
        ServiceOffering service = requireActiveService(business, serviceId);
        requireProfessional(business, professionalId, serviceId);
        return availabilityService.availableSlots(business, service, professionalId, date).stream()
                .map(start -> SlotResponse.from(start, business.zoneId()))
                .toList();
    }

    /**
     * Final validation happens here, inside the transaction: the slot is recomputed right
     * before the insert. The exclusion constraint on the table still has the last word if two
     * requests pass this check at the same time.
     */
    @Transactional
    public BookingConfirmationResponse book(String slug, CreateAppointmentRequest request) {
        Business business = businessService.requireBySlug(slug);
        ServiceOffering service = requireActiveService(business, request.serviceId());
        Professional professional = requireProfessional(business, request.professionalId(), request.serviceId());

        bookingLock.lockProfessional(professional.getId());
        if (!availabilityService.isSlotAvailable(business, service, professional.getId(), request.startsAt())) {
            throw new ConflictException(
                    "Este horario acabou de ser reservado por outra pessoa. Escolha outro horario.");
        }

        Instant endsAt = request.startsAt().plus(service.duration());
        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                business.getId(), professional.getId(), service.getId(), request.customerName().trim(),
                request.customerWhatsapp().trim(), blankToNull(request.notes()), request.startsAt(),
                endsAt, service.getPriceCents()));

        return new BookingConfirmationResponse(appointment.getId(), business.getName(),
                business.getAddress(), service.getName(), professional.getName(),
                appointment.getCustomerName(), appointment.getStartsAt(), appointment.getEndsAt(),
                appointment.getPriceCents(), business.getTimezone());
    }

    private ServiceOffering requireActiveService(Business business, UUID serviceId) {
        ServiceOffering service = serviceRepository.findByIdAndBusinessId(serviceId, business.getId())
                .orElseThrow(() -> new NotFoundException("Servico nao encontrado."));
        if (!service.isActive()) {
            throw new NotFoundException("Servico nao encontrado.");
        }
        return service;
    }

    private Professional requireProfessional(Business business, UUID professionalId, UUID serviceId) {
        Professional professional = professionalRepository
                .findByIdAndBusinessId(professionalId, business.getId())
                .orElseThrow(() -> new NotFoundException("Profissional nao encontrado."));
        if (!professional.isActive() || !professionalRepository.performsService(professionalId, serviceId)) {
            throw new NotFoundException("Profissional nao encontrado.");
        }
        return professional;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
