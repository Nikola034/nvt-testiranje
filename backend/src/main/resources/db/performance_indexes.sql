-- Performance indexes for vehicle search
-- Run this script manually in PostgreSQL to create optimized indexes

-- Enable the pg_trgm extension for pattern matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Functional indexes with LOWER() for case-insensitive exact/prefix matching
CREATE INDEX IF NOT EXISTS idx_vehicle_license_plate_lower ON vehicles (LOWER(license_plate));
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_name_lower ON vehicle_brands (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_vehicle_model_name_lower ON vehicle_models (LOWER(name));

-- GIN trigram indexes for LIKE '%pattern%' searches (much faster for substring search)
CREATE INDEX IF NOT EXISTS idx_vehicle_license_plate_trgm ON vehicles USING GIN (license_plate gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_name_trgm ON vehicle_brands USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_vehicle_model_name_trgm ON vehicle_models USING GIN (name gin_trgm_ops);

-- User table indexes for authentication (critical for JWT filter performance)
CREATE INDEX IF NOT EXISTS idx_user_username ON "user" (username);
CREATE INDEX IF NOT EXISTS idx_user_username_lower ON "user" (LOWER(username));

-- Token table indexes
CREATE INDEX IF NOT EXISTS idx_token_access_token ON token (access_token);
CREATE INDEX IF NOT EXISTS idx_token_refresh_token ON token (refresh_token);
CREATE INDEX IF NOT EXISTS idx_token_user_id ON token (user_id);

-- Registration request indexes
CREATE INDEX IF NOT EXISTS idx_registration_request_status ON registration_requests (status);
CREATE INDEX IF NOT EXISTS idx_registration_request_created_at ON registration_requests (created_at);
CREATE INDEX IF NOT EXISTS idx_registration_request_owner ON registration_requests (owner_id);

-- Analyze tables to update statistics
ANALYZE vehicles;
ANALYZE vehicle_brands;
ANALYZE vehicle_models;
ANALYZE "user";
ANALYZE token;
ANALYZE registration_requests;
