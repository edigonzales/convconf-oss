package guru.interlis.convconf;

import guru.interlis.convconf.api.ConvConfService;
import guru.interlis.convconf.file.CsvDirectoryAdapter;
import guru.interlis.convconf.file.XlsxWorkbookAdapter;
import guru.interlis.convconf.h2.H2Adapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class CsvAndXlsxAdapterIntegrationTest {
    @Test
    void convertsFromCsvToH2() throws Exception {
        String tgtJdbc = "jdbc:h2:mem:csvtgt;DB_CLOSE_DELAY=-1";
        try (Connection tgt = DriverManager.getConnection(tgtJdbc, "sa", "")) {
            runSql(tgt, Path.of("../examples/h2-to-h2/sql/target-schema.sql"));
            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/h2-to-h2/lm/source.lm"),
                    Path.of("../examples/h2-to-h2/lm/target.lm"),
                    new CsvDirectoryAdapter(Path.of("../examples/csv-to-h2/source")),
                    new H2Adapter(tgt)
            );
            try (var rs = tgt.createStatement().executeQuery("SELECT COUNT(*) FROM TGT_ORGANISATION")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    @Test
    void convertsFromXlsxToH2() throws Exception {
        String tgtJdbc = "jdbc:h2:mem:xlsxtgt;DB_CLOSE_DELAY=-1";
        try (Connection tgt = DriverManager.getConnection(tgtJdbc, "sa", "")) {
            runSql(tgt, Path.of("../examples/h2-to-h2/sql/target-schema.sql"));
            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/h2-to-h2/lm/source.lm"),
                    Path.of("../examples/h2-to-h2/lm/target.lm"),
                    new XlsxWorkbookAdapter(Path.of("../examples/xlsx-to-h2/source/source.xlsx")),
                    new H2Adapter(tgt)
            );
            try (var rs = tgt.createStatement().executeQuery("SELECT COUNT(*) FROM TGT_PERSON")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(3);
            }
        }
    }

    @Test
    void writesCsvAndXlsxAsTargets() throws Exception {
        String srcJdbc = "jdbc:h2:mem:src_file_writer;DB_CLOSE_DELAY=-1";
        try (Connection src = DriverManager.getConnection(srcJdbc, "sa", "")) {
            runSql(src, Path.of("../examples/h2-to-h2/sql/source-schema.sql"));
            runSql(src, Path.of("../examples/h2-to-h2/sql/source-seed.sql"));
            Path csvOut = Files.createTempDirectory("csv-out");
            Path xlsxOut = Files.createTempDirectory("xlsx-out").resolve("out.xlsx");
            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/h2-to-h2/lm/source.lm"),
                    Path.of("../examples/h2-to-h2/lm/target.lm"),
                    new H2Adapter(src),
                    new CsvDirectoryAdapter(csvOut)
            );
            assertThat(Files.exists(csvOut.resolve("TGT_PERSON.csv"))).isTrue();

            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/h2-to-h2/lm/source.lm"),
                    Path.of("../examples/h2-to-h2/lm/target.lm"),
                    new H2Adapter(src),
                    new XlsxWorkbookAdapter(xlsxOut)
            );
            assertThat(Files.exists(xlsxOut)).isTrue();
        }
    }


    @Test
    void convertsFromCustomCsvToH2WithDedicatedLm() throws Exception {
        String tgtJdbc = "jdbc:h2:mem:csvcustom;DB_CLOSE_DELAY=-1";
        try (Connection tgt = DriverManager.getConnection(tgtJdbc, "sa", "")) {
            runSql(tgt, Path.of("../examples/h2-to-h2/sql/target-schema.sql"));
            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/csv-custom-to-h2/source_custom.lm"),
                    Path.of("../examples/h2-to-h2/lm/target.lm"),
                    new CsvDirectoryAdapter(Path.of("../examples/csv-custom-to-h2/source")),
                    new H2Adapter(tgt)
            );
            try (var rs = tgt.createStatement().executeQuery("SELECT COUNT(*) FROM TGT_ORGANISATION")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    @Test
    void writesCustomXlsxLayoutWithDedicatedTargetLm() throws Exception {
        String srcJdbc = "jdbc:h2:mem:h2_to_xlsx_custom;DB_CLOSE_DELAY=-1";
        try (Connection src = DriverManager.getConnection(srcJdbc, "sa", "")) {
            runSql(src, Path.of("../examples/h2-to-h2/sql/source-schema.sql"));
            runSql(src, Path.of("../examples/h2-to-h2/sql/source-seed.sql"));
            Path xlsxOut = Files.createTempDirectory("xlsx-custom-out").resolve("out_custom.xlsx");
            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/h2-to-h2/lm/source.lm"),
                    Path.of("../examples/h2-to-xlsx-custom/lm/target_xlsx_custom.lm"),
                    new H2Adapter(src),
                    new XlsxWorkbookAdapter(xlsxOut)
            );
            assertThat(Files.exists(xlsxOut)).isTrue();
        }
    }

    private void runSql(Connection con, Path file) throws Exception {
        for (String stmt : Files.readString(file).split(";")) {
            if (stmt.isBlank()) continue;
            try (Statement s = con.createStatement()) {
                s.execute(stmt);
            }
        }
    }
}
