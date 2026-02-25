package guru.interlis.convconf.runtime;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for writing records to a target backend.
 * <p>
 * Implementations are free to map {@code targetName} to a table name, CSV file,
 * worksheet name, API endpoint, etc.
 * </p>
 */
public interface RecordTargetWriter {
    /**
     * Writes rows to a backend object.
     *
     * @param targetName backend-specific target object name
     * @param rows rows to write as column/value maps
     * @throws Exception on backend access failures
     */
    void write(String targetName, List<Map<String, Object>> rows) throws Exception;
}
