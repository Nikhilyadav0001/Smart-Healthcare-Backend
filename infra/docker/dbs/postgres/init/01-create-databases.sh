#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
  CREATE USER auth_user WITH PASSWORD '${AUTH_DB_PASSWORD}';
  CREATE DATABASE auth_db OWNER auth_user;

  CREATE USER prediction_user WITH PASSWORD '${PREDICTION_DB_PASSWORD}';
  CREATE DATABASE prediction_db OWNER prediction_user;

  REVOKE CONNECT ON DATABASE auth_db FROM PUBLIC;
  REVOKE CONNECT ON DATABASE prediction_db FROM PUBLIC;
EOSQL
