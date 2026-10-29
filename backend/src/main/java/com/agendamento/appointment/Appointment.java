package com.agendamento.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_whatsapp", nullable = false)
    private String customerWhatsapp;

    private String notes;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Appointment() {
    }

    public Appointment(UUID businessId, UUID professionalId, UUID serviceId, String customerName,
            String customerWhatsapp, String notes, Instant startsAt, Instant endsAt, int priceCents) {
        this.id = UUID.randomUUID();
        this.businessId = businessId;
        this.professionalId = professionalId;
        this.serviceId = serviceId;
        this.customerName = customerName;
        this.customerWhatsapp = customerWhatsapp;
        this.notes = notes;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.priceCents = priceCents;
    }

    public void changeStatus(AppointmentStatus target) {
        this.status = target;
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

    public UUID getServiceId() {
        return serviceId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerWhatsapp() {
        return customerWhatsapp;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public int getPriceCents() {
        return priceCents;
    }
}
