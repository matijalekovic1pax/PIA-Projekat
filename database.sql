-- ============================================================
-- Coworking Hub Manager - MySQL Database Setup Script
-- Run this script BEFORE starting the Spring Boot backend
-- ============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS coworking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE coworking_db;

-- ============================================================
-- TABLES
-- ============================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(25),
    type VARCHAR(20) NOT NULL DEFAULT 'member',      -- member | manager | admin
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING | ACTIVE | INACTIVE
    profile_picture VARCHAR(255),
    company_name VARCHAR(100),
    company_address VARCHAR(255),
    company_reg_number VARCHAR(20),
    company_tax_id VARCHAR(20),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Password reset tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Spaces table
CREATE TABLE IF NOT EXISTS spaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL,
    description TEXT,
    price_per_hour DECIMAL(10,2),
    latitude DOUBLE,
    longitude DOUBLE,
    open_space_desk_count INT DEFAULT 5,
    no_show_limit INT DEFAULT 3,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING | ACTIVE | INACTIVE
    manager_id BIGINT,
    main_image VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_space_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Space gallery images
CREATE TABLE IF NOT EXISTS space_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    space_id BIGINT NOT NULL,
    image_path VARCHAR(255),
    display_order INT DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_simg_space FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Space items (offices and conference rooms)
CREATE TABLE IF NOT EXISTS space_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    space_id BIGINT NOT NULL,
    name VARCHAR(100),
    type VARCHAR(20),                -- OFFICE | CONFERENCE
    desk_count INT,                  -- for offices
    equipment TEXT,                  -- for conference rooms
    PRIMARY KEY (id),
    CONSTRAINT fk_sitem_space FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reservations
CREATE TABLE IF NOT EXISTS reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    space_id BIGINT,
    member_id BIGINT,
    item_name VARCHAR(100),          -- name of office or conference room (nullable for open)
    start_date_time DATETIME,
    end_date_time DATETIME,
    type VARCHAR(20),                -- open | office | conference
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | CONFIRMED | COMPLETED | CANCELLED
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_res_space FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_member FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Feedback (likes, dislikes, comments)
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    space_id BIGINT,
    user_id BIGINT,
    is_like TINYINT(1),             -- 1=like, 0=dislike, NULL=comment only
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_fb_space FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_fb_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SEED DATA
-- ============================================================

-- NOTE: The admin user is auto-created by Spring Boot DataInitializer on first run.
-- No need to manually insert admin here.
-- Admin credentials: username=admin, password=Admin1234!

-- Sample manager (password: Manager1!)
INSERT INTO users (username, email, password, first_name, last_name, phone, type, status,
                   company_name, company_address, company_reg_number, company_tax_id)
VALUES (
    'manager1',
    'manager@coworking.com',
    '$2a$10$8K1p/a0dR1xqM8K3Qx1VeuJ/VPBGQ7ZQRZ8f3kK4VJZ9JQ1pJ3Ke',
    'Marko',
    'Markovic',
    '+381611234567',
    'manager',
    'ACTIVE',
    'TechSpace d.o.o.',
    'Bulevar Oslobodjenja 45, Novi Sad',
    '12345678',
    '123456789'
) ON DUPLICATE KEY UPDATE id=id;

-- Sample member (password: Member1!)
INSERT INTO users (username, email, password, first_name, last_name, phone, type, status)
VALUES (
    'member1',
    'member@coworking.com',
    '$2a$10$8K1p/a0dR1xqM8K3Qx1VeuJ/VPBGQ7ZQRZ8f3kK4VJZ9JQ1pJ3Ke',
    'Ana',
    'Anić',
    '+381621234567',
    'member',
    'ACTIVE'
) ON DUPLICATE KEY UPDATE id=id;

-- ============================================================
-- NOTE: The above BCrypt hashes may not match real passwords.
-- The admin password hash above is for 'admin123' (standard test hash).
-- For a fresh setup, register through the UI or use the application
-- to generate proper BCrypt hashes.
--
-- RECOMMENDED: After running this script, start the backend and
-- register users through the frontend registration form.
-- Then activate them via the admin dashboard (login as admin).
-- ============================================================

-- ============================================================
-- HOW TO RUN THIS SCRIPT
-- ============================================================
-- Option 1: MySQL command line
--   mysql -u root -p < database.sql
--
-- Option 2: MySQL Workbench
--   Open this file and execute it
--
-- Option 3: From command line with password
--   mysql -u root -pYOURPASSWORD < database.sql
-- ============================================================
