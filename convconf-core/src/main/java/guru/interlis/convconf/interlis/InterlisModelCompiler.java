package guru.interlis.convconf.interlis;

import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.*;
import guru.interlis.convconf.km.KmSchema;

import java.nio.file.Path;
import java.util.*;

/**
 * Compiles INTERLIS models with ili2c and extracts KM metadata from {@link TransferDescription}.
 */
public final class InterlisModelCompiler {

    public CompileResult compile(Path iliFile) {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(iliFile.toAbsolutePath().toString(), FileEntryKind.ILIMODELFILE));
        TransferDescription td = ch.interlis.ili2c.Main.runCompiler(config);
        if (td == null) {
            throw new IllegalStateException("ili2c failed to compile model: " + iliFile);
        }
        return new CompileResult(td, extractSchema(td));
    }

    private KmSchema extractSchema(TransferDescription td) {
        Map<String, KmSchema.KmClassInfo> classes = new LinkedHashMap<>();
        Iterator<?> modeli = td.iterator();
        while (modeli.hasNext()) {
            Object obj = modeli.next();
            if (obj instanceof Model model && !(model instanceof PredefinedModel)) {
                Iterator<?> topicIt = model.iterator();
                while (topicIt.hasNext()) {
                    Object topicObj = topicIt.next();
                    if (topicObj instanceof Topic topic) {
                        Iterator<?> classIt = topic.iterator();
                        while (classIt.hasNext()) {
                            Object clObj = classIt.next();
                            if (clObj instanceof AbstractClassDef classDef) {
                                String qName = classDef.getScopedName(null);
                                boolean structure = classDef instanceof Table t && t.isIdentifiable() == false;
                                String superName = classDef.getExtending() != null ? classDef.getExtending().getScopedName(null) : null;
                                Map<String, KmSchema.KmAttributeInfo> attrMap = new LinkedHashMap<>();
                                Set<String> structureAttrs = new LinkedHashSet<>();
                                Iterator<?> attrIt = classDef.getAttributes();
                                while (attrIt.hasNext()) {
                                    AttributeDef a = (AttributeDef) attrIt.next();
                                    Type t = a.getDomainResolvingAll();
                                    boolean mandatory = a.getCardinality()!=null && a.getCardinality().getMinimum()>0;
                                    String refTarget = null;
                                    Set<String> enumValues = Set.of();
                                    String typeKind = "UNKNOWN";
                                    if (t instanceof ReferenceType rt) {
                                        typeKind = "REFERENCE";
                                        if (rt.getReferred() != null) {
                                            refTarget = rt.getReferred().getScopedName(null);
                                        }
                                    } else if (t instanceof CompositionType ct) {
                                        typeKind = "COMPOSITION";
                                        if (ct.getComponentType() != null) {
                                            structureAttrs.add(a.getName());
                                        }
                                    } else if (t instanceof EnumerationType et) {
                                        typeKind = "ENUM";
                                        enumValues = new LinkedHashSet<>();
                                        collectEnumValues("", et.getConsolidatedEnumeration(), enumValues);
                                    } else if (t instanceof NumericType) {
                                        typeKind = "NUMERIC";
                                    } else if (t instanceof TextType) {
                                        typeKind = "TEXT";
                                    }
                                    attrMap.put(a.getName(), new KmSchema.KmAttributeInfo(a.getName(), typeKind, mandatory, refTarget, enumValues));
                                }
                                classes.put(qName, new KmSchema.KmClassInfo(qName, structure, superName, attrMap, structureAttrs));
                            }
                        }
                    }
                }
            }
        }
        return new KmSchema(classes);
    }

    private void collectEnumValues(String prefix, ch.interlis.ili2c.metamodel.Enumeration enumeration, Set<String> out) {
        if (enumeration == null) return;
        Iterator<ch.interlis.ili2c.metamodel.Enumeration.Element> ele = enumeration.getElements();
        while (ele.hasNext()) {
            ch.interlis.ili2c.metamodel.Enumeration.Element e = ele.next();
            String path = prefix.isEmpty() ? e.getName() : prefix + "." + e.getName();
            out.add(path);
            collectEnumValues(path, e.getSubEnumeration(), out);
        }
    }

    /** Compilation output tuple. */
    public record CompileResult(TransferDescription transferDescription, KmSchema kmSchema) {}
}
