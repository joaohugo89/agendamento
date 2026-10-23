package com.agendamento.catalog;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agendamento.auth.AuthenticatedUser;
import com.agendamento.catalog.dto.BusinessHourResponse;
import com.agendamento.catalog.dto.BusinessHoursRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/business-hours")
@Tag(name = "Horario de funcionamento")
public class BusinessHourController {

    private final BusinessHourService service;

    public BusinessHourController(BusinessHourService service) {
        this.service = service;
    }

    @GetMapping
    public List<BusinessHourResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.businessId());
    }

    @PutMapping
    public List<BusinessHourResponse> replace(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BusinessHoursRequest request) {
        return service.replace(user.businessId(), request);
    }
}
