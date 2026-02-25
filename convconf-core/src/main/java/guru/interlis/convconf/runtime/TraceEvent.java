package guru.interlis.convconf.runtime;

/** Trace event for one mapped target value. */
public record TraceEvent(String phase,
                         String className,
                         String ident,
                         String sourceColumn,
                         String targetPath,
                         String value,
                         String detail) {
}
