#!/bin/sh
set -eu

# Executed only when PostgreSQL initializes an empty data volume.
psql --username "$POSTGRES_USER" --dbname postgres --set ON_ERROR_STOP=1 \
  --set db_password="$TMC_DB_PASSWORD" <<'SQL'
CREATE ROLE identity_app LOGIN PASSWORD :'db_password';
CREATE ROLE qr_app LOGIN PASSWORD :'db_password';
CREATE ROLE gate_station_app LOGIN PASSWORD :'db_password';
CREATE ROLE access_event_app LOGIN PASSWORD :'db_password';
CREATE ROLE attendance_report_app LOGIN PASSWORD :'db_password';
CREATE ROLE hr_integration_app LOGIN PASSWORD :'db_password';

CREATE DATABASE identity_user_db OWNER identity_app;
CREATE DATABASE qr_db OWNER qr_app;
CREATE DATABASE gate_station_db OWNER gate_station_app;
CREATE DATABASE access_event_db OWNER access_event_app;
CREATE DATABASE attendance_report_db OWNER attendance_report_app;
CREATE DATABASE hr_integration_db OWNER hr_integration_app;

REVOKE CONNECT ON DATABASE identity_user_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE qr_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE gate_station_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE access_event_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE attendance_report_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE hr_integration_db FROM PUBLIC;
SQL
