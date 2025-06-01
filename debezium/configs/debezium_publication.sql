CREATE PUBLICATION debezium_publication FOR TABLE users, orders;
SELECT * FROM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');