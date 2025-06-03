### ���������� ��������� ������ � ������� Debezium � Grafana  

#### ��������� �������:  

������ ������������ ��� ����������� ������ �� ���� ������ PostgreSQL � �������������� ����������� �����-��������, ���������� ����������� Debezium, Prometheus � Grafana. �������� ���� � ������������ ��������� � ������ � ������������ ���� ��������� ����� ������ � ������ ��������.  

```

```

#### �������� ���������� �������:  

- ����� ������������:  
  - [.env](.env): ���� ��������� ��� �������� ����������, ����������� ��� ��������� Docker-�����������.  
  - [docker-compose.yml](docker-compose.yml): �������� ����, ����������� ���������� ��� ���������� ������� � ������� Docker Compose. �������� ������������ ��� PostgreSQL, Kafka, Zookeeper, Debezium Connector, Prometheus � Grafana.  
  
```
version: '3.8'

services:

  zookeeper:
    image: quay.io/debezium/zookeeper:${DEBEZIUM_VERSION}
    ports:
     - 2181:2181
     - 2888:2888
     - 3888:3888

  kafka:
    image: quay.io/debezium/kafka:${DEBEZIUM_VERSION}
    ports:
     - 9092:9092
    links:
     - zookeeper
    environment:
     - ZOOKEEPER_CONNECT=zookeeper:2181

  postgres:
    image: postgres:13-alpine
    container_name: postgres
    environment:
      POSTGRES_DB: testDB
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Password!
    ports:
      - "5432:5432"
    volumes:
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql
      - ./postgresql.conf:/etc/postgresql/postgresql.conf
    command: ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  data-generator:
    image: python:3.11-slim
    container_name: data_generator
    restart: always
    volumes:
      - ./generate_data.py:/app/generate_data.py
    command: >
      bash -c "
        pip install psycopg2-binary && 
        while true; do 
          python /app/generate_data.py; 
          sleep 1;
        done
      "
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_DB: testDB
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Password!

  connect:
    build:
      context: debezium-jmx-exporter
      args:
        DEBEZIUM_VERSION: ${DEBEZIUM_VERSION}
        JMX_AGENT_VERSION: 0.15.0
    ports:
     - 8083:8083
     - 1976:1976
    links:
     - kafka
     - postgres
    environment:
     - BOOTSTRAP_SERVERS=kafka:9092
     - GROUP_ID=1
     - CONFIG_STORAGE_TOPIC=my_connect_configs
     - OFFSET_STORAGE_TOPIC=my_connect_offsets
     - STATUS_STORAGE_TOPIC=my_connect_statuses
     - KAFKA_OPTS=-javaagent:/kafka/etc/jmx_prometheus_javaagent.jar=8080:/kafka/etc/config.yml
     - JMXHOST=localhost
     - JMXPORT=1976

  prometheus:
    build:
      context: debezium-prometheus
      args:
        PROMETHEUS_VERSION: v2.43.0
    ports:
     - 9090:9090
    links:
     - connect

  grafana:
    build:
      context: debezium-grafana
      args:
        GRAFANA_VERSION: 9.4.7
    ports:
     - 3000:3000
    links:
     - prometheus
    environment:
     - DS_PROMETHEUS=prometheus
```
  
  - [postgresql.conf](postgresql.conf): �������������� ������������ PostgreSQL:

```
# �������� ���������� ���������� (����������� ��� Debezium)
wal_level = logical

# ��������� ����������� �� ���� �����������
listen_addresses = '*'

# �������������� ��������������� ��������� ��� ���������� (�����������)
max_replication_slots = 10
max_wal_senders = 10
wal_keep_size = 64MB
```
  
- ������ ��������� ������������� (DDL ��� ������ ���������� (������������� ��), 
������ � �� � �� ���� �������, ������ ������� Python 
(`data-generator`, ��. [docker-compose.yml](docker-compose.yml))).  

