package guru.interlis.convconf.lm;

import java.util.List;
import java.util.Map;

/** Data declaration for one source mapped to one KM class. */
public record DataDecl(
        String name,
        String sourceTable,
        String className,
        MappingDirection direction,
        String identColumn,
        Map<String, String> whereEquals,
        List<ConversionDecl> conversions,
        List<AliasDecl> aliases,
        List<WithBlock> withBlocks,
        List<String> annexeTargets,
        List<String> annexedSources,
        List<JoinDecl> joins,
        List<NestingDecl> nestings,
        List<ColumnMap> columns) {
}
