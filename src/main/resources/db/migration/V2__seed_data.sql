-- ============================================================
-- V2: Seed Data — Departments, Districts, Admin User
-- ============================================================

-- ── Departments ───────────────────────────────────────────
INSERT INTO departments (name, code, email, phone) VALUES
    ('Infrastructure & Roads',   'INFRA',   'infra@smartcity.gov',   '+1-555-0101'),
    ('Water & Sewage',           'WATER',   'water@smartcity.gov',   '+1-555-0102'),
    ('Electricity & Power',      'POWER',   'power@smartcity.gov',   '+1-555-0103'),
    ('Sanitation & Waste',       'SANIT',   'sanit@smartcity.gov',   '+1-555-0104'),
    ('Parks & Public Spaces',    'PARKS',   'parks@smartcity.gov',   '+1-555-0105'),
    ('Public Safety',            'SAFETY',  'safety@smartcity.gov',  '+1-555-0106'),
    ('Transport & Traffic',      'TRANS',   'trans@smartcity.gov',   '+1-555-0107'),
    ('General Services',         'GENERAL', 'general@smartcity.gov', '+1-555-0108');

-- ── Districts ─────────────────────────────────────────────
INSERT INTO districts (name, city) VALUES
    ('Downtown',      'Metro City'),
    ('Northside',     'Metro City'),
    ('Southside',     'Metro City'),
    ('East End',      'Metro City'),
    ('West Quarter',  'Metro City'),
    ('Industrial Zone','Metro City'),
    ('Riverside',     'Metro City'),
    ('Uptown',        'Metro City');

-- ── Admin User (password: admin123 — bcrypt hashed) ───────
INSERT INTO users (full_name, email, password, role) VALUES
    ('System Admin', 'admin@smartcity.gov',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i',
     'ADMIN');

-- ── Staff users per department ────────────────────────────
INSERT INTO users (full_name, email, password, role, department_id) VALUES
    ('John Roads',    'infra.staff@smartcity.gov',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i',
     'STAFF', (SELECT id FROM departments WHERE code = 'INFRA')),

    ('Sara Waters',   'water.staff@smartcity.gov',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i',
     'STAFF', (SELECT id FROM departments WHERE code = 'WATER')),

    ('Mike Power',    'power.staff@smartcity.gov',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i',
     'STAFF', (SELECT id FROM departments WHERE code = 'POWER'));

-- All seeded passwords are: admin123
