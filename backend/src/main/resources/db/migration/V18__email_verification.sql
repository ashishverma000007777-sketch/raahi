-- Add email_verified flag to users table to support verified KYC requirements
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
