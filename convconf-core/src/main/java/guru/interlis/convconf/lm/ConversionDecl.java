package guru.interlis.convconf.lm;

/** Conversion call declaration (e.g., CONVERSION map(column -- target)). */
public record ConversionDecl(String name, String sourceColumn, String targetAlias) {
}