��� ������� ��������� ������ ��. � [generate_data.py](generate_data.py). DDL ��. � [init-db.sql](init-db.sql):
```
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),
    product_name VARCHAR(100),
    quantity INT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- ������������� (������� � �������� ������):
  - [inventory.sql](inventory.sql): ��� �� ����� ��� ��������������� ����� ������������ ������������, ������, ���������� SQL-������� ��� �������� � ���������� ���� ������ ��������� �������. �������� ������� ��� ���������, �������� � �������, ������� ����� ������������� Debezium (� ������� ������ ������������ ��������� ������ ��������� �������� � �������� ��������������� ������ ��� ������ ���������� PostgreSQL).  

#### �����������:

- `debezium-grafana/`:   
  - �������� ����� ������������ ��� Grafana, � ��� �����:  
    - [dashboard.yml](debezium-grafana/dashboard.yml): ������������ ��� ��������� ������ ����������� � Grafana.  
    - [datasource.yml](debezium-grafana/datasource.yml): ���� ��� ����������� ���������� ������ � Grafana.  
    - [debezium-dashboard.json](debezium-grafana/debezium-dashboard.json): JSON-���� � ����������� �������� ��� ������������ ������, ���������� �� Debezium.  

�������� ���������� ����� [debezium-dashboard.json](debezium-grafana/debezium-dashboard.json):

1. ���������� ��������  
   - `title`: �������� ��������, ��������, "Debezium Dashboard". ��� ��, ��� ����� ������������ � ���������� Grafana.  
   - `uid`: ���������� ������������� ��������, ������������ Grafana ��� ������ �� �������. ������ ���� ���������� � ������ �������� Grafana.  
   - `version`: ������ ��������, ������������ ��� ������������ ���������.  
   - `schemaVersion`: ������ �����, ������������ � JSON-����� ��� ������������� � ���������� �������� Grafana.  

2. ����� ���������  
   - `templating`: ���������� ����������, ������� ����� �������������� � ��������. ��������, ���������� ����� �������������� ��� ���������� ������ �� �������, ������� ��� ������ ���������.  
   - `annotations`: ��������� ��������� ��������� �� �������, ��������, ��� ����������� �������, ����� ��� ������������� ����� ������ ���������� ��� ���� �������.  

3. ������ (Panels)  
   - ������ ������ �������� �� ������������ ���������� ������� ��� ������ ������. ��� ����� ���� ������ �����: �������, �������, ����������� � �. �.  
   - `type`: ��� ������ (��������, "graph", "table", "singlestat").  
   - `title`: ��������� ������, ������������ �� �������.  
   - `datasource`: �������� ������, � ������� ������� ������ (��������, Prometheus ��� Loki).  
   - `targets`: ���������� �������, ������� ������ ���� ��������� ��� ���� ������. ����� ����������� ������� � ��������� ������.  
   - `options`: ���������, ����������� ��� ���� ������ (��������, ��������� ������������, �����, ������� � ��.).  

4. ��������� ��������  
   - `xaxis` � `yaxis`: ������������ ���� �������, ����� ��� ����� � ���� (�������, ��������������� � ��.).  
   - `legend`: ��������� ����������� ������� ��� ��������, ���������� �� ���������� � ������.  
   - `thresholds`: �������� ��������, ������� ������� ��������� ��������������� � ���������� ����������� �������� (��������, �������� ��������� ������ ��� ��������� � �������� ���������).  

5. ���������  
   - ���������, ������������, ��� ������ ����������� �� �������� (��������, ������ � ������ ������). ��� ��������� ��������� ���������� � ������ ��� ���������� ����������.  
   - `gridPos`: ���������� ������� ������ � ����� �������� � ��������� ��������� � �������� (��������, `x`, `y`, `w` � `h`).  

� ����������� ������ ���������� ����� ������������ ��� ��������� ����� (`targets`) � Grafana, ������� ����������� ������ ��� ������������ ������, ��������� � ������������ Debezium � Kafka Connect. ������� ��������������� ������ ������ ������� ������:

1. ������� ������� �� ���������� �������  

```
{
  "expr": "debezium_metrics_MilliSecondsSinceLastEvent{plugin=\"$connector_type\",name=\"$connector_name\",context=~\"(binlog|streaming)\"}",
  "format": "time_series",
  "intervalFactor": 1,
  "refId": "A"
}
```

- **��������**: ��� ������� �������� ����� � ������������� � ������� ��������� ���������� ������� ��� ��������� ���������� � ��������� (`binlog` ��� `streaming`). ��� �������� ����������� �������� � ��������� �������.  

### 2. ������� ���������� �������  

```
{
  "expr": "debezium_metrics_TotalNumberOfEventsSeen{plugin=\"$connector_type\",name=\"$connector_name\",context=~\"(binlog|streaming)\"}",
  "format": "time_series",
  "legendFormat": "Total events received",
  "refId": "A"
},
{
  "expr": "debezium_metrics_NumberOfEventsSkipped{plugin=\"$connector_type\",name=\"$connector_name\",context=~\"(binlog|streaming)\"}",
  "format": "time_series",
  "legendFormat": "Events skipped",
  "refId": "B"
}
```

- **��������**: 
  - ������ ������� ����������� ����� ���������� �������, ���������� �� ����������.  
  - ������ ������� ���������� ���������� ����������� �������. ������ ��� �������� ������� ������������� ���������� � ������� �������� � ��������� ������.  

3. �����������  

```
{
  "expr": "debezium_metrics_Connected{plugin=\"$connector_type\",name=\"$connector_name\",context=~\"(binlog|streaming)\"}",
  "format": "time_series",
  "refId": "A"
}
```

- **��������**: ��� ������� ���������� ������� ��������� ����������� ����������, ��� ��������� ������� �� ��� ������������ � �������������������.  

4. ������� ��� ������ �������  

```
{
  "expr": "debezium_metrics_TotalTableCount{plugin=\"$connector_type\",name=\"$connector_name\",context=\"snapshot\"}",
  "format": "time_series",
  "legendFormat": "Total",
  "refId": "A"
},
{
  "expr": "debezium_metrics_RemainingTableCount{plugin=\"$connector_type\",name=\"$connector_name\",context=\"snapshot\"}",
  "format": "time_series",
  "legendFormat": "Remaining",
  "refId": "B"
}
```

- **��������**:   
  - ������ ������� ���������� ����� ���������� ������, ����������� � �������� ������.  
  - ������ ������� ��������� ���������� ���������� ������, ������� ��� ����� ����������. ��� ������� ��� ������������ ��������� �������� ������ ������.  

5. ��������� ������  

```
{
  "expr": "debezium_metrics_SnapshotRunning{plugin=\"$connector_type\",name=\"$connector_name\",context=\"snapshot\"}",
  "format": "time_series",
  "refId": "A"
},
{
  "expr": "debezium_metrics_SnapshotCompleted{plugin=\"$connector_type\",name=\"$connector_name\",context=\"snapshot\"}",
  "format": "time_series",
  "refId": "A"
},
{
  "expr": "debezium_metrics_SnapshotAborted{plugin=\"$connector_type\",name=\"$connector_name\",context=\"snapshot\"}",
  "format": "time_series",
  "refId": "A"
}
```

- ��������: ��� ������� ����������� ��������� �������� ������ ������:  
  - `SnapshotRunning` � ����������, ��� ������� ��� �����������.  
  - `SnapshotCompleted` � ������������� � ���������� ��������.  
  - `SnapshotAborted` � ����������� � ���, ��� ������� ��� �������. ��� ������� ����� ��� �������� �� ��������� ������� � ���������� ������.  

6. ������� ������������ �����  
```
{
  "expr": "debezium_metrics_RowsScanned{plugin=\"$connector_type\",name=\"$connector_name\"}",
  "format": "table",
  "refId": "A"
}
```
- **��������**: ��� ������� ����������� � ���������� �����, ���������������� ����������� Debezium, ��� ����� ������ ������� ������������������ � ����� �������������� ������.  

7. ������� Kafka Connect  

```
{
  "expr": "kafka_connect_metrics_incoming_byte_rate",
  "format": "time_series",
  "legendFormat": "Incoming({{client}})",
  "refId": "A"
},
{
  "expr": "kafka_connect_metrics_outgoing_byte_rate",
  "format": "time_series",
  "legendFormat": "Outgoing({{client}})",
  "refId": "B"
}
```
- **��������**: ��� ������� ����������� �������� �������� � ��������� ������ (� ������) ��� Kafka Connect. ��� ����� ��� ��������� �������� �� ������� � ���������� ����������� ��������� ������.  

    - [Dockerfile](debezium-grafana/Dockerfile): ����������, ��� ������� ��������� Grafana � ������������ ��������� � ��������������.  

- `debezium-jmx-exporter/`:  
  - �������� ����� ��� ��������� JMX Exporter, ������� �������� ������� �� ���������� Java (��������, �� Kafka):  
    - [config.yml](debezium-jmx-exporter/config.yml): ������������ ��� JMX Exporter.  
	
��������� �������� ���� ������. ������ ������� ������������� ������������ ������� ������ Kafka Connect � Debezium:

1. Kafka Connect Worker Metrics  
- �������: `kafka.connect<type=connect-worker-metrics>([^:]+):`  
- ��� �������: `kafka_connect_worker_metrics_$1`  
- ��������:  
  ������ ������� �������� �������, ��������� � ������� ��������� Kafka Connect. ������� �������� �������� ����� �������� � ���� ���������� � ������������������, ���������, ���������� ������ � ������ ����������, ������� �������� ��������� ������������� ������ �������.  

2. Kafka Connect Metrics  
- �������: `kafka.connect<type=connect-metrics, client-id=([^:]+)><>([^:]+)`  
- ��� �������: `kafka_connect_metrics_$2`  
- ��������:  
  ���� ������� ��������� ������� �� ������ ���������� ��� ������������� ������� � Kafka Connect. ��������� ���������� ����� �������� ����� ������� ����������, ���������� ������������ ��������� � ������ ������.  
- �����:  
  - `client`: `"$1"` (������������� ������� ����������).  

3. Debezium Rows Scanned Metrics

- �������: `debezium.([^:]+)<type=connector-metrics, context=([^,]+), server=([^,]+), key=([^>]+)><>RowsScanned`  
- ��� �������: `debezium_metrics_RowsScanned`  
- ��������: 
  ���� ������� ������������ ��� ��������� ������, ��������� � ����������� �����, ���������������� � �������� ������ ���������� Debezium. ����� ������� �������� ������� ����� ������, ������� ��������������, � ����� ���� ����� ��� ������ ������������������ �������.
- �����:
  - plugin: "$1" (�������� �������).
  - name: "$3" (��� �������).
  - `context: "$2" (�������� ����������).
  - `table: "$4" (��� �������, �� ������� �������� ������).

4. Debezium Connector Metrics (� ����� ������)  
- �������: `debezium.([^:]+)<type=connector-metrics, server=([^,]+), task=([^,]+), context=([^,]+), database=([^>]+)>([^:]+)`  
- ��� �������: `debezium_metrics_$6`  
- ��������:  
  ������ ������� �������� �������, ����������� � ���������� ������� ���������� Debezium � ������ ���� �������� ����������, ������� ������, ������, �������� � ���� ������. ��� ��������� ����������� ������������������ ����������� ���������� � ��� �������������� � ��.  
- �����:  
  - `plugin`: `"$1"` (�������� �������).  
  - `name`: `"$2"` (��� �������).  
  - `task`: `"$3"` (���������� ������ ����������).  
  - `context`: `"$4"` (�������� ����������).  
  - `database`: `"$5"` (������ �� ������� ���� ������).  

5. Debezium Connector Task Metrics (��� ���� ������)  
- �������: `debezium.([^:]+)<type=connector-metrics, server=([^,]+), task=([^,]+), context=([^>]+)>([^:]+)`  
- ��� �������: `debezium_metrics_$5`  
- ��������:  
  ���� ������� ����� �������� ������� ����������, �� ��� ���������� ���� ������. �� ������������ ������ �� ������ ��������, ����� ��� ������, ������ � ��������.  
- �����:
  - `plugin`: `"$1"` (�������� �������).  
  - `name`: `"$2"` (��� �������).  
  - `task`: `"$3"` (���������� ������).  
  - `context`: `"$4"` (�������� ����������).  

6. Debezium Connector Metrics (� ���������� � ��������)  

- �������: `debezium.([^:]+)<type=connector-metrics, context=([^,]+), server=([^>]+)>([^:]+)`  
- ��� �������: `debezium_metrics_$4`  
- ��������:  
���� ������� �������� ������ �������, ����������� � ��������� � �������, ��� ����������� �� ������� � ����� ������. ��� ����� �������������� ��� ������ ����������� ��������� ���������� � ������ ������ ������������ ������� � ���������.  
- �����:  
  - `plugin`: `"$1"` (�������� �������).  
  - `name`: `"$3"` (��� �������).  
  - `context`: `"$2"` (�������� ������).  

    - [Dockerfile](debezium-jmx-exporter/Dockerfile): ����������, ��� ������� ����� ���������� ��� JMX Exporter.  
```
ARG DEBEZIUM_VERSION
FROM quay.io/debezium/connect:${DEBEZIUM_VERSION}

