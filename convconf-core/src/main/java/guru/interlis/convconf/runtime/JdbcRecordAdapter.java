package guru.interlis.convconf.runtime;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/** Generic JDBC implementation reusable for H2/PostgreSQL and other SQL databases. */
public class JdbcRecordAdapter implements RecordSourceReader, RecordTargetWriter {
    private final Connection connection;

    public JdbcRecordAdapter(Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    @Override
    public List<Map<String, Object>> read(String sourceName, Map<String, String> equalsFilter) throws SQLException {
        String sql = "SELECT * FROM " + sourceName;
        List<String> whereColumns = new ArrayList<>(equalsFilter.keySet());
        if (!whereColumns.isEmpty()) {
            sql += " WHERE " + whereColumns.stream().map(c -> c + "=?").collect(Collectors.joining(" AND "));
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < whereColumns.size(); i++) {
                ps.setString(i + 1, equalsFilter.get(whereColumns.get(i)));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        row.put(md.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    @Override
    public void write(String targetName, List<Map<String, Object>> rows) throws SQLException {
        if (rows.isEmpty()) {
            return;
        }
        List<String> cols = new ArrayList<>(rows.getFirst().keySet());
        String sql = "INSERT INTO " + targetName + "(" + String.join(",", cols) + ") VALUES(" + String.join(",", Collections.nCopies(cols.size(), "?")) + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map<String, Object> row : rows) {
                for (int i = 0; i < cols.size(); i++) {
                    ps.setObject(i + 1, row.get(cols.get(i)));
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
