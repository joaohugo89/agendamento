package com.agendamento.business;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "businesses")
public class Business {

    public static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    private String address;

    private String phone;

    @Column(nullable = false)
    private String timezone = DEFAULT_TIMEZONE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Business() {
    }

    public Business(String name, String slug, String timezone) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.timezone = timezone == null ? DEFAULT_TIMEZONE : timezone;
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
