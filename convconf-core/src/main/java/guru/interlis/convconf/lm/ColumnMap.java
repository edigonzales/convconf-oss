package guru.interlis.convconf.lm;

/** Single column to target path mapping plus optional value map. */
public record ColumnMap(String column, String targetPath, String valueMapName) {
}
