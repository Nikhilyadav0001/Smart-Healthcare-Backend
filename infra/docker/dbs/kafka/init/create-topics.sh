#!/bin/sh
set -e
BOOTSTRAP=kafka-1:9092
KT=/opt/kafka/bin/kafka-topics.sh

create() {
  $KT --bootstrap-server $BOOTSTRAP --create --if-not-exists \
    --topic "$1" --partitions "$2" --replication-factor 1 \
    --config min.insync.replicas=1
}

TOPICS="user.registered user.profile.created user.profile.failed health.record.created prediction.completed notification.requested user.deleted"

for t in $TOPICS; do
  create "$t" 3
  create "$t.dlq" 1
done

echo "--- topics ---"
$KT --bootstrap-server $BOOTSTRAP --list
