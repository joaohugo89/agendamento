-- Working hours of a single professional, in the same shape as business_hours:
-- day_of_week follows java.time.DayOfWeek (1 = Monday .. 7 = Sunday) and several rows for
-- the same day represent several periods.
--
-- No row for a professional at all means the professional follows the business hours, which
-- is how every professional created before this migration keeps behaving.
CREATE TABLE professional_hours (
    id              uuid     PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     uuid     NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    professional_id uuid     NOT NULL REFERENCES professionals (id) ON DELETE CASCADE,
    day_of_week     smallint NOT NULL,
    start_time      time     NOT NULL,
    end_time        time     NOT NULL,
    CONSTRAINT ck_professional_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_professional_hours_range CHECK (start_time < end_time)
);
CREATE INDEX idx_professional_hours_professional_day
    ON professional_hours (professional_id, day_of_week);
CREATE INDEX idx_professional_hours_business ON professional_hours (business_id);
