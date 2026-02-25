package guru.interlis.convconf.cli;

import guru.interlis.convconf.api.ConvConfService;
import guru.interlis.convconf.file.CsvDirectoryAdapter;
import guru.interlis.convconf.file.XlsxWorkbookAdapter;
import guru.interlis.convconf.h2.H2Adapter;
import guru.interlis.convconf.postgresql.PostgreSqlAdapter;
import guru.interlis.convconf.runtime.RecordSourceReader;
import guru.interlis.convconf.runtime.RecordTargetWriter;
import guru.interlis.convconf.runtime.TraceEvent;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/** CLI for compile/check/plan/convert tasks in the ConvConf PoC. */
@Command(name = "convconf", mixinStandardHelpOptions = true,
        subcommands = {
                ConvConfCli.CheckLm.class,
                ConvConfCli.Plan.class,
                ConvConfCli.Convert.class,
                ConvConfCli.ConvertH2.class,
                ConvConfCli.ConvertPostgres.class,
                ConvConfCli.ConvertCsvToH2.class,
                ConvConfCli.ConvertH2ToCsv.class,
                ConvConfCli.ConvertXlsxToH2.class,
                ConvConfCli.ConvertH2ToXlsx.class
        })
public class ConvConfCli implements Runnable {
    @Override public void run() { CommandLine.usage(this, System.out); }

    @Command(name = "check-lm", description = "Validates an LM against INTERLIS KM")
    static class CheckLm implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--lm", required = true) Path lm;

