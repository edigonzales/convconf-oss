package guru.interlis.convconf;

import guru.interlis.convconf.api.ConvConfService;
import guru.interlis.convconf.postgresql.PostgreSqlAdapter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlAdapterTest {
    @Test
    void worksOnPostgresDialectConnections() throws Exception {
        String srcJdbc = "jdbc:h2:mem:pgsrc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        String tgtJdbc = "jdbc:h2:mem:pgtgt;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (Connection src = DriverManager.getConnection(srcJdbc, "sa", "");
             Connection tgt = DriverManager.getConnection(tgtJdbc, "sa", "")) {

            runSql(src, Path.of("../examples/postgres-to-postgres/sql/source-schema.sql"));
            runSql(src, Path.of("../examples/postgres-to-postgres/sql/source-seed.sql"));
            runSql(tgt, Path.of("../examples/postgres-to-postgres/sql/target-schema.sql"));

            new ConvConfService().convert(
                    Path.of("../examples/h2-to-h2/km/verein.ili"),
                    Path.of("../examples/postgres-to-postgres/lm/source_pg.lm"),
                    Path.of("../examples/postgres-to-postgres/lm/target_pg.lm"),
                    new PostgreSqlAdapter(src),
                    new PostgreSqlAdapter(tgt)
            );

            try (var rs = tgt.createStatement().executeQuery("SELECT COUNT(*) FROM tgt_person")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(3);
            }
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
