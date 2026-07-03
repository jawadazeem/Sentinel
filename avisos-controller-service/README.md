# Avisos Controller Service

Spring Boot service that acts as the central AVISOS control plane. It receives telemetry from datacenter nodes over MQTT, stores node/alarm state, coordinates vision analysis, exposes REST APIs, serves the React web dashboard, and broadcasts live updates over WebSocket.

The controller is the main operator-facing service in the project. It owns the dashboard experience, alarm lifecycle, staff records, image retrieval, health reporting, RAG/Sherwood analysis endpoints, and integration points for supporting services such as CodeProject.AI, LocalStack/S3, Ollama, PgVector, and the fleet anomaly detector.

## Responsibilities

- Subscribe to MQTT telemetry from node services.
- Maintain SQLite-backed transactional state through JDBI repositories.
- Store and retrieve flagged images through the configured S3-compatible backend.
- Expose REST APIs under `/api/**`.
- Serve the React/Vite frontend from the Spring Boot jar.
- Publish live dashboard updates through STOMP over WebSocket at `/ws`.
- Coordinate AI/RAG and fleet anomaly integrations.

## Local Notes

From the repository root:

```bash
mvn -pl avisos-controller-service clean compile
```

The full application is usually run through Docker Compose:

```bash
docker compose -f docker-compose.dev.yml up --build -d
```

The dashboard is available at `http://localhost:8083` when the controller is running through Compose.

## Project Role

This is the central service. Other components either feed it data, support its analysis pipeline, or help create realistic demo/test conditions.
