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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

/**
 * Represents an Excel row or a JSON row used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public class ExcelRow implements IRow {
	private Row row;
	
	public ExcelRow(Row row) {
		this.row = row;
	}

	/**
	 * short representing the last logical cell in the row <b>PLUS ONE</b>,
     *   or -1 if the row does not contain any cells.
	 * 
	 * @return cell column number
	 */
	public short getLastCellNum() {
		return row.getLastCellNum();
	}

	/**
	 * Create a cell based on the column
	 * @param column number
	 * @return created cell object
	 */
	public ICell createCell(int column) {
		Cell cell = row.createCell(column);
		return new ExcelCell(cell);
	}
	
	/**
	 * Get the cell representing a given column (logical cell) 0-based. If you ask for a cell that is not defined....you get a null.
	 * 
	 * @param cellNum column number
	 * @return Cell object
	 */
	public ICell getCell(int cellNum) {
		Cell cell = row.getCell(cellNum);
		return new ExcelCell(cell);
	}
	
}
