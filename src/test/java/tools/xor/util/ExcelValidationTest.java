package tools.xor.util;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.junit.jupiter.api.Test;

public class ExcelValidationTest {

	@Test
	public void testExcel() throws IOException  {
		SXSSFWorkbook wb = new SXSSFWorkbook(); 
		wb.setCompressTempFiles(true);

		SXSSFSheet sh = (SXSSFSheet) wb.createSheet("Data Validation");
		sh.setRandomAccessWindowSize(100);// keep 100 rows in memory, exceeding rows will be flushed to disk
		
		DataValidationHelper dvHelper = sh.getDataValidationHelper();
		CellRangeAddressList addressList = new CellRangeAddressList(
			    4, 9999, 0, 0);
		XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint) dvHelper.createExplicitListConstraint(
			    new String[]{"Yes", "No"});
		sh.addValidationData(dvHelper.createValidation(dvConstraint, addressList));
		for(int rownum = 4; rownum < 10000; rownum++){
			Row row = sh.createRow(rownum);
			for(int cellnum = 0; cellnum < 10; cellnum++){
				Cell cell = row.createCell(cellnum);
				if(cellnum != 0) {
					String address = new CellReference(cell).formatAsString();
					cell.setCellValue(address);
				} else {
					cell.setCellValue("Yes");
				}
			}
		}
		
		// Create the excel file
	    FileOutputStream out = new FileOutputStream("testExcel.xlsx");
	    wb.write(out);
	    out.close();
	    wb.close();
	}
}