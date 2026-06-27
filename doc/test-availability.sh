#!/bin/bash
# SkillsOps Availability Test
# Tests all API endpoints return proper HTTP 200 with valid tokens

set -e
BASE="${1:-http://localhost:8080}"
PASS=0
FAIL=0

check() {
  local desc="$1" method="$2" url="$3" token="$4" body="$5" expected_code="${6:-200}"
  local auth_header=""
  [ -n "$token" ] && auth_header="-H 'Authorization: Bearer $token'"
  local body_arg=""
  [ -n "$body" ] && body_arg="-d '$body'"

  local cmd="curl -s -o /dev/null -w '%{http_code}' -X $method '$BASE$url' -H 'Content-Type: application/json' $auth_header $body_arg"
  local code=$(eval $cmd 2>/dev/null)

  if [ "${code}" = "${expected_code}" ]; then
    echo "  ✓ $desc"
    PASS=$((PASS+1))
  else
    echo "  ✗ $desc (expected $expected_code, got $code)"
    FAIL=$((FAIL+1))
  fi
}

echo "=== SkillsOps API Availability Test ==="

# 1. Auth
echo "[Auth]"
check "Register user" POST /api/v1/auth/register "" '{"username":"availtest","password":"123456"}'
UTOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' -d '{"username":"availtest","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)
check "Login" POST /api/v1/auth/login "" '{"username":"availtest","password":"123456"}'

ATOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)
check "Admin login" POST /api/v1/auth/login "" '{"username":"admin","password":"admin123"}'

# 2. Skills
echo "[Skills]"
check "Create skill" POST /api/v1/skills "$UTOKEN" '{"name":"Avail Skill","description":"Test","categoryId":1}'
check "Get skill" GET /api/v1/skills/1 "$UTOKEN"
check "Submit" POST /api/v1/skills/1/submit "$UTOKEN"
check "Approve" POST /api/v1/admin/skills/1/approve "$ATOKEN"
check "Publish version" POST /api/v1/skills/1/versions "$UTOKEN" '{"version":"1.0.0"}'

# 3. Market
echo "[Market]"
check "Market list" GET "/api/v1/market/skills?page=1&size=10" "$UTOKEN"
check "Install" POST /api/v1/market/skills/1/install "$UTOKEN"
check "Install status" GET /api/v1/market/skills/1/install-status "$UTOKEN"

# 4. Ratings
echo "[Ratings]"
check "Rate" POST /api/v1/skills/1/ratings "$UTOKEN" '{"rating":4,"comment":"Good"}'
check "Get ratings" GET /api/v1/skills/1/ratings "$UTOKEN"

# 5. Admin
echo "[Admin]"
check "Pending skills" GET /api/v1/admin/pending-skills "$ATOKEN"
check "Stats" GET /api/v1/admin/stats "$ATOKEN"

# 6. Workspace
echo "[Workspace]"
check "My skills" GET /api/v1/workspace/my-skills "$UTOKEN"
check "Installed" GET /api/v1/workspace/installed "$UTOKEN"

# 7. Security
echo "[Security]"
check "No token → 401" GET /api/v1/skills/1 "" "" 401
check "Bad token → 401" GET /api/v1/skills/1 "invalid-token" "" 401
check "User can't admin" POST /api/v1/admin/skills/1/approve "$UTOKEN" "" 403

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
[ $FAIL -eq 0 ] && echo "ALL TESTS PASSED!" || echo "SOME TESTS FAILED!"
exit $FAIL
