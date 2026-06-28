#!/usr/bin/env bash
# -------------------------------------------------------------------
# spawn-test-fleet.sh — Build, spawn, and manage simulator+node pairs
#
# Usage:
#   ./scripts/spawn-test-fleet.sh build           # build node + simulator images
#   ./scripts/spawn-test-fleet.sh                  # spawn deterministic 20-node demo fleet
#   ./scripts/spawn-test-fleet.sh random 10        # spawn 10 random pairs
#   ./scripts/spawn-test-fleet.sh 100              # spawn 100 random pairs for load testing
#   ./scripts/spawn-test-fleet.sh teardown         # remove all pairs
#   ./scripts/spawn-test-fleet.sh status           # show running pairs
#
# Prerequisites:
#   docker compose up -d                           # core infra (controller, mosquitto, etc.)
# -------------------------------------------------------------------

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

NETWORK="${AVISOS_DOCKER_NETWORK:-}"
COMPOSE_NETWORK_KEY="avisos-net"
NODE_IMAGE="avisos-node:latest"
SIM_IMAGE="avisos-hardware-simulator:latest"
LABEL="avisos.fleet=test"

MQTT_BROKER="tcp://mosquitto:1883"
MQTT_TOPIC="avisos/telemetry"

DEMO_NODE_IDS=(
    "00000000-0000-4000-8000-000000000001"
    "00000000-0000-4000-8000-000000000002"
    "00000000-0000-4000-8000-000000000003"
    "00000000-0000-4000-8000-000000000004"
    "00000000-0000-4000-8000-000000000005"
    "00000000-0000-4000-8000-000000000006"
    "00000000-0000-4000-8000-000000000007"
    "00000000-0000-4000-8000-000000000008"
    "00000000-0000-4000-8000-000000000009"
    "00000000-0000-4000-8000-000000000010"
    "00000000-0000-4000-8000-000000000011"
    "00000000-0000-4000-8000-000000000012"
    "00000000-0000-4000-8000-000000000013"
    "00000000-0000-4000-8000-000000000014"
    "00000000-0000-4000-8000-000000000015"
    "00000000-0000-4000-8000-000000000016"
    "00000000-0000-4000-8000-000000000017"
    "00000000-0000-4000-8000-000000000018"
    "00000000-0000-4000-8000-000000000019"
    "00000000-0000-4000-8000-000000000020"
)

DEMO_NODE_NAMES=(
    "A-A2-ENV-01"
    "A-A5-ENV-02"
    "A-SF-LEAK-01"
    "A-SF-LEAK-02"
    "B-MECH-ENV-01"
    "B-MECH-LEAK-01"
    "G-NET-ENV-01"
    "G-NET-LEAK-01"
    "G-PATCH-ENV-01"
    "D-TRANSIT-LEAK-01"
    "E-ELEC-ENV-01"
    "E-ELEC-LEAK-01"
    "A-A1-ENV-03"
    "A-A3-ENV-04"
    "A-A4-ENV-05"
    "A-A6-ENV-06"
    "B-MECH-ENV-02"
    "D-TRANSIT-ENV-01"
    "D-TRANSIT-ENV-02"
    "E-ELEC-ENV-02"
)

# -- helpers --

log()  { echo "[fleet] $*"; }
fail() { echo "[fleet] ERROR: $*" >&2; exit 1; }

trap 'fail "Command failed at line $LINENO: $BASH_COMMAND"' ERR

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        fail "Required command '$1' is not installed or not on PATH."
    fi
}

new_uuid() {
    if command -v uuidgen >/dev/null 2>&1; then
        uuidgen | tr '[:upper:]' '[:lower:]'
        return
    fi

    if [ -r /proc/sys/kernel/random/uuid ]; then
        cat /proc/sys/kernel/random/uuid
        return
    fi

    fail "Cannot generate node UUID: install uuidgen or provide /proc/sys/kernel/random/uuid."
}

container_safe_name() {
    printf "%s" "$1" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9_.-' '-'
}

ensure_network() {
    resolve_network

    if ! docker network inspect "$NETWORK" &>/dev/null; then
        fail "Docker network '$NETWORK' does not exist. Start the core stack first: docker compose up -d, or set AVISOS_DOCKER_NETWORK explicitly."
    fi

    log "Using Docker network: $NETWORK"
}

