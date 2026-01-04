-- Performance Indexes for Vehicle Search
-- This script uses pg_trgm extension for fast LIKE '%pattern%' searches

-- Step 1: Enable the pg_trgm extension
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Step 2: Create GIN trigram indexes for text search columns
-- These indexes support LIKE/ILIKE with leading wildcards like '%pattern%'

-- Index for vehicle license plate search
CREATE INDEX IF NOT EXISTS idx_vehicle_license_plate_trgm 
ON vehicles USING GIN (license_plate gin_trgm_ops);

-- Index for brand name search
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_name_trgm 
ON vehicle_brands USING GIN (name gin_trgm_ops);

-- Index for model name search  
CREATE INDEX IF NOT EXISTS idx_vehicle_model_name_trgm 
ON vehicle_models USING GIN (name gin_trgm_ops);

-- Step 3: Update table statistics for query planner
ANALYZE vehicles;
ANALYZE vehicle_brands;
ANALYZE vehicle_models;

-- Verify indexes were created
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE indexname LIKE '%trgm%';
