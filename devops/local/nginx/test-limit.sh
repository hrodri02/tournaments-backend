#!/bin/bash
set -uo pipefail

HOST="${HOST:-http://localhost}"
URL="$HOST/api/v1/auth/login"
PAYLOAD='{"email":"admin@example.com","password":"admin123"}'

RATE=10
BURST=20
COOLDOWN=3

FAILURES=0

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAILURES=$((FAILURES + 1)); }

req() {
  curl -s -o /dev/null -w "%{http_code}" "$URL" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" "$@"
}

flood() {
  local n="$1"
  shift
  seq "$n" | xargs -P "$n" -I{} curl -s -o /dev/null -w "%{http_code}\n" "$URL" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" "$@"
}

xff_flood() {
  local n="$1"
  seq "$n" | xargs -P "$n" -I{} curl -s -o /dev/null -w "%{http_code}\n" "$URL" \
    -H "Content-Type: application/json" \
    -H "X-Forwarded-For: 203.0.113.{}" \
    -d "$PAYLOAD"
}

count() { grep -c "$1" <<<"$2" || true; }

if ! curl -s -o /dev/null "$URL"; then
  echo "ERROR: cannot reach $URL (is docker compose up?)"
  exit 1
fi

echo "== Test 1: burst ceiling =="
codes=$(flood 50)
total=$(count '' "$codes")
blocked=$(count '429' "$codes")
passed=$((total - blocked))
echo "sent=$total passed=$passed blocked=$blocked (expect passed>=$((BURST + 1)), blocked>=5)"
if [ "$passed" -ge "$((BURST + 1))" ] && [ "$blocked" -ge 5 ]; then
  pass "burst honored and limiter engaged"
else
  fail "unexpected burst behavior"
fi

sleep "$COOLDOWN"

echo "== Test 2: sustained rate under limit =="
codes=""
for _ in $(seq 20); do
  codes+="$(req)"$'\n'
  sleep 0.15
done
blocked=$(count '429' "$codes")
echo "sent=20 at ~6.7r/s blocked=$blocked (expect 0)"
if [ "$blocked" -eq 0 ]; then
  pass "steady traffic below ${RATE}r/s never throttled"
else
  fail "throttled while under the configured rate"
fi

sleep "$COOLDOWN"

echo "== Test 3: recovery after cooldown =="
codes=$(flood 50)
blocked=$(count '429' "$codes")
sleep "$COOLDOWN"
after=$(req)
echo "tripped blocked=$blocked, after ${COOLDOWN}s code=$after (expect blocked>=1, after!=429)"
if [ "$blocked" -ge 1 ] && [ "$after" != "429" ]; then
  pass "bucket refilled and served again"
else
  fail "did not recover after cooldown"
fi

sleep "$COOLDOWN"

echo "== Test 4: X-Forwarded-For does not bypass =="
codes=$(xff_flood 50)
blocked=$(count '429' "$codes")
echo "sent=50 with unique XFF ips blocked=$blocked (expect blocked>=5)"
if [ "$blocked" -ge 5 ]; then
  pass "limiter keys on real ip, XFF spoofing ineffective"
else
  fail "varied XFF bypassed the limit"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "ALL TESTS PASSED"
  exit 0
else
  echo "$FAILURES TEST(S) FAILED"
  exit 1
fi
