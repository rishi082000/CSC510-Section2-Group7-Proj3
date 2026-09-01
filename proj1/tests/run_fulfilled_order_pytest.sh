#!/usr/bin/env bash
set -u

# Override if python3 is not the interpreter you want:
#   PYTHON=/usr/bin/python3 proj1/tests/<script>.sh
PYTHON="${PYTHON:-python3}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJ1_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="$PROJ1_DIR/test-output"
TIMESTAMP="$(date +"%Y%m%d-%H%M%S")"
OUTPUT_FILE="$OUTPUT_DIR/fulfilled-order-pytest-$TIMESTAMP.txt"

mkdir -p "$OUTPUT_DIR"

{
  echo "Use Case #6 pytest run: Fulfilled Order"
  echo "Started: $(date)"
  echo "API_BASE_URL: ${API_BASE_URL:-http://localhost:8080}"
  echo ""
} | tee "$OUTPUT_FILE"

"$PYTHON" -m pytest -vv "$SCRIPT_DIR/test_fulfilled_order_pytest.py" 2>&1 | tee -a "$OUTPUT_FILE"
PYTEST_STATUS="${PIPESTATUS[0]}"

{
  echo ""
  echo "Finished: $(date)"
  echo "Exit status: $PYTEST_STATUS"
  echo "Output file: $OUTPUT_FILE"
} | tee -a "$OUTPUT_FILE"

exit "$PYTEST_STATUS"
