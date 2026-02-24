# Interview Q&A – Connected Insurance / Vehicle Data Project

Possible questions and concise answers for presenting this project in an interview.

---

## Architecture & design

**Q: How would you describe the overall architecture of this application?**

**A:** It’s an event-driven backend: vehicle telemetry arrives via **Kafka**; a consumer processes each message, runs **risk assessment** (score, level, insurance impact), and persists **VehicleData** (and **RiskEvent** when risk is HIGH) in **MongoDB**. The same data is exposed through a **reactive REST API** (WebFlux). Everything is non-blocking (reactive streams, reactive MongoDB, Kafka consumer). Dev profile seeds data by sending a JSON file to Kafka so the full flow can be demoed without external producers.

---

**Q: Why did you choose a reactive stack (WebFlux, reactive MongoDB) instead of a classic blocking one?**

**A:** To handle many concurrent messages and API calls with a small number of threads: non-blocking I/O and backpressure (Reactor) scale better under load. Reactive MongoDB and Spring WebFlux fit that model and avoid thread-per-request bottlenecks. For a telematics/insurance pipeline with Kafka and DB-heavy work, this is a good fit.

---

**Q: Why Kafka for vehicle data instead of a simple REST endpoint or a queue like RabbitMQ?**

**A:** Kafka gives durable, ordered streams per partition, replay capability, and consumer groups for scaling (multiple instances share partitions). Producers (vehicles, gateways) can send at their own rate without blocking; we consume as fast as we can. That decouples ingestion from processing and fits a distributed/autoscaling deployment. RabbitMQ could work, but Kafka is standard for high-volume event streaming and fits “many producers, one or more consumers” well.

---

## Kafka

**Q: How does your consumer commit offsets, and why that strategy?**

**A:** We use **record-based commit** (`AckMode.RECORD`): after each message is processed (risk assessment + save to MongoDB), the offset is committed. It’s a balance between “at-most-once” (commit before process) and “at-least-once” with large batches: we avoid losing progress on crash and still get one-message granularity. Combined with idempotent processing (see below), we avoid duplicate records even if the same message is redelivered.

---

**Q: How do you avoid duplicate vehicle data when the same Kafka message is processed twice (e.g. rebalance, redelivery)?**

**A:** We treat **vehicleId + contractId + timestamp** as the business key. Before saving **VehicleData**, we check if a document with that key already exists; if yes we return it, if no we insert. We also have a **unique compound index** on those three fields in MongoDB, so a concurrent duplicate insert fails with `DuplicateKeyException`; we catch it and load the existing document. **RiskEvent** uses the same idea: we only create one event per (vehicleId, contractId, timestamp) for HIGH risk. So processing is idempotent and safe for redelivery and multiple instances.

---

**Q: In a distributed deployment with autoscaling, can two instances process the same message?**

**A:** No. Each partition is consumed by only one consumer in the group, so a given message is processed by a single instance. Duplicates only appear if the *same* message is **redelivered** (e.g. crash before commit, or offset reset). Our idempotency (find-by-key + unique index) handles that: the second processing finds the existing document and does not insert again.

---

## MongoDB & data model

**Q: Why MongoDB for this use case?**

**A:** Vehicle data and risk assessments are document-shaped (nested sensors, risk factors, insurance impact); MongoDB stores them naturally. The reactive driver fits our non-blocking stack. We need flexible schema for evolving telemetry (new sensors) and good support for time-range and paginated queries, which we do with indexes and reactive repositories.

---

**Q: How do you guarantee no duplicate documents for the same logical event in a concurrent or distributed setup?**

**A:** Two layers: (1) **Application:** before insert we do a find by (vehicleId, contractId, timestamp); only if absent we save. (2) **Database:** a unique compound index on (vehicleId, contractId, timestamp). If two instances try to insert the same key, one succeeds and the other gets `DuplicateKeyException`; we then load and return the existing document. So we get at-most-one document per logical event even under concurrency.

---

**Q: What is the role of the unique index on `vehicle_data`?**

**A:** It enforces uniqueness at the DB level for (vehicleId, contractId, timestamp). It protects against races (e.g. two processes both “not finding” and then inserting) and makes the idempotency guarantee robust. It also helps queries that filter or sort by these fields.

---

## Business logic (risk)

**Q: How do you compute the risk score and level?**

**A:** We evaluate **risk factors** from sensor data (e.g. speed &gt; 90 km/h, RPM &gt; 3000, engine load &gt; 60%, throttle &gt; 80%). For each factor we compute a contribution: `(value - threshold) / threshold * weight`, and sum them into a **total score**. The **level** is: LOW (&lt; 0.4), MEDIUM (0.4–0.8), HIGH (≥ 0.8). We attach this **RiskAssessment** (score, level, factors, insurance impact) to the **VehicleData** and persist it. So the logic is rule-based and configurable via thresholds and weights.

---

**Q: When do you create a RiskEvent, and what does it represent?**

**A:** We create a **RiskEvent** only when the risk **level is HIGH** (score ≥ 0.8). It represents a notable driving risk (e.g. speeding) that may trigger follow-up (premium adjustment, review). We store vehicleId, contractId, type (e.g. SPEEDING), severity, timestamp, and description. It’s an audit/alert record, separate from the full vehicle data document.

---

**Q: How does insurance impact depend on risk?**

