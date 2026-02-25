# Testing

## Abgedeckte Ebenen

- Parser-Tests (ANTLR und LM-Struktur inkl. erweiterter Konstrukte)
- INTERLIS-Import-Tests (ili2c + `TransferDescription`)
- Semantiktests (strict mode)
- H2-Integrations-Test
- CSV/XLSX-Adapter-Integrationstests
- PostgreSQL-Adapter-Integrationstest (PostgreSQL-Dialekt)
- Plan/Trace/Validierungs-Tests

## Ausführung

```bash
./gradlew clean test
```

## Manuelle Checks

```bash
./gradlew :convconf-cli:run --args="plan --km examples/h2-to-h2/km/verein.ili --source-lm examples/h2-to-h2/lm/source.lm --target-lm examples/h2-to-h2/lm/target.lm --out build/plan.json"
./gradlew :convconf-cli:run --args="convert-h2 --km examples/h2-to-h2/km/verein.ili --source-lm examples/h2-to-h2/lm/source.lm --target-lm examples/h2-to-h2/lm/target.lm --source-jdbc jdbc:h2:file:./build/source-db --target-jdbc jdbc:h2:file:./build/target-db --trace-out build/trace.log"
```
