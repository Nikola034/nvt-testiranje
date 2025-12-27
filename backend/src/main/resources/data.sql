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
     '$2b$12$7Q6XrU3pN2e6uE0Yz3A9kOrPpYx8n5yD1JtZp7AqH8pJ1X2G9EwWy',
     2, 'ADMIN', true, NULL, NULL, 'Manager')
    ON CONFLICT (id) DO NOTHING;


-- 2. CA korisnik 1
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (2, 'Goran', 'Bijelic', 'tetak',
     '$2b$12$7Q6XrU3pN2e6uE0Yz3A9kOrPpYx8n5yD1JtZp7AqH8pJ1X2G9EwWy',
     1, 'CA', true, NULL, NULL, 'Manager')
    ON CONFLICT (id) DO NOTHING;


-- 3. CA korisnik 2
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (3, 'Miki', 'Bijelic', 'miki',
     '$2b$12$7Q6XrU3pN2e6uE0Yz3A9kOrPpYx8n5yD1JtZp7AqH8pJ1X2G9EwWy',
     0, 'CA', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;


-- 4. Običan korisnik
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (4, 'Stefan', 'Nikolić', 'user@company.com',
     '$2b$12$7Q6XrU3pN2e6uE0Yz3A9kOrPpYx8n5yD1JtZp7AqH8pJ1X2G9EwWy',
     0, 'COMMON', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;


-- 5. Test korisnik
INSERT INTO "user"
(id, name, surname, username, password, role, authorities, active, activation_token, token_expiration, user_type)
VALUES
    (5, 'Test', 'Testović', 'test@test.com',
     '$2b$12$7Q6XrU3pN2e6uE0Yz3A9kOrPpYx8n5yD1JtZp7AqH8pJ1X2G9EwWy',
     0, 'COMMON', true, NULL, NULL, 'Customer')
    ON CONFLICT (id) DO NOTHING;