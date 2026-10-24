-- V25__compensation_audit_and_settlement_separation.sql
-- Enforces policy auditability and separates admin compensation approval from wallet/payout settlement

-- 1. Add audit inputs and policy version to job_cancellation_compensations
ALTER TABLE job_cancellation_compensations
    ADD COLUMN IF NOT EXISTS policy_version VARCHAR(32) NOT NULL DEFAULT '2026.10-admin-v1',
    ADD COLUMN IF NOT EXISTS base_callout_fee DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS base_rate_per_km DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS return_factor DOUBLE PRECISION;

-- 2. Add explicit settlement / payout tracking columns
-- Default is strictly 'UNPAID'. Admin approval alters approval status, not payout status.
ALTER TABLE job_cancellation_compensations
    ADD COLUMN IF NOT EXISTS payout_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID'
        CHECK (payout_status IN ('UNPAID', 'PENDING_PAYOUT', 'PAID', 'FAILED')),
    ADD COLUMN IF NOT EXISTS payout_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS payout_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS payout_notes TEXT;

-- 3. Index on payout_status for reconciliation and future payout processing
CREATE INDEX IF NOT EXISTS idx_cancellation_comp_payout
    ON job_cancellation_compensations(payout_status, created_at DESC);
