#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew -q :convconf-core:classes :convconf-cli:installDist
H2_JAR=$(find ~/.gradle/caches/modules-2/files-2.1/com.h2database/h2/2.3.232 -name 'h2-2.3.232.jar' | head -n1)
mkdir -p build/h2-xlsx-out

java -cp "$H2_JAR" org.h2.tools.RunScript -url jdbc:h2:file:./build/h2-xlsx-source-db -user sa -script examples/h2-to-h2/sql/source-schema.sql
java -cp "$H2_JAR" org.h2.tools.RunScript -url jdbc:h2:file:./build/h2-xlsx-source-db -user sa -script examples/h2-to-h2/sql/source-seed.sql

./convconf-cli/build/install/convconf-cli/bin/convconf-cli convert-h2-to-xlsx \
  --km examples/h2-to-h2/km/verein.ili \
  --source-lm examples/h2-to-h2/lm/source.lm \
  --target-lm examples/h2-to-h2/lm/target.lm \
  --source-jdbc jdbc:h2:file:./build/h2-xlsx-source-db \
  --target-xlsx build/h2-xlsx-out/target.xlsx

echo "XLSX output written to build/h2-xlsx-out/target.xlsx"
