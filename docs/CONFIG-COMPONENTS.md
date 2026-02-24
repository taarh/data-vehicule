# Configuration components – what each does and why

This document explains every configuration component in the project so you can present the project clearly (e.g. in a demo or interview).

---

## 1. Java config classes (`src/main/java/com/example/config/`)

### 1.1 `KafkaConfig`

**Role:** Defines how the application connects to Kafka as both **consumer** and **producer**.

| Element | What it does | Why |
|--------|----------------|-----|
| **`@Profile("!test")`** | This config is active for all profiles **except** `test`. | In tests we use `TestKafkaConfig` (e.g. with Testcontainers). In dev/prod we use this one. |
| **`@EnableKafka`** | Enables Kafka listeners in the app. | Needed so `@KafkaListener` in `VehicleDataConsumer` is registered and consumes from the topic. |
| **`bootstrapServers`** | Injected from `spring.kafka.bootstrap-servers` (e.g. `localhost:9092` in dev). | Tells the client which Kafka broker(s) to connect to. |
| **`groupId`** | Injected from `kafka.consumer.group-id` (e.g. `insurance-group`). | Consumer group ID: all instances with the same ID share partitions; used for scaling and offset commit. |
| **`ConsumerFactory`** | Builds the consumer with bootstrap servers, group ID, and String deserializers. | The consumer receives messages as `String`; the app then parses JSON to `VehicleData` in code. |
| **`AUTO_OFFSET_RESET_CONFIG: "latest"`** | If no committed offset exists, start from the latest message. | In dev/prod we usually don’t want to reprocess old messages after a new deploy. (Tests use `earliest` to process seed data.) |
| **`ConcurrentKafkaListenerContainerFactory`** | Factory for the container that runs the `@KafkaListener`. | Controls concurrency and ack behaviour. |
| **`AckMode.RECORD`** | Offset is committed after each record is processed. | Good balance: we don’t lose progress on crash, and we don’t wait for a batch. |
| **`ProducerFactory` + `KafkaTemplate`** | Producer that sends `String` keys and values. | Used by `DevDataSeeder` (and any future producer) to send vehicle data JSON to the topic. |

**Why it exists:** The app is a Kafka consumer (and in dev a producer for seeding). This class centralises broker address, group ID, serialisation, and ack behaviour so the rest of the app only uses `@KafkaListener` and `KafkaTemplate`.

---

### 1.2 `JacksonConfig`

**Role:** Configures the single shared **ObjectMapper** used for JSON ↔ Java (e.g. Kafka payloads, REST, MongoDB).

| Element | What it does | Why |
|--------|----------------|-----|
| **`ObjectMapper` bean** | One ObjectMapper instance for the whole application. | Ensures the same rules (dates, unknown fields, etc.) everywhere: consumer, REST, dev seeder. |
| **`JavaTimeModule`** | Registers support for `Instant`, `LocalDateTime`, etc. | Vehicle data and events use `Instant`; without this, Jackson cannot serialise/deserialise them correctly. |
| **`FAIL_ON_UNKNOWN_PROPERTIES: false`** | Ignores extra fields in JSON. | Telemetry can have many optional sensors; we don’t want one unknown field to break parsing. |

**Why it exists:** Kafka messages and REST bodies are JSON. A single, well-configured ObjectMapper avoids bugs (e.g. dates, unknown fields) and keeps behaviour consistent across consumer, API, and dev seed.

---

### 1.3 `DevDataSeeder`

**Role:** **Dev-only** startup logic: seeds clients/contrats in MongoDB and sends vehicle data from a JSON file **to Kafka** (so the normal consumer flow inserts them into MongoDB).

| Element | What it does | Why |
|--------|----------------|-----|
| **`@Profile("dev")`** | Loaded only when `spring.profiles.active=dev`. | We don’t seed or produce in test (Testcontainers + tests control data) or in prod. |
| **`CommandLineRunner`** | Runs once after the application context is up. | We need Kafka and MongoDB ready before sending messages and inserting clients/contrats. |
| **`seedClientsAndContrats()`** | If there are no clients, inserts 5 clients and 2 contrats each. | Gives reference data for demos/DB tools; clients/contrats are not in the Kafka flow in this app. |
| **`sendVehicleDataFromJsonToKafka()`** | If `vehicle_data` is empty, reads `data/vehicle-data-dev.json` and sends each record to the `vehicle-data` topic. | Demonstrates the real flow: JSON → Kafka → consumer → MongoDB, without external producers. |
| **`vehicleDataRepository.count() == 0`** | Sends to Kafka only when no vehicle data exists. | Prevents duplicate documents on every restart (idempotent dev seed). |
| **`kafka.topic.vehicle-data`** | Topic name from config (e.g. `vehicle-data`). | Same topic the consumer listens to; allows overriding per environment. |

