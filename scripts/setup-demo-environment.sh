#!/usr/bin/env bash
# -------------------------------------------------------------------
# setup-demo-environment.sh — Start the local Avisos demo environment
#
# Starts the Docker demo stack, seeds staff, and launches a node fleet.
#
# Usage:
#   ./scripts/setup-demo-environment.sh
#   ./scripts/setup-demo-environment.sh --nodes 10
#   ./scripts/setup-demo-environment.sh --random-nodes 50
#   ./scripts/setup-demo-environment.sh --skip-build
# -------------------------------------------------------------------

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMPOSE_FILE="$PROJECT_ROOT/docker-compose.dev.yml"
BUILD=true
NODE_MODE="demo"
NODE_COUNT=""

log() { echo "[demo-setup] $*"; }
fail() {
    echo "[demo-setup] ERROR: $*" >&2
    exit 1
}

usage() {
    cat <<'EOF'
Usage: ./scripts/setup-demo-environment.sh [options]

Options:
  --nodes <n>          Spawn deterministic demo fleet when n is 20, otherwise spawn n random nodes.
  --random-nodes <n>   Spawn n random nodes.
  --skip-build         Start compose without rebuilding controller image.
  -h, --help           Show this help.
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command '$1' is not installed or not on PATH."
}

parse_args() {
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --nodes)
                [ "${2:-}" ] || fail "--nodes requires a value."
                NODE_COUNT="$2"
                shift 2
                ;;
            --random-nodes)
                [ "${2:-}" ] || fail "--random-nodes requires a value."
                NODE_MODE="random"
                NODE_COUNT="$2"
                shift 2
                ;;
            --skip-build)
                BUILD=false
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                fail "Unknown argument: $1"
                ;;
        esac
    done
}

# wait_for_controller() {
#     local url="http://localhost:8083/actuator/health"
#     local attempts=60

#     log "Waiting for controller health endpoint..."
#     for _ in $(seq 1 "$attempts"); do
#         if curl -fsS "$url" >/dev/null 2>&1; then
#             log "Controller is healthy."
#             return
#         fi
#         sleep 2
#     done

#     fail "Controller did not become healthy at $url"
# }

main() {
    parse_args "$@"
    require_command docker
    require_command curl

    cd "$PROJECT_ROOT"

    if [ "$BUILD" = true ]; then
        log "Starting core stack with rebuild..."
        docker compose -f "$COMPOSE_FILE" up -d --build
    else
        log "Starting core stack..."
        docker compose -f "$COMPOSE_FILE" up -d
    fi

    # wait_for_controller

    log "Seeding staff directory..."
    "$PROJECT_ROOT/scripts/seed-staff.sh" --yes

    log "Starting node fleet..."
    if [ "$NODE_MODE" = "random" ]; then
        "$PROJECT_ROOT/scripts/spawn-test-fleet.sh" random "${NODE_COUNT:-20}"
    elif [ -z "$NODE_COUNT" ] || [ "$NODE_COUNT" = "20" ]; then
        "$PROJECT_ROOT/scripts/spawn-test-fleet.sh"
    else
        "$PROJECT_ROOT/scripts/spawn-test-fleet.sh" "$NODE_COUNT"
    fi

    log "Demo environment is ready at http://localhost:8083"
}

main "$@"
