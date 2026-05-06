#!/usr/bin/env bash
# Wait for both schema registries to become ready.
# Usage: ./wait-for-ready.sh [timeout_seconds]

set -euo pipefail

TIMEOUT=${1:-120}
CONFLUENT_URL="${CONFLUENT_URL:-http://localhost:8081}"
APICURIO_URL="${APICURIO_URL:-http://localhost:8082}"

echo "Waiting up to ${TIMEOUT}s for registries to be ready..."

check_url() {
    local name="$1" url="$2" start elapsed
    start=$(date +%s)
    while true; do
        if curl -sf "${url}" > /dev/null 2>&1; then
            elapsed=$(($(date +%s) - start))
            echo "${name} is ready (${elapsed}s)"
            return 0
        fi
        elapsed=$(($(date +%s) - start))
        if [ "${elapsed}" -ge "${TIMEOUT}" ]; then
            echo "ERROR: ${name} did not become ready within ${TIMEOUT}s"
            return 1
        fi
        sleep 2
    done
}

check_url "Confluent-SR" "${CONFLUENT_URL}/subjects"
check_url "Apicurio-SR"  "${APICURIO_URL}/apis/ccompat/v7/subjects"

echo "All registries are ready."
