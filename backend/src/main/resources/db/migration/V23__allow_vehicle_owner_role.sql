-- V23__allow_vehicle_owner_role.sql
-- Allow VEHICLE_OWNER and OWNER in users.role check constraint

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('DRIVER', 'MECHANIC', 'HELPER', 'ADMIN', 'TRAVEL_AGENT', 'VEHICLE_OWNER', 'OWNER'));
