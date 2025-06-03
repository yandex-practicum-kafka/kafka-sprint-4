### Мониторинг изменений данных с помощью Debezium и Grafana  

#### Структура проекта:  

Проект предназначен для мониторинга данных из базы данных PostgreSQL с использованием архитектуры микро-сервисов, включающей инструменты Debezium, Prometheus и Grafana. Основная цель — отслеживание изменений в данных и визуализация этих изменений через мощные и гибкие дашборды.  

```

```

#### Основные компоненты проекта:  

- Файлы конфигурации:  
  - [.env](.env): Файл окружения для хранения переменных, необходимых для настройки Docker-контейнеров.  
  - [docker-compose.yml](docker-compose.yml): Основной файл, позволяющий развернуть все контейнеры проекта с помощью Docker Compose. Включает конфигурации для PostgreSQL, Kafka, Zookeeper, Debezium Connector, Prometheus и Grafana.  
  
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
  
  - [postgresql.conf](postgresql.conf): Дополнительная конфигурация PostgreSQL:

```
# Включаем логическую репликацию (обязательно для Debezium)
wal_level = logical

# Принимаем подключения со всех интерфейсов
listen_addresses = '*'

# Дополнительные рекомендованные параметры для репликации (опционально)
max_replication_slots = 10
max_wal_senders = 10
wal_keep_size = 64MB
```
  
- Данные создаются автоматически (DDL при старте приложения (инициализация бд), 
записи в бд — по мере скрипта, работа сервиса Python 
(`data-generator`, см. [docker-compose.yml](docker-compose.yml))).  

Код скрипта генерации данных см. в [generate_data.py](generate_data.py). DDL см. в [init-db.sql](init-db.sql):
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

- Дополнительно (скрипты и тестовые данные):
  - [inventory.sql](inventory.sql): Так же можно при соответствующей смене конфигурации использовать, скрипт, содержащий SQL-команды для создания и заполнения базы данных тестовыми данными. Включает таблицы для продуктов, клиентов и заказов, которые будут отслеживаться Debezium (в текущий момент используется генерация данных отдельным сервисом и создание соответствующих таблиц при старте контейнера PostgreSQL).  

#### Подкаталоги:

- `debezium-grafana/`:   
  - Содержит файлы конфигурации для Grafana, в том числе:  
    - [dashboard.yml](debezium-grafana/dashboard.yml): Конфигурация для настройки панели мониторинга в Grafana.  
    - [datasource.yml](debezium-grafana/datasource.yml): Файл для определения источников данных в Grafana.  
    - [debezium-dashboard.json](debezium-grafana/debezium-dashboard.json): JSON-файл с настройками дашборда для визуализации данных, получаемых от Debezium.  

Основные компоненты файла [debezium-dashboard.json](debezium-grafana/debezium-dashboard.json):

1. Метаданные дашборда  
   - `title`: Название дашборда, например, "Debezium Dashboard". Это то, что будет отображаться в интерфейсе Grafana.  
   - `uid`: Уникальный идентификатор дашборда, используемый Grafana для ссылки на дашборд. Должен быть уникальным в рамках инстанса Grafana.  
   - `version`: Версия дашборда, используется для отслеживания изменений.  
   - `schemaVersion`: Версия схемы, используемой в JSON-файле для совместимости с различными версиями Grafana.  

2. Общие параметры  
   - `templating`: Определяет переменные, которые могут использоваться в дашборде. Например, переменные могут использоваться для фильтрации метрик по серверу, времени или другим критериям.  
   - `annotations`: Позволяет добавлять аннотации на графики, например, для отображения событий, таких как развертывание новой версии приложения или сбой системы.  

3. Панели (Panels)  
   - Каждая панель отвечает за визуализацию конкретной метрики или набора метрик. Они могут быть разных типов: графики, таблицы, гистограммы и т. д.  
   - `type`: Тип панели (например, "graph", "table", "singlestat").  
   - `title`: Заголовок панели, отображаемый на графике.  
   - `datasource`: Источник данных, с которым связана панель (например, Prometheus или Loki).  
   - `targets`: Определяет метрики, которые должны быть извлечены для этой панели. Здесь указываются запросы к источнику данных.  
   - `options`: Настройки, специфичные для типа панели (например, настройки визуализации, цвета, легенды и пр.).  

