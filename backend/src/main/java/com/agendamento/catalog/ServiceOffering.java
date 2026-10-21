package com.agendamento.catalog;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A service offered by a business. Mapped to {@code services}. */
@Entity
@Table(name = "services")
public class ServiceOffering {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ServiceOffering() {
    }

    public ServiceOffering(UUID businessId, String name, String description, int priceCents,
            int durationMinutes, boolean active) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.durationMinutes = durationMinutes;
        this.active = active;
    }

    public Duration duration() {
        return Duration.ofMinutes(durationMinutes);
    }

    public void update(String name, String description, int priceCents, int durationMinutes, boolean active) {
        this.name = name;
        this.description = description;
        this.priceCents = priceCents;
        this.durationMinutes = durationMinutes;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isActive() {
        return active;
    }
}
