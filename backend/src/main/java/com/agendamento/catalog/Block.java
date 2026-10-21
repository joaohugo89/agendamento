package com.agendamento.catalog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A period in which bookings are not accepted. A null professional blocks the whole business. */
@Entity
@Table(name = "blocks")
public class Block {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Block() {
    }

    public Block(UUID businessId, UUID professionalId, Instant startsAt, Instant endsAt, String reason) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.professionalId = professionalId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
    }

    public void update(UUID professionalId, Instant startsAt, Instant endsAt, String reason) {
        this.professionalId = professionalId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getProfessionalId() {
        return professionalId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getReason() {
        return reason;
    }
}
