# Connected Insurance – Vehicle Data & Risk

Backend **Spring Boot 3** (Java 17) application for vehicle monitoring and insurance risk. It uses **WebFlux**, **MongoDB (reactive)**, **Kafka**, and **MQTT**.

---

## Tech stack

| Component        | Technology                          |
|-----------------|--------------------------------------|
| Framework       | Spring Boot 3.2.3, Spring WebFlux    |
| Database        | MongoDB (reactive driver)            |
| Messaging       | Apache Kafka, MQTT                   |
| API docs        | SpringDoc OpenAPI (Swagger UI)       |
| Build           | Maven                               |
| Java            | 17                                  |

---

## What the app does

- **Vehicle data**: ingest and store vehicle telemetry (Kafka/MQTT).
- **Clients & contracts**: clients, insurance contracts (`Contrat`), contract types and statuses.
- **Risk**: risk factors, risk events, risk assessments, insurance impact.
- **REST API**: reactive controllers for vehicles, clients, contracts, risk events; exposed on port **8080** with Swagger at `/swagger-ui.html`.

With profile **dev**, on startup the app loads vehicle data from **`src/main/resources/data/vehicle-data-dev.json`**, sends each record to the Kafka topic `vehicle-data`, and the existing consumer inserts them into MongoDB. Clients and contrats are still seeded directly if collections are empty. You can edit the JSON file to add or change initial vehicle data.

**Authentication:** API is protected by JWT. Users are stored in MongoDB (`users` collection); passwords are hashed with **BCrypt** (salt is part of the hash). You log in with **POST /api/auth/login** (username + password), get a token, then call other APIs with header **`Authorization: Bearer <token>`**. In dev, a default user **admin / admin123** is created if no users exist.

---

## Prerequisites

- **Java 21**
- **Maven 3.8+**
- **Docker & Docker Compose** (for running with MongoDB + Kafka)

---

## How to run

### Option 1 – Everything in Docker (easiest)

Starts the app + MongoDB + Zookeeper + Kafka.

**Linux/macOS:**

```bash
./start.sh dev
```

**Windows (PowerShell):**

```powershell
docker-compose -f docker-compose.dev.yml --env-file .env.dev up --build
```

- App: **http://localhost:8080**
- Swagger UI: **http://localhost:8080/swagger-ui.html**
- Health: **http://localhost:8080/actuator/health**
- MongoDB: `localhost:27017`, Kafka: `localhost:9092`

### Authentication (all options)

1. **Login** to get a token:
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```
   Response: `{"token":"eyJ...", "type":"Bearer"}`

2. **Call any other API** with the token:
   ```bash
   curl -H "Authorization: Bearer <your-token>" http://localhost:8080/api/vehicle-data/DEV-VEH-001/latest
   ```

3. **In Swagger UI:** click **Authorize**, enter `Bearer <your-token>` (or just the token if the UI adds "Bearer "), then **Authorize**. All requests will include the header.

---

### Option 2 – App locally, infra in Docker

1. Start only MongoDB and Kafka:

```bash
docker-compose -f docker-compose.dev.yml --env-file .env.dev up -d mongodb kafka
```

2. Run the app with Maven (profile `dev` uses `localhost` for MongoDB and Kafka):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or:

```bash
mvn clean package -DskipTests
java -jar target/connected-insurance-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

- Same URLs as above (port 8080).

---

### Option 3 – Production-like (Docker)

Uses `docker-compose.prod.yml`. You must provide `.env.prod` with at least:

- `MONGODB_URI`
- `KAFKA_BROKERS`
- `KAFKA_SASL_JAAS_CONFIG` (and related Kafka auth vars if using SASL)
- `JWT_SECRET` (min 32 characters for HS256; use a strong secret in production)

Then:

```bash
./start.sh prod
# or
docker-compose -f docker-compose.prod.yml --env-file .env.prod up --build
```

---

## Sending test data via Kafka

Data is inserted into MongoDB only when the app consumes messages from the `vehicle-data` topic. To create documents:

1. Start the app (and Kafka + MongoDB).
2. Produce a JSON message to topic `vehicle-data`. Example payload:

```json
{"vehicle_id":"VH-001","contractId":"CONTRACT-001","timestamp":"2025-02-23T12:00:00Z","data":{"SPEED":{"value":70,"unit":"km/h"},"RPM":{"value":2500,"unit":"rpm"},"timestamp":"2025-02-23T12:00:00Z"}}
```

3. With Kafka running in Docker, you can use the broker’s console producer:

```bash
docker exec -it <kafka-container-name> kafka-console-producer.sh --bootstrap-server localhost:9092 --topic vehicle-data
```

Then paste the JSON above and press Enter. The app will consume it, assess risk, and save a document in the `vehicle_data` collection.

---

## Configuration

| Profile   | Config file                    | Use case                          |
|----------|---------------------------------|-----------------------------------|
| default  | `application.properties`        | Base config                       |
| `dev`    | `application-dev.properties`    | Local / Docker dev (localhost)    |
| `prod`   | `application-prod.properties`   | Production (env-based)            |
| `test`   | `application-test.properties`   | Tests (Testcontainers)            |

Dev expects:

- MongoDB: `localhost:27017`, DB `vehicledb`
- Kafka: `localhost:9092`, topic `vehicle-data`

---

## Tests

```bash
mvn test
```

Tests use Testcontainers for MongoDB, Kafka, and HiveMQ (MQTT).

---

## Useful commands

| Goal              | Command |
|-------------------|--------|
| Run app (dev)     | `mvn spring-boot:run -Dspring-boot.run.profiles=dev` |
| Package (no tests)| `mvn clean package -DskipTests` |
| Run tests         | `mvn test` |
| Dev stack (Docker)| `docker-compose -f docker-compose.dev.yml --env-file .env.dev up --build` |
| Stop dev stack   | `docker-compose -f docker-compose.dev.yml down` |

---

## Project layout (main parts)

```
src/main/java/com/example/
├── ReactiveApplication.java          # Entry point
├── config/                           # Kafka, Jackson, etc.
├── controller/                       # REST (VehicleData, Client, InsuranceContract, RiskEvent)
├── model/                            # Entities (VehicleData, Client, Contrat, Risk*, etc.)
├── repository/                       # Reactive MongoDB repositories
└── service/                          # VehicleDataService, RiskAssessmentService, Kafka consumer, etc.
```

`docker-compose.dev.yml` and `Dockerfile` are at the project root; `.env.dev` holds dev environment variables.
