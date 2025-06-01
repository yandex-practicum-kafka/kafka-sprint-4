curl -X POST -H "Content-Type: application/json" --data '{
  "name": "postgres-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname": "mydb",
    "schema.include.list": "public",
    "table.include.list": "public.users,public.orders",
    "database.server.name": "dbserver1",
    "slot.name": "debezium_slot",
    "publication.name": "debezium_publication",
    "tombstones.on.delete": "false"
  }
}' http://localhost:8083/connectors