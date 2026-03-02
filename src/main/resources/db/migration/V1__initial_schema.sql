-- ============================================================
-- V1: Initial Schema — Citizen Issue Processing System
-- ============================================================

-- ── Departments (who handles which category) ─────────────
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    code        VARCHAR(20)  NOT NULL UNIQUE,  -- e.g. INFRA, WATER, POWER
    email       VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Users (citizens + staff) ──────────────────────────────
CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    full_name    VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'CITIZEN',  -- CITIZEN | STAFF | ADMIN
    department_id BIGINT REFERENCES departments(id),
    phone        VARCHAR(20),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Districts / Zones ─────────────────────────────────────
CREATE TABLE districts (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    city       VARCHAR(100) NOT NULL DEFAULT 'Metro City',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── Issues (core table) ───────────────────────────────────
CREATE TABLE issues (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(200)  NOT NULL,
    description         TEXT          NOT NULL,
    category            VARCHAR(30)   NOT NULL,   -- see IssueCategory enum
    status              VARCHAR(30)   NOT NULL DEFAULT 'OPEN',
    priority            VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',

    -- Location
    address             VARCHAR(300),
    latitude            DECIMAL(10,7),
    longitude           DECIMAL(10,7),
    district_id         BIGINT REFERENCES districts(id),

    -- Ownership
    reported_by_id      BIGINT NOT NULL REFERENCES users(id),
    assigned_dept_id    BIGINT REFERENCES departments(id),
    assigned_to_id      BIGINT REFERENCES users(id),

    -- Tracking
    resolved_at         TIMESTAMP,
    resolution_notes    TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── Status History (audit trail) ─────────────────────────
CREATE TABLE issue_status_history (
    id              BIGSERIAL PRIMARY KEY,
    issue_id        BIGINT      NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    old_status      VARCHAR(30),
    new_status      VARCHAR(30) NOT NULL,
    changed_by_id   BIGINT      NOT NULL REFERENCES users(id),
    notes           TEXT,
    changed_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Issue Media (photos attached to reports) ─────────────
CREATE TABLE issue_media (
    id         BIGSERIAL PRIMARY KEY,
    issue_id   BIGINT       NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    url        VARCHAR(500) NOT NULL,
    media_type VARCHAR(20)  NOT NULL DEFAULT 'IMAGE',
    uploaded_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Notifications log ─────────────────────────────────────
CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    issue_id     BIGINT       REFERENCES issues(id),
    recipient_id BIGINT       NOT NULL REFERENCES users(id),
    type         VARCHAR(30)  NOT NULL,   -- EMAIL | SMS | IN_APP
    subject      VARCHAR(200),
    message      TEXT         NOT NULL,
    sent_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ── Indexes for common queries ────────────────────────────
CREATE INDEX idx_issues_status      ON issues(status);
CREATE INDEX idx_issues_category    ON issues(category);
CREATE INDEX idx_issues_district    ON issues(district_id);
CREATE INDEX idx_issues_dept        ON issues(assigned_dept_id);
CREATE INDEX idx_issues_reporter    ON issues(reported_by_id);
CREATE INDEX idx_issues_created     ON issues(created_at DESC);
CREATE INDEX idx_status_history_issue ON issue_status_history(issue_id);

-- ── Trigger: auto-update updated_at ───────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER issues_updated_at
    BEFORE UPDATE ON issues
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
