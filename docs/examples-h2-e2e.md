# End-to-End Beispiele

## H2 → H2

```bash
./scripts/run-h2-example.sh
```

## CSV → H2

CSV-Dateien liegen im Repo unter `examples/csv-to-h2/source`.

```bash
./scripts/run-csv-h2-example.sh
```

## XLSX → H2

XLSX-Datei liegt im Repo unter `examples/xlsx-to-h2/source/source.xlsx`.

```bash
./scripts/run-xlsx-h2-example.sh
```

## PostgreSQL → PostgreSQL

Dieses Beispiel nutzt lokal Docker (im Repo ist alles für den Ablauf enthalten):

- `examples/postgres-to-postgres/docker-compose.yml`
- `examples/postgres-to-postgres/sql/*.sql`
- `examples/postgres-to-postgres/lm/*.lm`

```bash
./scripts/run-postgres-example.sh
```


## H2 → CSV

```bash
./scripts/run-h2-csv-example.sh
```

Ergebnisdateien liegen danach unter `build/h2-csv-out/*.csv`.

## H2 → XLSX

```bash
./scripts/run-h2-xlsx-example.sh
```

Ergebnisdatei liegt danach unter `build/h2-xlsx-out/target.xlsx`.


## Varianten mit dediziertem LM (abweichende Layouts)

Die folgenden Beispiele zeigen explizit den Fall, dass **nicht** das gleiche LM verwendet wird, weil die Feld-/Source-Namen anders sind.

### CSV (custom) → H2

```bash
./scripts/run-csv-custom-h2-example.sh
```

Dabei wird `examples/csv-custom-to-h2/source_custom.lm` verwendet.

### H2 → XLSX (custom layout)

```bash
./scripts/run-h2-xlsx-custom-example.sh
```

Dabei wird `examples/h2-to-xlsx-custom/lm/target_xlsx_custom.lm` verwendet.



## Windows PowerShell (separate Aufrufe, **ungetestet**)

> ⚠️ Die folgenden `*.ps1`-Skripte sind als Windows-Pendants vorhanden, wurden im Projektkontext aber bislang **nicht automatisiert getestet**.

```powershell
.\scripts\run-h2-example.ps1
.\scripts\run-csv-h2-example.ps1
.\scripts\run-xlsx-h2-example.ps1
.\scripts\run-postgres-example.ps1
.\scripts\run-h2-csv-example.ps1
.\scripts\run-h2-xlsx-example.ps1
.\scripts\run-csv-custom-h2-example.ps1
.\scripts\run-h2-xlsx-custom-example.ps1
```

## Entscheidungs-Matrix: gleiches LM vs. dediziertes LM

| Szenario | Gleiches LM wiederverwendbar? | Warum |
|---|---:|---|
| H2 → H2 | Ja | Quelle/Ziel folgen demselben logischen Tabellen-/Spaltenlayout. |
| CSV → H2 (`examples/csv-to-h2`) | Ja | CSV-Header entsprechen den Source-Tabellen-/Spaltennamen aus `source.lm`. |
| XLSX → H2 (`examples/xlsx-to-h2`) | Ja | Sheet-/Spaltennamen entsprechen den Source-Namen aus `source.lm`. |
| H2 → CSV | Ja | Ziel-CSV wird mit den im `target.lm` definierten Zielnamen geschrieben. |
| H2 → XLSX | Ja | Ziel-Sheets/-Spalten folgen direkt `target.lm`. |
| CSV(custom) → H2 (`examples/csv-custom-to-h2`) | Nein | Abweichende Header/Source-Namen erfordern `source_custom.lm`. |
| H2 → XLSX(custom) (`examples/h2-to-xlsx-custom`) | Nein | Abweichende Ziel-Sheet-/Spaltennamen erfordern `target_xlsx_custom.lm`. |

Faustregel: **Transportformat ist sekundär**. Entscheidend ist, ob das **logische Layout** (Source-/Target-Namen + Feldsemantik) übereinstimmt.
