# Avisos Hardware Simulator

C++17 service that simulates node hardware readings for AVISOS demos, testing, and local development. It exposes a small REST API that the Java node service can poll when running in simulator mode.

The simulator is deliberately not a production device service. It exists to create realistic enough hardware conditions for demos, load testing, and integration testing without needing physical sensors. In a real deployment, the node service would read from actual device hardware or a real hardware integration layer instead.

## Responsibilities

- Generate simulated hardware snapshots with battery, temperature, pressure, humidity, leak status, signal quality, and timestamp values.
- Serve the latest snapshot at `GET /readings`.
- Serve a simple liveness response at `GET /health`.
- Optionally serve demo camera frames at `GET /frame`.
- Run as a standalone process or as a Docker container paired with a Java node container.

## Local Notes

Build with CMake from this directory:

```bash
cmake -S . -B build
cmake --build build
```

Run the compiled binary:

```bash
./build/avisos_hardware_simulator
```

By default the HTTP server listens on port `5000`.

## Project Role

Use this service when you need repeatable demo/test telemetry. The controller does not talk to it directly; the Java node service polls the simulator and then publishes the normal MQTT telemetry contract to the controller.
