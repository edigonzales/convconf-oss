# Architektur

## Nachhaltige Variante

Der PoC nutzt eine **planbasierte In-Memory-Core-Runtime** mit generischen Reader/Writer-Schnittstellen:

- `RecordSourceReader`
- `RecordTargetWriter`
- `FileSourceReader`
- `FileTargetWriter`

Zusätzlich enthält der PoC:

- `ConversionPlanner` für ein explizites KK(s2e)-Planartefakt (`ConversionPlan`)
- Zweistufige Validierung:
  - `LmDataValidator` (Rohdaten gegen LM)
  - `KmResultValidator` (transformierte Daten gegen KM-Mandatory-Regeln)
- Optionales Feld-Trace mit `TraceEvent`

## Adapter

- SQL/JDBC:
  - `JdbcRecordAdapter` (generisch)
  - `H2Adapter`
  - `PostgreSqlAdapter`
- Files:
  - `CsvDirectoryAdapter` (eine Datei je DATA-Name)
  - `XlsxWorkbookAdapter` (ein Sheet je DATA-Name)

## Module

- `convconf-core`
  - INTERLIS-Import (`InterlisModelCompiler`)
  - LM-ANTLR-Parser (`LmParserFacade`)
  - Semantikprüfung (`LmSemanticChecker`)
  - Planner (`ConversionPlanner`)
  - Runtime (`ConversionEngine`)
  - Reader/Writer Adapter (H2/PostgreSQL/CSV/XLSX)
  - Validatoren (`LmDataValidator`, `KmResultValidator`)
- `convconf-cli`
  - `check-lm`
  - `plan`
  - `convert-*` (inkl. `--trace-out`)