resolve_network() {
    if [ -n "$NETWORK" ]; then
        return
    fi

    local compose_env_args=()
    if [ -f "$PROJECT_ROOT/.env.example" ]; then
        compose_env_args=(--env-file "$PROJECT_ROOT/.env.example")
    fi

    local resolved
    resolved=$(docker network ls \
        --filter "label=com.docker.compose.project=avisos" \
        --filter "label=com.docker.compose.network=$COMPOSE_NETWORK_KEY" \
        --format "{{.Name}}" \
        | head -n 1 || true)

    if [ -z "$resolved" ]; then
        resolved=$(docker compose "${compose_env_args[@]}" -f "$PROJECT_ROOT/docker-compose.yml" config 2>/dev/null \
            | awk -v key="$COMPOSE_NETWORK_KEY" '
                /^networks:/ { in_networks = 1; next }
                in_networks && $1 == key ":" { in_target = 1; next }
                in_target && $1 == "name:" { print $2; exit }
            ' || true)
    fi

    NETWORK="${resolved:-avisos_${COMPOSE_NETWORK_KEY}}"
}

ensure_images() {
    local missing=()
    for img in "$NODE_IMAGE" "$SIM_IMAGE"; do
        if ! docker image inspect "$img" &>/dev/null; then
            missing+=("$img")
        fi
    done
    if [ ${#missing[@]} -gt 0 ]; then
        log "Missing images: ${missing[*]}"
        log "Building missing fleet images now..."
        cmd_build
    fi
}

# -- commands --

cmd_build() {
    log "Building node image..."
    docker build -t "$NODE_IMAGE" -f "$PROJECT_ROOT/avisos-node-service/node.Dockerfile" "$PROJECT_ROOT"

    log "Building simulator image..."
    docker build -t "$SIM_IMAGE" -f "$PROJECT_ROOT/avisos-hardware-simulator/hardware-simulator.Dockerfile" "$PROJECT_ROOT/avisos-hardware-simulator"

    log "Images built."
}

cmd_spawn() {
    local count="${1:-10}"
    require_command docker

    if ! [[ "$count" =~ ^[0-9]+$ ]] || [ "$count" -lt 1 ]; then
        fail "Count must be a positive integer. Received: '$count'"
    fi

    ensure_network
    ensure_images

    log "Spawning $count simulator+node pair(s)..."

    local spawned=0
    for i in $(seq 1 "$count"); do
        local node_id
        local id
        node_id=$(new_uuid)
        id=$(echo "$node_id" | tr -d '-' | cut -c 1-6)
        local sim_name="sim-${id}"
        local node_name="node-${id}"

        spawn_pair "$node_id" "$node_name" "$sim_name" "$node_name"
        spawned=$((spawned + 1))
        log "  [$spawned/$count] $node_name <-> $sim_name"
    done

    log "Done. $spawned pair(s) running."
}

cmd_spawn_demo() {
    require_command docker

    ensure_network
    ensure_images

    local count="${#DEMO_NODE_NAMES[@]}"
    log "Spawning deterministic $count-node demo fleet..."

    local spawned=0
    for index in "${!DEMO_NODE_NAMES[@]}"; do
        local node_id="${DEMO_NODE_IDS[$index]}"
        local node_name="${DEMO_NODE_NAMES[$index]}"
        local sim_name
        sim_name="sim-$(container_safe_name "$node_name")"

        spawn_pair "$node_id" "$node_name" "$sim_name"
        spawned=$((spawned + 1))
        log "  [$spawned/$count] $node_name <-> $sim_name"
    done

    log "Done. $spawned deterministic pair(s) running."
}

spawn_pair() {
    local node_id="$1"
    local node_name="$2"
    local sim_name="$3"
    local container_node_name
    container_node_name="${4:-node-$(container_safe_name "$node_name")}"

    if docker container inspect "$sim_name" &>/dev/null || docker container inspect "$container_node_name" &>/dev/null; then
        fail "Container '$sim_name' or '$container_node_name' already exists. Run './scripts/spawn-test-fleet.sh teardown' first."
    fi

    # Start simulator (serves GET /readings on :5000)
    docker run -d \
        --name "$sim_name" \
        --network "$NETWORK" \
        --label "$LABEL" \
        --restart unless-stopped \
        "$SIM_IMAGE" > /dev/null

    # Start node (polls simulator, publishes telemetry over MQTT)
    docker run -d \
        --name "$container_node_name" \
        --network "$NETWORK" \
        --label "$LABEL" \
        --restart unless-stopped \
        -e HARDWARE_PROVIDER=simulator-rest \
        -e HARDWARE_SIMULATOR_BASE_URL="http://${sim_name}:5000" \
        -e MQTT_BROKER_URL="$MQTT_BROKER" \
        -e MQTT_TOPIC="$MQTT_TOPIC" \
        -e NODE_ID="$node_id" \
        -e NODE_NAME="$node_name" \
        -e NODE_TYPE=data-acquisition-device \
        "$NODE_IMAGE" > /dev/null
}

cmd_teardown() {
    log "Tearing down fleet containers..."

    local containers
    containers=$(docker ps -aq --filter "label=$LABEL" || true)

    if [ -z "$containers" ]; then
        log "No fleet containers found."
        return
    fi

    local count
    count=$(echo "$containers" | wc -l | tr -d ' ')

    docker rm -f $containers > /dev/null
    log "Removed $count containers."
}

cmd_status() {
    local count
    count=$(docker ps -q --filter "label=$LABEL" 2>/dev/null | wc -l | tr -d ' ')

    if [ "$count" -eq 0 ]; then
        log "No fleet containers running."
        return
    fi

    docker ps --filter "label=$LABEL" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"
    log "$((count / 2)) pair(s) running."
}

# -- main --

case "${1:-}" in
    build)    cmd_build ;;
    teardown) cmd_teardown ;;
    status)   cmd_status ;;
    random)   cmd_spawn "${2:-10}" ;;
    -h|--help)
        echo "Usage: $0 [build|random count|count|teardown|status]"
        echo ""
        echo "  build        Build node + simulator Docker images"
        echo "  (no args)    Spawn deterministic 20-node demo fleet"
        echo "  random <n>   Spawn N random pairs (default: 10)"
        echo "  <count>      Spawn N random pairs (e.g. 1, 100, 1000)"
        echo "  teardown     Remove all fleet containers"
        echo "  status       Show running fleet containers"
        ;;
    "")       cmd_spawn_demo ;;
    *)        cmd_spawn "$1" ;;
esac
