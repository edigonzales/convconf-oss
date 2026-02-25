package guru.interlis.convconf.file;

import guru.interlis.convconf.runtime.FileSourceReader;
import guru.interlis.convconf.runtime.FileTargetWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * File source/writer using one XLSX workbook with one sheet per source/target name.
 */
public final class XlsxWorkbookAdapter implements FileSourceReader, FileTargetWriter {
    private final Path workbook;

    public XlsxWorkbookAdapter(Path workbook) {
        this.workbook = workbook;
    }

    @Override
    public List<Map<String, Object>> read(String sourceName, Map<String, String> equalsFilter) throws Exception {
        if (!Files.exists(workbook)) {
            return List.of();
        }
        try (InputStream is = Files.newInputStream(workbook); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheet(sourceName);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) return List.of();
            List<String> headers = headers(sheet.getRow(0));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row rowObj = sheet.getRow(r);
                if (rowObj == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = rowObj.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    row.put(headers.get(c), cellToString(cell));
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
        Workbook wb;
        if (Files.exists(workbook)) {
            try (InputStream is = Files.newInputStream(workbook)) {
                wb = WorkbookFactory.create(is);
            }
        } else {
            wb = new XSSFWorkbook();
        }
        try (wb) {
            Sheet existing = wb.getSheet(targetName);
            if (existing != null) {
                wb.removeSheetAt(wb.getSheetIndex(existing));
            }
            Sheet sheet = wb.createSheet(targetName);
            List<String> headers = new ArrayList<>(rows.getFirst().keySet());
            Row head = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                head.createCell(c).setCellValue(headers.get(c));
            }
            int rowNum = 1;
            for (Map<String, Object> row : rows) {
                Row xRow = sheet.createRow(rowNum++);
                for (int c = 0; c < headers.size(); c++) {
                    xRow.createCell(c).setCellValue(Objects.toString(row.get(headers.get(c)), ""));
                }
            }
            Files.createDirectories(workbook.getParent() == null ? Path.of(".") : workbook.getParent());
            try (OutputStream os = Files.newOutputStream(workbook)) {
                wb.write(os);
            }
        }
    }

    private List<String> headers(Row row) {
        List<String> h = new ArrayList<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            h.add(cellToString(row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)));
        }
        return h;
    }

    private String cellToString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> Double.toString(cell.getNumericCellValue());
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK, _NONE, ERROR -> "";
        };
    }
}
