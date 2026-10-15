-- V20__allow_travel_agent_role.sql
-- Allow TRAVEL_AGENT in users.role check constraint

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('DRIVER', 'MECHANIC', 'HELPER', 'ADMIN', 'TRAVEL_AGENT'));
