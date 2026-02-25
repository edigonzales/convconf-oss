package guru.interlis.convconf.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/** Canonical record bridging source and target logical models. */
public record CanonicalRecord(
        String className,
        String ident,
        String parent,
        String structAttr,
        Map<String, String> values) {
    public static CanonicalRecord of(String className, String ident) {
        return new CanonicalRecord(className, ident, null, null, new LinkedHashMap<>());
    }
}
