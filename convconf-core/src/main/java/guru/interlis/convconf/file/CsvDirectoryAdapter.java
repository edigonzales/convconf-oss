package guru.interlis.convconf.file;

import guru.interlis.convconf.runtime.FileSourceReader;
import guru.interlis.convconf.runtime.FileTargetWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * File source/writer using one CSV file per source/target name in a directory.
 */
public final class CsvDirectoryAdapter implements FileSourceReader, FileTargetWriter {
    private final Path directory;

    public CsvDirectoryAdapter(Path directory) {
        this.directory = directory;
    }

    @Override
    public List<Map<String, Object>> read(String sourceName, Map<String, String> equalsFilter) throws Exception {
        Path csvFile = directory.resolve(sourceName + ".csv");
        if (!Files.exists(csvFile)) {
            return List.of();
        }
        try (CSVParser parser = CSVParser.parse(csvFile, java.nio.charset.StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (var record : parser) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String header : parser.getHeaderMap().keySet()) {
                    row.put(header, record.get(header));
                }
                boolean keep = equalsFilter.entrySet().stream()
                        .allMatch(e -> Objects.equals(Objects.toString(row.get(e.getKey()), null), e.getValue()));
                if (keep) rows.add(row);
            }
            return rows;
        }
    }

    @Override
    public void write(String targetName, List<Map<String, Object>> rows) throws Exception {
        if (rows.isEmpty()) {
            return;
        }
        Files.createDirectories(directory);
        Path csvFile = directory.resolve(targetName + ".csv");
        List<String> headers = new ArrayList<>(rows.getFirst().keySet());
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers.toArray(String[]::new)).build())) {
            for (Map<String, Object> row : rows) {
                List<String> values = headers.stream().map(h -> Objects.toString(row.get(h), "")).toList();
                printer.printRecord(values);
            }
        }
    }
}
