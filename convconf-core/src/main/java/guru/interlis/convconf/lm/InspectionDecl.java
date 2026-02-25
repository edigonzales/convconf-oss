package guru.interlis.convconf.lm;

import java.util.List;

/** Inspection declaration used for structure rows with $PARENT/$CLASS/$STRUCTATTR semantics. */
public record InspectionDecl(
        String name,
        String sourceTable,
        String className,
        MappingDirection direction,
        String identColumn,
        String parentColumn,
        String structAttrColumn,
        String structAttrMap,
        String classColumn,
        String classMap,
        List<ConversionDecl> conversions,
        List<AliasDecl> aliases,
        List<WithBlock> withBlocks,
        List<ColumnMap> columns) {
}
