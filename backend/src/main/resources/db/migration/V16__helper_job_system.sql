-- Helper onboarding details, full job lifecycle, immutable history, cancellation tracking,
-- commission/settlement ledger, ratings, reports/disputes and audit logs.

-- ---- numeric columns the entities map as Double (hibernate ddl-auto=validate) ----
ALTER TABLE jobs  ALTER COLUMN reward_amount  TYPE DOUBLE PRECISION;
ALTER TABLE users ALTER COLUMN rating_avg     TYPE DOUBLE PRECISION;
ALTER TABLE users ALTER COLUMN wallet_balance TYPE DOUBLE PRECISION;

-- ---- users: temporary blocks + cancellation-abuse counter ----
ALTER TABLE users ADD COLUMN blocked_until            TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN block_reason             TEXT;
ALTER TABLE users ADD COLUMN cancellation_violations  INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_violation_at        TIMESTAMPTZ;

-- ---- mechanic_profiles / helper_applications: SUSPENDED state ----
ALTER TABLE mechanic_profiles DROP CONSTRAINT IF EXISTS mechanic_profiles_verification_status_check;
ALTER TABLE mechanic_profiles ADD CONSTRAINT mechanic_profiles_verification_status_check
    CHECK (verification_status IN ('PENDING','APPROVED','REJECTED','SUSPENDED'));
ALTER TABLE helper_applications DROP CONSTRAINT IF EXISTS helper_applications_status_check;
ALTER TABLE helper_applications ADD CONSTRAINT helper_applications_status_check
    CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED'));

ALTER TABLE helper_applications ADD COLUMN experience_years   INT;
ALTER TABLE helper_applications ADD COLUMN service_area       VARCHAR(120);
ALTER TABLE helper_applications ADD COLUMN services           TEXT;
ALTER TABLE helper_applications ADD COLUMN equipment          TEXT;
ALTER TABLE helper_applications ADD COLUMN vehicle_type       VARCHAR(30);
ALTER TABLE helper_applications ADD COLUMN vehicle_brand      VARCHAR(60);
ALTER TABLE helper_applications ADD COLUMN vehicle_model      VARCHAR(60);
ALTER TABLE helper_applications ADD COLUMN vehicle_variant    VARCHAR(60);
ALTER TABLE helper_applications ADD COLUMN vehicle_reg        VARCHAR(20);
ALTER TABLE helper_applications ADD COLUMN service_radius_km  INT;
ALTER TABLE helper_applications ADD COLUMN payout_upi         VARCHAR(80);
ALTER TABLE helper_applications ADD COLUMN payout_holder      VARCHAR(100);
ALTER TABLE helper_applications ADD COLUMN payout_bank_acct   VARCHAR(40);
ALTER TABLE helper_applications ADD COLUMN payout_ifsc        VARCHAR(15);
ALTER TABLE helper_applications ADD COLUMN suspension_reason  TEXT;

-- ---- jobs: lifecycle PENDING(requested) > MATCHED(accepted) > ARRIVED > IN_PROGRESS > WORK_DONE > COMPLETED (+rated flag) ----
ALTER TABLE jobs DROP CONSTRAINT IF EXISTS jobs_status_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_status_check
    CHECK (status IN ('PENDING','MATCHED','ARRIVED','IN_PROGRESS','WORK_DONE','COMPLETED','CANCELLED','EXPIRED'));
ALTER TABLE jobs ADD COLUMN arrived_at              TIMESTAMPTZ;
ALTER TABLE jobs ADD COLUMN arrival_lat             DOUBLE PRECISION;
ALTER TABLE jobs ADD COLUMN arrival_lng             DOUBLE PRECISION;
ALTER TABLE jobs ADD COLUMN arrival_method          VARCHAR(20);
ALTER TABLE jobs ADD COLUMN completion_otp          VARCHAR(6);
ALTER TABLE jobs ADD COLUMN completion_otp_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE jobs ADD COLUMN work_done_at            TIMESTAMPTZ;
ALTER TABLE jobs ADD COLUMN completed_at            TIMESTAMPTZ;
ALTER TABLE jobs ADD COLUMN final_amount            DOUBLE PRECISION;
ALTER TABLE jobs ADD COLUMN payment_mode            VARCHAR(20);
ALTER TABLE jobs ADD COLUMN rated                   BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE jobs ADD COLUMN cancelled_by            UUID REFERENCES users(id);
ALTER TABLE jobs ADD COLUMN cancelled_at            TIMESTAMPTZ;
ALTER TABLE jobs ADD COLUMN cancel_reason           VARCHAR(200);