ARG JMX_AGENT_VERSION
RUN mkdir /kafka/etc && cd /kafka/etc &&\
        curl -so jmx_prometheus_javaagent.jar \
        https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/$JMX_AGENT_VERSION/jmx_prometheus_javaagent-$JMX_AGENT_VERSION.jar

COPY config.yml /kafka/etc/config.yml
```

- `debezium-prometheus/`:  
  - �������� [Dockerfile](debezium-prometheus/Dockerfile) ��� �������� ���������� Prometheus, ������� �������� �� ���� � �������� ������ �� ������ � ����������.  
  
### �������� ����� ������������ ��� Debezium Connector PostgreSQL

���� ������������ JSON (��������, [register-postgresql.json](register-postgresql.json)) �������� ��������� ��� ����������� Debezium � ���� ������ PostgreSQL � ����������� ����, ����� ��������� ������ ������������� � ���� ��� ������������. �������� ��������� � ����� ��������:

- `name`: ��� ����������. � ������ ������ ��� `postgres-connector`.
- `connector.class`: ����� ����������, ������� ���������, ��� �� ���������� PostgreSQL � �������� ��������� ������.
- `tasks.max`: ������������ ���������� �����, ������� ����� ���� �������� ����������� ������������. ������ ��������������� � `1` ��� PostgreSQL.
- `database.hostname`: ����� ���� ������ PostgreSQL. ����� ��� `postgres`, �����������, ��� ��������� � PostgreSQL ������� ��� ���� ������.
- `database.port`: ���� PostgreSQL. ����������� ���� � `5432`.
- `database.user`: ��� ������������ ��� ����������� � ���� ������.
- `database.password`: ������ ������������ ��� �����������.
- `database.dbname`: ��� ���� ������, ������� ���������� �����������.
- `database.server.id`: ���������� ������������� ������� ��� ����������� �������������.
- `database.server.name`: ��� �������, ������� ����� �������������� � ����� Kafka.
- `table.include.list`: ������ ������, ��������� � ������� ����� �����������.
- `plugin.name`: ������ ��� ����������� �������������, ������������ � PostgreSQL.
- `topic.prefix`: ������� ��� Kafka, � ������� ����� ������������ ���������. ��������, ���� ������� `test`, ��������� � ������� `public.products` ����� ������������ � ���� `test.public.products`.

������� `curl` ������������ ��� �������� POST-������� �� ������ Kafka Connect, ����� ���������������� ����� ��������� �� ������ ��������, ��������� � ����� ������������. ��� �������� ��������� ������� ������������ ��������� � ��������� �������� PostgreSQL � �� �������� � ��������������� ���� Kafka. � ���������, �� ���������� ��������� �������:

```
curl -X POST -H "Content-Type: application/json" --data @register-postgresql.json http://localhost:8083/connectors
```

- `-X POST`: ���������, ��� �� ��������� POST-������.
- `-H "Content-Type: application/json"`: ������������� ��������� ��� �������� ���� ������������� �������� (� ������ ������ JSON).
- `--data @register-postgresql.json`: ��������� ����, ���������� ������ ������������, ������� �� ����������.
- `http://localhost:8083/connectors`: URL-����� �������� ����� Kafka Connect, � ������� ������������ ������ ��� ����������� ������ ����������.

