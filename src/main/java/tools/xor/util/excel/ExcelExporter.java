/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.util.excel;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import tools.xor.Settings;
import tools.xor.util.ClassUtil;


public class ExcelExporter {
	private final OutputStream  outputStream;
	private final Settings      settings;
	private final SXSSFWorkbook wb = new SXSSFWorkbook(); 
	private SXSSFSheet          sheet;
	private int                 currentRow;
	private Font                defaultFont;
	private Font                headerFont;
	
	public ExcelExporter(OutputStream os, Settings settings) {
		this.outputStream = os;
		this.settings = settings;
		
		wb.setCompressTempFiles(true);
		sheet = (SXSSFSheet) wb.createSheet("XOR");
		
		// keep 100 rows in memory, exceeding rows will be flushed to disk
		sheet.setRandomAccessWindowSize(100);
	}
	
	/**
	 * 
	 * @param obj should be an object[]
	 */
	public void writeRow(Object obj) {
		
		Row row = sheet.createRow(currentRow++);
		
		int cellnum = 0;
		for(Object cellContent: (Object[]) obj) {
			Cell cell = row.createCell(cellnum++);
			if(cellContent != null) {
				cell.setCellValue(cellContent.toString());
			}
		}
	}

	public void writeValidations() {
		// TODO: Implement once validation support is added
	}

	public void finish() {
	    try {
			wb.write(outputStream);
		} catch (IOException e) {
			ClassUtil.wrapRun(e);
		}
	}

	
}