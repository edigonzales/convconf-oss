$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

./gradlew.bat -q :convconf-core:classes :convconf-cli:installDist
$h2Jar = Get-ChildItem "$HOME/.gradle/caches/modules-2/files-2.1/com.h2database/h2/2.3.232" -Filter 'h2-2.3.232.jar' -Recurse | Select-Object -First 1 -ExpandProperty FullName
New-Item -ItemType Directory -Force -Path 'build' | Out-Null
java -cp "$h2Jar" org.h2.tools.RunScript -url 'jdbc:h2:file:./build/source-db' -user sa -script 'examples/h2-to-h2/sql/source-schema.sql'
java -cp "$h2Jar" org.h2.tools.RunScript -url 'jdbc:h2:file:./build/source-db' -user sa -script 'examples/h2-to-h2/sql/source-seed.sql'
java -cp "$h2Jar" org.h2.tools.RunScript -url 'jdbc:h2:file:./build/target-db' -user sa -script 'examples/h2-to-h2/sql/target-schema.sql'

./convconf-cli/build/install/convconf-cli/bin/convconf-cli.bat convert-h2 `
  --km examples/h2-to-h2/km/verein.ili `
  --source-lm examples/h2-to-h2/lm/source.lm `
  --target-lm examples/h2-to-h2/lm/target.lm `
  --source-jdbc jdbc:h2:file:./build/source-db `
  --target-jdbc jdbc:h2:file:./build/target-db
