-- Enforces unique check-in per user per date at database constraint level
-- Prevents race conditions where concurrent requests attempt double-checkins
CREATE TABLE daily_checkins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    checkin_date DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_daily_checkins_user_date UNIQUE (user_id, checkin_date)
);

CREATE INDEX idx_daily_checkins_user_date ON daily_checkins(user_id, checkin_date);
