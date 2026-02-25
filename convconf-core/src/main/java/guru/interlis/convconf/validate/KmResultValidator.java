package guru.interlis.convconf.validate;

import guru.interlis.convconf.km.KmSchema;
import guru.interlis.convconf.runtime.CanonicalRecord;

import java.util.ArrayList;
import java.util.List;

/** Validates canonical conversion results against KM constraints (mandatory attributes). */
public final class KmResultValidator {
    public List<String> validate(KmSchema kmSchema, List<CanonicalRecord> records) {
        List<String> errors = new ArrayList<>();
        for (CanonicalRecord rec : records) {
            var classInfo = kmSchema.classes().get(rec.className());
            if (classInfo == null) {
                errors.add("Result contains unknown class " + rec.className());
                continue;
            }
            classInfo.attributes().values().forEach(attr -> {
                if (attr.mandatory()) {
                    String value = rec.values().get(attr.name());
                    if (value == null || value.isBlank()) {
                        errors.add("Mandatory attribute " + rec.className() + "." + attr.name() + " is empty for ident " + rec.ident());
                    }
                }
            });
        }
        return errors;
    }
}
