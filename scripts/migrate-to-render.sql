-- Migration script for Render PostgreSQL
-- Run this after creating the Render PostgreSQL database
-- Connect to the logistics database as a superuser, then run this script

-- Create read-only user (if not exists)
-- Note: In Render, the database user is created automatically
-- This script assumes you're connected as the superuser
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'reader') THEN
        CREATE USER reader WITH PASSWORD 'readonly';
    END IF;
END
$$;

-- Grant privileges to reader user
GRANT CONNECT ON DATABASE logistics TO reader;
GRANT USAGE ON SCHEMA public TO reader;

-- Apply schema
\i database/init.sql

-- Grant SELECT on all tables to reader
GRANT SELECT ON ALL TABLES IN SCHEMA public TO reader;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO reader;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO reader;

-- Load sample data
\i database/sample-data.sql

-- Verify setup
SELECT
    'Database initialized successfully' as message,
    (SELECT COUNT(*) FROM orders) as total_orders,
    (SELECT COUNT(*) FROM orders WHERE status = 'delivered') as delivered_orders;

-- Show user permissions
SELECT
    r.rolname,
    array_agg(privilege_type) as privileges
FROM information_schema.table_privileges tp
JOIN pg_roles r ON r.rolname = tp.grantee
WHERE tp.table_schema = 'public'
    AND tp.grantee = 'reader'
GROUP BY r.rolname;