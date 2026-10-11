#!/bin/bash
set -e

mariadb -uroot -p"${MARIADB_ROOT_PASSWORD}" <<-EOSQL
  CREATE DATABASE IF NOT EXISTS profile_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  CREATE USER 'profile_user'@'%' IDENTIFIED BY '${PROFILE_DB_PASSWORD}';
  GRANT ALL PRIVILEGES ON profile_db.* TO 'profile_user'@'%';

  FLUSH PRIVILEGES;
EOSQL