4. Настройки графиков  
   - `xaxis` и `yaxis`: Конфигурация осей графика, такие как метки и типы (обычные, логарифмические и пр.).  
   - `legend`: Настройки отображения легенды для графиков, управление их видимостью и стилем.  
   - `thresholds`: Указание пределов, которые помогут визуально сигнализировать о достижении критических значений (например, цветовое изменение панели при сравнении с заданным значением).  

5. Раскладка  
   - Параметры, определяющие, как панели размещаются на дашборде (например, ширина и высота панели). Это позволяет создавать адаптивные и легкие для восприятия интерфейсы.  
   - `gridPos`: Определяет позицию панели в сетке дашборда с указанием координат и размеров (например, `x`, `y`, `w` и `h`).  

В приведенных данных содержится набор конфигураций для различных целей (`targets`) в Grafana, которые закладывают основы для визуализации метрик, связанных с коннекторами Debezium и Kafka Connect. Коротко прокомментируем каждую группу целевых метрик:

1. Метрики времени до последнего события  

```
{
  "expr": "debezium_metrics_MilliSecondsSinceLastEvent{plugin=\"$connector_type\",name=\"$connector_name\",context=~\"(binlog|streaming)\"}",
  "format": "time_series",
  "intervalFactor": 1,
  "refId": "A"
}
```

- **Описание**: Эта метрика измеряет время в миллисекундах с момента получения последнего события для заданного коннектора и контекста (`binlog` или `streaming`). Это помогает отслеживать задержки в обработке событий.  

### 2. Метрики количества событий  

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

- **Описание**: 
  - Первая метрика отслеживает общее количество событий, полученных от коннектора.  
  - Вторая метрика показывает количество пропущенных событий. Вместе они помогают оценить эффективность коннектора и выявить проблемы в обработке данных.  

3. Подключение  

```
{
  "expr": "debezium_metrics_Connected{plugin=\"$connector_type\",name=\"$connector_name\",context=~\"(binlog|streaming)\"}",
  "format": "time_series",
  "refId": "A"
}
```

- **Описание**: Эта метрика показывает текущее состояние подключения коннектора, что позволяет следить за его доступностью и производительностью.  

4. Метрики для снимка таблицы  

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

- **Описание**:   
  - Первая метрика показывает общее количество таблиц, участвующих в процессе снимка.  
  - Вторая метрика указывает оставшееся количество таблиц, которые еще нужно обработать. Это полезно для отслеживания прогресса процесса снимка данных.  

5. Состояние снимка  

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

- Описание: Эти метрики отслеживают состояние процесса снимка данных:  
  - `SnapshotRunning` — показывает, что процесс еще выполняется.  
  - `SnapshotCompleted` — сигнализирует о завершении процесса.  
  - `SnapshotAborted` — информирует о том, что процесс был прерван. Эти метрики важны для контроля за качеством снимков и возможными сбоями.  

6. Метрики сканирования строк  
```
{
  "expr": "debezium_metrics_RowsScanned{plugin=\"$connector_type\",name=\"$connector_name\"}",
  "format": "table",
  "refId": "A"
}
```
- **Описание**: Эта метрика информирует о количестве строк, просканированных коннектором Debezium, что может помочь оценить производительность и объем обрабатываемых данных.  

7. Метрики Kafka Connect  

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
- **Описание**: Эти метрики отслеживают скорость входящих и исходящих данных (в байтах) для Kafka Connect. Они важны для понимания нагрузки на систему и пропускной способности обработки данных.  

    - [Dockerfile](debezium-grafana/Dockerfile): Определяет, как создать контейнер Grafana с необходимыми плагинами и конфигурациями.  

- `debezium-jmx-exporter/`:  
  - Содержит файлы для настройки JMX Exporter, который собирает метрики из приложений Java (например, из Kafka):  
    - [config.yml](debezium-jmx-exporter/config.yml): Конфигурация для JMX Exporter.  
	
Подробное описание всех метрик. Каждая метрика характеризует определенные аспекты работы Kafka Connect и Debezium:

1. Kafka Connect Worker Metrics  
- Паттерн: `kafka.connect<type=connect-worker-metrics>([^:]+):`  
- Имя метрики: `kafka_connect_worker_metrics_$1`  
- Описание:  
  Данный паттерн собирает метрики, связанные с рабочим процессом Kafka Connect. Метрики рабочего процесса могут включать в себя информацию о производительности, состоянии, количестве ошибок и других параметрах, которые помогают оценивать эффективность работы системы.  