        @Override public Integer call() throws Exception {
            var errors = new ConvConfService().check(km, lm);
            if (errors.isEmpty()) {
                System.out.println("LM valid");
                return 0;
            }
            errors.forEach(System.err::println);
            return 2;
        }
    }

    @Command(name = "plan", description = "Generates conversion plan (KK) from source and target LM")
    static class Plan implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--out", required = true) Path out;

        @Override public Integer call() throws Exception {
            var plan = new ConvConfService().plan(km, sourceLm, targetLm);
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"kmModel\": \"").append(plan.kmModel()).append("\",\n");
            sb.append("  \"sourceLm\": \"").append(plan.sourceLm()).append("\",\n");
            sb.append("  \"targetLm\": \"").append(plan.targetLm()).append("\",\n");
            sb.append("  \"steps\": [\n");
            for (int i = 0; i < plan.steps().size(); i++) {
                var s = plan.steps().get(i);
                sb.append("    {\"phase\":\"").append(s.phase()).append("\",\"source\":\"").append(s.source())
                        .append("\",\"target\":\"").append(s.target()).append("\",\"detail\":\"").append(s.detail()).append("\"}");
                if (i + 1 < plan.steps().size()) sb.append(',');
                sb.append('\n');
            }
            sb.append("  ]\n}");
            Files.writeString(out, sb.toString());
            System.out.println("Plan written: " + out);
            return 0;
        }
    }

    abstract static class BaseConvert {
        @Option(names = "--trace-out") Path traceOut;

        protected void writeTrace(java.util.List<TraceEvent> trace) throws Exception {
            if (traceOut == null) return;
            StringBuilder sb = new StringBuilder();
            for (TraceEvent e : trace) {
                sb.append(e.phase()).append('|').append(e.className()).append('|').append(e.ident()).append('|')
                        .append(e.sourceColumn()).append("->").append(e.targetPath()).append('|').append(e.value()).append('|').append(e.detail()).append('\n');
            }
            Files.writeString(traceOut, sb.toString());
        }
    }

    enum EndpointType {
        H2,
        POSTGRES,
        CSV,
        XLSX;

        static EndpointType parse(String raw) {
            try {
                return EndpointType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (Exception ignored) {
                throw new IllegalArgumentException("Unsupported endpoint type '" + raw + "'. Expected one of: h2, postgres, csv, xlsx");
            }
        }
    }

    record EndpointOptions(String jdbc, String user, String password, Path dir, Path xlsx) {}

    record SourceEndpoint(RecordSourceReader reader, List<AutoCloseable> closeables) {}

    record TargetEndpoint(RecordTargetWriter writer, List<AutoCloseable> closeables) {}

    @Command(name = "convert", description = "Generic conversion between any supported source and target type")
    static class Convert extends BaseConvert implements Callable<Integer> {
        @Spec CommandSpec spec;

        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;

        @Option(names = "--source-type", required = true, description = "Source type: ${COMPLETION-CANDIDATES}") String sourceType;
        @Option(names = "--target-type", required = true, description = "Target type: ${COMPLETION-CANDIDATES}") String targetType;

        @Option(names = "--source-jdbc") String sourceJdbc;
        @Option(names = "--source-user") String sourceUser;
        @Option(names = "--source-password") String sourcePassword;
        @Option(names = "--source-dir") Path sourceDir;
        @Option(names = "--source-xlsx") Path sourceXlsx;

        @Option(names = "--target-jdbc") String targetJdbc;
        @Option(names = "--target-user") String targetUser;
        @Option(names = "--target-password") String targetPassword;
        @Option(names = "--target-dir") Path targetDir;
        @Option(names = "--target-xlsx") Path targetXlsx;

        @Override
        public Integer call() throws Exception {
            EndpointType srcType = EndpointType.parse(sourceType);
            EndpointType tgtType = EndpointType.parse(targetType);

            EndpointOptions srcOptions = new EndpointOptions(sourceJdbc, sourceUser, sourcePassword, sourceDir, sourceXlsx);
            EndpointOptions tgtOptions = new EndpointOptions(targetJdbc, targetUser, targetPassword, targetDir, targetXlsx);

            validateEndpoint("source", srcType, srcOptions);
            validateEndpoint("target", tgtType, tgtOptions);

            SourceEndpoint src = createSource(srcType, srcOptions);
            TargetEndpoint tgt = createTarget(tgtType, tgtOptions);

            try {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, src.reader(), tgt.writer(), traceOut != null);
                writeTrace(result.traceEvents());
            } finally {
                closeAll(src.closeables());
                closeAll(tgt.closeables());
            }
            System.out.println("Conversion done");
            return 0;
        }

        static void validateEndpoint(String side, EndpointType type, EndpointOptions options) {
            switch (type) {
                case H2 -> {
                    require(side, "jdbc", options.jdbc());
                    forbid(side, "user", options.user());
                    forbid(side, "password", options.password());
                    forbid(side, "dir", options.dir());
                    forbid(side, "xlsx", options.xlsx());
                }
                case POSTGRES -> {
                    require(side, "jdbc", options.jdbc());
                    require(side, "user", options.user());
                    require(side, "password", options.password());
                    forbid(side, "dir", options.dir());
                    forbid(side, "xlsx", options.xlsx());
                }
                case CSV -> {
                    require(side, "dir", options.dir());
                    forbid(side, "jdbc", options.jdbc());
                    forbid(side, "user", options.user());
                    forbid(side, "password", options.password());
                    forbid(side, "xlsx", options.xlsx());
                }
                case XLSX -> {
                    require(side, "xlsx", options.xlsx());
                    forbid(side, "jdbc", options.jdbc());
                    forbid(side, "user", options.user());
                    forbid(side, "password", options.password());
                    forbid(side, "dir", options.dir());
                }
            }
        }

        private static void require(String side, String name, Object value) {
            if (value == null || (value instanceof String s && s.isBlank())) {
                throw new IllegalArgumentException("--" + side + "-" + name + " is required for --" + side + "-type");
            }
        }

        private static void forbid(String side, String name, Object value) {
            if (value != null && (!(value instanceof String s) || !s.isBlank())) {
                throw new IllegalArgumentException("--" + side + "-" + name + " is not allowed for selected --" + side + "-type");
            }
        }

        private SourceEndpoint createSource(EndpointType type, EndpointOptions options) throws Exception {
            return switch (type) {
                case H2 -> {
                    Connection c = DriverManager.getConnection(options.jdbc(), "sa", "");
                    yield new SourceEndpoint(new H2Adapter(c), List.of(c));
                }
                case POSTGRES -> {
                    Connection c = DriverManager.getConnection(options.jdbc(), options.user(), options.password());
                    yield new SourceEndpoint(new PostgreSqlAdapter(c), List.of(c));
                }
                case CSV -> new SourceEndpoint(new CsvDirectoryAdapter(options.dir()), List.of());
                case XLSX -> new SourceEndpoint(new XlsxWorkbookAdapter(options.xlsx()), List.of());
            };
        }

        private TargetEndpoint createTarget(EndpointType type, EndpointOptions options) throws Exception {
            return switch (type) {
                case H2 -> {
                    Connection c = DriverManager.getConnection(options.jdbc(), "sa", "");
                    yield new TargetEndpoint(new H2Adapter(c), List.of(c));
                }
                case POSTGRES -> {
                    Connection c = DriverManager.getConnection(options.jdbc(), options.user(), options.password());
                    yield new TargetEndpoint(new PostgreSqlAdapter(c), List.of(c));
                }
                case CSV -> new TargetEndpoint(new CsvDirectoryAdapter(options.dir()), List.of());
                case XLSX -> new TargetEndpoint(new XlsxWorkbookAdapter(options.xlsx()), List.of());
            };
        }

        private void closeAll(List<AutoCloseable> closeables) {
            for (var closeable : closeables) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    throw new CommandLine.ExecutionException(spec.commandLine(), "Error while closing resources", e);
                }
            }
        }
    }

    @Command(name = "convert-h2", description = "Converts data between two H2 databases")
    static class ConvertH2 extends BaseConvert implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--source-jdbc", required = true) String sourceJdbc;
        @Option(names = "--target-jdbc", required = true) String targetJdbc;

        @Override public Integer call() throws Exception {
            try (Connection source = DriverManager.getConnection(sourceJdbc, "sa", "");
                 Connection target = DriverManager.getConnection(targetJdbc, "sa", "")) {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, new H2Adapter(source), new H2Adapter(target), traceOut != null);
                writeTrace(result.traceEvents());
            }
            System.out.println("Conversion done");
            return 0;
        }
    }

    @Command(name = "convert-postgres", description = "Converts data between two PostgreSQL databases")
    static class ConvertPostgres extends BaseConvert implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--source-jdbc", required = true) String sourceJdbc;
        @Option(names = "--target-jdbc", required = true) String targetJdbc;
        @Option(names = "--source-user", required = true) String sourceUser;
        @Option(names = "--source-password", required = true) String sourcePassword;
        @Option(names = "--target-user", required = true) String targetUser;
        @Option(names = "--target-password", required = true) String targetPassword;

        @Override public Integer call() throws Exception {
            try (Connection source = DriverManager.getConnection(sourceJdbc, sourceUser, sourcePassword);
                 Connection target = DriverManager.getConnection(targetJdbc, targetUser, targetPassword)) {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, new PostgreSqlAdapter(source), new PostgreSqlAdapter(target), traceOut != null);
                writeTrace(result.traceEvents());
            }
            System.out.println("Conversion done");
            return 0;
        }
    }

    @Command(name = "convert-csv-to-h2", description = "Converts source CSV directory to H2 database")
    static class ConvertCsvToH2 extends BaseConvert implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--source-dir", required = true) Path sourceDir;
        @Option(names = "--target-jdbc", required = true) String targetJdbc;

        @Override public Integer call() throws Exception {
            try (Connection target = DriverManager.getConnection(targetJdbc, "sa", "")) {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, new CsvDirectoryAdapter(sourceDir), new H2Adapter(target), traceOut != null);
                writeTrace(result.traceEvents());
            }
            System.out.println("Conversion done");
            return 0;
        }
    }

    @Command(name = "convert-h2-to-csv", description = "Converts source H2 database to target CSV directory")
    static class ConvertH2ToCsv extends BaseConvert implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--source-jdbc", required = true) String sourceJdbc;
        @Option(names = "--target-dir", required = true) Path targetDir;

        @Override public Integer call() throws Exception {
            try (Connection source = DriverManager.getConnection(sourceJdbc, "sa", "")) {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, new H2Adapter(source), new CsvDirectoryAdapter(targetDir), traceOut != null);
                writeTrace(result.traceEvents());
            }
            System.out.println("Conversion done");
            return 0;
        }
    }

    @Command(name = "convert-xlsx-to-h2", description = "Converts source XLSX workbook to H2 database")
    static class ConvertXlsxToH2 extends BaseConvert implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--source-xlsx", required = true) Path sourceXlsx;
        @Option(names = "--target-jdbc", required = true) String targetJdbc;

        @Override public Integer call() throws Exception {
            try (Connection target = DriverManager.getConnection(targetJdbc, "sa", "")) {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, new XlsxWorkbookAdapter(sourceXlsx), new H2Adapter(target), traceOut != null);
                writeTrace(result.traceEvents());
            }
            System.out.println("Conversion done");
            return 0;
        }
    }

    @Command(name = "convert-h2-to-xlsx", description = "Converts source H2 database to target XLSX workbook")
    static class ConvertH2ToXlsx extends BaseConvert implements Callable<Integer> {
        @Option(names = "--km", required = true) Path km;
        @Option(names = "--source-lm", required = true) Path sourceLm;
        @Option(names = "--target-lm", required = true) Path targetLm;
        @Option(names = "--source-jdbc", required = true) String sourceJdbc;
        @Option(names = "--target-xlsx", required = true) Path targetXlsx;

        @Override public Integer call() throws Exception {
            try (Connection source = DriverManager.getConnection(sourceJdbc, "sa", "")) {
                var result = new ConvConfService().convert(km, sourceLm, targetLm, new H2Adapter(source), new XlsxWorkbookAdapter(targetXlsx), traceOut != null);
                writeTrace(result.traceEvents());
            }
            System.out.println("Conversion done");
            return 0;
        }
    }

    public static void main(String[] args) {
        int code = new CommandLine(new ConvConfCli()).execute(args);
        System.exit(code);
    }
}