-- ---- immutable helpers: no UPDATE / DELETE on append-only tables ----
CREATE OR REPLACE FUNCTION raahi_block_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE job_status_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id              UUID NOT NULL REFERENCES jobs(id),
    from_status         VARCHAR(20),
    to_status           VARCHAR(20) NOT NULL,
    actor_id            UUID REFERENCES users(id),
    actor_role          VARCHAR(20) NOT NULL,
    verification_method VARCHAR(30),
    lat                 DOUBLE PRECISION,
    lng                 DOUBLE PRECISION,
    note                VARCHAR(300),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_job_status_history_job ON job_status_history(job_id, created_at);
CREATE TRIGGER trg_job_status_history_immutable BEFORE UPDATE OR DELETE ON job_status_history
    FOR EACH ROW EXECUTE FUNCTION raahi_block_mutation();

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID REFERENCES users(id),
    actor_role  VARCHAR(20) NOT NULL,
    action      VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id   VARCHAR(64),
    detail      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE TRIGGER trg_audit_logs_immutable BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION raahi_block_mutation();

-- Commission / settlement ledger (NOT a stored-money wallet). Signed amounts:
-- COMMISSION rows are negative (helper owes Raahi), SETTLEMENT rows are positive.
-- balance = SUM(amount); balance < 0 blocks accepting new jobs.
CREATE TABLE commission_ledger (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    helper_id       UUID NOT NULL REFERENCES users(id),
    job_id          UUID REFERENCES jobs(id),
    entry_type      VARCHAR(12) NOT NULL CHECK (entry_type IN ('COMMISSION','SETTLEMENT')),
    amount          DOUBLE PRECISION NOT NULL,
    job_amount      DOUBLE PRECISION,
    commission_rate DOUBLE PRECISION,
    payment_mode    VARCHAR(20),
    reference       VARCHAR(100),
    recorded_by     UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_commission_job ON commission_ledger(job_id) WHERE entry_type = 'COMMISSION';
CREATE INDEX idx_commission_helper ON commission_ledger(helper_id, created_at DESC);
CREATE TRIGGER trg_commission_ledger_immutable BEFORE UPDATE OR DELETE ON commission_ledger
    FOR EACH ROW EXECUTE FUNCTION raahi_block_mutation();

CREATE TABLE cancellation_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    job_id      UUID NOT NULL REFERENCES jobs(id),
    actor_role  VARCHAR(20) NOT NULL,
    stage       VARCHAR(20) NOT NULL,
    abusive     BOOLEAN NOT NULL,
    excused     BOOLEAN NOT NULL DEFAULT FALSE,
    excused_by  UUID REFERENCES users(id),
    excuse_note VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cancellation_user ON cancellation_events(user_id, created_at DESC);

CREATE TABLE job_ratings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id     UUID NOT NULL UNIQUE REFERENCES jobs(id),
    rater_id   UUID NOT NULL REFERENCES users(id),
    helper_id  UUID NOT NULL REFERENCES users(id),
    stars      INT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment    VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_job_ratings_helper ON job_ratings(helper_id);

CREATE TABLE job_reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id      UUID NOT NULL REFERENCES jobs(id),
    reporter_id UUID NOT NULL REFERENCES users(id),
    kind        VARCHAR(20) NOT NULL CHECK (kind IN ('ISSUE','DISPUTE','COMPLAINT')),
    message     VARCHAR(1000) NOT NULL,
    status      VARCHAR(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED')),
    resolution  VARCHAR(500),
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_job_reports_status ON job_reports(status, created_at DESC);