�.�. ������� `curl` � ���� ������������ ��������� ��������� ���������� ����� ����� Debezium � ����� ����� ������ PostgreSQL ��� ������������ ������������ � �������� ��������� ������.

#### ������ �������

1. ������ �����������

��������� ������ [docker-compose.yml](docker-compose.yml):

```
docker-compose build
```
... � ������:

```
docker-compose up -d
```

2. ������������� � ���� PostgreSQL, ������ ���������� � ���� ����������:  

```
docker exec -it postgres psql -U postgres -d testDB
```

```
CREATE PUBLICATION debezium_publication FOR TABLE users, orders;
SELECT * FROM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');
```

3. �������������� Debezium connector:  

```
curl -X POST -H "Content-Type: application/json" --data @register-postgresql.json  http://localhost:8083/connectors
```

4. �������� dashboard-�� � Grarafa:

��������� �� ������ http://localhost:3000 � ������ ������ 'admin/admin'. 

����� ��������� � ������ http://localhost:3000/dashboards � �������� Debezium:

![grafana_dashboards.png](grafana_dashboards.png)

�����, ��������: 

![grafana_dashboard_1.png](grafana_dashboard_1.png)

![grafana_dashboard_2.png](grafana_dashboard_2.png)

![grafana_dashboard_3.png](grafana_dashboard_3.png)

