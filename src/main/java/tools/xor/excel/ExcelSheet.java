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

package tools.xor.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Represents an Excel sheet or a JSON sheet used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public class ExcelSheet implements ISheet {
	private Sheet sheet;
	
	public ExcelSheet(Sheet sheet) {
		this.sheet = sheet;
	}
	
	/**
	 * Gets the last row num in the sheet
	 * Similar to total number of rows
	 * 
	 * @return row number
	 */
	public int getLastRowNum() {
		return sheet.getLastRowNum();
	}
	
	/**
	 * Creates and returns an object that represents a row
	 * 
	 * @param rowNum row number
	 * @return Row object
	 */
	public IRow createRow(int rowNum) {
		Row row = sheet.createRow(rowNum);
		return new ExcelRow(row);
	}
	
	/**
	 * Returns the row at position rowNum
	 * 
	 * @param rowNum row number
	 * @return Row object
	 */
	public IRow getRow(int rowNum) {
		Row row = sheet.getRow(rowNum);
		return new ExcelRow(row);
	}
	
	/**
	 * Utility method to adjust the rendering width of the column
	 * Not relevant for a JSON representation. Is only applicable
	 * for the Excel representation.
	 * @param column number
	 */
	public void autoSizeColumn(int column) {
		sheet.autoSizeColumn(column);
	}
}
