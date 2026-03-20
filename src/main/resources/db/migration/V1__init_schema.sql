-- =============================================
-- SmartLift — Initial database schema
-- =============================================

-- Organizations
CREATE TABLE organizations (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(150)    NOT NULL UNIQUE,
    type            VARCHAR(30)     NOT NULL,
    address         VARCHAR(255),
    contact_email   VARCHAR(100),
    contact_phone   VARCHAR(50),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

-- Roles
CREATE TABLE roles (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(50)     NOT NULL UNIQUE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

-- Users
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(100)    NOT NULL UNIQUE,
    email           VARCHAR(150)    NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    organization_id BIGINT          REFERENCES organizations(id),
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

-- User-Role join table
CREATE TABLE user_roles (
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT          NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Lifts
CREATE TABLE lifts (
    id                              BIGSERIAL       PRIMARY KEY,
    serial_number                   VARCHAR(100)    NOT NULL UNIQUE,
    model                           VARCHAR(100)    NOT NULL,
    manufacturer                    VARCHAR(100),
    status                          VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    manufacturer_organization_id    BIGINT          REFERENCES organizations(id),
    service_organization_id         BIGINT          REFERENCES organizations(id),
    management_organization_id      BIGINT          REFERENCES organizations(id),
    created_at                      TIMESTAMP       NOT NULL,
    updated_at                      TIMESTAMP       NOT NULL
);

-- Lift events
CREATE TABLE lift_events (
    id                      BIGSERIAL       PRIMARY KEY,
    lift_id                 BIGINT          NOT NULL REFERENCES lifts(id) ON DELETE CASCADE,
    type                    VARCHAR(30)     NOT NULL,
    event_at                TIMESTAMP       NOT NULL,
    description             VARCHAR(500)    NOT NULL,
    performed_by_user_id    BIGINT          REFERENCES users(id),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL
);

-- Maintenances
CREATE TABLE maintenances (
    id                      BIGSERIAL       PRIMARY KEY,
    lift_id                 BIGINT          NOT NULL REFERENCES lifts(id) ON DELETE CASCADE,
    title                   VARCHAR(255)    NOT NULL,
    description             VARCHAR(1000),
    status                  VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    assigned_technician_id  BIGINT          REFERENCES users(id),
    requested_by_user_id    BIGINT          REFERENCES users(id),
    requested_at            TIMESTAMP       NOT NULL,
    started_at              TIMESTAMP,
    completed_at            TIMESTAMP,
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL
);

-- Documents
CREATE TABLE documents (
    id                      BIGSERIAL       PRIMARY KEY,
    file_name               VARCHAR(255)    NOT NULL,
    file_path               VARCHAR(500)    NOT NULL,
    content_type            VARCHAR(100),
    lift_id                 BIGINT          REFERENCES lifts(id),
    maintenance_id          BIGINT          REFERENCES maintenances(id),
    uploaded_by_user_id     BIGINT          REFERENCES users(id),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL
);

-- =============================================
-- Indexes for frequent queries
-- =============================================
CREATE INDEX idx_lifts_status ON lifts(status);
CREATE INDEX idx_lifts_serial_number ON lifts(serial_number);
CREATE INDEX idx_lift_events_lift_id ON lift_events(lift_id);
CREATE INDEX idx_lift_events_event_at ON lift_events(event_at);
CREATE INDEX idx_maintenances_lift_id ON maintenances(lift_id);
CREATE INDEX idx_maintenances_status ON maintenances(status);
CREATE INDEX idx_documents_lift_id ON documents(lift_id);
CREATE INDEX idx_documents_maintenance_id ON documents(maintenance_id);
CREATE INDEX idx_users_organization_id ON users(organization_id);

-- =============================================
-- Seed default roles
-- =============================================
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('ADMIN',        NOW(), NOW()),
    ('SERVICE',      NOW(), NOW()),
    ('MANAGEMENT',   NOW(), NOW()),
    ('MANUFACTURER', NOW(), NOW());
