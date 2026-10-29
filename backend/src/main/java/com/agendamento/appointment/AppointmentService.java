package com.agendamento.appointment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.appointment.dto.AppointmentResponse;
import com.agendamento.business.Business;
import com.agendamento.business.BusinessService;
import com.agendamento.catalog.Professional;
import com.agendamento.catalog.ProfessionalRepository;
import com.agendamento.catalog.ServiceOffering;
import com.agendamento.catalog.ServiceOfferingRepository;
import com.agendamento.common.BusinessRuleException;
import com.agendamento.common.NotFoundException;

/** Dashboard side of appointments: the day agenda and status changes. */
@Service
public class AppointmentService {

    private final AppointmentRepository repository;
    private final ServiceOfferingRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final BusinessService businessService;

    public AppointmentService(AppointmentRepository repository,
            ServiceOfferingRepository serviceRepository,
            ProfessionalRepository professionalRepository,
            BusinessService businessService) {
        this.repository = repository;
        this.serviceRepository = serviceRepository;
        this.professionalRepository = professionalRepository;
        this.businessService = businessService;
    }

    /** The day boundaries come from the business timezone, not from the server one. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listDay(UUID businessId, LocalDate date, UUID professionalId) {
        Business business = businessService.require(businessId);
        ZoneId zone = business.zoneId();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

        Map<UUID, String> serviceNames = serviceNames(businessId);
        Map<UUID, String> professionalNames = professionalNames(businessId);

        List<Appointment> appointments = professionalId == null
                ? repository.findForDay(businessId, dayStart, dayEnd)
                : repository.findForDayAndProfessional(businessId, professionalId, dayStart, dayEnd);

        return appointments.stream()
                .map(appointment -> AppointmentResponse.from(appointment,
                        serviceNames.get(appointment.getServiceId()),
                        professionalNames.get(appointment.getProfessionalId()), zone))
                .toList();
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID businessId, UUID id, AppointmentStatus target) {
        Business business = businessService.require(businessId);
        Appointment appointment = repository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Agendamento nao encontrado."));
        if (appointment.getStatus() == target) {
            return toResponse(business, appointment);
        }
        if (!appointment.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException("Nao e possivel mudar de "
                    + appointment.getStatus() + " para " + target + ".");
        }
        appointment.changeStatus(target);
        return toResponse(business, repository.save(appointment));
    }

    private AppointmentResponse toResponse(Business business, Appointment appointment) {
        UUID businessId = business.getId();
        String serviceName = serviceRepository
                .findByIdAndBusinessId(appointment.getServiceId(), businessId)
                .map(ServiceOffering::getName)
                .orElse(null);
        String professionalName = professionalRepository
                .findByIdAndBusinessId(appointment.getProfessionalId(), businessId)
                .map(Professional::getName)
                .orElse(null);
        return AppointmentResponse.from(appointment, serviceName, professionalName, business.zoneId());
    }

    private Map<UUID, String> serviceNames(UUID businessId) {
        Map<UUID, String> names = new HashMap<>();
        serviceRepository.findByBusinessIdOrderByNameAsc(businessId)
                .forEach(service -> names.put(service.getId(), service.getName()));
        return names;
    }

    private Map<UUID, String> professionalNames(UUID businessId) {
        Map<UUID, String> names = new HashMap<>();
        professionalRepository.findByBusinessIdOrderByNameAsc(businessId)
                .forEach(professional -> names.put(professional.getId(), professional.getName()));
        return names;
    }
}
