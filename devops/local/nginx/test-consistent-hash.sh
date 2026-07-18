#!/bin/bash
set -uo pipefail

HOST="${HOST:-http://localhost}"
PATH_PREFIX="/v3/api-docs"
COMPOSE_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/docker-compose.yml"

FAILURES=0

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAILURES=$((FAILURES + 1)); }

instance_id() {
  curl -s -o /dev/null -D - "$1" | grep -i '^X-Instance-Id:' | tr -d '\r' | awk '{print $2}'
}

count_distinct() { tr ' ' '\n' <<<"$1" | sort -u | grep -c '.' || true; }

if ! curl -s -o /dev/null "$HOST$PATH_PREFIX"; then
  echo "ERROR: cannot reach $HOST (is docker compose up?)"
  exit 1
fi

echo "== Test 1: different paths spread across the pool =="
ids=""
for i in $(seq 1 8); do
  id=$(instance_id "$HOST$PATH_PREFIX?path=$i")
  ids+="$id "
  echo "path=$i -> instance=$id"
  sleep 0.15
done
distinct=$(count_distinct "$ids")
echo "distinct instances seen=$distinct (expect >=2)"
if [ "$distinct" -ge 2 ]; then
  pass "requests spread across more than one backend instance"
else
  fail "all requests landed on a single instance"
fi

sleep 1

echo
echo "== Test 2: same path is sticky to one instance =="
sticky_url="$HOST$PATH_PREFIX?path=sticky"
first_id=$(instance_id "$sticky_url")
sticky_ids="$first_id "
for _ in $(seq 1 4); do
  sticky_ids+="$(instance_id "$sticky_url") "
  sleep 0.15
done
distinct=$(count_distinct "$sticky_ids")
echo "repeated requests to same path -> instances=[$sticky_ids] (expect exactly 1 distinct)"
if [ "$distinct" -eq 1 ] && [ -n "$first_id" ]; then
  pass "same path consistently routed to the same instance"
else
  fail "same path routed to differing instances"
fi

sleep 1

echo
echo "== Test 3: graceful failover when one instance is stopped =="
failover_url="$HOST$PATH_PREFIX?path=failover"
before_id=$(instance_id "$failover_url")
echo "instance serving before stop: $before_id"

if [ -z "$before_id" ]; then
  fail "could not determine serving instance before failover test"
else
  docker compose -f "$COMPOSE_FILE" stop "$before_id" >/dev/null
  sleep 1

  code=$(curl -s -o /dev/null -w "%{http_code}" "$failover_url")
  after_id=$(instance_id "$failover_url")
  echo "after stopping $before_id: code=$code instance=$after_id"

  if [ "$code" = "200" ] && [ -n "$after_id" ] && [ "$after_id" != "$before_id" ]; then
    pass "traffic failed over to the remaining healthy instance"
  else
    fail "did not fail over gracefully"
  fi

  docker compose -f "$COMPOSE_FILE" start "$before_id" >/dev/null
  echo "restored $before_id"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "ALL TESTS PASSED"
  exit 0
else
  echo "$FAILURES TEST(S) FAILED"
  exit 1
fi
