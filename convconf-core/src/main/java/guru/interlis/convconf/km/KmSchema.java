package guru.interlis.convconf.km;

import java.util.*;

/** KM schema extracted from INTERLIS with class and attribute metadata for validation. */
public record KmSchema(Map<String, KmClassInfo> classes) {
    public boolean hasClass(String className) {
        return classes.containsKey(className);
    }

    public boolean hasAttribute(String className, String attribute) {
        KmClassInfo info = classes.get(className);
        return info != null && info.attributes().containsKey(attribute);
    }

    public Optional<KmAttributeInfo> attribute(String className, String attribute) {
        KmClassInfo info = classes.get(className);
        if (info == null) return Optional.empty();
        return Optional.ofNullable(info.attributes().get(attribute));
    }

    public Set<String> classNames() {
        return classes.keySet();
    }

    public record KmClassInfo(String name,
                              boolean structure,
                              String superClass,
                              Map<String, KmAttributeInfo> attributes,
                              Set<String> structureAttributes) {
    }

    public record KmAttributeInfo(String name,
                                  String typeKind,
                                  boolean mandatory,
                                  String referenceTarget,
                                  Set<String> enumValues) {
    }
}
