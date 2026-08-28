#!/bin/bash
# Smoke-test the stdio MCP server without an MCP client.
#
# The client normally owns this process, so the only way to exercise it by hand
# is to speak JSON-RPC into stdin and read stdout. That also makes this the only
# check that can catch the failure mode unique to stdio: anything the process
# prints to stdout other than protocol corrupts the stream.
#
# Requires: Postgres up (docker compose up -d postgres) and target/*.jar built
# (./mvnw package -DskipTests).
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../../.." || exit 1
JAR=$(ls target/tournaments-backend-*.jar 2>/dev/null | head -1)

if [ -z "$JAR" ]; then
  echo "ERROR: no jar in target/. Run: ./mvnw package -DskipTests"
  exit 1
fi

OUT=$(mktemp) ; ERR=$(mktemp)
trap 'rm -f "$OUT" "$ERR"' EXIT

{
  printf '%s\n' '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"smoke","version":"1"}}}'
  printf '%s\n' '{"jsonrpc":"2.0","method":"notifications/initialized"}'
  printf '%s\n' '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
  printf '%s\n' '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_leagues","arguments":{}}}'
  printf '%s\n' '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_league","arguments":{"leagueId":1}}}'
  sleep 12
} | java -jar "$JAR" --spring.profiles.active=mcp 2>"$ERR" >"$OUT"

FAIL=0
pass() { echo "PASS: $1"; }
bad()  { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }

echo "== stdout carries protocol only =="
lines=0 nonjson=0
while IFS= read -r line; do
  [ -z "$line" ] && continue
  lines=$((lines + 1))
  jq -e . >/dev/null 2>&1 <<<"$line" || { nonjson=$((nonjson + 1)); echo "  non-JSON: ${line:0:100}"; }
done < "$OUT"
echo "  lines=$lines non-json=$nonjson"
[ "$lines" -gt 0 ] && [ "$nonjson" -eq 0 ] \
  && pass "no banner or log output leaked into the stream" \
  || bad "stdout is polluted -- a client would fail to parse this"

echo "== tools registered =="
tools=$(jq -r 'select(.id==2) | [.result.tools[].name] | sort | join(", ")' "$OUT" 2>/dev/null)
echo "  $tools"
[ "$tools" = "get_league, list_leagues" ] && pass "both tools present" || bad "unexpected tool list"

echo "== list_leagues returns summaries =="
if jq -e 'select(.id==3) | .result.isError == true' "$OUT" >/dev/null 2>&1; then
  bad "list_leagues errored: $(jq -r 'select(.id==3) | .result.content[0].text' "$OUT")"
else
  withteams=$(jq -r 'select(.id==3) | .result.content[0].text | fromjson | map(select(.teams != null)) | length' "$OUT" 2>/dev/null)
  count=$(jq -r 'select(.id==3) | .result.content[0].text | fromjson | length' "$OUT" 2>/dev/null)
  echo "  $count leagues, $withteams carrying rosters"
  [ "$withteams" = "0" ] && pass "summaries omit teams" || bad "summaries are carrying rosters"
fi

echo "== get_league returns the full graph =="
# This is the one that fails without an explicit transaction: there is no web
# request here, so spring.jpa.open-in-view cannot hold a session open.
if jq -e 'select(.id==4) | .result.isError == true' "$OUT" >/dev/null 2>&1; then
  bad "get_league errored: $(jq -r 'select(.id==4) | .result.content[0].text' "$OUT")"
else
  teams=$(jq -r 'select(.id==4) | .result.content[0].text | fromjson | .teams | length' "$OUT" 2>/dev/null)
  echo "  teams=$teams"
  [ "${teams:-0}" -gt 0 ] && pass "lazy collection initialized" || bad "no teams returned"
fi

echo "== stderr quiet =="
errbytes=$(wc -c < "$ERR" | tr -d ' ')
echo "  $errbytes bytes"
[ "$errbytes" -eq 0 ] && pass "clean stderr" || { echo "  $(head -3 "$ERR")"; bad "stderr not empty"; }

echo
[ "$FAIL" -eq 0 ] && echo "STDIO MCP SERVER OK" || echo "$FAIL CHECK(S) FAILED"
exit "$FAIL"
