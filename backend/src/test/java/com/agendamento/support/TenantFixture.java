package com.agendamento.support;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.auth.JwtService;
import com.agendamento.auth.User;
import com.agendamento.auth.UserRepository;
import com.agendamento.business.Business;
import com.agendamento.business.BusinessRepository;
import com.agendamento.catalog.BusinessHour;
import com.agendamento.catalog.BusinessHourRepository;
import com.agendamento.catalog.Professional;
import com.agendamento.catalog.ProfessionalRepository;
import com.agendamento.catalog.ServiceOffering;
import com.agendamento.catalog.ServiceOfferingRepository;

/** Builds a ready-to-book tenant: business, owner, opening hours, one service, one professional. */
@Component
public class TenantFixture {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final BusinessHourRepository businessHourRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TenantFixture(BusinessRepository businessRepository, UserRepository userRepository,
            BusinessHourRepository businessHourRepository, ServiceOfferingRepository serviceRepository,
            ProfessionalRepository professionalRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.businessHourRepository = businessHourRepository;
        this.serviceRepository = serviceRepository;
        this.professionalRepository = professionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record Tenant(UUID businessId, String slug, UUID serviceId, UUID professionalId, String token) {
    }

    @Transactional
    public Tenant create(String slug) {
        Business business = businessRepository.save(
                new Business("Negocio " + slug, slug, Business.DEFAULT_TIMEZONE));

        for (DayOfWeek day : DayOfWeek.values()) {
            businessHourRepository.save(new BusinessHour(business.getId(), day,
                    LocalTime.of(8, 0), LocalTime.of(18, 0)));
        }

        ServiceOffering service = serviceRepository.save(new ServiceOffering(
                business.getId(), "Corte", "Corte de cabelo", 4000, 30, true));

        Professional professional = new Professional(business.getId(), "Joao", true);
        professional.update("Joao", true, Set.of(service));
        professional = professionalRepository.save(professional);

        User user = userRepository.save(new User(business.getId(), "Dono " + slug,
                slug + "@example.com", passwordEncoder.encode("senha-de-teste")));

        return new Tenant(business.getId(), slug, service.getId(), professional.getId(),
                jwtService.generateToken(user));
    }
}
