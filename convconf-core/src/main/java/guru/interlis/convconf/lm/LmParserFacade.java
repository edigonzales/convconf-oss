package guru.interlis.convconf.lm;

import guru.interlis.convconf.parser.LmBaseVisitor;
import guru.interlis.convconf.parser.LmLexer;
import guru.interlis.convconf.parser.LmParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Facade to parse LM files using ANTLR grammar. */
public final class LmParserFacade {
    public LmModel parse(Path path) throws IOException {
        var lexer = new LmLexer(CharStreams.fromString(Files.readString(path)));
        var parser = new LmParser(new CommonTokenStream(lexer));
        var tree = parser.lmFile();
        return new Visitor().visitLmFile(tree);
    }

    private static final class Visitor extends LmBaseVisitor<LmModel> {
        @Override
        public LmModel visitLmFile(LmParser.LmFileContext ctx) {
            String name = ctx.ID().getText();
            Map<String, Map<Integer, String>> valueMaps = new HashMap<>();
            List<DataDecl> dataDecls = new ArrayList<>();
            List<InspectionDecl> inspections = new ArrayList<>();

            for (var stmt : ctx.statement()) {
                if (stmt.valueMapDecl() != null) {
                    var vm = stmt.valueMapDecl();
                    Map<Integer, String> entries = new LinkedHashMap<>();
                    for (var e : vm.valueMapEntry()) {
                        entries.put(Integer.parseInt(e.INT().getText()), qname(e.qname()));
                    }
                    valueMaps.put(vm.ID().getText(), entries);
                } else if (stmt.dataDecl() != null) {
                    dataDecls.add(parseData(stmt.dataDecl()));
                } else if (stmt.inspectionDecl() != null) {
                    inspections.add(parseInspection(stmt.inspectionDecl()));
                }
            }
            return new LmModel(name, valueMaps, dataDecls, inspections);
        }

        private DataDecl parseData(LmParser.DataDeclContext ctx) {
            String name = ctx.ID(0).getText();
            String table = ctx.ID(1).getText();
            String className = qname(ctx.qname());
            String ident = null;
            MappingDirection direction = MappingDirection.BIDIRECTIONAL;
            List<ColumnMap> cols = new ArrayList<>();
            Map<String, String> where = new LinkedHashMap<>();
            List<ConversionDecl> conversions = new ArrayList<>();
            List<AliasDecl> aliases = new ArrayList<>();
            List<WithBlock> withBlocks = new ArrayList<>();
            List<String> annexeTargets = new ArrayList<>();
            List<String> annexedSources = new ArrayList<>();
            List<JoinDecl> joins = new ArrayList<>();
            List<NestingDecl> nestings = new ArrayList<>();

            for (var s : ctx.dataStmt()) {
                if (s.columnStmt() != null) {
                    cols.add(parseColumn(s.columnStmt()));
                } else if (s.getText().startsWith("IDENT")) {
                    ident = s.ID().getText();
                } else if (s.getText().startsWith("WHERE")) {
                    where.put(s.ID().getText(), unquote(s.STRING().getText()));
                } else if (s.directionStmt() != null) {
                    direction = parseDirection(s.directionStmt().getChild(1).getText());
                } else if (s.conversionStmt() != null) {
                    conversions.add(new ConversionDecl(s.conversionStmt().ID(0).getText(), s.conversionStmt().ID(1).getText(), s.conversionStmt().ID(2).getText()));
                } else if (s.aliasStmt() != null) {
                    aliases.add(new AliasDecl(s.aliasStmt().ID().getText(), qname(s.aliasStmt().qname())));
                } else if (s.withStmt() != null) {
                    List<ColumnMap> withCols = s.withStmt().columnStmt().stream().map(this::parseColumn).toList();
                    withBlocks.add(new WithBlock(s.withStmt().ID().getText(), withCols));
                } else if (s.annexeStmt() != null) {
                    annexeTargets.add(qname(s.annexeStmt().qname()));
                } else if (s.annexedStmt() != null) {
                    annexedSources.add(s.annexedStmt().ID().getText());
                } else if (s.joinStmt() != null) {
                    joins.add(new JoinDecl(s.joinStmt().getChild(1).getText(), s.joinStmt().ID(0).getText(), s.joinStmt().ID(1).getText(), s.joinStmt().ID(2).getText()));
                } else if (s.nestingStmt() != null) {
                    nestings.add(new NestingDecl(s.nestingStmt().ID(0).getText(), s.nestingStmt().ID(1).getText()));
                }
            }
            return new DataDecl(name, table, className, direction, ident, where, conversions, aliases, withBlocks,
                    annexeTargets, annexedSources, joins, nestings, cols);
        }

