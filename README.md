# AVISOS
***A***dvanced ***V***isual ***I***nfrastructure ***S***ecure ***O***perational ***S***ystems

**Author:** Jawad Azeem
**Live:** [avisos.jawadazeem.com](https://avisos.jawadazeem.com)

AVISOS is a SCADA orchestration platform that secures and monitors high-reliability environments using computer vision AI for real-time threat detection. Edge nodes publish telemetry over MQTT, the central controller evaluates frames through a vision AI pipeline, and a Python-based machine learning service continually monitors fleet-wide metrics for behavioral anomalies. Operators interact through a React web dashboard with live WebSocket updates and an embedded CLI terminal.

### Architecture

![Architecture](images/architecture-diagram.svg)

### Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 25 (preview), Spring Boot 3.4.1, Virtual Threads |
| Frontend | React 19, TypeScript, Vite, xterm.js |
| AI / RAG | Ollama (llama3.2 + nomic-embed-text), Spring AI, pgvector |
| Fleet ML | Python 3, FastAPI, scikit-learn (Isolation Forest), Pydantic |
| Messaging | Eclipse Mosquitto (MQTT), Protobuf telemetry |
| Real-time | STOMP over WebSocket (SockJS) |
| Database | SQLite (JDBI) + PostgreSQL (pgvector, anomaly data) |
| Vision AI | CodeProject.AI object detection |
| Hardware Simulation | C++17, CMake, cpp-httplib, nlohmann/json |
| Cloud | AWS S3 + SNS via LocalStack |
| Notifications | Spring Mail (JavaMailSender), dry-run safety mode |
| Security | Argon2id password hashing |
| Build | Maven multi-module, frontend-maven-plugin (Node 22) |
| Deploy | Docker Compose + on-demand simulator/node fleet containers |

### Module Structure

```
avisos-common-lib/                      Protobuf definitions + generated code (telemetry.proto)
avisos-controller-service/              Central orchestration: REST API, dashboard, alarms, vision, CLI
avisos-fleet-anomaly-detection-service/ Python/FastAPI service using scikit-learn for fleet anomaly detection
avisos-node-service/                    Lightweight datacenter sensor node: heartbeat, battery, telemetry
avisos-hardware-simulator/              C++ hardware simulator: REST readings for node-service polling
avisos-knowledge/                       Datacenter runbooks and facility docs for future RAG enrichment
mosquitto/                  MQTT broker configuration (Eclipse Mosquitto)
```

### Quick Start

```bash
# Full stack (controller + node + broker + vision + cloud)
docker compose up --build

# Frontend dev server with hot reload (proxies to backend on :8080)
cd avisos-controller-service/src/main/frontend && npm run dev

# Build all modules
mvn clean install

# Build node + C++ hardware simulator images for fleet testing
./scripts/spawn-test-fleet.sh build

# Spawn 10 simulator+node pairs against the running core stack
./scripts/spawn-test-fleet.sh 10

# Dry-run removal of random/load-test nodes from the SQLite DB
./scripts/purge-test-nodes.sh
```

Dashboard at `http://localhost:8083` -- pages for system overview, node monitoring, alarm management, and an embedded CLI terminal. All updates stream in real-time over WebSocket.

The C++ hardware simulator runs as a standalone REST process and exposes hardware readings for the Java node service. In simulator mode, each node polls its paired simulator via `HARDWARE_SIMULATOR_BASE_URL`, then publishes the existing MQTT telemetry contract to the controller.

`scripts/purge-test-nodes.sh` is an operator-only maintenance helper for demos and load tests. It defaults to dry-run mode and targets random/load-test nodes while preserving the deterministic 20-node demo fleet. Add `--yes` to execute deletion, or `--include-demo --yes` when intentionally resetting the canonical demo fleet.

### API

| Endpoint | Description |
|---|---|
| `GET /api/nodes` | List registered edge nodes |
| `GET /api/alarms` | Active alarms with severity |
| `POST /api/alarms/{id}/resolve` | Resolve an alarm |
| `GET /api/alarms/{id}/image` | Retrieve flagged image from S3 |
| `GET /api/ai/analyses/{id}` | AI-generated incident analysis for an alarm |
| `GET /api/ai/ask-sme/chat?query=` | RAG-powered Q&A against facility knowledge base |
| `POST /api/rag/load` | Manually re-ingest knowledge base into vector store |
| `GET /api/health` | System health report |
| `GET /api/system/stats` | JVM runtime statistics |
| `GET /api/fleet-metrics/anomaly-report/latest` | Latest fleet anomaly report (from Python ML service) |
| `GET /api/fleet-metrics/metrics/latest` | Latest raw fleet metrics (from Python ML service) |
| `WS /ws` | STOMP WebSocket (topics: `/topic/nodes`, `/topic/alarms`, `/topic/vision`, `/topic/alarm`, `/topic/cli`) |

### Roadmap

See [`docs/future-directions.md`](docs/future-directions.md) for planned features including the AI SOC Analyst Agent.

### License

Apache 2.0
