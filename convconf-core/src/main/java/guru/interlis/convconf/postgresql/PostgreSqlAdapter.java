package guru.interlis.convconf.postgresql;

import guru.interlis.convconf.runtime.JdbcRecordAdapter;

import java.sql.Connection;

/** PostgreSQL-specific JDBC adapter (inherits generic JDBC behavior). */
public final class PostgreSqlAdapter extends JdbcRecordAdapter {
    public PostgreSqlAdapter(Connection connection) {
        super(connection);
    }
}