        private InspectionDecl parseInspection(LmParser.InspectionDeclContext ctx) {
            String name = ctx.ID(0).getText();
            String table = ctx.ID(1).getText();
            String className = qname(ctx.qname());
            MappingDirection direction = MappingDirection.BIDIRECTIONAL;
            String ident = null, parent = null, saCol = null, saMap = null, classCol = null, classMap = null;
            List<ColumnMap> cols = new ArrayList<>();
            List<ConversionDecl> conversions = new ArrayList<>();
            List<AliasDecl> aliases = new ArrayList<>();
            List<WithBlock> withBlocks = new ArrayList<>();
            for (var s : ctx.inspectionStmt()) {
                if (s.columnStmt() != null) {
                    cols.add(parseColumn(s.columnStmt()));
                } else if (s.getText().startsWith("IDENT")) {
                    ident = s.ID().get(0).getText();
                } else if (s.getText().startsWith("PARENT")) {
                    parent = s.ID().get(0).getText();
                } else if (s.getText().startsWith("STRUCTATTR")) {
                    saCol = s.ID().get(0).getText();
                    saMap = s.ID().get(1).getText();
                } else if (s.getText().startsWith("CLASSCOL")) {
                    classCol = s.ID().get(0).getText();
                    classMap = s.ID().get(1).getText();
                } else if (s.directionStmt() != null) {
                    direction = parseDirection(s.directionStmt().getChild(1).getText());
                } else if (s.conversionStmt() != null) {
                    conversions.add(new ConversionDecl(s.conversionStmt().ID(0).getText(), s.conversionStmt().ID(1).getText(), s.conversionStmt().ID(2).getText()));
                } else if (s.aliasStmt() != null) {
                    aliases.add(new AliasDecl(s.aliasStmt().ID().getText(), qname(s.aliasStmt().qname())));
                } else if (s.withStmt() != null) {
                    List<ColumnMap> withCols = s.withStmt().columnStmt().stream().map(this::parseColumn).toList();
                    withBlocks.add(new WithBlock(s.withStmt().ID().getText(), withCols));
                }
            }
            return new InspectionDecl(name, table, className, direction, ident, parent, saCol, saMap, classCol, classMap,
                    conversions, aliases, withBlocks, cols);
        }

        private ColumnMap parseColumn(LmParser.ColumnStmtContext c) {
            return new ColumnMap(c.ID(0).getText(), targetPath(c.targetPath()), c.ID().size() > 1 ? c.ID(1).getText() : null);
        }

        private MappingDirection parseDirection(String token) {
            return switch (token) {
                case "<->" -> MappingDirection.BIDIRECTIONAL;
                case "<-" -> MappingDirection.INPUT_ONLY;
                case "->" -> MappingDirection.OUTPUT_ONLY;
                default -> MappingDirection.BIDIRECTIONAL;
            };
        }

        private static String qname(LmParser.QnameContext ctx) {
            return String.join(".", ctx.ID().stream().map(t -> t.getText()).toList());
        }

        private static String targetPath(LmParser.TargetPathContext ctx) {
            if (ctx.DOLLAR_ID() != null) return ctx.DOLLAR_ID().getText();
            return String.join(".", ctx.ID().stream().map(t -> t.getText()).toList());
        }

        private static String unquote(String s) {
            return s.substring(1, s.length() - 1);
        }
    }
}
