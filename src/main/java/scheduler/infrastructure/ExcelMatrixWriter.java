package scheduler.infrastructure;

import lombok.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExcelMatrixWriter {
    private final Path path;
    private final Map<String, Collection<Variant>> cases = new HashMap<>();

    public ExcelMatrixWriter(Path path) {
        this.path = path;
    }

    @AllArgsConstructor
    @ToString(of = "name")
    @EqualsAndHashCode(of = "name")
    private static class Variant {
        String name;
        List<List<String>> data;
    }

    public void addSection(String sheet, String section, List<List<String>> result) {
        cases
                .computeIfAbsent(sheet, name -> new ArrayList<>())
                .add(new Variant(section, result));
    }

    private static class Sheet {
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        int rowNum = 0;
        int colNum = 0;
        int maxColNum = 0;
        Row row;
        Cell cell;

        CellStyle defaultStyle;

        Sheet(XSSFWorkbook workbook, String name) {
            this.workbook = workbook;
            sheet = workbook.createSheet(name);

            val font = workbook.createFont();
            font.setFontHeight(8.0);
            defaultStyle = workbook.createCellStyle();
            defaultStyle.setFont(font);
        }

        void nextRow() {
            colNum = 0;
            row = sheet.createRow(rowNum++);
        }

        void nextCell() {
            cell = row.createCell(colNum++);
            val font = workbook.createFont();
            font.setFontHeight(8.0);
            cell.setCellStyle(defaultStyle);
        }

        void set(String value) {
            cell.setCellValue(value);
            if (colNum > maxColNum)
                maxColNum = colNum;
        }

        void autoSizeColumns() {
            for (int i = 0; i < maxColNum ; ++i)
                sheet.autoSizeColumn(i);
        }
    }

    private static XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    @SneakyThrows
    public void write() {
        val workbook = createWorkbook();
        for (val entry : cases.entrySet()) {
            val sheet = new Sheet(workbook, entry.getKey());
            for (val variant : entry.getValue()) {
                sheet.nextRow();
                sheet.nextCell();
                sheet.set(variant.name);
                for (val row : variant.data) {
                    sheet.nextRow();
                    for (val cell : row) {
                        sheet.nextCell();
                        sheet.set(cell);
                    }
                }
                sheet.nextRow();
            }
            sheet.autoSizeColumns();
        }
        try (val output = Files.newOutputStream(path)) {
            workbook.write(output);
        }
    }
}
