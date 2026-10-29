package com.agendamento.auth;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agendamento.auth.dto.AuthResponse;
import com.agendamento.auth.dto.LoginRequest;
import com.agendamento.auth.dto.RegisterRequest;
import com.agendamento.business.Business;
import com.agendamento.business.BusinessRepository;
import com.agendamento.business.SlugGenerator;
import com.agendamento.catalog.BusinessHour;
import com.agendamento.catalog.BusinessHourRepository;
import com.agendamento.common.ConflictException;
import com.agendamento.common.NotFoundException;

@Service
public class AuthService {

    private static final LocalTime DEFAULT_OPENING = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSING = LocalTime.of(18, 0);

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessHourRepository businessHourRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, BusinessRepository businessRepository,
            BusinessHourRepository businessHourRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.businessHourRepository = businessHourRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Signing up creates the owner and the business, with its slug derived from the name. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Ja existe uma conta com este e-mail.");
        }

        String slug = SlugGenerator.uniqueSlug(request.businessName(), businessRepository::existsBySlug);
        Business business = businessRepository.save(
                new Business(request.businessName(), slug, Business.DEFAULT_TIMEZONE));
        businessHourRepository.saveAll(defaultOpeningHours(business));

        User user = userRepository.save(new User(business.getId(), request.ownerName(),
                request.email().toLowerCase(Locale.ROOT), passwordEncoder.encode(request.password())));

        return toResponse(user, business);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("invalid credentials");
        }
        Business business = businessRepository.findById(user.getBusinessId())
                .orElseThrow(() -> new NotFoundException("Estabelecimento nao encontrado."));
        return toResponse(user, business);
    }

    /** New businesses start open Monday to Saturday so the public page works right away. */
    private List<BusinessHour> defaultOpeningHours(Business business) {
        return List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).stream()
                .map(day -> new BusinessHour(business.getId(), day, DEFAULT_OPENING, DEFAULT_CLOSING))
                .toList();
    }

    private AuthResponse toResponse(User user, Business business) {
        return new AuthResponse(
                jwtService.generateToken(user),
                jwtService.expiresInSeconds(),
                user.getId(),
                user.getName(),
                business.getId(),
                business.getName(),
                business.getSlug());
    }
}
