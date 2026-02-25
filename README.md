# ConvConf OSS PoC

## Warum machen wir das? (Kurzfassung der PDFs)

Die ConvConf-Idee adressiert ein klassisches Interoperabilitätsproblem:

- Heute entsteht bei Datenaustausch oft ein **n×m-Aufwand**: Für viele Sender-/Empfänger-Kombinationen werden eigene Konversionen gebaut.
- Das ist teuer, fehleranfällig und schwer wartbar (insbesondere bei Modelländerungen).

ConvConf reduziert dieses Problem durch ein modellzentriertes Vorgehen:

- Es gibt ein gemeinsames **KM** (konzeptionelles Modell, typischerweise INTERLIS).
- Jedes System beschreibt nur sein eigenes **LM** (logisches Modell) in Bezug auf das KM.
- Aus `KM + LM(source) + LM(target)` wird automatisch ein Konfigurations-/Ausführungsplan abgeleitet.

### Wie wird das „automatisch“ abgeleitet?

Wichtig: „automatisch“ bedeutet hier **nicht**, dass Spaltennamen erraten werden.
Stattdessen wird über die gemeinsame KM-Semantik gearbeitet:

1. `LM(source)` beschreibt: *welche Quellspalte auf welches KM-Attribut zeigt*.
2. `LM(target)` beschreibt: *welches KM-Attribut in welche Zielspalte geschrieben wird*.
3. Der Plan verbindet beide Seiten über das gleiche KM-Attribut.

Dadurch dürfen Quell- und Zielnamen komplett unterschiedlich sein.

Beispiel:

```lm
# Source-LM
COLUMN FAMILY -> Name;

# Target-LM
COLUMN LAST_NAME -> Name;
```

Interpretation:
- Beim Lesen wird `FAMILY` auf das KM-Attribut `Name` gemappt.
- Beim Schreiben wird das KM-Attribut `Name` in `LAST_NAME` geschrieben.

Also kein direktes `FAMILY -> LAST_NAME`, sondern `FAMILY -> KM.Name -> LAST_NAME`.

Für Codes/Enums funktioniert dasselbe über `VALUEMAP` (z. B. `FORM_NO -> Form USING OrgFormMap`).

Damit löst ConvConf insbesondere diese Probleme:

- weniger punktuelle Einzelkonverter je Systempaar,
- klarere Verantwortlichkeiten (jeder Betreiber beschreibt primär sein eigenes System),
- bessere Wiederverwendbarkeit und Nachvollziehbarkeit der Mappings,
- robustere Evolution bei Modellversionen.

## Was fehlt im PoC noch für Alltagstauglichkeit?

Der vorliegende PoC ist bewusst stark, aber noch nicht „voll produktiv“.
Wesentliche nächste Schritte wären:

- **Geometrie-/Blackbox-Unterstützung** (aktuell Fokus auf nicht-geometrische Kernfälle),
- umfassendere **Typkonvertierungen** (z. B. Datum/Zeit, string-basierte Normierungen),
- deutlich tiefere **fachliche Validierungen** und präzisere Fehlermeldungen,
- Performance-/Skalierungsaspekte (Streaming, große Datenmengen, Optimierungen),
- robustes Betriebs- und Packaging-Setup (Versionierung, Migrationshilfen, Hardening, Monitoring),
- breitere Abdeckung der LM-Sprache und komplexer Mappingmuster.

Hinweis: Ein Webservice kann sinnvoll sein, ist aber **nicht zwingend** für den Kernnutzen;
entscheidend ist zuerst ein verlässlicher Konfigurations- und Konversionskern.

Publikationsreifer Java-21-Proof-of-Concept für ConvConf mit:

- Gradle (Groovy DSL)
- Package-Basis `guru.interlis`
- INTERLIS-KM-Import via `ili2c` (`TransferDescription`)
- LM-Parser mit ANTLR
- Strikte LM-Semantikprüfung
- Generischen Reader/Writer-Interfaces für DB und Files
- Adaptern für H2, PostgreSQL, CSV und XLSX
- Konfigurationsplanung (`plan` / KK(s2e))
- Zweistufiger Validierung (LM-Daten, KM-Ergebnis)
- Optionalem Trace-Output (`--trace-out`)
- End-to-End-Konversionen (H2↔H2, CSV→H2, XLSX→H2, H2→CSV, H2→XLSX, PostgreSQL↔PostgreSQL)

