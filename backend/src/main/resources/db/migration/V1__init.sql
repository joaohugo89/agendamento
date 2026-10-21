CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

CREATE TABLE businesses (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(120) NOT NULL,
    slug        varchar(80)  NOT NULL,
    description text,
    address     varchar(255),
    phone       varchar(30),
    timezone    varchar(60)  NOT NULL DEFAULT 'America/Sao_Paulo',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_businesses_slug UNIQUE (slug)
);

CREATE TABLE users (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id   uuid         NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name          varchar(120) NOT NULL,
    email         varchar(180) NOT NULL,
    password_hash varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);
CREATE INDEX idx_users_business ON users (business_id);

CREATE TABLE services (
    id               uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      uuid         NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name             varchar(120) NOT NULL,
    description      text,
    price_cents      integer      NOT NULL,
    duration_minutes integer      NOT NULL,
    active           boolean      NOT NULL DEFAULT true,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_services_price CHECK (price_cents >= 0),
    CONSTRAINT ck_services_duration CHECK (duration_minutes > 0)
);
CREATE INDEX idx_services_business_active ON services (business_id, active);

CREATE TABLE professionals (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id uuid         NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name        varchar(120) NOT NULL,
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_professionals_business_active ON professionals (business_id, active);

CREATE TABLE professional_services (
    professional_id uuid NOT NULL REFERENCES professionals (id) ON DELETE CASCADE,
    service_id      uuid NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    PRIMARY KEY (professional_id, service_id)
);
CREATE INDEX idx_professional_services_service ON professional_services (service_id);

-- day_of_week follows java.time.DayOfWeek: 1 = Monday .. 7 = Sunday.
-- Several rows for the same day represent several opening periods.
CREATE TABLE business_hours (
    id          uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id uuid     NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    day_of_week smallint NOT NULL,
    start_time  time     NOT NULL,
    end_time    time     NOT NULL,
    CONSTRAINT ck_business_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_business_hours_range CHECK (start_time < end_time)
);
CREATE INDEX idx_business_hours_business_day ON business_hours (business_id, day_of_week);

-- professional_id NULL blocks the whole business.
CREATE TABLE blocks (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     uuid        NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    professional_id uuid        REFERENCES professionals (id) ON DELETE CASCADE,
    starts_at       timestamptz NOT NULL,
    ends_at         timestamptz NOT NULL,
    reason          varchar(255),
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_blocks_range CHECK (starts_at < ends_at)
);
CREATE INDEX idx_blocks_business_starts ON blocks (business_id, starts_at);

CREATE TABLE appointments (
    id                uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id       uuid         NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    professional_id   uuid         NOT NULL REFERENCES professionals (id),
    service_id        uuid         NOT NULL REFERENCES services (id),
    customer_name     varchar(120) NOT NULL,
    customer_whatsapp varchar(30)  NOT NULL,
    notes             text,
    starts_at         timestamptz  NOT NULL,
    ends_at           timestamptz  NOT NULL,
    status            varchar(20)  NOT NULL,
    price_cents       integer      NOT NULL,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_appointments_range CHECK (starts_at < ends_at),
    CONSTRAINT ck_appointments_status
        CHECK (status IN ('CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);
CREATE INDEX idx_appointments_business_starts ON appointments (business_id, starts_at);
CREATE INDEX idx_appointments_professional_starts ON appointments (professional_id, starts_at);

-- Definitive double-booking guard: no two non-cancelled appointments of the same
-- professional may overlap in time. Enforced by the database, not by the application.
ALTER TABLE appointments
    ADD CONSTRAINT excl_appointments_professional_overlap
    EXCLUDE USING gist (
        professional_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status <> 'CANCELLED');
