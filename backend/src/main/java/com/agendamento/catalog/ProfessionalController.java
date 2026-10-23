package com.agendamento.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agendamento.auth.AuthenticatedUser;
import com.agendamento.catalog.dto.ProfessionalRequest;
import com.agendamento.catalog.dto.ProfessionalResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/professionals")
@Tag(name = "Profissionais")
public class ProfessionalController {

    private final ProfessionalCatalogService service;

    public ProfessionalController(ProfessionalCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfessionalResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.businessId());
    }

    @PostMapping
    public ResponseEntity<ProfessionalResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProfessionalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.businessId(), request));
    }

    @PutMapping("/{id}")
    public ProfessionalResponse update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id, @Valid @RequestBody ProfessionalRequest request) {
        return service.update(user.businessId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        service.delete(user.businessId(), id);
        return ResponseEntity.noContent().build();
    }
}
