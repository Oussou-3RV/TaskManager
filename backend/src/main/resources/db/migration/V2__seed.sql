-- ============================================================
-- V2__seed.sql  —  Données de test initiales
-- ============================================================

-- ------------------------------------------------------------
-- Utilisateurs (3 rôles)
-- Mot de passe pour tous : "password123"
-- hashé avec BCrypt (cost=10)
-- ------------------------------------------------------------
INSERT INTO users (username, password_hash, role) VALUES
                                                      ('admin',    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN'),
                                                      ('operateur','$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'OPERATOR'),
                                                      ('auditeur', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'AUDITOR');

-- ------------------------------------------------------------
-- Machines (correspond exactement à NodeState du moteur Python)
-- ------------------------------------------------------------
INSERT INTO machines (id, level, cpu_available, memory_available_mb, latency_ms, healthy) VALUES
                                                                                              ('edge-01',  'edge',  1.0,  512,  8.0,  TRUE),
                                                                                              ('edge-02',  'edge',  2.0,  1024, 10.0, TRUE),
                                                                                              ('fog-01',   'fog',   4.0,  2048, 20.0, TRUE),
                                                                                              ('fog-02',   'fog',   6.0,  4096, 25.0, TRUE),
                                                                                              ('cloud-01', 'cloud', 12.0, 8192, 70.0, TRUE),
                                                                                              ('cloud-02', 'cloud', 16.0, 16384,80.0, FALSE);

-- ------------------------------------------------------------
-- Règles de placement (créées par l'admin)
-- ------------------------------------------------------------
INSERT INTO placement_rules (name, policy, is_active, created_by) VALUES
                                                                      ('Règle par défaut — balanced',        'balanced',        TRUE,  1),
                                                                      ('Règle basse latence',                'lowest_latency',  FALSE, 1),
                                                                      ('Règle charge minimale',              'least_loaded',    FALSE, 1);