2. Kafka Connect Metrics  
- Паттерн: `kafka.connect<type=connect-metrics, client-id=([^:]+)><>([^:]+)`  
- Имя метрики: `kafka_connect_metrics_$2`  
- Описание:  
  Этот паттерн извлекает метрики на уровне коннектора для определенного клиента в Kafka Connect. Собранная информация может включать время отклика коннектора, количество обработанных сообщений и другие данные.  
- Метки:  
  - `client`: `"$1"` (идентификатор клиента коннектора).  

3. Debezium Rows Scanned Metrics

- Паттерн: `debezium.([^:]+)<type=connector-metrics, context=([^,]+), server=([^,]+), key=([^>]+)><>RowsScanned`  
- Имя метрики: `debezium_metrics_RowsScanned`  
- Описание: 
  Этот паттерн используется для получения метрик, связанных с количеством строк, просканированных в процессе работы коннектора Debezium. Такие метрики помогают оценить объем данных, который обрабатывается, и могут быть важны для оценки производительности системы.
- Метки:
  - plugin: "$1" (название плагина).
  - name: "$3" (имя сервера).
  - `context: "$2" (контекст выполнения).
  - `table: "$4" (имя таблицы, из которой получены строки).

4. Debezium Connector Metrics (с базой данных)  
- Паттерн: `debezium.([^:]+)<type=connector-metrics, server=([^,]+), task=([^,]+), context=([^,]+), database=([^>]+)>([^:]+)`  
- Имя метрики: `debezium_metrics_$6`  
- Описание:  
  Данный паттерн собирает метрики, относящиеся к конкретным задачам коннектора Debezium с учетом всех ключевых параметров, включая сервер, задачу, контекст и базу данных. Это позволяет отслеживать производительность конкретного коннектора и его взаимодействие с БД.  
- Метки:  
  - `plugin`: `"$1"` (название плагина).  
  - `name`: `"$2"` (имя сервера).  
  - `task`: `"$3"` (конкретная задача коннектора).  
  - `context`: `"$4"` (контекст выполнения).  
  - `database`: `"$5"` (ссылка на целевую базу данных).  

5. Debezium Connector Task Metrics (без базы данных)  
- Паттерн: `debezium.([^:]+)<type=connector-metrics, server=([^,]+), task=([^,]+), context=([^>]+)>([^:]+)`  
- Имя метрики: `debezium_metrics_$5`  
- Описание:  
  Этот паттерн также собирает метрики коннектора, но без упоминания базы данных. Он фокусируется только на важных аспектах, таких как сервер, задача и контекст.  
- Метки:
  - `plugin`: `"$1"` (название плагина).  
  - `name`: `"$2"` (имя сервера).  
  - `task`: `"$3"` (конкретная задача).  
  - `context`: `"$4"` (контекст выполнения).  

6. Debezium Connector Metrics (с контекстом и сервером)  

- Паттерн: `debezium.([^:]+)<type=connector-metrics, context=([^,]+), server=([^>]+)>([^:]+)`  
- Имя метрики: `debezium_metrics_$4`  
- Описание:  
Этот паттерн собирает только метрики, относящиеся к контексту и серверу, без детализации по задачам и базам данных. Это может использоваться для общего мониторинга состояния коннектора в рамках данной конфигурации сервера и контекста.  
- Метки:  
  - `plugin`: `"$1"` (название плагина).  
  - `name`: `"$3"` (имя сервера).  
  - `context`: `"$2"` (контекст работы).  

    - [Dockerfile](debezium-jmx-exporter/Dockerfile): Определяет, как создать образ контейнера для JMX Exporter.  
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
  - Содержит [Dockerfile](debezium-prometheus/Dockerfile) для создания контейнера Prometheus, который отвечает за сбор и хранение метрик из систем и приложений.  
  
### Описание файла конфигурации для Debezium Connector PostgreSQL

Файл конфигурации JSON (например, [register-postgresql.json](register-postgresql.json)) содержит настройки для подключения Debezium к базе данных PostgreSQL и определения того, какие изменения должны отслеживаться и куда они отправляются. Основные параметры в файле включают:

