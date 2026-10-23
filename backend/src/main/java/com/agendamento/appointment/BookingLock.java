package com.agendamento.appointment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

/**
 * Serialises booking attempts for the same professional.
 *
 * <p>Without it, simultaneous inserts for the same slot all reach the exclusion constraint and
 * wait on each other's speculative index entry, which PostgreSQL resolves as a deadlock and
 * reports as a server error. Taking a transaction-scoped advisory lock first means the losers
 * simply see the slot already taken when they recompute availability, and get a clean 409.
 * The exclusion constraint stays as the last line of defence.
 */
@Component
public class BookingLock {

    private final EntityManager entityManager;

    public BookingLock(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Must be called inside a transaction; the lock is released when it ends. */
    public void lockProfessional(UUID professionalId) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", professionalId.getMostSignificantBits())
                .getSingleResult();
    }
}
