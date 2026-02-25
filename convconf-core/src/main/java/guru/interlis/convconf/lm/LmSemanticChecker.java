package guru.interlis.convconf.lm;

import guru.interlis.convconf.km.KmSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Strict semantic checker for LM declarations against KM classes and attributes. */
public final class LmSemanticChecker {
    public List<String> check(LmModel lm, KmSchema kmSchema) {
        List<String> errors = new ArrayList<>();
        for (var d : lm.dataDecls()) {
            if (!kmSchema.hasClass(d.className())) {
                errors.add("Unknown class: " + d.className());
                continue;
            }
            checkColumns(errors, kmSchema, d.className(), d.columns(), lm.valueMaps());
            d.withBlocks().forEach(w -> checkColumns(errors, kmSchema, d.className(), w.columns(), lm.valueMaps()));
            for (ConversionDecl conv : d.conversions()) {
                if (!lm.valueMaps().containsKey(conv.name())) {
                    errors.add("Unknown conversion map " + conv.name() + " in DATA " + d.name());
                }
            }
        }
        for (var i : lm.inspections()) {
            if (!kmSchema.hasClass(i.className())) {
                errors.add("Unknown inspection class: " + i.className());
                continue;
            }
            checkColumns(errors, kmSchema, i.className(), i.columns(), lm.valueMaps());
            i.withBlocks().forEach(w -> checkColumns(errors, kmSchema, i.className(), w.columns(), lm.valueMaps()));
            for (ConversionDecl conv : i.conversions()) {
                if (!lm.valueMaps().containsKey(conv.name())) {
                    errors.add("Unknown conversion map " + conv.name() + " in INSPECTION " + i.name());
                }
            }
        }
        return errors;
    }

    private void checkColumns(List<String> errors,
                              KmSchema kmSchema,
                              String className,
                              List<ColumnMap> columns,
                              Map<String, Map<Integer, String>> valueMaps) {
        for (var c : columns) {
            if (c.targetPath().startsWith("$")) {
                continue;
            }
            String attr = c.targetPath().contains(".") ? c.targetPath().substring(c.targetPath().lastIndexOf('.') + 1) : c.targetPath();
            if (!attributeExistsOnClassOrSubclass(kmSchema, className, attr)) {
                errors.add("Unknown attribute " + attr + " on " + className);
                continue;
            }
            var infoOpt = kmSchema.attribute(className, attr);
            if (c.valueMapName() != null && !valueMaps.containsKey(c.valueMapName())) {
                errors.add("Unknown VALUEMAP " + c.valueMapName() + " for " + className + "." + attr);
            }
        }
    }

    private boolean attributeExistsOnClassOrSubclass(KmSchema kmSchema, String className, String attr) {
        if (kmSchema.hasAttribute(className, attr)) {
            return true;
        }
        for (var ci : kmSchema.classes().values()) {
            if (className.equals(ci.superClass()) && ci.attributes().containsKey(attr)) {
                return true;
            }
        }
        return false;
    }

}
