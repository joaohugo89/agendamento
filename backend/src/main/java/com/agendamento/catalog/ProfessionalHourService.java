package com.agendamento.catalog;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.catalog.dto.ProfessionalHourResponse;
import com.agendamento.catalog.dto.ProfessionalScheduleRequest;
import com.agendamento.catalog.dto.ProfessionalScheduleResponse;
import com.agendamento.common.BusinessRuleException;
import com.agendamento.common.NotFoundException;

/**
 * The working hours of one professional. Having no row at all is a meaningful state: it means
 * the professional follows the business hours, so "follows the business" and "works only these
 * periods" never disagree in the database.
 */
@Service
public class ProfessionalHourService {

    private final ProfessionalHourRepository repository;
    private final ProfessionalRepository professionalRepository;

    public ProfessionalHourService(ProfessionalHourRepository repository,
            ProfessionalRepository professionalRepository) {
        this.repository = repository;
        this.professionalRepository = professionalRepository;
    }

    @Transactional(readOnly = true)
    public ProfessionalScheduleResponse get(UUID businessId, UUID professionalId) {
        requireProfessional(businessId, professionalId);
        return toResponse(repository.findByProfessionalIdOrderByDayOfWeekAscStartTimeAsc(professionalId));
    }

    @Transactional
    public ProfessionalScheduleResponse replace(UUID businessId, UUID professionalId,
            ProfessionalScheduleRequest request) {
        requireProfessional(businessId, professionalId);

        repository.deleteByProfessionalId(professionalId);
        repository.flush();

        if (request.followsBusinessHours()) {
            return toResponse(List.of());
        }
        if (request.entries().isEmpty()) {
            throw new BusinessRuleException(
                    "Informe ao menos um periodo ou deixe o profissional seguir o horario do estabelecimento.");
        }
        SchedulePeriods.validate(request.entries().stream()
                .map(entry -> new SchedulePeriods.Period(entry.dayOfWeek(), entry.startTime(),
                        entry.endTime()))
                .toList());

        List<ProfessionalHour> hours = request.entries().stream()
                .map(entry -> new ProfessionalHour(businessId, professionalId,
                        DayOfWeek.of(entry.dayOfWeek()), entry.startTime(), entry.endTime()))
                .toList();
        return toResponse(repository.saveAll(hours));
    }

    /** Resolved inside the tenant, so a professional of another business simply does not exist. */
    private void requireProfessional(UUID businessId, UUID professionalId) {
        professionalRepository.findByIdAndBusinessId(professionalId, businessId)
                .orElseThrow(() -> new NotFoundException("Profissional nao encontrado."));
    }

    private ProfessionalScheduleResponse toResponse(List<ProfessionalHour> hours) {
        return new ProfessionalScheduleResponse(hours.isEmpty(), hours.stream()
                .sorted(Comparator.comparing(ProfessionalHour::getDayOfWeek)
                        .thenComparing(ProfessionalHour::getStartTime))
                .map(ProfessionalHourResponse::from)
                .toList());
    }
}