**A:** We set **InsuranceImpact** from the score: HIGH (≥ 0.8) → e.g. +15% premium, “immediate review”; MEDIUM (≥ 0.4) → e.g. +8%, “driver training”; LOW → no adjustment, “continue monitoring”. This is embedded in the **RiskAssessment** on each **VehicleData** and can drive pricing and actions in downstream systems.

---

## Configuration & deployment

**Q: How do you separate dev, test, and production configuration?**

**A:** **Profiles:** `dev`, `test`, `prod` with corresponding `application-*.properties`. **Dev:** local MongoDB/Kafka, DEBUG logging, dev seeder (JSON → Kafka). **Prod:** MongoDB and Kafka from **environment variables** (URIs, brokers, SASL/SSL), INFO logging, no seeder. **Test:** minimal properties; Kafka/MongoDB come from tests (e.g. Testcontainers) via `@DynamicPropertySource`. **KafkaConfig** is active for `!test`; tests use **TestKafkaConfig** so they can point to container brokers and use `earliest` offset.

---

**Q: Why is KafkaConfig disabled in the test profile?**

**A:** In tests we use **Testcontainers** for Kafka (and MongoDB). The broker URL is dynamic (host/port from the container). We inject it via `@DynamicPropertySource` into `spring.kafka.bootstrap-servers`. **TestKafkaConfig** reads that and also uses `AUTO_OFFSET_RESET=earliest` so tests can produce and consume in the same run. The main **KafkaConfig** is for fixed dev/prod brokers, so we exclude it in test to avoid conflicts and use test-specific config.

---

**Q: How does the dev seeder work and why only when collections are empty?**

**A:** With profile **dev**, a **CommandLineRunner** runs after startup. It (1) seeds **clients** and **contrats** in MongoDB if there are no clients, and (2) if **vehicle_data** is empty, reads **vehicle-data-dev.json** and **sends each record to the Kafka topic**. The normal consumer then processes them and writes to MongoDB. We only send when vehicle_data is empty to avoid duplicates on every restart; the rest is idempotent (same key = same document).

---

## Testing

**Q: How do you test the Kafka consumer flow (JSON → consumer → MongoDB)?**

**A:** We use an **integration test** (e.g. **VehicleDataConsumerIntegrationTest**) with **Testcontainers**: MongoDB and Kafka (e.g. Confluent or Apache Kafka image). We register container URLs via `@DynamicPropertySource`, use **TestKafkaConfig**, and with `@SpringBootTest` the real consumer runs. We produce a test message to the topic, wait for consumption, and assert that the expected **VehicleData** (and optionally **RiskEvent**) appears in MongoDB or via the consumer’s API. So we test the full path without real Kafka/Mongo.

---

**Q: Why Testcontainers instead of embedded Kafka or in-memory MongoDB?**

**A:** Testcontainers runs real Kafka and MongoDB in Docker, so we test against the same clients and protocols we use in production. Embedded or in-memory alternatives often differ in behaviour or API; Testcontainers gives higher confidence for integration tests. The cost is slower startup and Docker dependency, which we accept for a few critical integration tests.

---

## API & OpenAPI

**Q: How do you document the API for date-time parameters (e.g. timerange)?**

**A:** We use **SpringDoc (OpenAPI 3)** and **@Parameter** on the request params: description (“Start of time range (ISO-8601)”), **example** (e.g. `2025-02-23T00:00:00Z`), and **schema** (type string, format date-time). The controller uses **@DateTimeFormat(iso = ISO.DATE_TIME)** so Spring accepts ISO-8601. In Swagger UI we tell users to enter values like `2025-02-23T10:15:00Z` for `startTime` and `endTime`.

---

## Scalability & production

**Q: How would you scale this application horizontally?**

**A:** Run **multiple instances** behind a load balancer. **Kafka:** same consumer group; partitions are shared among instances (each partition = one consumer). **MongoDB:** same cluster for all instances; the unique index and find-before-save keep idempotency. **REST:** stateless; any instance can serve any request. So we scale by adding instances; Kafka partition count can be increased if we need more parallelism.

---

**Q: What would you add or change for production (observability, security, resilience)?**

**A:** **Observability:** structured logging (e.g. JSON), distributed tracing (e.g. Sleuth/Micrometer), metrics (Kafka lag, DB latency, error rates), alerts. **Security:** auth (e.g. OAuth2/JWT) and authorization on REST; Kafka SASL/SSL (we already have prod config for it); secrets from a vault, not env in plain text. **Resilience:** retries with backoff for transient Kafka/DB errors; circuit breaker (e.g. Resilience4j) for external calls; health checks (we have Actuator) and readiness/liveness for Kubernetes. **Data:** backup and retention for MongoDB; Kafka retention and compaction where needed.

---

## Summary table (quick revision)

| Topic | Main point |
|-------|------------|
| Architecture | Event-driven: Kafka → consumer → risk assessment → MongoDB; reactive REST to read. |
| Idempotency | Business key = vehicleId + contractId + timestamp; find-before-save + unique index. |
| Risk | Factors from sensors → score → level (LOW/MEDIUM/HIGH) → RiskEvent only for HIGH. |
| Config | Profiles (dev/test/prod); Kafka disabled in test; dev seeder only when collections empty. |
| Testing | Testcontainers for Kafka + MongoDB; full consumer flow in integration test. |
| Scaling | Multiple instances, same consumer group; stateless app; shared MongoDB and idempotency. |

You can use this document to prepare answers and to structure your presentation of the project in an interview.
