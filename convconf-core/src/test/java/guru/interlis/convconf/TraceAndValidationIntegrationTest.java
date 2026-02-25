package guru.interlis.convconf;

import guru.interlis.convconf.api.ConvConfService;
import guru.interlis.convconf.h2.H2Adapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class TraceAndValidationIntegrationTest {
    @Test
    void returnsTraceEvents() throws Exception {
        String srcJdbc = "jdbc:h2:mem:trace_src;DB_CLOSE_DELAY=-1";
        String tgtJdbc = "jdbc:h2:mem:trace_tgt;DB_CLOSE_DELAY=-1";
        try (Connection src = DriverManager.getConnection(srcJdbc, "sa", "");
             Connection tgt = DriverManager.getConnection(tgtJdbc, "sa", "")) {
            runSql(src, Path.of("../examples/h2-to-h2/sql/source-schema.sql"));
            runSql(src, Path.of("../examples/h2-to-h2/sql/source-seed.sql"));
            runSql(tgt, Path.of("../examples/h2-to-h2/sql/target-schema.sql"));

            var result = new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/h2-to-h2/lm/source.lm"),
                    Path.of("../examples/h2-to-h2/lm/target.lm"),
                    new H2Adapter(src),
                    new H2Adapter(tgt),
                    true
            );
            assertThat(result.traceEvents()).isNotEmpty();
            assertThat(result.canonicalRecords()).isNotEmpty();
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
