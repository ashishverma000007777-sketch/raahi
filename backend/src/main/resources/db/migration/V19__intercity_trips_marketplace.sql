-- V19__intercity_trips_marketplace.sql
-- Raahi Intercity Trips Marketplace (Travel Agents <-> Drivers)

-- 1. Extend users table for Travel Agent profile attributes
ALTER TABLE users ADD COLUMN IF NOT EXISTS agency_name VARCHAR(150);
ALTER TABLE users ADD COLUMN IF NOT EXISTS agency_gst VARCHAR(50);

-- 2. Intercity Trips table
CREATE TABLE IF NOT EXISTS intercity_trips (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id                UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    driver_id               UUID REFERENCES users(id) ON DELETE SET NULL,

    pickup_city             VARCHAR(100) NOT NULL,
    dest_city               VARCHAR(100) NOT NULL,
    pickup_address          VARCHAR(255) NOT NULL,
    dest_address            VARCHAR(255) NOT NULL,
    pickup_lat              DOUBLE PRECISION NOT NULL,
    pickup_lng              DOUBLE PRECISION NOT NULL,
    dest_lat                DOUBLE PRECISION NOT NULL,
    dest_lng                DOUBLE PRECISION NOT NULL,

    departure_time          TIMESTAMPTZ NOT NULL,
    trip_type               VARCHAR(20) NOT NULL DEFAULT 'ONE_WAY' CHECK (trip_type IN ('ONE_WAY', 'ROUND_TRIP')),
    return_time             TIMESTAMPTZ,
    vehicle_category        VARCHAR(50) NOT NULL,
    estimated_distance_km   NUMERIC(8, 2),
    estimated_duration_min  INT,

    fare                    NUMERIC(10, 2) NOT NULL CHECK (fare > 0),
    payment_mode            VARCHAR(30) NOT NULL DEFAULT 'DIRECT_CASH',
    payment_status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID', 'SETTLED', 'FAILED')),
    cancellation_terms      VARCHAR(255),
    special_instructions    TEXT,

    pickup_otp              VARCHAR(6),
    pickup_otp_attempts     INT NOT NULL DEFAULT 0,
    completion_otp          VARCHAR(6),
    completion_otp_attempts INT NOT NULL DEFAULT 0,

    status                  VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),

    assigned_at             TIMESTAMPTZ,
    arrived_at              TIMESTAMPTZ,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancelled_by            UUID REFERENCES users(id),
    cancel_reason           VARCHAR(255),

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Intercity Trip Applications table
CREATE TABLE IF NOT EXISTS intercity_trip_applications (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id                 UUID NOT NULL REFERENCES intercity_trips(id) ON DELETE CASCADE,
    driver_id               UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('APPLIED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    driver_notes            VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_intercity_trip_driver_app UNIQUE (trip_id, driver_id)
);

-- 4. Indexes for optimal query performance
CREATE INDEX IF NOT EXISTS idx_intercity_trips_search ON intercity_trips(pickup_city, dest_city, departure_time, status);
CREATE INDEX IF NOT EXISTS idx_intercity_trips_agent ON intercity_trips(agent_id, status);
CREATE INDEX IF NOT EXISTS idx_intercity_trips_driver ON intercity_trips(driver_id, status);
CREATE INDEX IF NOT EXISTS idx_intercity_trips_status ON intercity_trips(status, departure_time);
CREATE INDEX IF NOT EXISTS idx_intercity_apps_trip ON intercity_trip_applications(trip_id, status);
CREATE INDEX IF NOT EXISTS idx_intercity_apps_driver ON intercity_trip_applications(driver_id, status);
