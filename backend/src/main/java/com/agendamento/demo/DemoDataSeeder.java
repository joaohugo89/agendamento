package com.agendamento.demo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.appointment.Appointment;
import com.agendamento.appointment.AppointmentRepository;
import com.agendamento.auth.User;
import com.agendamento.auth.UserRepository;
import com.agendamento.business.Business;
import com.agendamento.business.BusinessRepository;
import com.agendamento.catalog.BusinessHour;
import com.agendamento.catalog.BusinessHourRepository;
import com.agendamento.catalog.Professional;
import com.agendamento.catalog.ProfessionalHour;
import com.agendamento.catalog.ProfessionalHourRepository;
import com.agendamento.catalog.ProfessionalRepository;
import com.agendamento.catalog.ServiceOffering;
import com.agendamento.catalog.ServiceOfferingRepository;

/**
 * Seeds a demo business when the {@code demo} profile is active, so the project can be
 * explored right after {@code docker compose up}. It runs only once: if the slug already
 * exists, nothing is touched.
 */
@Configuration
@Profile("demo")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String SLUG = "barbearia-prime";
    private static final String DEMO_EMAIL = "demo@barbeariaprime.com";
    private static final String DEMO_PASSWORD = "demo1234";

    @Bean
    ApplicationRunner seedDemoData(BusinessRepository businessRepository, UserRepository userRepository,
            BusinessHourRepository businessHourRepository, ServiceOfferingRepository serviceRepository,
            ProfessionalRepository professionalRepository,
            ProfessionalHourRepository professionalHourRepository,
            AppointmentRepository appointmentRepository, PasswordEncoder passwordEncoder) {
        return args -> seed(businessRepository, userRepository, businessHourRepository,
                serviceRepository, professionalRepository, professionalHourRepository,
                appointmentRepository, passwordEncoder);
    }

    @Transactional
    void seed(BusinessRepository businessRepository, UserRepository userRepository,
            BusinessHourRepository businessHourRepository, ServiceOfferingRepository serviceRepository,
            ProfessionalRepository professionalRepository,
            ProfessionalHourRepository professionalHourRepository,
            AppointmentRepository appointmentRepository, PasswordEncoder passwordEncoder) {
        if (businessRepository.existsBySlug(SLUG)) {
            log.info("Demo data already present, skipping seed.");
            return;
        }

        Business business = businessRepository.save(
                new Business("Barbearia Prime", SLUG, Business.DEFAULT_TIMEZONE));
        business.setDescription("Cortes classicos e barba feita no centro da cidade.");
        business.setAddress("Rua das Palmeiras, 120 - Centro");
        business.setPhone("(11) 4002-8922");
        businessRepository.save(business);

        userRepository.save(new User(business.getId(), "Dono da Barbearia", DEMO_EMAIL,
                passwordEncoder.encode(DEMO_PASSWORD)));

        // Monday to Friday with a lunch break, Saturday straight through.
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            businessHourRepository.save(
                    new BusinessHour(business.getId(), day, LocalTime.of(9, 0), LocalTime.of(12, 0)));
            businessHourRepository.save(
                    new BusinessHour(business.getId(), day, LocalTime.of(13, 0), LocalTime.of(19, 0)));
        }
        businessHourRepository.save(new BusinessHour(business.getId(), DayOfWeek.SATURDAY,
                LocalTime.of(9, 0), LocalTime.of(17, 0)));

        ServiceOffering haircut = serviceRepository.save(new ServiceOffering(business.getId(),
                "Corte", "Corte na maquina ou tesoura, com finalizacao.", 4000, 30, true));
        ServiceOffering beard = serviceRepository.save(new ServiceOffering(business.getId(),
                "Barba", "Barba feita na navalha com toalha quente.", 3000, 30, true));
        ServiceOffering combo = serviceRepository.save(new ServiceOffering(business.getId(),
                "Corte + Barba", "O combo completo, com desconto.", 6500, 60, true));

        Professional joao = new Professional(business.getId(), "Joao", true);
        joao.update("Joao", true, Set.of(haircut, beard, combo));
        joao = professionalRepository.save(joao);

        Professional carlos = new Professional(business.getId(), "Carlos", true);
        carlos.update("Carlos", true, Set.of(haircut, combo));
        carlos = professionalRepository.save(carlos);

        // Joao has no schedule of his own, so he works whenever the shop is open. Carlos starts
        // later from Tuesday to Friday, is off on Monday and leaves early on Saturday.
        for (DayOfWeek day : List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY)) {
            professionalHourRepository.save(new ProfessionalHour(business.getId(), carlos.getId(),
                    day, LocalTime.of(10, 0), LocalTime.of(12, 0)));
            professionalHourRepository.save(new ProfessionalHour(business.getId(), carlos.getId(),
                    day, LocalTime.of(13, 0), LocalTime.of(19, 0)));
        }
        professionalHourRepository.save(new ProfessionalHour(business.getId(), carlos.getId(),
                DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(14, 0)));

        seedAppointments(business, appointmentRepository, joao, carlos, haircut, beard, combo);

        log.info("Demo data created. Business: /agendar/{} - user: {} / {}", SLUG, DEMO_EMAIL,
                DEMO_PASSWORD);
    }

    /** A handful of appointments spread over the current week. */
    private void seedAppointments(Business business, AppointmentRepository appointmentRepository,
            Professional joao, Professional carlos, ServiceOffering haircut, ServiceOffering beard,
            ServiceOffering combo) {
        ZoneId zone = business.zoneId();
        LocalDate monday = LocalDate.now(zone).with(DayOfWeek.MONDAY);

        record Seed(Professional professional, ServiceOffering service, DayOfWeek day, LocalTime time,
                String customer, String whatsapp, String notes) {
        }

        List<Seed> seeds = List.of(
                new Seed(joao, haircut, DayOfWeek.MONDAY, LocalTime.of(9, 0), "Rafael Lima",
                        "11988880001", null),
                new Seed(joao, combo, DayOfWeek.MONDAY, LocalTime.of(14, 0), "Bruno Alves",
                        "11988880002", "Prefere maquina 2 nas laterais."),
                new Seed(carlos, haircut, DayOfWeek.TUESDAY, LocalTime.of(10, 0), "Diego Souza",
                        "11988880003", null),
                new Seed(joao, beard, DayOfWeek.WEDNESDAY, LocalTime.of(16, 30), "Marcos Pereira",
                        "11988880004", null),
                new Seed(carlos, combo, DayOfWeek.FRIDAY, LocalTime.of(11, 0), "Thiago Costa",
                        "11988880005", null),
                new Seed(joao, haircut, DayOfWeek.SATURDAY, LocalTime.of(9, 30), "Andre Ramos",
                        "11988880006", null));

        for (Seed seed : seeds) {
            LocalDate date = monday.with(seed.day());
            Instant startsAt = ZonedDateTime.of(date, seed.time(), zone).toInstant();
            appointmentRepository.save(new Appointment(business.getId(), seed.professional().getId(),
                    seed.service().getId(), seed.customer(), seed.whatsapp(), seed.notes(), startsAt,
                    startsAt.plus(seed.service().duration()), seed.service().getPriceCents()));
        }
    }
}
