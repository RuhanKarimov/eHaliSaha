-- V1__init.sql
-- eHalisaha initial schema (PostgreSQL 17)

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ======================
-- USERS / ROLES
-- ======================
CREATE TABLE IF NOT EXISTS app_users (
                                         id BIGSERIAL PRIMARY KEY,
                                         username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    display_name VARCHAR(120),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_user_role CHECK (role IN ('OWNER','MEMBER','ADMIN'))
    );


CREATE TABLE IF NOT EXISTS roles (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(20) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
    );

INSERT INTO roles(name) VALUES ('OWNER')  ON CONFLICT (name) DO NOTHING;
INSERT INTO roles(name) VALUES ('MEMBER') ON CONFLICT (name) DO NOTHING;

-- ======================
-- FACILITIES & PITCHES
-- ======================
CREATE TABLE IF NOT EXISTS facilities (
                                          id BIGSERIAL PRIMARY KEY,
                                          owner_user_id BIGINT NOT NULL,
                                          name VARCHAR(120) NOT NULL,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_fac_owner FOREIGN KEY (owner_user_id) REFERENCES app_users(id)
    );

CREATE TABLE IF NOT EXISTS pitches (
                                       id BIGSERIAL PRIMARY KEY,
                                       facility_id BIGINT NOT NULL,
                                       name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pitch_fac FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE,
    CONSTRAINT uk_pitch_fac_name UNIQUE (facility_id, name)
    );

-- ======================
-- MEMBERSHIP (owner approval)
-- ======================
CREATE TABLE IF NOT EXISTS membership_requests (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   facility_id BIGINT NOT NULL,
                                                   user_id BIGINT NOT NULL,
                                                   status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    decided_by BIGINT,
    CONSTRAINT fk_mr_fac FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mr_decider FOREIGN KEY (decided_by) REFERENCES app_users(id),
    CONSTRAINT uk_mr UNIQUE (facility_id, user_id),
    CONSTRAINT ck_mr_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
    );

CREATE TABLE IF NOT EXISTS memberships (
                                           id BIGSERIAL PRIMARY KEY,
                                           facility_id BIGINT NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/SUSPENDED/CANCELLED
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_mem_fac FOREIGN KEY (facility_id) REFERENCES facilities(id) ON DELETE CASCADE,
    CONSTRAINT fk_mem_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_mem UNIQUE (facility_id, user_id),
    CONSTRAINT ck_mem_status CHECK (status IN ('ACTIVE','SUSPENDED','CANCELLED'))
    );

-- ======================
-- DURATION & PRICING
-- ======================
CREATE TABLE IF NOT EXISTS duration_options (
                                                id BIGSERIAL PRIMARY KEY,
                                                minutes INT NOT NULL UNIQUE,
                                                label VARCHAR(30) NOT NULL
    );

CREATE TABLE IF NOT EXISTS pricing_rules (
                                             id BIGSERIAL PRIMARY KEY,
                                             pitch_id BIGINT NOT NULL,
                                             duration_option_id BIGINT NOT NULL,
                                             price NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pr_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    CONSTRAINT fk_pr_dur FOREIGN KEY (duration_option_id) REFERENCES duration_options(id),
    CONSTRAINT uk_pr UNIQUE (pitch_id, duration_option_id)
    );

-- Seed duration options (1h .. 10h, +30m steps)
INSERT INTO duration_options(minutes, label) VALUES
                                                 (60,'1 saat'),
                                                 (90,'1 saat 30 dk'),
                                                 (120,'2 saat'),
                                                 (150,'2 saat 30 dk'),
                                                 (180,'3 saat'),
                                                 (210,'3 saat 30 dk'),
                                                 (240,'4 saat'),
                                                 (270,'4 saat 30 dk'),
                                                 (300,'5 saat'),
                                                 (330,'5 saat 30 dk'),
                                                 (360,'6 saat'),
                                                 (390,'6 saat 30 dk'),
                                                 (420,'7 saat'),
                                                 (450,'7 saat 30 dk'),
                                                 (480,'8 saat'),
                                                 (510,'8 saat 30 dk'),
                                                 (540,'9 saat'),
                                                 (570,'9 saat 30 dk'),
                                                 (600,'10 saat')
    ON CONFLICT (minutes) DO NOTHING;

-- ======================
-- RESERVATIONS
-- ======================
CREATE TABLE IF NOT EXISTS reservations (
                                            id BIGSERIAL PRIMARY KEY,
                                            pitch_id BIGINT NOT NULL,
                                            membership_id BIGINT NOT NULL,
                                            start_time TIMESTAMPTZ NOT NULL,
                                            end_time TIMESTAMPTZ NOT NULL,
                                            status VARCHAR(20) NOT NULL DEFAULT 'CREATED', -- CREATED/CONFIRMED/CANCELLED/COMPLETED
    total_price NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_res_pitch FOREIGN KEY (pitch_id) REFERENCES pitches(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_mem FOREIGN KEY (membership_id) REFERENCES memberships(id) ON DELETE CASCADE,
    CONSTRAINT ck_time CHECK (end_time > start_time),
    CONSTRAINT ck_res_status CHECK (status IN ('CREATED','CONFIRMED','CANCELLED','COMPLETED'))
    );

-- Overlap prevention: same pitch cannot have overlapping time ranges when status is active
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'no_overlap_reservations') THEN
ALTER TABLE reservations
    ADD CONSTRAINT no_overlap_reservations
    EXCLUDE USING gist (
        pitch_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
      )
      WHERE (status IN ('CREATED','CONFIRMED'));
END IF;
END$$;

CREATE TABLE IF NOT EXISTS reservation_players (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   reservation_id BIGINT NOT NULL,
                                                   full_name VARCHAR(120) NOT NULL,
    jersey_no INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_rp_res FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
    );

-- ======================
-- PAYMENTS
-- ======================
CREATE TABLE IF NOT EXISTS payments (
                                        id BIGSERIAL PRIMARY KEY,
                                        reservation_id BIGINT NOT NULL UNIQUE,
                                        method VARCHAR(10) NOT NULL,         -- CASH/CARD
    status VARCHAR(20) NOT NULL DEFAULT 'INIT', -- INIT/PAID/FAILED/CANCELLED
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    provider_ref VARCHAR(120),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_pay_res FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT ck_pay_method CHECK (method IN ('CASH','CARD')),
    CONSTRAINT ck_pay_status CHECK (status IN ('INIT','PAID','FAILED','CANCELLED'))
    );

-- ======================
-- MATCH VIDEOS (auto publish after match end)
-- ======================
CREATE TABLE IF NOT EXISTS match_videos (
                                            id BIGSERIAL PRIMARY KEY,
                                            reservation_id BIGINT NOT NULL UNIQUE,
                                            status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', -- PUBLISHED/HIDDEN/PROCESSING
    storage_url VARCHAR(255) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_vid_res FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    CONSTRAINT ck_vid_status CHECK (status IN ('PUBLISHED','HIDDEN','PROCESSING'))
    );

-- ======================
-- AUDIT LOGS
-- ======================
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGSERIAL PRIMARY KEY,
                                          actor_user_id BIGINT,
                                          action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(40),
    entity_id BIGINT,
    detail TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_users(id)
    );

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_res_pitch_time ON reservations(pitch_id, start_time);
CREATE INDEX IF NOT EXISTS idx_mr_fac_status ON membership_requests(facility_id, status);
CREATE INDEX IF NOT EXISTS idx_fac_owner ON facilities(owner_user_id);
