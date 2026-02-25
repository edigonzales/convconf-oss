package guru.interlis.convconf.lm;

/** JOIN declaration for LM source composition. */
public record JoinDecl(String joinType, String sourceName, String leftColumn, String rightColumn) {
}
