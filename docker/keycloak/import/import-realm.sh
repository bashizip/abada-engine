#!/usr/bin/env sh
# Helper: Import realm into a running Keycloak container (if needed)
# For local dev the compose file sets KEYCLOAK_IMPORT so this is optional.

set -e

REALM_FILE="/opt/keycloak/data/import/realm-export.json"

if [ "$1" = "docker-import" ]; then
  echo "Copying realm file into running 'keycloak' container and importing..."
  docker cp ./docker/keycloak/import/realm-export.json keycloak:/tmp/realm-export.json
  docker exec -it keycloak /opt/keycloak/bin/kc.sh import --file /tmp/realm-export.json
  echo "Import requested. Check Keycloak logs for results."
else
  echo "No action requested. To import into a running container run:"
  echo "  ./docker/keycloak/import/import-realm.sh docker-import"
  echo "Note: The dev compose already mounts the import folder and KEYCLOAK_IMPORT is configured."
fi
