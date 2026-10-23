package com.agendamento.business;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agendamento.auth.AuthenticatedUser;
import com.agendamento.business.dto.BusinessResponse;
import com.agendamento.business.dto.BusinessUpdateRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/business")
@Tag(name = "Estabelecimento")
public class BusinessController {

    private final BusinessService service;

    public BusinessController(BusinessService service) {
        this.service = service;
    }

    @GetMapping
    public BusinessResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(user.businessId());
    }

    @PutMapping
    public BusinessResponse update(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BusinessUpdateRequest request) {
        return service.update(user.businessId(), request);
    }
}