**Why it exists:** In dev you can run the app and immediately see data in MongoDB and test the full pipeline (Kafka + risk + persistence) without running a separate producer; it also shows how the app is intended to receive data (via Kafka).

---

## 2. Profile-based properties

### 2.1 `application-dev.properties` (profile: `dev`)

Used when running locally or in a dev environment (e.g. `--spring.profiles.active=dev`).

| Section | Purpose |
|--------|---------|
| **MongoDB** | `uri` and `database` point to local MongoDB (e.g. `localhost:27017`, `vehicledb`). `auto-index-creation=true` so indexes (e.g. unique index on vehicle data) are created automatically. |
| **Kafka** | `bootstrap-servers=localhost:9092`, topic and consumer group names. Serialisers/deserialisers set so all Kafka usage is String (JSON in the payload). |
| **Server** | `server.port=8080` for REST and Swagger. |
| **Logging** | DEBUG for `com.example` and Spring Kafka/Mongo so you can trace consumption and DB writes. |
| **Actuator** | Health, info, metrics exposed for local checks and container probes (liveness/readiness). |

**Why:** Single place to tune dev behaviour (DB, Kafka, logs, port) without changing code.

---

### 2.2 `application-prod.properties` (profile: `prod`)

Used in production (e.g. Docker, Kubernetes) with environment variables.

| Section | Purpose |
|--------|---------|
| **MongoDB** | `uri` and `database` from env (e.g. `MONGODB_URI`, `MONGODB_DATABASE`) so each environment can use its own cluster. |
| **Kafka** | `bootstrap-servers` from `KAFKA_BROKERS`; **SASL/SSL** (e.g. `SASL_SSL`, `PLAIN`, `KAFKA_SASL_JAAS_CONFIG`) for secure production clusters. Topic and group from env with defaults. |
| **Server** | Port from `PORT` (e.g. for cloud/docker). |
| **Logging** | INFO by default, `LOG_LEVEL` override; file logging for persistence. |
| **Actuator** | Same health/metrics as dev for orchestration and monitoring. |

**Why:** No secrets or hostnames in code; production uses env-specific brokers, auth, and DB.

---

### 2.3 `application-test.properties` (profile: `test`)

Used during tests (e.g. `@ActiveProfiles("test")`).

| Content | Purpose |
|--------|---------|
| **MQTT** | Placeholder `mqtt.username` / `mqtt.password` so MQTT config doesn’t fail when tests don’t use it. |
| **Rest** | Other settings (MongoDB, Kafka) come from test code (e.g. Testcontainers) via `@DynamicPropertySource`. |

**Why:** Tests use in-memory or containerised infra; this file avoids missing-property errors and keeps test config separate from dev/prod.

---

## 3. Test-only config (`src/test/...`)

### 3.1 `TestKafkaConfig`

**Role:** Provides Kafka consumer/producer beans for **integration tests** that use Testcontainers (or an external Kafka).

| Difference vs `KafkaConfig` | Why |
|----------------------------|-----|
| **`@TestConfiguration`** | Loaded only in test context when explicitly imported (e.g. by `VehicleDataConsumerIntegrationTest`). |
| **`AUTO_OFFSET_RESET_CONFIG: "earliest"`** | So the test consumer can read messages produced during the test (e.g. right after sending). |
| **`bootstrapServers` from `@Value`** | Tests set `spring.kafka.bootstrap-servers` dynamically from the Testcontainers Kafka container, so this picks up the right broker. |

**Why:** Main `KafkaConfig` is excluded in tests (`@Profile("!test")`). Tests need their own Kafka config that points to the test broker and uses `earliest` for deterministic consumption.

---

## 4. How it fits together (for your presentation)

- **KafkaConfig** + **application-dev/prod**: define how the app talks to Kafka (brokers, group, auth in prod) and how the consumer commits offsets.
- **JacksonConfig**: ensures JSON (Kafka, REST) is parsed and written consistently, with dates and optional fields handled.
- **DevDataSeeder** + **application-dev**: show the “JSON → Kafka → consumer → MongoDB” flow in one environment and avoid duplicate seed data.
- **application-prod**: shows production readiness (env-based MongoDB/Kafka, SASL, health endpoints).
- **TestKafkaConfig** + **application-test**: show that Kafka behaviour is testable in isolation with containers and different settings (e.g. `earliest`).

You can say: *“Configuration is split into: shared JSON and Kafka behaviour (Jackson + KafkaConfig), environment-specific settings (profile properties), dev-only seeding (DevDataSeeder), and test-only Kafka (TestKafkaConfig), so the same app runs locally, in tests, and in production with clear, explainable behaviour.”*
