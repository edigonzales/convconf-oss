package guru.interlis.convconf.lm;

import java.util.List;

/** WITH alias block containing additional column mappings. */
public record WithBlock(String alias, List<ColumnMap> columns) {
}
