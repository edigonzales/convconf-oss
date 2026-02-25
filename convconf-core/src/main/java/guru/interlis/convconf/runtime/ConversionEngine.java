package guru.interlis.convconf.runtime;

import guru.interlis.convconf.lm.ColumnMap;
import guru.interlis.convconf.lm.DataDecl;
import guru.interlis.convconf.lm.InspectionDecl;
import guru.interlis.convconf.lm.LmModel;
import guru.interlis.convconf.plan.ConversionPlan;

import java.util.*;

/**
 * Executes source-to-target conversion through canonical records.
 * <p>
 * The engine first reads source rows into KM-shaped canonical records and then
 * writes those records using target LM mappings.
 * </p>
 */
public final class ConversionEngine {
    /**
     * Executes conversion from a pre-built plan.
     */
    public ConversionResult convert(RecordSourceReader sourceReader,
                                    RecordTargetWriter targetWriter,
                                    ConversionPlan plan,
                                    boolean traceEnabled) throws Exception {
        return convert(sourceReader, targetWriter, plan.sourceModel(), plan.targetModel(), traceEnabled);
    }

    /**
     * Executes conversion directly from parsed source/target LM models.
     */
    public ConversionResult convert(RecordSourceReader sourceReader,
                                    RecordTargetWriter targetWriter,
                                    LmModel sourceLm,
                                    LmModel targetLm,
                                    boolean traceEnabled) throws Exception {
        List<TraceEvent> trace = traceEnabled ? new ArrayList<>() : null;
        List<CanonicalRecord> canonical = readCanonical(sourceReader, sourceLm, trace);
        writeCanonical(targetWriter, targetLm, canonical, trace);
        return new ConversionResult(canonical, trace == null ? List.of() : trace);
    }

    public List<CanonicalRecord> readCanonical(RecordSourceReader sourceReader, LmModel lm, List<TraceEvent> trace) throws Exception {
        List<CanonicalRecord> out = new ArrayList<>();
        for (DataDecl d : lm.dataDecls()) {
            var rows = sourceReader.read(d.sourceTable(), d.whereEquals());
            for (var row : rows) {
                String ident = d.identColumn() == null ? UUID.randomUUID().toString() : Objects.toString(get(row, d.identColumn()), null);
                Map<String, String> vals = new LinkedHashMap<>();
                for (ColumnMap c : d.columns()) {
                    String raw = Objects.toString(get(row, c.column()), null);
                    String mapped = mapValue(lm, c.valueMapName(), raw);
                    vals.put(c.targetPath(), mapped);
                    addTrace(trace, new TraceEvent("READ", d.className(), ident, c.column(), c.targetPath(), mapped, "source=" + d.sourceTable()));
                }
                out.add(new CanonicalRecord(d.className(), ident, null, null, vals));
            }
        }
        for (InspectionDecl i : lm.inspections()) {
            var rows = sourceReader.read(i.sourceTable(), Map.of());
            for (var row : rows) {
                String ident = Objects.toString(get(row, i.identColumn()), null);
                String parent = Objects.toString(get(row, i.parentColumn()), null);
                String structAttr = mapValue(lm, i.structAttrMap(), Objects.toString(get(row, i.structAttrColumn()), null));
                String klass = mapValue(lm, i.classMap(), Objects.toString(get(row, i.classColumn()), null));
                Map<String, String> vals = new LinkedHashMap<>();
                for (ColumnMap c : i.columns()) {
                    String mapped = mapValue(lm, c.valueMapName(), Objects.toString(get(row, c.column()), null));
                    vals.put(c.targetPath(), mapped);
                    addTrace(trace, new TraceEvent("READ", klass != null ? klass : i.className(), ident, c.column(), c.targetPath(), mapped, "inspection=" + i.sourceTable()));
                }
                out.add(new CanonicalRecord(klass != null ? klass : i.className(), ident, parent, structAttr, vals));
            }
        }
        return out;
    }

    public void writeCanonical(RecordTargetWriter targetWriter, LmModel lm, List<CanonicalRecord> records, List<TraceEvent> trace) throws Exception {
        for (DataDecl d : lm.dataDecls()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (CanonicalRecord rec : records) {
                if (!rec.className().equals(d.className())) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                if (d.identColumn() != null) row.put(d.identColumn(), rec.ident());
                for (ColumnMap c : d.columns()) {
                    String v = switch (c.targetPath()) {
                        case "$PARENT" -> rec.parent();
                        case "$STRUCTATTR" -> rec.structAttr();
                        case "$CLASS" -> rec.className();
                        case "$IDENT" -> rec.ident();
                        default -> rec.values().get(c.targetPath());
                    };
                    Object finalValue = reverseMap(lm, c.valueMapName(), v);
                    row.put(c.column(), finalValue);
                    addTrace(trace, new TraceEvent("WRITE", rec.className(), rec.ident(), c.targetPath(), c.column(), Objects.toString(finalValue, null), "target=" + d.sourceTable()));
                }
                rows.add(row);
            }
            targetWriter.write(d.sourceTable(), rows);
        }
    }

    private void addTrace(List<TraceEvent> trace, TraceEvent event) {
        if (trace != null) {
            trace.add(event);
        }
    }

    private Object get(Map<String, Object> row, String column) {
        if (row.containsKey(column)) {
            return row.get(column);
        }
        for (var e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(column)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String mapValue(LmModel lm, String mapName, String value) {
        if (mapName == null || value == null || "@".equals(value)) return value;
        Map<Integer, String> map = lm.valueMaps().get(mapName);
        if (map == null) throw new IllegalArgumentException("Unknown map " + mapName);
        return map.get(Integer.parseInt(value));
    }

    private Object reverseMap(LmModel lm, String mapName, String value) {
        if (mapName == null || value == null || "@".equals(value)) return value;
        Map<Integer, String> map = lm.valueMaps().get(mapName);
        return map.entrySet().stream().filter(e -> e.getValue().equals(value)).findFirst().orElseThrow().getKey();
    }

    /** Result tuple with canonical records and optional trace events. */
    public record ConversionResult(List<CanonicalRecord> canonicalRecords, List<TraceEvent> traceEvents) {}
}
