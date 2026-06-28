# AVISOS Architecture Guide

> **Advanced Visual Infrastructure Secure Operational Systems**
>
> A SCADA orchestration platform with computer vision AI and an on-prem LLM agent for predictive threat modeling.

This document is a developer-oriented walkthrough of how the system works end-to-end. It is designed to get a new contributor (or a returning one) up to speed without reading every source file.

---

## Table of Contents

- [System Overview](#system-overview)
- [Module Breakdown](#module-breakdown)
- [Runtime Flows](#runtime-flows)
- [Data Model](#data-model)
- [Concurrency Model](#concurrency-model)
- [Security Model](#security-model)
- [Configuration](#configuration)
- [Infrastructure Services (Docker Compose)](#infrastructure-services-docker-compose)
- [Key Design Decisions](#key-design-decisions)

---

## System Overview

AVISOS is a monorepo containing two Java microservices, a Python machine learning service, a C++ hardware simulator, and an AI knowledge base. Nodes publish telemetry over MQTT, the controller evaluates camera frames through a vision AI pipeline, a Python anomaly detection service monitors fleet-wide metrics using scikit-learn (Isolation Forest), and an on-prem LLM generates incident analysis using RAG over facility documentation.

```
                            ┌──────────────────────────┐
                            │       MQTT Broker         │
                            │    (Eclipse Mosquitto)    │
                            │     avisos/alerts         │
                            └─────┬──────────────┬──────┘
                           publish│              │subscribe
                                  │              │
┌──────────────────────┐   ┌──────┴──┐   ┌───────┴──────────────────────────────┐
│  Hardware Simulator  │   │  Node   │   │        Controller Service            │
│       (C++17)        │   │ Service │   │                                      │
│                      │   │         │   │  Telemetry ingress ──► Vision AI     │
│  GET /readings       ◄───┤ Polls   │   │       │                    │         │
│  GET /frame          │   │ both    │   │       ▼                    ▼         │
│  GET /health         │   │         │   │  Node tracking     Threat detection  │
│                      │   │         │   │                         │            │
│  resources/frames/   │   │         │   │                         ▼            │
│  ├─ alarm/   (10%)   │   │         │   │                   Alarm service      │
│  └─ normal/  (90%)   │   │         │   │                    │    │    │       │
└──────────────────────┘   └─────────┘   │                    │    │    │       │
                                         │           ┌────────┘    │    └──┐    │
                                         │           ▼             ▼      ▼    │
                                         │     S3 storage    LLM analyst  Web  │
                                         │     (async)       (async RAG)  WS   │
                                         │                                     │
                                         │       ┌───────────────────────┐     │
                                         │       │ FleetMetricScheduler  │──┐  │
                                         │       └───────────────────────┘  │  │
                                         └───┬────┬────┬────┬────┬────┬─────┼──┘
                                             │    │    │    │    │    │     │
                                 ┌───────────┘    │    │    │    │    │     ▼
                                 ▼                ▼    │    ▼    ▼    │  ┌──────────────┐
                           ┌──────────┐  ┌──────────┐ │ ┌────────┐  │ │  │ Python ML    │
                           │  SQLite  │  │ pgvector │ │ │ Ollama │  │ │  │ Service      │
                           │(JDBI)     │ │(vectors) │ │ │ (LLM)  │  │ │  │ (FastAPI)    │
                           └──────────┘  └──────────┘ │ └────────┘  │ │  └──────┬───────┘
                                                      │             │ │         │
                                                      ▼             ▼ │         ▼
                                                ┌──────────┐  ┌───────┴──────┐┌──────────┐
                                                │    S3    │  │ CodeProject  ││ Postgres │
                                                │(LocalStack)││   .AI        ││ (ML Data)│
                                                └──────────┘  └──────────────┘└──────────┘
```

One controller instance manages many node instances. Each node represents a physical data acquisition device deployed in the field. The hardware simulator runs alongside each node to provide realistic sensor readings and camera frames.

---

## Module Breakdown

### `avisos-common-lib`

Shared Protobuf definitions (`telemetry.proto`) and generated Java classes. Both services depend on this module for message serialization contracts.

### `avisos-hardware-simulator`

C++17 REST service built with CMake, cpp-httplib, and nlohmann/json. Models datacenter hardware vitals and serves camera frame images to node instances.

Endpoints:

- **`GET /readings`** — latest `HardwareSnapshot` as JSON (battery, temperature, pressure, humidity, leak detection, signal quality).
- **`GET /frame`** — random camera frame image from `resources/frames/`. Weighted 90% normal, 10% alarm. Images are loaded into memory at startup by `FrameProvider`.
- **`GET /health`** — liveness check.

The simulation loop (`Simulator`) generates a new `HardwareSnapshot` every 30 seconds with weighted random tiers: shambles (10%), average (80%), strong (10%).

Frame images live in `resources/frames/alarm/` and `resources/frames/normal/` — realistic CCTV-style datacenter photos used by CodeProject.AI for meaningful object detection.

### `avisos-node-service`

A lightweight edge service — one instance per physical device. Uses a custom DIY IoC framework (`framework/` package) instead of Spring Boot to keep the deployment footprint small for edge hardware.

Responsibilities:

- **Heartbeat generation** — sends telemetry packets every 30 seconds with battery level, node metadata, and a camera frame from the simulator.
- **Camera frame acquisition** — `HttpHardwareTelemetryProvider.readFrame()` fetches a real image from the simulator's `GET /frame` endpoint (replaces the earlier random-byte stub).
- **Battery monitoring** — queries hardware via OSHI (local) or the simulator (Docker mode); warns if below 15%.
- **MQTT connectivity** — Eclipse Paho client with auto-reconnect and exponential backoff (1s to 60s).
- **Reactive buffering** — bounded in-memory queue (5000 items) with backpressure for unreliable networks.

The node runs three concurrent virtual-thread loops managed by `NodeRuntime`:

1. **Connection supervisor** — connects to the broker, retries with backoff on failure.
2. **Heartbeat loop** — periodic telemetry publish.
3. **Watchdog** — monitors battery and connection state.

### `avisos-controller-service`

The central orchestration service, built on **Spring Boot 3.4.1**. Uses auto-configuration, component scanning, and `@ConfigurationProperties` records for type-safe configuration under the `avisos.*` prefix.

Responsibilities:

- **Telemetry ingress** — subscribes to MQTT, deserializes JSON payloads, routes packets through the analysis pipeline.
- **Vision AI** — forwards image data to CodeProject.AI for object detection, evaluates detected labels against configured threat keywords.
- **Alarm management** — persists alarms with severity (NONE/WARNING/CRITICAL) when threats are detected. Alarms have an associated S3 image key (populated asynchronously) and an AI-generated analysis.
- **AI incident analysis (RAG)** — when an alarm fires, an async event handler invokes the on-prem LLM (Ollama) with RAG context from the facility knowledge base to generate a structured incident brief.
- **RAG knowledge base** — on startup, loads all markdown files from `avisos-knowledge/` into pgvector via Spring AI. Supports manual re-ingestion via `POST /api/rag/load`.
- **Subject matter expert chat** — RAG-powered Q&A endpoint (`GET /api/ai/ask-sme/chat`) for operators to query facility documentation in natural language.
- **S3 image storage** — flagged camera frames are stored in S3 (LocalStack) asynchronously via `ImageFlaggedEvent` + `@Async @EventListener`. Never blocks the alarm pipeline.
- **Staff directory** — staff records with jurisdiction, zone assignments, and shift info for alarm routing. Seeded from `avisos-knowledge/staff/staff.md` via `scripts/seed-staff.sh`.
- **Email notifications** — `EmailService` with dry-run safety mode (default on). Validates and sends via Spring's `JavaMailSender`.
- **Node lifecycle** — tracks heartbeats in an in-memory cache with flood protection, detects stale nodes via scheduled cleanup.
- **Web dashboard** — React 19 SPA with SCADA dark-mode theme. Pages: Dashboard, Nodes, Alarms, CLI terminal. Real-time updates over STOMP WebSocket.
- **REST API** — endpoints under `/api/` for nodes, alarms, health, system stats, AI analysis, RAG chat, and image retrieval.
- **CLI** — optional JLine-based REPL (disabled by default in web/Docker mode), also accessible via the WebSocket CLI bridge.
- **Health monitoring** — periodic database connectivity and disk space checks.
- **Persistence** — dual-database architecture: SQLite (JDBI) for transactional data; PostgreSQL (pgvector) for vector embeddings.

### `avisos-fleet-anomaly-detection-service`

A specialized Python microservice providing predictive anomaly detection for the entire node fleet. Built with **FastAPI** and **scikit-learn**.

Responsibilities:
- **Telemetry Ingestion** — Receives aggregated fleet metrics (responsive ratio, average battery level, last seen time) from the Java controller via HTTP POST `/fleet-health`.
- **Anomaly Detection** — Evaluates the incoming metrics using an **Isolation Forest** model to detect multivariate anomalies (e.g., a sudden drop in battery across multiple nodes, or a spike in unresponsiveness).
- **Callback Syncing** — Sends an `AnomalyReport` back to the Java controller's webhook asynchronously, which then caches the result and broadcasts it to the React dashboard.
- **Model Training** — Supports standalone training runs using a dedicated `anomaly-detector-training.Dockerfile` which serializes the fitted Isolation Forest model to disk.

### `avisos-knowledge`

Facility documentation and runbooks used as the RAG knowledge base for the AI analyst. Loaded into pgvector at startup.

```
avisos-knowledge/
├── facility/
│   ├── datacenter-layout.md
│   ├── equipment-zones.md
│   └── escalation-policy.md
├── nodes/
│   └── node-maintenance-history.md
├── runbooks/
│   ├── camera-obstruction.md
│   ├── environmental-drift.md
│   ├── low-battery.md
│   └── water-ingress.md
├── staff/
│   └── staff.md
└── ai/
    └── incident-analysis-guidelines.md
```

---

## Runtime Flows

### 1. Node Startup and Heartbeat

```
NodeApplication.main()
  │
  ├─ AppLifeCycle.init()
  │    ├─ Creates AppContainer (HashMap-based bean registry)
  │    ├─ AspectProcessor scans for annotations (scaffold)
  │    └─ AppContainer.init()
  │         ├─ ConfigLoader loads application.yml + env var overrides
  │         ├─ Wires: HardwareTelemetryProvider, PahoMqttProvider,
  │         │         ObjectMapper, HeartbeatService, NodeRuntime
  │         └─ All dependencies registered via constructor injection
  │
  ├─ Registers JVM shutdown hook → lifecycle.close()
  │
  └─ NodeRuntime.start()
       ├─ Spawns Connection Supervisor (virtual thread)
       │    └─ Connects to MQTT broker with exponential backoff
       ├─ Spawns Heartbeat Loop (virtual thread)
       │    └─ Every 30s:
       │         HeartbeatService.sendTelemetry()
       │           ├─ HardwareTelemetryProvider.readFrame()
       │           │    └─ HTTP GET /frame from simulator
       │           ├─ HardwareTelemetryProvider.readSnapshot()
       │           │    └─ HTTP GET /readings from simulator
       │           ├─ Build TelemetryPacketDto (JSON + image bytes)
       │           └─ MqttProvider.publish(topic, payload)
       └─ Spawns Watchdog (virtual thread)
            └─ Every 60s: check battery, log connection state
```

### 2. Telemetry Ingress, Threat Detection, and AI Analysis

This is the core data flow — what happens when telemetry arrives at the controller:

```
MQTT Message arrives on avisos/alerts
  │
  ├─ MqttIngressListener (Paho callback, dispatches to virtual thread)
  │
  ├─ MqttIngressAdapter
  │    └─ Converts Paho MqttMessage → IngressMessage (topic, payload, timestamp)
  │
  └─ TelemetryIngressHandler.handle(IngressMessage)
       │
       ├─ Deserialize JSON → TelemetryPacketDto
       │    (malformed packets are logged and discarded)
       │
       ├─ NodeService.updateNodeHeartbeat(dto)
       │    ├─ Flood check: reject if < minimum interval since last heartbeat
       │    ├─ Upsert node in SQLite (INSERT OR REPLACE)
       │    └─ Update in-memory cache
       │
       ├─ Publish NodeHeartbeatEvent → /topic/nodes (WebSocket)
       │
       ├─ If packet type == HEARTBEAT → return (no further processing)
       │
       ├─ VisionService.analyze(image data)
       │    └─ CodeProjectVisionClient → HTTP POST multipart/form-data
       │         → CodeProject.AI /v1/vision/detection
       │
       ├─ Publish VisionAnalysisEvent → /topic/vision (WebSocket)
       │
       ├─ ThreatDetector.evaluate(detected labels)
       │    └─ KeywordThreatDetector: case-insensitive match against
       │         critical/warning keywords from problematic-labels.yml
       │         → Returns AlarmSeverity (CRITICAL > WARNING > NONE)
       │
       └─ If severity > NONE:
            ├─ AlarmRecord created with s3ImageKey = null
            ├─ AlarmService.save(alarm) → SQLite
            │
            ├─ Publish AlarmCreatedEvent
            │    ├─ → /topic/alarms (WebSocket, via DashboardEventBroadcaster)
            │    │
            │    └─ → AlertAnalysistEventHandler (@Async)
            │         ├─ AlarmAnalystService.analyze(alarm)
            │         │    ├─ Builds prompt with alarm context + RAG knowledge
            │         │    ├─ Calls Ollama (llama3.2) via Spring AI ChatClient
            │         │    └─ Returns structured incident brief
            │         ├─ Saves AlarmAnalysisRecord to SQLite
            │         └─ Publishes AlarmAnalysisCreatedEvent
            │              └─ → /topic/alarm (WebSocket)
            │
            └─ Publish ImageFlaggedEvent
                 └─ → ImageStorageEventHandler (@Async)
                      ├─ ImageStorageService.store() → S3 (LocalStack)
                      │    key: {source}/{nodeId}/{timestamp}.jpg
                      └─ AlarmService.attachImage(alarmId, s3Key)
                           └─ Updates alarm record in SQLite
```

### 3. Controller Startup

```
AvisosControllerServiceApplication.main()
  │
  └─ SpringApplication.run()
       │
       ├─ Embedded Tomcat starts on port 8083
       │    └─ Actuator: /actuator/health, /actuator/info, /actuator/metrics
       │
       ├─ @Configuration classes initialize beans:
       │    ├─ JdbiConfiguration
       │    │    ├─ DataSource (HikariCP + SQLite JDBC)
       │    │    ├─ Jdbi instance with SqlObjectPlugin
       │    │    └─ Repositories (alarm, node, user, staff, alarm_analysis)
       │    ├─ PgVectorDataSourceConfiguration
       │    │    └─ Separate PostgreSQL DataSource for Spring AI vector store
       │    ├─ AwsConfiguration
       │    │    └─ S3Client bean (pointing to LocalStack)
       │    ├─ ServiceConfiguration
       │    │    ├─ Bridge beans (properties → domain config records)
       │    │    ├─ ObjectMapper, ThreatDetector, SecurityContext
       │    │    └─ @EnableAsync for event-driven side effects
       │    ├─ IngressConfiguration
       │    │    └─ MqttIngressListener (@PostConstruct subscribes to topic)
       │    ├─ HealthConfiguration
       │    │    └─ Health scheduler (every 10s) + stale node cleanup
       │    ├─ WebSocketConfiguration
       │    │    └─ STOMP broker, SockJS endpoint at /ws
       │    └─ CliConfiguration (conditional: avisos.cli.enabled=true)
       │
       ├─ VectorDatabaseInitializer (CommandLineRunner)
       │    └─ MarkdownLoaderService loads avisos-knowledge/*.md → pgvector
       │
       └─ Spring AI auto-configuration
            ├─ OllamaChatModel (llama3.2) + OllamaEmbeddingModel (nomic-embed-text)
            ├─ PgVectorStore (HNSW index, 768 dimensions)
            └─ Pull-model-strategy: WHEN_MISSING
```

### 4. Web Dashboard and Real-Time Updates

```
Browser loads http://localhost:8083
  │
  ├─ SpaForwardingController → forward:/index.html
  │    └─ React SPA loads (Vite-built bundle from /static/)
  │
  ├─ REST API (initial data load)
  │    ├─ GET /api/nodes         → NodeController → NodeService
  │    ├─ GET /api/alarms        → AlarmController → AlarmService
  │    ├─ GET /api/alarms/{id}/image → S3 image retrieval
  │    ├─ GET /api/health        → HealthController → SystemHealthMonitor
  │    ├─ GET /api/system/*      → SystemController → JVM runtime stats
  │    ├─ GET /api/ai/analyses/{id}  → AlarmAnalystController
  │    └─ GET /api/ai/ask-sme/chat   → SubjectMatterExpertChatController
  │
  └─ WebSocket (live updates)
       ├─ STOMP connect to /ws (SockJS)
       ├─ /topic/nodes   ← NodeHeartbeatEvent (every heartbeat)
       ├─ /topic/alarms  ← AlarmCreatedEvent (new alarm)
       ├─ /topic/vision  ← VisionAnalysisEvent (detection results)
       ├─ /topic/alarm   ← AlarmAnalysisCreatedEvent (AI analysis)
       └─ CLI Terminal via /app/cli (STOMP publish) → /topic/cli
```

### 5. Health Monitoring

```
Scheduled every 10 seconds:
  │
  SystemHealthMonitor.checkSystemHealth()
    │
    ├─ DatabaseHealthCheck
    │    └─ Execute "SELECT 1" with 500ms timeout
    │         → HEALTHY (with latency) or UNHEALTHY (timeout/error)
    │
    ├─ Disk Space Check
    │    └─ File("/").getUsableSpace()
    │         → HEALTHY (>= 100 MB) or DEGRADED (< 100 MB)
    │
    └─ Compute overall status:
         Any UNHEALTHY → system UNHEALTHY
         Else any DEGRADED → system DEGRADED
         Else → HEALTHY
```

---

## Data Model

### SQLite Tables (JDBI)

| Table | Primary Key | Purpose |
|---|---|---|
| `nodes` | `uuid` (TEXT) | Registered edge nodes with status, battery level, last_seen |
| `alarms` | `id` (TEXT, UUID) | Triggered alarms with severity, reason, status, s3_image_key |
| `alarm_analysis` | `id` (TEXT, UUID) | AI-generated incident analysis linked to alarms (FK cascade) |
| `staff` | `staff_id` (TEXT) | Datacenter personnel: name, email, phone, role, jurisdiction, zone, shift |
| `telemetry_audit` | auto | Audit log of received telemetry packets |
| `users` | `username` (TEXT) | Operator accounts with Argon2id password hashes |

All timestamps are stored as TEXT in ISO 8601 format. Transactional data is stored in SQLite and accessed through JDBI SQL Object repositories.

### PostgreSQL Tables (pgvector)

| Table | Purpose |
|---|---|
| `vector_store` | Spring AI managed table storing document chunks with embeddings (768-dimensional, HNSW index) |

### Domain Models

Domain objects are Java records (immutable). The codebase maintains a clear separation between:

- **Entities** (`entity/`) — database row representations, used by JDBI mappers.
- **Models** (`model/`) — domain objects used by services and the CLI: `AlarmRecord`, `AlarmAnalysisRecord`, `StaffRecord`, `NodeRecord`, `EmailMessage`, `EmailDeliveryResult`.
- **DTOs** (`common/`) — serialization types for MQTT telemetry.
- **Mappers** (`mapper/`) — convert between entities and domain models.

---

## Concurrency Model

Both services use Java 25 **virtual threads** (`Executors.newVirtualThreadPerTaskExecutor()`):

- **Node service** — three virtual-thread loops (connection, heartbeat, watchdog) managed by `NodeRuntime`.
- **Controller service** — MQTT message handling, CLI command execution, health checks, and `@Async` event handlers all run on virtual threads.

Async event handlers (enabled via `@EnableAsync` on `ServiceConfiguration`):

- `ImageStorageEventHandler` — S3 upload on alarm creation.
- `AlertAnalysistEventHandler` — LLM incident analysis on alarm creation.

Thread safety considerations:

- `SimpleNodeService` uses `ConcurrentHashMap` for the in-memory node cache and `AtomicLong` for metrics counters.
- `SecurityContext` uses `InheritableThreadLocal` so child virtual threads inherit the authenticated user.
- `DeduplicatingLogger` uses `ConcurrentHashMap` with `AtomicInteger` counters.
- `SystemHealthMonitor` stores the latest health report in a `volatile` field.
- `ImageStorageService.ensureBucketExists()` uses a `boolean` flag (safe under `@Async` single-caller pattern).

---

## Security Model

- **Password hashing** — Argon2id (3 iterations, 64 MB memory, 1 thread).
- **Session context** — `InheritableThreadLocal<UserRecord>` cleared after use.
- **Password hashing** — Argon2id for authentication secrets.
- **First-run bootstrap** — when no users exist, the CLI prompts to create the initial operator account.
- **Email dry-run** — `EmailService` defaults to dry-run mode to prevent accidental sends.
- **Role field** — stored in the database but role-based access control is not yet enforced.

---

## Configuration

### Controller Service

Configuration is managed via Spring Boot `@ConfigurationProperties` records bound to the `avisos.*` prefix in `application.yml`. Spring Boot's standard environment variable binding applies (e.g., `AVISOS_MQTT_BROKER` overrides `avisos.mqtt.broker`).

| Properties class | Prefix | Key fields |
|---|---|---|
| `AvisosMqttProperties` | `avisos.mqtt` | controllerClientId, broker, topic, connectionTimeout, cleanSession, automaticReconnect |
| `AvisosVisionProperties` | `avisos.vision` | apiUrl, minConfidence, timeoutSeconds |
| `AvisosDatabaseProperties` | `avisos.database` | url |
| `AvisosNodeServiceProperties` | `avisos.node-service` | staleThreshold, minHeartbeatIntervalMs |
| `AvisosCliProperties` | `avisos.cli` | enabled |
| `AvisosAwsProperties` | `avisos.aws` | endpoint, region, s3BucketName, accessKeyId, secretAccessKey |
| `RagProperties` | `avisos.ai.rag` | knowledgeBaseDir |

Spring AI configuration (under `spring.ai.*`):

| Property | Value | Purpose |
|---|---|---|
| `spring.ai.ollama.base-url` | `http://avisos-ollama:11434` | Ollama server |
| `spring.ai.ollama.chat.model` | `llama3.2` | Chat/reasoning model |
| `spring.ai.ollama.embedding.model` | `nomic-embed-text` | Embedding model (768 dims) |
| `spring.ai.ollama.init.pull-model-strategy` | `WHEN_MISSING` | Auto-pull on first start |
| `spring.ai.vectorstore.pgvector.index-type` | `HNSW` | Vector index type |
| `spring.ai.vectorstore.pgvector.dimensions` | `768` | Matches nomic-embed-text |

### Environment Variables (Docker)

| Variable | Purpose |
|---|---|
| `AVISOS_MQTT_BROKER` | MQTT broker connection string |
| `AVISOS_MQTT_TOPIC` | Telemetry subscription topic |
| `AVISOS_VISION_API_URL` | CodeProject.AI endpoint |
| `AVISOS_DATABASE_URL` | SQLite JDBC connection string |
| `AVISOS_CLI_ENABLED` | Enable JLine CLI (default `false`) |
| `AVISOS_AWS_ENDPOINT` | S3/LocalStack endpoint |
| `AVISOS_AWS_REGION` | AWS region |
| `AVISOS_AWS_S3_BUCKET_NAME` | Bucket for flagged images |
| `AVISOS_AWS_ACCESS_KEY_ID` | AWS credentials (LocalStack: `test`) |
| `AVISOS_AWS_SECRET_ACCESS_KEY` | AWS credentials (LocalStack: `test`) |
| `AVISOS_AI_OLLAMA_BASE_URL` | Ollama server URL |

### Node Service

Configuration is loaded from `application.yml` by the custom `ConfigLoader` with environment variable overrides.

| Variable | Purpose |
|---|---|
| `MQTT_BROKER_URL` | MQTT broker connection string |
| `MQTT_TOPIC` | Telemetry publish topic |
| `NODE_NAME` | Human-readable node identifier |
| `NODE_TYPE` | Device classification |
| `HARDWARE_PROVIDER` | `local` or `simulator-rest` |
| `HARDWARE_SIMULATOR_BASE_URL` | Simulator REST URL (simulator mode) |

---

## Infrastructure Services (Docker Compose)

| Service | Container | Port | Purpose |
|---|---|---|---|
| `controller` | avisos-controller | 8083 | Central orchestration (Spring Boot, CLI disabled) |
| `fleet-anomaly-detection-api` | fleet-anomaly-detection-api | 8000 | Python/FastAPI fleet anomaly detection |
| `postgres-anomaly-detection-db` | postgres-anomaly-detection-db | 5434→5432 | Dedicated PostgreSQL DB for ML service |
| `mosquitto` | avisos-broker | 1883 | MQTT message broker |
| `localstack` | avisos-cloud | 4567→4566 | AWS S3/SNS emulation |
| `vision-api` | avisos-VisionRequest | 32168 | CodeProject.AI object detection |
| `ollama` | avisos-ollama | 11434 | On-prem LLM (llama3.2 + nomic-embed-text) |
| `pgvector` | avisos-pgvector | 5433→5432 | PostgreSQL with pgvector for embeddings |

All services run on the `avisos-net` bridge network. The controller also joins `proxy-net` (external) for reverse proxy access.

Simulator-backed nodes are launched on demand by `scripts/spawn-test-fleet.sh`. The full demo environment can be bootstrapped with `scripts/setup-demo-environment.sh` which starts the stack, seeds staff, and spawns a fleet.

A separate `docker-compose.dev.yml` exists for local development without the external proxy network.

---

## Key Design Decisions

### Spring Boot for Controller, Custom IoC for Node

The controller uses Spring Boot 3.4.1 for auto-configuration, embedded web server, and actuator. The Spring Boot BOM is imported (not used as parent POM) to preserve the `avisos-parent` Maven hierarchy. The node uses a hand-rolled IoC container (`framework/` package) to keep the edge deployment footprint minimal.

### Dual-Database Architecture

**SQLite** stores transactional data — alarms, nodes, staff, users, audit logs. JDBI SQL Object interfaces provide lightweight, SQL-first access. **PostgreSQL** (pgvector) stores vector embeddings for the RAG knowledge base. A separate `@Primary` DataSource bean (`PgVectorDataSourceConfiguration`) ensures Spring AI auto-configuration uses PostgreSQL while JDBI continues using the SQLite DataSource.

### Event-Driven Side Effects

Alarm creation fires `AlarmCreatedEvent`, which triggers two async listeners: S3 image upload (`ImageStorageEventHandler`) and AI incident analysis (`AlertAnalysistEventHandler`). Neither blocks the alarm pipeline. The `@Async` + `@EventListener` pattern ensures the alarm is saved and broadcast to WebSocket clients immediately; S3 storage and LLM analysis happen in the background and update the alarm record when complete.

### RAG over REST (Not MCP)

The app is the orchestrator — it decides when to call the AI, what to send it, and how to handle the response. This is a standard REST client pattern (Spring AI wrapping Ollama's REST API), not MCP. MCP would be appropriate if the AI needed to autonomously discover and call external tools.

### MQTT over HTTP/gRPC for Telemetry

Nodes publish telemetry over MQTT (pub/sub) rather than making direct HTTP calls. This decouples node availability from controller availability and lets the broker handle message buffering.

### Virtual Threads Everywhere

Both services use Java 25 virtual threads for all concurrent work, eliminating the need for reactive frameworks or manual thread pool sizing.

### Simulator-Provided Camera Frames

The C++ hardware simulator serves real datacenter CCTV images (not random bytes) via `GET /frame`, weighted 90% normal / 10% alarm. This gives CodeProject.AI meaningful images to analyze during demos and testing.
