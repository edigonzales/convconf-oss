#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew -q :convconf-core:classes :convconf-cli:installDist
H2_JAR=$(find ~/.gradle/caches/modules-2/files-2.1/com.h2database/h2/2.3.232 -name 'h2-2.3.232.jar' | head -n1)
mkdir -p build
java -cp "$H2_JAR" org.h2.tools.RunScript -url jdbc:h2:file:./build/csv-target-db -user sa -script examples/h2-to-h2/sql/target-schema.sql
./convconf-cli/build/install/convconf-cli/bin/convconf-cli convert-csv-to-h2 \
  --km examples/h2-to-h2/km/verein.ili \
  --source-lm examples/h2-to-h2/lm/source.lm \
  --target-lm examples/h2-to-h2/lm/target.lm \
  --source-dir examples/csv-to-h2/source \
  --target-jdbc jdbc:h2:file:./build/csv-target-db
