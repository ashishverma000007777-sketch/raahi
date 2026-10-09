-- Persist OSM mechanic shop leads separately from verified Raahi mechanic accounts.
CREATE TABLE osm_mechanic_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    osm_type VARCHAR(12) NOT NULL CHECK (osm_type IN ('node','way','relation')),
    osm_id BIGINT NOT NULL,
    shop_name VARCHAR(240) NOT NULL,
    phone VARCHAR(100),
    website TEXT,
    address TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW','CONTACTED','VERIFIED','REJECTED')),
    osm_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (osm_type, osm_id)
);
CREATE INDEX idx_osm_mechanic_leads_status ON osm_mechanic_leads(status, shop_name);
