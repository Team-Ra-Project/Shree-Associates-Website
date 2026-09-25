-- ==========================================================================
-- Shree Associates - database schema
-- Run this once to create the database and the enquiries table.
--   mysql -u root -p < schema.sql
-- (If you let Hibernate manage the schema instead via
--  spring.jpa.hibernate.ddl-auto=update, you can skip this file for local
--  dev - it's still useful as the source of truth for production setup,
--  where ddl-auto should be set to "validate" instead of "update".)
-- ==========================================================================

CREATE DATABASE IF NOT EXISTS shree_associates
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE shree_associates;

CREATE TABLE IF NOT EXISTS enquiries (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                  VARCHAR(150)  NOT NULL,
    phone                 VARCHAR(20)   NOT NULL,
    email                 VARCHAR(150)  NOT NULL,
    service               VARCHAR(150)  NULL,
    subject               VARCHAR(150)  NULL,
    message               VARCHAR(2000) NOT NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    whatsapp_status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    whatsapp_message_id   VARCHAR(150)  NULL,
    client_ip             VARCHAR(64)   NULL,

    CONSTRAINT chk_whatsapp_status CHECK (whatsapp_status IN ('PENDING', 'SENT', 'FAILED'))
) ENGINE=InnoDB;

-- Speeds up duplicate-submission checks and admin lookups.
CREATE INDEX idx_enquiries_email_phone_created ON enquiries (email, phone, created_at);
CREATE INDEX idx_enquiries_whatsapp_status ON enquiries (whatsapp_status);
CREATE INDEX idx_enquiries_created_at ON enquiries (created_at);

-- ==========================================================================
-- Optional: a dedicated, least-privilege application DB user instead of
-- using root in production. Replace CHANGE_ME with a strong password and
-- put it only in your environment variables (DB_USERNAME / DB_PASSWORD),
-- never in source control.
-- ==========================================================================
-- CREATE USER IF NOT EXISTS 'shree_app'@'%' IDENTIFIED BY 'CHANGE_ME';
-- GRANT SELECT, INSERT, UPDATE ON shree_associates.enquiries TO 'shree_app'@'%';
-- FLUSH PRIVILEGES;
