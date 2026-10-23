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
import com.agendamento.catalog.dto.BlockRequest;
import com.agendamento.catalog.dto.BlockResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/blocks")
@Tag(name = "Bloqueios")
public class BlockController {

    private final BlockService service;

    public BlockController(BlockService service) {
        this.service = service;
    }

    @GetMapping
    public List<BlockResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.businessId());
    }

    @PostMapping
    public ResponseEntity<BlockResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody BlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.businessId(), request));
    }

    @PutMapping("/{id}")
    public BlockResponse update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id, @Valid @RequestBody BlockRequest request) {
        return service.update(user.businessId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        service.delete(user.businessId(), id);
        return ResponseEntity.noContent().build();
    }
}
