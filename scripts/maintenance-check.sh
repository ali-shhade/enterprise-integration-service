
#!/usr/bin/env bash

set -uo pipefail

# ---------------------------------------
# Configuration
# ---------------------------------------

PROJECT_DIR="${PROJECT_DIR:-$HOME/projects/enterprise-integration-service}"

HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"

CERT_HOST="${CERT_HOST:-}"
CERT_PORT="${CERT_PORT:-443}"
CERT_FILE="${CERT_FILE:-}"

CERT_WARN_DAYS="${CERT_WARN_DAYS:-30}"
DISK_WARN_PERCENT="${DISK_WARN_PERCENT:-80}"
MEM_WARN_MB="${MEM_WARN_MB:-512}"

ISSUES=0
CERT_TEMP_FILE=""
CERT_TO_CHECK=""

# ---------------------------------------
# Cleanup temporary files on exit
# ---------------------------------------

cleanup() {
    if [[ -n "$CERT_TEMP_FILE" && -f "$CERT_TEMP_FILE" ]]; then
        rm -f -- "$CERT_TEMP_FILE"
    fi
}

trap cleanup EXIT

# ---------------------------------------
# Helper function
# ---------------------------------------

report_issue() {
    printf 'WARNING: %s\n' "$1"
    ISSUES=$((ISSUES + 1))
}

# ---------------------------------------
# Check required commands
# ---------------------------------------

for cmd in curl openssl df awk free timeout mktemp; do

    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "ERROR: Required command not found: $cmd"
        exit 2
    fi

done

# ---------------------------------------
# Start report
# ---------------------------------------

echo "======================================"
echo " Integration Server Maintenance Check"
echo "======================================"

echo "Time: $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo

# ---------------------------------------
# 1. Check application availability
# ---------------------------------------

echo "[1] APPLICATION HEALTH"

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
    else
        report_issue "Application returned HTTP $HTTP_CODE"
    fi

else
    report_issue "Application health endpoint is unavailable"
fi

echo

# ---------------------------------------
# 2. Check TLS certificate expiration
# ---------------------------------------

echo "[2] TLS CERTIFICATE"

if [[ -n "$CERT_FILE" ]]; then

    if [[ -r "$CERT_FILE" ]]; then
        CERT_TO_CHECK="$CERT_FILE"
    else
        report_issue "Cannot read configured certificate file"
    fi

elif [[ -n "$CERT_HOST" ]]; then

    CERT_TEMP_FILE=$(mktemp)

    if timeout 10s openssl s_client \
        -connect "${CERT_HOST}:${CERT_PORT}" \
        -servername "$CERT_HOST" \
        -showcerts \
        </dev/null 2>/dev/null \
        | openssl x509 -out "$CERT_TEMP_FILE" 2>/dev/null; then

        CERT_TO_CHECK="$CERT_TEMP_FILE"

    else
        report_issue "Could not retrieve certificate from configured host"
    fi

else

    echo "SKIPPED: Set CERT_HOST or CERT_FILE to enable this check"

fi

if [[ -n "$CERT_TO_CHECK" ]]; then

    CERT_END_DATE=$(openssl x509 \
        -in "$CERT_TO_CHECK" \
        -noout -enddate 2>/dev/null | cut -d= -f2-)

    if [[ -z "$CERT_END_DATE" ]]; then

        report_issue "Could not read certificate expiration date"

    elif openssl x509 \
        -in "$CERT_TO_CHECK" \
        -noout \
        -checkend "$((CERT_WARN_DAYS * 86400))" \
        >/dev/null 2>&1; then

        echo "STATUS: OK"
        echo "Expires: $CERT_END_DATE"

    else

        report_issue "Certificate expires within ${CERT_WARN_DAYS} days or is already expired"
        echo "Expires: $CERT_END_DATE"

    fi

fi

echo

# ---------------------------------------
# 3. Check disk usage
# ---------------------------------------

echo "[3] DISK SPACE"

if [[ -d "$PROJECT_DIR" ]]; then

    DISK_USED=$(df -P "$PROJECT_DIR" \
        | awk 'NR == 2 {gsub("%", "", $5); print $5}')

    if [[ "$DISK_USED" =~ ^[0-9]+$ ]]; then

        echo "Disk usage: ${DISK_USED}%"

        if (( DISK_USED >= DISK_WARN_PERCENT )); then
            report_issue "Disk usage reached the ${DISK_WARN_PERCENT}% warning threshold"
        else
            echo "STATUS: OK"
        fi

    else
        report_issue "Could not determine disk usage"
    fi

else
    report_issue "Project directory does not exist"
fi

echo

# ---------------------------------------
# 4. Check available memory
# ---------------------------------------

echo "[4] MEMORY"

MEM_AVAILABLE=$(free -m | awk '/^Mem:/ {print $7}')

if [[ "$MEM_AVAILABLE" =~ ^[0-9]+$ ]]; then

    echo "Available memory: ${MEM_AVAILABLE} MB"

    if (( MEM_AVAILABLE < MEM_WARN_MB )); then
        report_issue "Available memory is below ${MEM_WARN_MB} MB"
    else
        echo "STATUS: OK"
    fi

else
    report_issue "Could not determine available memory"
fi

echo

# ---------------------------------------
# Final result
# ---------------------------------------

echo "======================================"

if (( ISSUES == 0 )); then

    echo "OVERALL STATUS: OK"

else

    echo "OVERALL STATUS: ISSUES FOUND ($ISSUES)"

fi

echo "======================================"

if (( ISSUES > 0 )); then
    exit 1
fi

exit 0
