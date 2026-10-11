#!/bin/bash
# Phase 0 data-layer health check. Run from anywhere: bash infra/docker/check.sh
export MSYS_NO_PATHCONV=1
pass=0; fail=0

check() {
  local name="$1"; shift
  if "$@" >/dev/null 2>&1; then echo "PASS  $name"; pass=$((pass+1))
  else echo "FAIL  $name"; fail=$((fail+1)); fi
}

healthy() { [ "$(docker inspect -f '{{.State.Health.Status}}' "$1")" = "healthy" ]; }

maria_repl() {
  local out
  out=$(docker exec db-mariadb-replica sh -c 'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD" -e "SHOW REPLICA STATUS\G"')
  echo "$out" | grep -q "Slave_IO_Running: Yes" && echo "$out" | grep -q "Slave_SQL_Running: Yes"
}

mongo_rs() {
  local out
  out=$(docker exec db-mongo1 mongosh --port 27117 --quiet --eval 'rs.status().members.map(m => m.stateStr).join(",")')
  echo "$out" | grep -q PRIMARY && [ "$(echo "$out" | tr ',' '\n' | grep -c SECONDARY)" -eq 2 ]
}

kafka_topics() {
  [ "$(docker exec db-kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1:9092 --list | grep -c .)" -ge 14 ]
}

redis_ping() { docker exec db-redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping 2>/dev/null | grep -q PONG'; }
pg_dbs()     { docker exec db-postgres psql -U postgres -d auth_db -c "select 1" && docker exec db-postgres psql -U postgres -d prediction_db -c "select 1"; }

for c in db-postgres db-redis tool-mailpit db-mariadb-primary db-mariadb-replica db-mongo1 db-mongo2 db-mongo3 db-kafka-1; do
  check "healthy: $c" healthy "$c"
done

check "postgres: auth_db + prediction_db exist" pg_dbs
check "redis: ping" redis_ping
check "mariadb: replication running (IO + SQL)" maria_repl
check "mongo: 1 PRIMARY + 2 SECONDARY" mongo_rs
check "kafka: 14 topics exist" kafka_topics
check "mailpit UI reachable (8025)" curl -sf http://localhost:8025
check "kafka-ui reachable (8089)" curl -sf http://localhost:8089

echo "----"
echo "passed: $pass  failed: $fail"
[ "$fail" -eq 0 ]
