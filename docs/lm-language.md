# LM-Sprache (PoC-Subset)

Unterstützte Konstrukte:

- `VALUEMAP`
- `DATA`
- `INSPECTION ALL`
- Richtungen: `DIRECTION <->`, `DIRECTION <-`, `DIRECTION ->`
- `CONVERSION`, `ALIAS`, `WITH`
- `ANNEXE`, `ANNEXED`
- `JOIN`, `NESTING`
- Spezialfelder: `$CLASS`, `$PARENT`, `$STRUCTATTR`, `$IDENT`

## Beispiel

```text
LM Source;
VALUEMAP OrgFormMap {
  5 -> Verein.Domain.Koerperschaft.HandelsGesellschaft.AG;
}
DATA Organisation FROM SRC_ORGANISATION CLASS Verein.Domain.Organisation {
  DIRECTION <->;
  IDENT ID;
  COLUMN ID -> Nummer;
  COLUMN FORM_CODE -> Form USING OrgFormMap;
}
```

## Strikte Semantik

- Unbekannte KM-Klassen -> Fehler
- Unbekannte KM-Attribute -> Fehler
- Unbekannte VALUEMAP/CONVERSION -> Fehler
- LM-Instanzdaten werden vor Konversion gegen benötigte Spalten geprüft
- Ergebnisdaten werden nach Konversion gegen KM-Mandatory-Attribute geprüft


## Backend-unabhängigkeit

Ein LM beschreibt die **logische Struktur** der Daten, nicht den Transportkanal.
Darum kann ein LM oft für H2/CSV/XLSX wiederverwendet werden, solange Tabellen-/Sheet-/Spaltennamen gleich bleiben.
Bei abweichenden Layouts (z. B. andere Headernamen) braucht es ein dediziertes LM.
