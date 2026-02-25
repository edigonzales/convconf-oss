package guru.interlis.convconf.validate;

import guru.interlis.convconf.lm.DataDecl;
import guru.interlis.convconf.lm.LmModel;
import guru.interlis.convconf.runtime.RecordSourceReader;

import java.util.ArrayList;
import java.util.List;

/** Validates source instance data against LM declarations before conversion. */
public final class LmDataValidator {
    public List<String> validate(RecordSourceReader reader, LmModel lm) throws Exception {
        List<String> errors = new ArrayList<>();
        for (DataDecl d : lm.dataDecls()) {
            var rows = reader.read(d.sourceTable(), d.whereEquals());
            for (var row : rows) {
                if (d.identColumn() != null && !hasColumn(row, d.identColumn())) {
                    errors.add("Missing ident column " + d.identColumn() + " in source " + d.sourceTable());
                }
                d.columns().forEach(c -> {
                    if (!hasColumn(row, c.column())) {
                        errors.add("Missing column " + c.column() + " in source " + d.sourceTable());
                    }
                });
            }
        }
        return errors;
    }

    private boolean hasColumn(java.util.Map<String, Object> row, String name) {
        if (row.containsKey(name)) return true;
        return row.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(name));
    }
}
