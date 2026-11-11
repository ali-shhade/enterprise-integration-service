
#!/usr/bin/env bash

set -uo pipefail

# ------------------------------------------------
# Configuration
# ------------------------------------------------

PROJECT_DIR="${PROJECT_DIR:-$HOME/projects/enterprise-integration-service}"

LOG_FILE="${LOG_FILE:-$PROJECT_DIR/logs/integration-service.log}"

HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"

ISSUES=0

# ------------------------------------------------
# Check required commands
# ------------------------------------------------

for cmd in curl jq awk; do

    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "ERROR: Required command missing: $cmd"
        exit 2
    fi

done

# ------------------------------------------------
# Report header
# ------------------------------------------------

echo "=========================================="
echo " Integration Monitoring Report"
echo "=========================================="

echo "Time: $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo

# ------------------------------------------------
# 1. Application health
# ------------------------------------------------

echo "[1] APPLICATION HEALTH"

APP_UP=0

if HTTP_CODE=$(curl \
    --silent \
    --show-error \
    --output /dev/null \
    --write-out '%{http_code}' \
    --connect-timeout 3 \
    --max-time 5 \
    "$HEALTH_URL" 2>/dev/null); then

    if [[ "$HTTP_CODE" == "200" ]]; then
        echo "STATUS: UP (HTTP 200)"
        APP_UP=1
    else
        echo "STATUS: UNHEALTHY (HTTP $HTTP_CODE)"
        ISSUES=$((ISSUES + 1))
    fi

else
    echo "STATUS: DOWN (health endpoint unavailable)"
    ISSUES=$((ISSUES + 1))
fi

echo

# ------------------------------------------------
# 2. Order and error statistics
# ------------------------------------------------

echo "[2] ORDER AND ERROR STATISTICS"

if [[ -f "$LOG_FILE" ]]; then

    # Verify that the log file contains valid JSON objects.
    if jq -e -s 'all(.[]; type == "object")' \
        "$LOG_FILE" >/dev/null 2>&1; then

        SUCCESS_COUNT=$(jq -r \
            'select(.event == "order_status_changed" and .status == "SUCCESS") | .orderId' \
            "$LOG_FILE" | awk 'END {print NR + 0}')

        FAILED_COUNT=$(jq -r \
            'select(.event == "order_status_changed" and .status == "FAILED") | .orderId' \
            "$LOG_FILE" | awk 'END {print NR + 0}')

        REJECTED_COUNT=$(jq -r \
            'select(.event == "order_status_changed" and .status == "REJECTED") | .orderId' \
            "$LOG_FILE" | awk 'END {print NR + 0}')

        ERROR_COUNT=$(jq -r \
            'select(.level == "ERROR") | .message' \
            "$LOG_FILE" | awk 'END {print NR + 0}')

        WARN_COUNT=$(jq -r \
            'select(.level == "WARN") | .message' \
            "$LOG_FILE" | awk 'END {print NR + 0}')

        echo "Log file: $LOG_FILE"
        echo "Scope: active log file only"
        echo
        echo "Successful order events: $SUCCESS_COUNT"
        echo "Failed order events:     $FAILED_COUNT"
        echo "Rejected order events:   $REJECTED_COUNT"
        echo "ERROR log entries:       $ERROR_COUNT"
        echo "WARN log entries:        $WARN_COUNT"

        if (( FAILED_COUNT > 0 )); then
            ISSUES=$((ISSUES + 1))
        fi

        if (( ERROR_COUNT > 0 )); then
            ISSUES=$((ISSUES + 1))
        fi

    else

        echo "WARNING: Log file is not valid JSON."
        echo "Check the structured logging configuration."
        ISSUES=$((ISSUES + 1))

    fi

else

    echo "WARNING: Application log file not found."
    ISSUES=$((ISSUES + 1))

fi

echo

# ------------------------------------------------
# 3. Final result
# ------------------------------------------------

echo "=========================================="

if (( ISSUES == 0 )); then

    echo "OVERALL STATUS: OK"

else

    echo "OVERALL STATUS: DEGRADED"
    echo "Issues detected: $ISSUES"

fi

echo "=========================================="

if (( ISSUES > 0 )); then
    exit 1
fi

exit 0
