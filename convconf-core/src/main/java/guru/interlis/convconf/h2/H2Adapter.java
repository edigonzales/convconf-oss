package guru.interlis.convconf.h2;

import guru.interlis.convconf.runtime.JdbcRecordAdapter;

import java.sql.Connection;

/** H2-specific JDBC adapter (inherits generic JDBC behavior). */
public final class H2Adapter extends JdbcRecordAdapter {
    public H2Adapter(Connection connection) {
        super(connection);
    }
}