� ����� ��� ����:

![containers_logs_all.png](containers_logs_all.png)

����� ������ ��������:

![containers_all.png](containers_all.png)

5. �������������, ����� ������ ������������ (�������� ������������ ������ � ������ 
��������� ������ �� Python, ��. �����), �������� �������� ������ ��������������:  

�. ����������� � ���������� PostgreSQL  

����� ������������ � PostgreSQL � ����������, ���������� ������� `docker exec`. 
��������� ���������� postgres (��. [docker-compose.yml](docker-compose.yml)),
���������:  

```
docker exec -it postgres psql -U postgres -d testDB
```

�. �������� ������ � ���������� �������� ������, � �������������� [inventory.sql](inventory.sql):  

```
docker cp path/to/inventory.sql postgres:/tmp/inventory.sql
```

�����, ������������� � ����������, ���������:  

```
docker exec -it postgres psql -U postgres -d testDB -f /tmp/inventory.sql
```

### ����������

����� �������, ������ ������������� ������������� � ��������������� �� ��������� ��� ����������� � ������������ ��������� ������ � PostgreSQL. � �������������� ����� ����������, ��� Debezium, Prometheus � Grafana, �� ����� ���������� ����������� ������ ��������� � ������������� ���������� ���������� ����� ������������� ��������.