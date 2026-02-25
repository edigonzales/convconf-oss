package guru.interlis.convconf.runtime;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for reading raw records from a source backend.
 * <p>
 * Implementations are free to map {@code sourceName} to a table name, CSV file,
 * worksheet name, API endpoint, etc.
 * </p>
 */
public interface RecordSourceReader {
    /**
     * Reads rows from a backend object with an equality filter.
     *
     * @param sourceName backend-specific source object name
     * @param equalsFilter key/value equality filter (column -> required value)
     * @return list of rows as column/value maps
     * @throws Exception on backend access failures
     */
    List<Map<String, Object>> read(String sourceName, Map<String, String> equalsFilter) throws Exception;
}
