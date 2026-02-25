#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew -q :convconf-core:classes :convconf-cli:installDist
pushd examples/postgres-to-postgres >/dev/null
  docker compose up -d
  trap 'docker compose down -v' EXIT
  until docker exec $(docker compose ps -q src-db) pg_isready -U source >/dev/null 2>&1; do sleep 1; done
  until docker exec $(docker compose ps -q tgt-db) pg_isready -U target >/dev/null 2>&1; do sleep 1; done

  docker exec -i $(docker compose ps -q src-db) psql -U source -d source < sql/source-schema.sql
  docker exec -i $(docker compose ps -q src-db) psql -U source -d source < sql/source-seed.sql
  docker exec -i $(docker compose ps -q tgt-db) psql -U target -d target < sql/target-schema.sql
popd >/dev/null

./convconf-cli/build/install/convconf-cli/bin/convconf-cli convert-postgres \
  --km examples/h2-to-h2/km/verein.ili \
  --source-lm examples/postgres-to-postgres/lm/source_pg.lm \
  --target-lm examples/postgres-to-postgres/lm/target_pg.lm \
  --source-jdbc jdbc:postgresql://localhost:55432/source \
  --source-user source --source-password source \
  --target-jdbc jdbc:postgresql://localhost:55433/target \
  --target-user target --target-password target
