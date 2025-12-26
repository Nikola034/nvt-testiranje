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
INSERT INTO id_generator (sequence_name, next_val) VALUES ('certificate', 100) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('private_key', 100) ON CONFLICT (sequence_name) DO NOTHING;
INSERT INTO id_generator (sequence_name, next_val) VALUES ('template', 100) ON CONFLICT (sequence_name) DO NOTHING;

-- 1. Admin korisnik
INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
VALUES (1, 'admin@pki.com', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Marko', 'Petrović', 'PKI Corp', 2, 'ADMIN', 'Admin', true, null, null)
    ON CONFLICT (id) DO NOTHING;

-- 2. CA korisnik
INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
VALUES (2, 'ca@organization1.com', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'John', 'Smith', 'Organization1', 1, 'CA', 'CAUser', true, null, null)
    ON CONFLICT (id) DO NOTHING;

-- 3. CA korisnik 2
INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
VALUES (3, 'ca@organization2.com', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Ana', 'Milić', 'Organization2', 1, 'CA', 'CAUser', true, null, null)
    ON CONFLICT (id) DO NOTHING;

-- 4. Obični korisnik
INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
VALUES (4, 'user@company.com', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Stefan', 'Nikolić', 'TestCompany', 0, 'COMMON', 'CommonUser', true, null, null)
    ON CONFLICT (id) DO NOTHING;

-- 5. Test korisnik
INSERT INTO "user" (id, username, password, name, surname, organization, role, authorities, user_type, active, activation_token, token_expiration)
VALUES (5, 'test@test.com', '$2b$12$AKNIH3jXWwaS/Cuxa9/PoO0FPnkK9tZhsdbxI.k0cqlAJr.x.b9kG', 'Test', 'Testović', 'TestOrg', 0, 'COMMON', 'CommonUser', true, null, null)
    ON CONFLICT (id) DO NOTHING;

-- -- Sample Root CA Certificate (would be created through the application)
-- INSERT INTO certificate (id, serial_number, subject_dn, issuer_dn, certificate_type, valid_from, valid_to,
--                          key_usage, extended_key_usage, basic_constraints, revoked, owner_id, issuer_certificate_id,
--                          certificate_data, created_at)
-- VALUES (1, '1', 'CN=PKI Root CA,O=PKI Corp,C=RS', 'CN=PKI Root CA,O=PKI Corp,C=RS', 'ROOT_CA',
--         '2025-01-01 00:00:00', '2035-01-01 00:00:00',
--         'KEY_CERT_SIGN,CRL_SIGN', null, 'CA:TRUE', false, 1, null,
--         '-----BEGIN CERTIFICATE-----
-- MIICXjCCAUYCAQEwDQYJKoZIhvcNAQELBQAwLjEOMAwGA1UEAwwFUm9vdENBMQ8w
-- DQYDVQQKDAZQSUkgQ29ycDELMAkGA1UEBhMCUlMwHhcNMjUwMTAxMDAwMDAwWhcN
-- MzUwMTAxMDAwMDAwWjAuMQ4wDAYDVQQDDAVSb290Q0ExDzANBgNVBAoMBlBLSSBD
-- b3JwMQswCQYDVQQGEwJSUzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAwLt8
-- ... [Certificate data would be here in real implementation]
-- -----END CERTIFICATE-----',
--         '2025-01-01 00:00:00')
--     ON CONFLICT (id) DO NOTHING;
--
-- -- Sample Intermediate CA Certificate for Organization1
-- INSERT INTO certificate (id, serial_number, subject_dn, issuer_dn, certificate_type, valid_from, valid_to,
--                          key_usage, extended_key_usage, basic_constraints, revoked, owner_id, issuer_certificate_id,
--                          certificate_data, created_at)
-- VALUES (2, '2', 'CN=Organization1 CA,O=Organization1,C=RS', 'CN=PKI Root CA,O=PKI Corp,C=RS', 'INTERMEDIATE_CA',
--         '2025-01-01 00:00:00', '2030-01-01 00:00:00',
--         'KEY_CERT_SIGN,CRL_SIGN', null, 'CA:TRUE,pathlen:0', false, 2, 1,
--         '-----BEGIN CERTIFICATE-----
-- MIICYjCCAUoCAQIwDQYJKoZIhvcNAQELBQAwLjEOMAwGA1UEAwwFUm9vdENBMQ8w
-- DQYDVQQKDAZQSUkgQ29ycDELMAkGA1UEBhMCUlMwHhcNMjUwMTAxMDAwMDAwWhcN
-- MzAwMTAxMDAwMDAwWjA4MRcwFQYDVQQDDA5Pcmdhbml6YXRpb24xIENBMRYwFAYD
-- VQQKDAhPcmdhbml6YXRpb24xMQswCQYDVQQGEwJSUzCBnzANBgkqhkiG9w0BAQEF
-- ... [Certificate data would be here in real implementation]
-- -----END CERTIFICATE-----',
--         '2025-01-01 00:00:00')
--     ON CONFLICT (id) DO NOTHING;
--
-- -- Sample certificate template for Organization1
-- INSERT INTO certificate_template (id, template_name, ca_issuer_id, common_name_regex, san_regex, max_ttl_days,
--                                   default_key_usage, default_extended_key_usage, created_by, created_at)
-- VALUES (1, 'Server Certificate Template', 2, '.*\.organization1\.com', '.*\.organization1\.com', 365,
--         'DIGITAL_SIGNATURE,KEY_ENCIPHERMENT', 'SERVER_AUTH', 2, '2025-01-01 00:00:00')
--     ON CONFLICT (id) DO NOTHING;
--
-- -- Sample certificate template for Client certificates
-- INSERT INTO certificate_template (id, template_name, ca_issuer_id, common_name_regex, san_regex, max_ttl_days,
--                                   default_key_usage, default_extended_key_usage, created_by, created_at)
-- VALUES (2, 'Client Certificate Template', 2, '.*@organization1\.com', null, 365,
--         'DIGITAL_SIGNATURE', 'CLIENT_AUTH', 2, '2025-01-01 00:00:00')
--     ON CONFLICT (id) DO NOTHING;

-- Update sequences
UPDATE id_generator SET next_val = 500 WHERE sequence_name = 'user';
UPDATE id_generator SET next_val = 100 WHERE sequence_name = 'certificate';
UPDATE id_generator SET next_val = 100 WHERE sequence_name = 'private_key';
UPDATE id_generator SET next_val = 100 WHERE sequence_name = 'template';