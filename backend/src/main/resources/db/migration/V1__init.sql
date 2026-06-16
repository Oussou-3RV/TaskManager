-- ============================================================
-- V1__init.sql  —  Schéma initial du projet D4
-- ============================================================

-- Extension pour générer des UUID automatiquement
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- Table : users
-- ------------------------------------------------------------
CREATE TABLE users (
                       id            BIGSERIAL     PRIMARY KEY,
                       username      VARCHAR(50)   NOT NULL UNIQUE,
                       password_hash VARCHAR(255)  NOT NULL,
                       role          VARCHAR(20)   NOT NULL CHECK (role IN ('OPERATOR', 'ADMIN', 'AUDITOR')),
                       created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Table : machines
-- ------------------------------------------------------------
CREATE TABLE machines (
                          id                   VARCHAR(50)  PRIMARY KEY,
                          level                VARCHAR(10)  NOT NULL CHECK (level IN ('edge', 'fog', 'cloud')),
                          cpu_available        NUMERIC(6,2) NOT NULL CHECK (cpu_available >= 0),
                          memory_available_mb  INTEGER      NOT NULL CHECK (memory_available_mb >= 0),
                          latency_ms           NUMERIC(8,2) NOT NULL CHECK (latency_ms >= 0),
                          healthy              BOOLEAN      NOT NULL DEFAULT TRUE,
                          updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Table : tasks
-- ------------------------------------------------------------
CREATE TABLE tasks (
                       id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       cpu_required      NUMERIC(6,2) NOT NULL CHECK (cpu_required > 0),
                       memory_required_mb INTEGER     NOT NULL CHECK (memory_required_mb > 0),
                       priority          VARCHAR(10)  NOT NULL DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high')),
                       status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PLACED', 'REJECTED', 'CANCELLED')),
                       submitted_by      BIGINT       NOT NULL REFERENCES users(id),
                       idempotency_key   VARCHAR(100) NOT NULL UNIQUE,
                       created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Table : placement_rules
-- ------------------------------------------------------------
CREATE TABLE placement_rules (
                                 id         BIGSERIAL    PRIMARY KEY,
                                 name       VARCHAR(100) NOT NULL,
                                 policy     VARCHAR(20)  NOT NULL CHECK (policy IN ('least_loaded', 'lowest_latency', 'balanced')),
                                 is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
                                 created_by BIGINT       NOT NULL REFERENCES users(id),
                                 created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Table : placement_decisions
-- ------------------------------------------------------------
CREATE TABLE placement_decisions (
                                     id               BIGSERIAL    PRIMARY KEY,
                                     task_id          UUID         NOT NULL REFERENCES tasks(id),
                                     selected_node_id VARCHAR(50),
                                     policy           VARCHAR(20)  NOT NULL,
                                     score            NUMERIC(10,6),
                                     accepted         BOOLEAN      NOT NULL,
                                     reason           TEXT         NOT NULL,
                                     decided_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Table : audit_events
-- ------------------------------------------------------------
CREATE TABLE audit_events (
                              id          BIGSERIAL    PRIMARY KEY,
                              event_type  VARCHAR(50)  NOT NULL,
                              actor_id    BIGINT       REFERENCES users(id),
                              entity_type VARCHAR(30)  NOT NULL,
                              entity_id   VARCHAR(100) NOT NULL,
                              details     JSONB,
                              created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ------------------------------------------------------------
-- Index utiles pour les performances
-- ------------------------------------------------------------
CREATE INDEX idx_tasks_status         ON tasks(status);
CREATE INDEX idx_tasks_submitted_by   ON tasks(submitted_by);
CREATE INDEX idx_placement_task_id    ON placement_decisions(task_id);
CREATE INDEX idx_audit_actor_id       ON audit_events(actor_id);
CREATE INDEX idx_audit_entity         ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_created_at     ON audit_events(created_at);