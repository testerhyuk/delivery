docker exec -it postgres psql -U hyuk -d delivery_eum -c "
SELECT pg_drop_replication_slot(slot_name)
FROM pg_replication_slots
WHERE slot_name IN (
    'order_outbox_slot',
    'pay_outbox_slot',
    'rider_outbox_slot',
    'seller_outbox_slot'
);"

docker compose up -d --force-recreate connector-setup