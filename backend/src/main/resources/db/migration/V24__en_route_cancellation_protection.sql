-- V24__en_route_cancellation_protection.sql
-- En-Route Cancellation Protection System for Raahi Roadside Assistance

-- 1. Update jobs status check constraint to include EN_ROUTE
ALTER TABLE jobs DROP CONSTRAINT IF EXISTS jobs_status_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_status_check
    CHECK (status IN ('PENDING','MATCHED','EN_ROUTE','ARRIVED','IN_PROGRESS','WORK_DONE','COMPLETED','CANCELLED','EXPIRED'));

-- 2. Add en-route and cancellation evidence columns to jobs
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS en_route_at TIMESTAMPTZ;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS en_route_lat DOUBLE PRECISION;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS en_route_lng DOUBLE PRECISION;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS cancellation_reason_code VARCHAR(50);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS cancellation_stage VARCHAR(30);

-- 3. Add reason_code to cancellation_events
ALTER TABLE cancellation_events ADD COLUMN IF NOT EXISTS reason_code VARCHAR(50);

-- 4. Create table for helper travel loss compensation records
CREATE TABLE IF NOT EXISTS job_cancellation_compensations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                  UUID NOT NULL UNIQUE REFERENCES jobs(id),
    helper_id               UUID NOT NULL REFERENCES users(id),
    requester_id            UUID NOT NULL REFERENCES users(id),
    cancellation_stage      VARCHAR(30) NOT NULL,
    reason_code             VARCHAR(50) NOT NULL,
    reason_text             VARCHAR(255),
    en_route_at             TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ NOT NULL,
    start_lat               DOUBLE PRECISION,
    start_lng               DOUBLE PRECISION,
    cancel_lat              DOUBLE PRECISION,
    cancel_lng              DOUBLE PRECISION,
    dest_lat                DOUBLE PRECISION,
    dest_lng                DOUBLE PRECISION,
    one_way_distance_km     DOUBLE PRECISION,
    actual_travel_km        DOUBLE PRECISION,
    return_distance_km      DOUBLE PRECISION,
    evidence_type           VARCHAR(30) NOT NULL,
    estimated_loss_amount   DOUBLE PRECISION NOT NULL,
    approved_amount         DOUBLE PRECISION,
    status                  VARCHAR(30) NOT NULL CHECK (status IN ('ELIGIBLE_PENDING_REVIEW','AUTO_APPROVED','ADMIN_APPROVED','REJECTED','NOT_ELIGIBLE')),
    eligibility_notes       TEXT,
    reviewed_by             UUID REFERENCES users(id),
    reviewed_at             TIMESTAMPTZ,
    review_notes            TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cancellation_comp_helper ON job_cancellation_compensations(helper_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cancellation_comp_status ON job_cancellation_compensations(status, created_at DESC);