## Quickstart

```bash
./gradlew clean test
```

## Plan generieren

```bash
./gradlew :convconf-cli:run --args="plan --km examples/h2-to-h2/km/verein.ili --source-lm examples/h2-to-h2/lm/source.lm --target-lm examples/h2-to-h2/lm/target.lm --out build/plan.json"
```


## Generische CLI-Konversion

Das CLI unterstützt zusätzlich ein **generisches** `convert`-Kommando, damit beliebige
Kombinationen möglich sind (z. B. PostgreSQL -> XLSX), ohne neue `convert-*-*` Befehle
pro Adapterpaar einzuführen.

Beispiel PostgreSQL -> XLSX:

```bash
./gradlew :convconf-cli:installDist
./convconf-cli/build/install/convconf-cli/bin/convconf-cli convert \
  --km examples/h2-to-h2/km/verein.ili \
  --source-lm examples/postgres-to-postgres/lm/source_pg.lm \
  --target-lm examples/h2-to-h2/lm/target.lm \
  --source-type postgres \
  --source-jdbc jdbc:postgresql://localhost:55432/source \
  --source-user source \
  --source-password source \
  --target-type xlsx \
  --target-xlsx build/postgres-to-xlsx.xlsx
```

Unterstützte Typen für `--source-type`/`--target-type`: `h2`, `postgres`, `csv`, `xlsx`.
Das Kommando validiert die jeweils passenden Optionen zentral (z. B. `--*-jdbc` für DB-Typen,
`--*-dir` für CSV, `--*-xlsx` für XLSX).

## Beispiele ausführen

Linux/macOS (Bash):

```bash
./scripts/run-h2-example.sh
./scripts/run-csv-h2-example.sh
./scripts/run-xlsx-h2-example.sh
./scripts/run-h2-csv-example.sh
./scripts/run-h2-xlsx-example.sh
# Varianten mit dedizierten LM (abweichende Layouts):
./scripts/run-csv-custom-h2-example.sh
./scripts/run-h2-xlsx-custom-example.sh
# benötigt docker lokal:
./scripts/run-postgres-example.sh
```

Windows (native PowerShell, keine WSL-Wrapper):

```powershell
.\scripts\run-h2-example.ps1
.\scripts\run-csv-h2-example.ps1
.\scripts\run-xlsx-h2-example.ps1
.\scripts\run-h2-csv-example.ps1
.\scripts\run-h2-xlsx-example.ps1
# Varianten mit dedizierten LM (abweichende Layouts):
.\scripts\run-csv-custom-h2-example.ps1
.\scripts\run-h2-xlsx-custom-example.ps1
# benötigt Docker Desktop lokal:
.\scripts\run-postgres-example.ps1
```


## CI / Publishing (GitHub Actions)

Die Workflow-Datei `.github/workflows/ci-build-test-publish.yml` führt aus:

1. `clean test` (Unit + Integrationstests)
2. Smoke-Test der CLI (`convconf-cli --help` nach `installDist`)
3. Snapshot-Publishing nach erfolgreichem Testlauf (nur `main`/`workflow_dispatch`):
   - Modul-Artefakte inkl. `-sources.jar` und `-javadoc.jar`
   - CLI-Distributions-ZIP als zusätzliches Maven-Artefakt (`classifier=bin`)

Für das Publishing werden folgende GitHub-Secrets benötigt:

- `SOGEO_MAVEN_USERNAME`
- `SOGEO_MAVEN_PASSWORD`

Target-Repository: `https://jars.sogeo.services/snapshots`

## Doku

- [Architektur](docs/architecture.md)
- [LM-Sprache](docs/lm-language.md)
- [End-to-End-Beispiele](docs/examples-h2-e2e.md)
- Entscheidungs-Matrix (gleiches vs. dediziertes LM): siehe Abschnitt in `docs/examples-h2-e2e.md`
- [Testing](docs/testing.md)
