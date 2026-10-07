-- Trip Mode: Planned, Active, and Completed Trips with legitimate stops, fuel, and weather.

CREATE TABLE trips (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id              UUID REFERENCES vehicles(id) ON DELETE SET NULL,

    title                   VARCHAR(150) NOT NULL,
    start_location_name     VARCHAR(255) NOT NULL,
    start_lat               DOUBLE PRECISION NOT NULL,
    start_lng               DOUBLE PRECISION NOT NULL,
    dest_location_name      VARCHAR(255) NOT NULL,
    dest_lat                DOUBLE PRECISION NOT NULL,
    dest_lng                DOUBLE PRECISION NOT NULL,

    distance_km             NUMERIC(8, 2) NOT NULL,
    duration_minutes        INT NOT NULL,

    fuel_type               VARCHAR(20),
    tank_full               BOOLEAN DEFAULT false,
    passengers              INT DEFAULT 1,
    preferred_route         VARCHAR(100),

    estimated_fuel_litres   NUMERIC(8, 2),
    estimated_fuel_cost     NUMERIC(10, 2),
    estimated_tolls         NUMERIC(10, 2), -- NULL if toll data provider unavailable
    weather_condition       VARCHAR(100),   -- Real weather e.g. "Clear", "Rain", or NULL if unavailable
    weather_warning         TEXT,            -- Legitimate warning or NULL

    status                  VARCHAR(20) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),

    start_odometer_km       INT,
    end_odometer_km         INT,
    actual_fuel_cost        NUMERIC(10, 2),
    actual_litres           NUMERIC(8, 2),
    stops_visited_count     INT DEFAULT 0,

    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE trip_stops (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id                 UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,

    stop_type               VARCHAR(30) NOT NULL CHECK (stop_type IN ('FUEL', 'DHABA', 'PARKING', 'REST', 'MECHANIC', 'EMERGENCY')),
    name                    VARCHAR(255) NOT NULL,
    lat                     DOUBLE PRECISION NOT NULL,
    lng                     DOUBLE PRECISION NOT NULL,
    distance_km             NUMERIC(8, 2),
    price_per_litre         NUMERIC(7, 2),
    price_diff_per_litre    NUMERIC(7, 2),
    rating                  NUMERIC(2, 1),
    amenities               VARCHAR(255),
    stop_order              INT NOT NULL DEFAULT 0,
    is_visited              BOOLEAN DEFAULT false,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trips_user_created ON trips(user_id, created_at DESC);
CREATE INDEX idx_trips_user_status ON trips(user_id, status);
CREATE INDEX idx_trip_stops_trip ON trip_stops(trip_id, stop_order ASC);
