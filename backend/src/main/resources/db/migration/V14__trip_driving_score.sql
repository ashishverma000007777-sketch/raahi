-- Add driving_score column to trips for driving telemetry and safety scoring
ALTER TABLE trips ADD COLUMN IF NOT EXISTS driving_score INT;
