$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

./gradlew.bat -q :convconf-core:classes :convconf-cli:installDist
Push-Location 'examples/postgres-to-postgres'
try {
    docker compose up -d

    $srcContainer = (docker compose ps -q src-db).Trim()
    $tgtContainer = (docker compose ps -q tgt-db).Trim()

    do {
        Start-Sleep -Seconds 1
        docker exec $srcContainer pg_isready -U source *> $null
    } while ($LASTEXITCODE -ne 0)

    do {
        Start-Sleep -Seconds 1
        docker exec $tgtContainer pg_isready -U target *> $null
    } while ($LASTEXITCODE -ne 0)

    Get-Content 'sql/source-schema.sql' | docker exec -i $srcContainer psql -U source -d source
    Get-Content 'sql/source-seed.sql' | docker exec -i $srcContainer psql -U source -d source
    Get-Content 'sql/target-schema.sql' | docker exec -i $tgtContainer psql -U target -d target
}
finally {
    docker compose down -v
    Pop-Location
}

./convconf-cli/build/install/convconf-cli/bin/convconf-cli.bat convert-postgres `
  --km examples/h2-to-h2/km/verein.ili `
  --source-lm examples/postgres-to-postgres/lm/source_pg.lm `
  --target-lm examples/postgres-to-postgres/lm/target_pg.lm `
  --source-jdbc jdbc:postgresql://localhost:55432/source `
  --source-user source --source-password source `
  --target-jdbc jdbc:postgresql://localhost:55433/target `
  --target-user target --target-password target
