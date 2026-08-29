#!/bin/bash
# Smoke-test the out-of-process MCP server against the running compose stack.
#
# This is the only check that covers the whole path at once:
#
#   curl -> nginx -> mcp-server -> nginx -> backend-1/2 -> postgres
#
# The unit tests build a transport context by hand and the integration test
# stubs the backend, so neither can catch a wrong nginx route, a container that
# cannot resolve another, or a token the real API rejects.
#
# Requires: docker compose -f devops/local/docker-compose.yml up -d
set -uo pipefail

BASE=${BASE:-http://localhost}
EMAIL=${EMAIL:-admin@example.com}
PASSWORD=${PASSWORD:-admin123}

FAIL=0
pass() { echo "PASS: $1"; }
bad()  { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }

# Speaks one JSON-RPC call to /mcp. $1 is the Authorization header value, or the
# empty string to send none; $2 is the request body.
mcp() {
  local auth="$1" body="$2"
  if [ -n "$auth" ]; then
    curl -s -X POST "$BASE/mcp" \
      -H 'Content-Type: application/json' \
      -H 'Accept: application/json, text/event-stream' \
      -H "Authorization: $auth" \
      -d "$body"
  else
    curl -s -X POST "$BASE/mcp" \
      -H 'Content-Type: application/json' \
      -H 'Accept: application/json, text/event-stream' \
      -d "$body"
  fi
}

# The text of a tool result, or the empty string when the call was not one.
tool_text() { jq -r '.result.content[0].text // empty'; }

echo "== a token for $EMAIL =="
LOGIN=$(curl -s -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(jq -r '.tokens.accessToken // empty' <<<"$LOGIN")

if [ -z "$TOKEN" ]; then
  echo "ERROR: could not log in. Is the stack up? Response was:"
  echo "  ${LOGIN:0:300}"
  exit 1
fi
pass "logged in"

echo "== tools are advertised =="
TOOLS=$(mcp "Bearer $TOKEN" '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' \
  | jq -r '[.result.tools[].name] | sort | join(", ")')
echo "  $TOOLS"
[ "$TOOLS" = "get_league, list_leagues, list_my_teams" ] \
  && pass "all three tools present" \
  || bad "unexpected tool list"

echo "== list_leagues returns summaries =="
LEAGUES=$(mcp "Bearer $TOKEN" \
  '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"list_leagues","arguments":{}}}' \
  | tool_text)
COUNT=$(jq -r 'length' <<<"$LEAGUES" 2>/dev/null)
WITH_TEAMS=$(jq -r 'map(select(.teams != null)) | length' <<<"$LEAGUES" 2>/dev/null)
echo "  $COUNT leagues, $WITH_TEAMS carrying rosters"
[ "${COUNT:-0}" -gt 0 ] && [ "${WITH_TEAMS:-1}" = "0" ] \
  && pass "summaries omit teams" \
  || bad "expected some leagues, none of them carrying rosters: ${LEAGUES:0:200}"

echo "== get_league returns the full roster =="
DETAIL=$(mcp "Bearer $TOKEN" \
  '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"get_league","arguments":{"leagueId":1}}}' \
  | tool_text)
TEAMS=$(jq -r '.teams | length' <<<"$DETAIL" 2>/dev/null)
PLAYERS=$(jq -r '[.teams[].players[]] | length' <<<"$DETAIL" 2>/dev/null)
echo "  teams=$TEAMS players=$PLAYERS"
[ "${TEAMS:-0}" -gt 0 ] && [ "${PLAYERS:-0}" -gt 0 ] \
  && pass "teams and players came through" \
  || bad "expected a populated roster: ${DETAIL:0:200}"

echo "== the players carry no account details =="
# The MCP server declares its own records; PlayerDTO's email must not survive.
grep -q '"email"' <<<"$DETAIL" \
  && bad "an email address reached the tool result" \
  || pass "no email addresses in the result"

echo "== list_my_teams answers as whoever asked =="
# The assertion this whole variant exists for. One running MCP server, two
# callers, two different answers -- decided entirely by which token arrived.
# The stdio server could not do this at all: it resolved one account at launch.
# The in-process HTTP server could, but only because the tool body happened to
# run on the request thread that held the security context.
PLAYER_TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"sconnor@example.com","password":"securepass1"}' \
  | jq -r '.tokens.accessToken // empty')

my_teams() {
  mcp "Bearer $1" \
    '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"list_my_teams","arguments":{}}}' \
    | tool_text | jq -c '[.teams[].name]' 2>/dev/null
}

ADMIN_TEAMS=$(my_teams "$TOKEN")
PLAYER_TEAMS=$(my_teams "$PLAYER_TOKEN")
echo "  $EMAIL: $ADMIN_TEAMS"
echo "  sconnor@example.com: $PLAYER_TEAMS"

[ -n "$PLAYER_TEAMS" ] && [ "$PLAYER_TEAMS" != "[]" ] \
  && pass "the player's own teams came back" \
  || bad "expected sconnor@example.com to be on a team, got: $PLAYER_TEAMS"

[ "$ADMIN_TEAMS" != "$PLAYER_TEAMS" ] \
  && pass "two callers, two answers, one server" \
  || bad "both callers got the same answer -- identity is not per-request"

echo "== a bad token is refused, and says why =="
BAD=$(mcp "Bearer not.a.real.token" \
  '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"list_leagues","arguments":{}}}')
if jq -e '.result.isError == true' <<<"$BAD" >/dev/null 2>&1 \
   && grep -q "401" <<<"$(tool_text <<<"$BAD")"; then
  pass "401 reported as a tool error"
else
  bad "expected an error result naming 401: ${BAD:0:300}"
fi

echo "== no token at all is refused =="
NONE=$(mcp "" \
  '{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"list_leagues","arguments":{}}}')
if jq -e '.result.isError == true' <<<"$NONE" >/dev/null 2>&1 \
   && grep -q "Authorization header" <<<"$(tool_text <<<"$NONE")"; then
  pass "missing credential explained rather than guessed at"
else
  bad "expected an error result about the missing header: ${NONE:0:300}"
fi

echo
[ "$FAIL" -eq 0 ] && echo "HTTP MCP SERVER OK" || echo "$FAIL CHECK(S) FAILED"
exit "$FAIL"
