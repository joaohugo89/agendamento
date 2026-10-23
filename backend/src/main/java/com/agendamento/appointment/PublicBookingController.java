package com.agendamento.appointment;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agendamento.appointment.dto.BookingConfirmationResponse;
import com.agendamento.appointment.dto.CreateAppointmentRequest;
import com.agendamento.appointment.dto.SlotResponse;
import com.agendamento.business.dto.PublicBusinessResponse;
import com.agendamento.catalog.dto.ProfessionalSummaryResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Public routes: the business is resolved by its slug, never by a tenant id. */
@RestController
@RequestMapping("/api/public/businesses/{slug}")
@Tag(name = "Agendamento publico")
public class PublicBookingController {

    private final BookingService bookingService;

    public PublicBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public PublicBusinessResponse business(@PathVariable String slug) {
        return bookingService.publicPage(slug);
    }

    @GetMapping("/services/{serviceId}/professionals")
    public List<ProfessionalSummaryResponse> professionals(@PathVariable String slug,
            @PathVariable UUID serviceId) {
        return bookingService.professionalsForService(slug, serviceId);
    }

    @GetMapping("/availability")
    public List<SlotResponse> availability(@PathVariable String slug,
            @RequestParam UUID serviceId,
            @RequestParam UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.slots(slug, serviceId, professionalId, date);
    }

    @PostMapping("/appointments")
    public ResponseEntity<BookingConfirmationResponse> book(@PathVariable String slug,
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.book(slug, request));
    }
}
