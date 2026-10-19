-- V22__owner_agent_marketplace_refinements.sql
-- Add Owner vs Agent ride separation, pricing, commission settlement, and dispute fields

-- 1. Add ride_type column with CHECK constraint
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS ride_type VARCHAR(20) NOT NULL DEFAULT 'OWNER';
ALTER TABLE intercity_trips DROP CONSTRAINT IF EXISTS intercity_trips_ride_type_check;
ALTER TABLE intercity_trips ADD CONSTRAINT intercity_trips_ride_type_check CHECK (ride_type IN ('OWNER', 'AGENT'));

-- 2. Add financial and commission terms columns
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS customer_fare NUMERIC(10, 2);
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS driver_payout NUMERIC(10, 2);
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS agent_commission NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS payment_recipient VARCHAR(50) DEFAULT 'DRIVER';
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS settlement_responsibility VARCHAR(50) DEFAULT 'NONE';
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS settlement_ref VARCHAR(100);
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS settlement_notes TEXT;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS settlement_submitted_at TIMESTAMPTZ;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS settlement_confirmed_at TIMESTAMPTZ;

-- 3. Add dispute tracking columns
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS disputed_at TIMESTAMPTZ;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS disputed_by UUID REFERENCES users(id);
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS dispute_reason VARCHAR(255);
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS dispute_evidence TEXT;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS dispute_resolution TEXT;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;
ALTER TABLE intercity_trips ADD COLUMN IF NOT EXISTS resolved_by UUID REFERENCES users(id);

-- 4. Extend status check constraint to include settlement and dispute states
ALTER TABLE intercity_trips DROP CONSTRAINT IF EXISTS intercity_trips_status_check;
ALTER TABLE intercity_trips ADD CONSTRAINT intercity_trips_status_check CHECK (status IN (
    'DRAFT',
    'PUBLISHED',
    'ASSIGNED',
    'EN_ROUTE',
    'ARRIVED',
    'IN_PROGRESS',
    'AWAITING_AGENT_SETTLEMENT',
    'SETTLEMENT_SUBMITTED',
    'SETTLEMENT_CONFIRMED',
    'COMPLETED',
    'CANCELLED',
    'DISPUTED'
));

-- 5. Indexes for fast active driver checks and filtering by ride_type
CREATE INDEX IF NOT EXISTS idx_intercity_trips_ride_type ON intercity_trips(ride_type, status);
CREATE INDEX IF NOT EXISTS idx_intercity_trips_driver_active ON intercity_trips(driver_id, status)
    WHERE status IN ('ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'AWAITING_AGENT_SETTLEMENT', 'SETTLEMENT_SUBMITTED', 'SETTLEMENT_CONFIRMED', 'DISPUTED');
