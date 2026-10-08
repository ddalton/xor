package tools.xor.service.exim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import tools.xor.StringType;
import tools.xor.util.Constants;

/**
 * Values larger than the Excel cell limit are stored in the overflow sheet.
 */
public class ExcelOverflowTest {

    // Random so the workbook does not trip the zip bomb detection on read
    private static String random(int count) {
        return StringType.randomAlphanumeric(count);
    }

    private static Workbook roundTrip(Workbook wb) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        wb.write(os);
        wb.close();
        return WorkbookFactory.create(new ByteArrayInputStream(os.toByteArray()));
    }

    private void checkRoundTrip(Workbook wb) throws IOException {
        String small = "small value";
        String exact = random(ExcelExportImport.MAX_CELL_LENGTH);
        String large = random(ExcelExportImport.MAX_CELL_LENGTH * 2 + 17);
        String larger = random(ExcelExportImport.MAX_CELL_LENGTH * 3);

        Sheet sheet = wb.createSheet(Constants.XOR.EXCEL_ENTITY_SHEET);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("small");
        header.createCell(1).setCellValue("exact");
        header.createCell(2).setCellValue("large");
        header.createCell(3).setCellValue("larger");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(small);
        row.createCell(1).setCellValue(exact);
        row.createCell(2).setCellValue(ExcelExportImport.writeOverflow(wb, large));
        row.createCell(3).setCellValue(ExcelExportImport.writeOverflow(wb, larger));

        Workbook read = roundTrip(wb);
        Map<String, Integer> colMap = ExcelExportImport.getHeaderMap(read.getSheet(Constants.XOR.EXCEL_ENTITY_SHEET));
        JSONObject json = AbstractExportImport.getJSON(colMap, read.getSheet(Constants.XOR.EXCEL_ENTITY_SHEET).getRow(1));

        assertEquals(small, json.getString("small"));
        assertEquals(exact, json.getString("exact"));
        assertEquals(large, json.getString("large"));
        assertEquals(larger, json.getString("larger"));

        Sheet overflow = read.getSheet(Constants.XOR.EXCEL_OVERFLOW_SHEET);
        assertNotNull(overflow);
        assertEquals(3, overflow.getRow(0).getLastCellNum());
        assertEquals(3, overflow.getRow(1).getLastCellNum());
    }

    @Test
    public void overflowRoundTrip() throws IOException {
        checkRoundTrip(new XSSFWorkbook());
    }

    @Test
    public void overflowRoundTripStreaming() throws IOException {
        checkRoundTrip(new SXSSFWorkbook());
    }

    @Test
    public void prefixWithoutOverflowSheet() throws IOException {
        // A genuine value that happens to look like a reference is returned unchanged
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(Constants.XOR.EXCEL_ENTITY_SHEET);
        sheet.createRow(0).createCell(0).setCellValue("value");
        String value = Constants.XOR.EXCEL_OVERFLOW_REF + "0";
        sheet.createRow(1).createCell(0).setCellValue(value);

        Map<String, Integer> colMap = new HashMap<>();
        colMap.put("value", 0);
        JSONObject json = AbstractExportImport.getJSON(colMap, roundTrip(wb).getSheet(Constants.XOR.EXCEL_ENTITY_SHEET).getRow(1));
        assertEquals(value, json.getString("value"));
    }

    @Test
    public void referenceIsSmall() {
        Workbook wb = new XSSFWorkbook();
        String ref = ExcelExportImport.writeOverflow(wb, random(ExcelExportImport.MAX_CELL_LENGTH + 1));
        assertTrue(ref.length() < 100);
        assertTrue(ref.startsWith(Constants.XOR.EXCEL_OVERFLOW_REF));
    }
}
