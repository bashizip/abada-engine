#!/bin/bash
set -e

# Test Auth Flow

# 1. Get Token (from inside container to match issuer URL)
echo "Getting Token for alice..."
# We run wget inside abada-engine because it can reach Keycloak via the internal Docker network
# and the issuer URL will match what oauth2-proxy expects (http://keycloak.localhost:8080...)
TOKEN_RESPONSE=$(docker exec abada-engine wget -q -O - \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --post-data "client_id=abada-frontend&username=alice&password=alice&grant_type=password" \
  http://keycloak.localhost:8080/realms/abada-dev/protocol/openid-connect/token) || true

# Check if docker exec failed or returned empty
if [ -z "$TOKEN_RESPONSE" ]; then
   echo "Failed to get token (empty response). Check container logs."
   exit 1
fi

# Parse access_token using jq
if command -v jq &> /dev/null; then
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r .access_token)
else
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*"' | sed 's/"access_token":"//;s/"//')
fi

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
    echo "Failed to get token:"
    echo "$TOKEN_RESPONSE"
    exit 1
fi
echo "Got Access Token (Length: ${#ACCESS_TOKEN} chars)"


# 2. Call API
echo "Calling API with Token..."
# We expect 200 OK.
# We store the output and code.
RESPONSE_FILE=$(mktemp)
HTTP_CODE=$(curl -k -s -o "$RESPONSE_FILE" -w "%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" https://localhost/api/v1/info)

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" == "200" ]; then
    echo "SUCCESS: Auth works."
    if command -v jq &> /dev/null; then
        cat "$RESPONSE_FILE" | jq .
    else
        cat "$RESPONSE_FILE"
    fi
else
    echo "FAILURE: Code $HTTP_CODE"
    echo "Response Content:"
    cat "$RESPONSE_FILE"
    echo
fi

rm "$RESPONSE_FILE"