- `name`: Имя коннектора. В данном случае это `postgres-connector`.
- `connector.class`: Класс коннектора, который указывает, что мы используем PostgreSQL в качестве источника данных.
- `tasks.max`: Максимальное количество задач, которые могут быть запущены коннектором одновременно. Обычно устанавливается в `1` для PostgreSQL.
- `database.hostname`: Адрес базы данных PostgreSQL. Здесь это `postgres`, предполагая, что контейнер с PostgreSQL запущен под этим именем.
- `database.port`: Порт PostgreSQL. Стандартный порт — `5432`.
- `database.user`: Имя пользователя для подключения к базе данных.
- `database.password`: Пароль пользователя для подключения.
- `database.dbname`: Имя базы данных, которую необходимо отслеживать.
- `database.server.id`: Уникальный идентификатор сервера для логического декодирования.
- `database.server.name`: Имя сервера, которое будет использоваться в темах Kafka.
- `table.include.list`: Список таблиц, изменения в которых нужно отслеживать.
- `plugin.name`: Плагин для логического декодирования, используемый в PostgreSQL.
- `topic.prefix`: Префикс тем Kafka, в которые будут отправляться изменения. Например, если префикс `test`, изменения в таблице `public.products` будут отправляться в тему `test.public.products`.

Команда `curl` используется для отправки POST-запроса на сервер Kafka Connect, чтобы зарегистрировать новый коннектор на основе настроек, указанных в файле конфигурации. Это действие запускает процесс отслеживания изменений в указанных таблицах PostgreSQL и их отправки в соответствующие темы Kafka. В частности, мы используем следующую команду:

```
curl -X POST -H "Content-Type: application/json" --data @register-postgresql.json http://localhost:8083/connectors
```

- `-X POST`: Указывает, что мы выполняем POST-запрос.
- `-H "Content-Type: application/json"`: Устанавливает заголовок для указания типа передаваемого контента (в данном случае JSON).
- `--data @register-postgresql.json`: Указывает файл, содержащий данные конфигурации, которые мы отправляем.
- `http://localhost:8083/connectors`: URL-адрес конечной точки Kafka Connect, к которой отправляется запрос для регистрации нового коннектора.

Т.е. команда `curl` и файл конфигурации совместно позволяют установить связь между Debezium и нашей базой данных PostgreSQL для эффективного отслеживания и передачи изменений данных.

#### Запуск проекта

1. Запуск контейнеров

Выполняем сборку [docker-compose.yml](docker-compose.yml):

```
docker-compose build
```
... и запуск:

```
docker-compose up -d
```

2. Подключившись к базе PostgreSQL, создаём публикацию и слот репликации:  

```
docker exec -it postgres psql -U postgres -d testDB
```

```
CREATE PUBLICATION debezium_publication FOR TABLE users, orders;
SELECT * FROM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');
```

3. Зарегистрируем Debezium connector:  

```
curl -X POST -H "Content-Type: application/json" --data @register-postgresql.json  http://localhost:8083/connectors
```

4. Просмотр dashboard-ов в Grarafa:

Переходим по адресу http://localhost:3000 и вводим дважды 'admin/admin'. 

Далее переходим в раздел http://localhost:3000/dashboards и выбираем Debezium:

![grafana_dashboards.png](grafana_dashboards.png)

Далее, например: 

![grafana_dashboard_1.png](grafana_dashboard_1.png)

![grafana_dashboard_2.png](grafana_dashboard_2.png)

![grafana_dashboard_3.png](grafana_dashboard_3.png)

В логах при этом:

![containers_logs_all.png](containers_logs_all.png)

Общая работа сервисов:

![containers_all.png](containers_all.png)

5. Дополнительно, можно сменив конфигурацию (отключив автосоздание таблиц и сервис 
генерации данных на Python, см. ранее), добавить тестовые данные самостоятельно:  

а. Подключение к контейнеру PostgreSQL  

Чтобы подключиться к PostgreSQL в контейнере, используем команду `docker exec`. 
Контейнер называется postgres (см. [docker-compose.yml](docker-compose.yml)),
выполняем:  

```
docker exec -it postgres psql -U postgres -d testDB
```

б. Создание таблиц и наполнение тестовых данных, с использованием [inventory.sql](inventory.sql):  

```
docker cp path/to/inventory.sql postgres:/tmp/inventory.sql
```

Затем, подключившись к контейнеру, выполняем:  

```
docker exec -it postgres psql -U postgres -d testDB -f /tmp/inventory.sql
```

### Заключение

Таким образом, проект предоставляет разработчикам и администраторам бд платформу для мониторинга и визуализации изменений данных в PostgreSQL. С использованием таких технологий, как Debezium, Prometheus и Grafana, мы можем эффективно отслеживать важные изменения и предоставлять актуальную информацию через интерактивные дашборды.