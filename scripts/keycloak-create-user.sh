#!/usr/bin/env bash
# Creates (or updates) a user in the groove realm and grants realm roles.
#
#   KEYCLOAK_URL=<base url> KEYCLOAK_ADMIN=admin KEYCLOAK_ADMIN_PASSWORD=... \
#     scripts/keycloak-create-user.sh <username> <password> <role> [role...]
#
# Example: scripts/keycloak-create-user.sh alice 's3cret' user admin
set -euo pipefail

: "${KEYCLOAK_URL:?Set KEYCLOAK_URL to the Keycloak base URL}"
: "${KEYCLOAK_ADMIN_PASSWORD:?Set KEYCLOAK_ADMIN_PASSWORD}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
REALM="${KEYCLOAK_REALM:-groove}"

[ $# -ge 3 ] || { echo "usage: $0 <username> <password> <role> [role...]" >&2; exit 64; }
USERNAME="$1"; PASSWORD="$2"; shift 2

TOKEN=$(curl -fsS "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=admin-cli \
  --data-urlencode "username=$ADMIN_USER" --data-urlencode "password=$KEYCLOAK_ADMIN_PASSWORD" \
  | jq -r .access_token)
AUTH=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
API="$KEYCLOAK_URL/admin/realms/$REALM"

# Keycloak's default user profile requires email and names before first login.
BODY=$(jq -n --arg u "$USERNAME" --arg p "$PASSWORD" '{
  username: $u, enabled: true, emailVerified: true,
  email: ($u + "@groove.example"), firstName: $u, lastName: "Groove",
  credentials: [{ type: "password", value: $p, temporary: false }]
}')

STATUS=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "${AUTH[@]}" "$API/users" -d "$BODY")
case "$STATUS" in
  201) echo "created user $USERNAME" ;;
  409) echo "user $USERNAME exists, resetting password" ;;
  *)   echo "failed to create $USERNAME (HTTP $STATUS)" >&2; exit 1 ;;
esac

USER_ID=$(curl -fsS "${AUTH[@]}" "$API/users?username=$USERNAME&exact=true" | jq -r '.[0].id')
curl -fsS -X PUT "${AUTH[@]}" "$API/users/$USER_ID/reset-password" \
  -d "$(jq -n --arg p "$PASSWORD" '{type:"password",value:$p,temporary:false}')"

ROLES=$(for r in "$@"; do curl -fsS "${AUTH[@]}" "$API/roles/$r"; done | jq -s '[.[] | {id, name}]')
curl -fsS -X POST "${AUTH[@]}" "$API/users/$USER_ID/role-mappings/realm" -d "$ROLES"
echo "granted roles to $USERNAME: $*"
