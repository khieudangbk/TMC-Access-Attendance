-- Central PostgreSQL cluster: one schema per owning service.
-- Cross-schema foreign keys and joins are forbidden (see docs/05-database/schema.md).
CREATE SCHEMA IF NOT EXISTS ics;
CREATE SCHEMA IF NOT EXISTS aas;
CREATE SCHEMA IF NOT EXISTS gss;
CREATE SCHEMA IF NOT EXISTS ris;
CREATE SCHEMA IF NOT EXISTS ads;

COMMENT ON SCHEMA ics IS 'ICS - Identity and Credential Service';
COMMENT ON SCHEMA aas IS 'AAS - Access and Attendance Service (central)';
COMMENT ON SCHEMA gss IS 'GSS - Gate Station Service';
COMMENT ON SCHEMA ris IS 'RIS - Reporting and Integration Service';
COMMENT ON SCHEMA ads IS 'ADS - Administration and Audit Service';
