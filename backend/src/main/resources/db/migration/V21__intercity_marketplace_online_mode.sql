ALTER TABLE users ADD COLUMN IF NOT EXISTS marketplace_online BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_pickup_city VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_users_marketplace_online ON users(marketplace_online, preferred_pickup_city);
