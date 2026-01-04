-- -- 1. Admin korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (1, 'admin', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Marko', 'Petrović', 'TechCorp', 2, 'ADMIN,USER', 'Admin', true, null, null);
--
-- -- 2. Regularni korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (2, 'john', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'John', 'Doe', 'MusicInc', 0, 'USER', 'User', true, null, null);
--
-- -- 3. Premium korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (3, 'ana.milic', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Ana', 'Milić', 'SoundWave', 0, 'USER,PREMIUM', 'PremiumUser', true, null, null);
--
-- -- 4. Moderator
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (4, 'stefan.mod', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Stefan', 'Nikolić', 'MusicPlatform', 1, 'MODERATOR,USER', 'Moderator', true, null, null);
--
-- -- 5. Test korisnik
-- INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
-- VALUES (5, 'test.user', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Test', 'Testović', 'TestOrg', 1, 'USER', 'User', true, null, null);
--
-- UPDATE public.id_generator
-- SET next_val = 300
-- WHERE sequence_name = 'user';

-- Create id_generator table if it doesn't exist
CREATE TABLE IF NOT EXISTS id_generator (
                                            sequence_name VARCHAR(255) PRIMARY KEY,
    next_val BIGINT NOT NULL
    );

-- Initialize sequences
INSERT INTO id_generator (sequence_name, next_val) VALUES ('user', 300) ON CONFLICT (sequence_name) DO NOTHING;

-- 1. ADMIN
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (1, 'Admin', 'Admin', 'admin',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     2, 'ADMIN', true, NULL, NULL, 'Manager')
    ON CONFLICT (id) DO NOTHING;


-- 2. CA korisnik 1
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (2, 'Goran', 'Bijelic', 'tetak',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     1, 'MANAGER', true, NULL, NULL, 'Manager')
    ON CONFLICT (id) DO NOTHING;


-- 3. CA korisnik 2
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (3, 'Miki', 'Bijelic', 'miki',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     0, 'CUSTOMER', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;


-- 4. Običan korisnik
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (4, 'Stefan', 'Nikolić', 'user@company.com',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     0, 'CUSTOMER', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;


-- 5. Test korisnik
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (5, 'Test', 'Testović', 'test@test.com',
     '$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku',
     0, 'CUSTOMER', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;

-- Countries
INSERT INTO countries (id, name) VALUES (1, 'Serbia');
INSERT INTO countries (id, name) VALUES (2, 'Croatia');
INSERT INTO countries (id, name) VALUES (3, 'Germany');
INSERT INTO countries (id, name) VALUES (4, 'Austria');
INSERT INTO countries (id, name) VALUES (5, 'Hungary');

-- Serbia cities
INSERT INTO cities (id, name, country_id) VALUES (1, 'Belgrade', 1);
INSERT INTO cities (id, name, country_id) VALUES (2, 'Novi Sad', 1);
INSERT INTO cities (id, name, country_id) VALUES (3, 'Niš', 1);
INSERT INTO cities (id, name, country_id) VALUES (4, 'Kragujevac', 1);
INSERT INTO cities (id, name, country_id) VALUES (5, 'Subotica', 1);

-- Croatia cities
INSERT INTO cities (id, name, country_id) VALUES (6, 'Zagreb', 2);
INSERT INTO cities (id, name, country_id) VALUES (7, 'Split', 2);
INSERT INTO cities (id, name, country_id) VALUES (8, 'Rijeka', 2);

-- Germany cities
INSERT INTO cities (id, name, country_id) VALUES (9, 'Berlin', 3);
INSERT INTO cities (id, name, country_id) VALUES (10, 'Munich', 3);
INSERT INTO cities (id, name, country_id) VALUES (11, 'Hamburg', 3);

-- Austria cities
INSERT INTO cities (id, name, country_id) VALUES (12, 'Vienna', 4);
INSERT INTO cities (id, name, country_id) VALUES (13, 'Salzburg', 4);

-- Hungary cities
INSERT INTO cities (id, name, country_id) VALUES (14, 'Budapest', 5);
INSERT INTO cities (id, name, country_id) VALUES (15, 'Debrecen', 5);

-- Vehicle Brands
INSERT INTO vehicle_brands (id, name) VALUES (1, 'Mercedes-Benz') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (2, 'Volkswagen') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (3, 'Ford') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (4, 'Iveco') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (5, 'MAN') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (6, 'Renault') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (7, 'Fiat') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (8, 'Peugeot') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (9, 'Citroen') ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_brands (id, name) VALUES (10, 'Toyota') ON CONFLICT (id) DO NOTHING;

-- Mercedes-Benz models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (1, 'Sprinter', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (2, 'Vito', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (3, 'Citan', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (4, 'Actros', 1) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (5, 'Atego', 1) ON CONFLICT (id) DO NOTHING;

-- Volkswagen models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (6, 'Crafter', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (7, 'Transporter', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (8, 'Caddy', 2) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (9, 'Amarok', 2) ON CONFLICT (id) DO NOTHING;

-- Ford models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (10, 'Transit', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (11, 'Transit Custom', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (12, 'Transit Connect', 3) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (13, 'Ranger', 3) ON CONFLICT (id) DO NOTHING;

-- Iveco models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (14, 'Daily', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (15, 'Eurocargo', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (16, 'Stralis', 4) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (17, 'S-Way', 4) ON CONFLICT (id) DO NOTHING;

-- MAN models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (18, 'TGE', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (19, 'TGL', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (20, 'TGM', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (21, 'TGS', 5) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (22, 'TGX', 5) ON CONFLICT (id) DO NOTHING;

-- Renault models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (23, 'Master', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (24, 'Trafic', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (25, 'Kangoo', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (26, 'T', 6) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (27, 'D', 6) ON CONFLICT (id) DO NOTHING;

-- Fiat models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (28, 'Ducato', 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (29, 'Scudo', 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (30, 'Doblo', 7) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (31, 'Fiorino', 7) ON CONFLICT (id) DO NOTHING;

-- Peugeot models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (32, 'Boxer', 8) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (33, 'Expert', 8) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (34, 'Partner', 8) ON CONFLICT (id) DO NOTHING;

-- Citroen models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (35, 'Jumper', 9) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (36, 'Jumpy', 9) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (37, 'Berlingo', 9) ON CONFLICT (id) DO NOTHING;

-- Toyota models
INSERT INTO vehicle_models (id, name, brand_id) VALUES (38, 'Hilux', 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (39, 'Proace', 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO vehicle_models (id, name, brand_id) VALUES (40, 'Proace City', 10) ON CONFLICT (id) DO NOTHING;
