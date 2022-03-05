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

import org.json.JSONArray;

/**
 * Represents an Excel sheet or a JSON sheet used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public class JsonSheet implements ISheet {
	private JSONArray sheet;
	
	public JsonSheet(JSONArray sheet) {
		this.sheet = sheet;
	}

	@Override
	public int getLastRowNum() {
		return sheet.length();
	}
	
	@Override
	public IRow createRow(int rowNum) {
		if(sheet.opt(rowNum) != null) {
			throw new IllegalArgumentException("Trying to add a new row at a location that has an existing row: " + rowNum);
		}
		
		JsonRow row = new JsonRow(new JSONArray());
		sheet.put(rowNum, row);
		
		return row;
	}
	
	@Override
	public IRow getRow(int rowNum) {
		JsonRow row = (JsonRow) sheet.opt(rowNum);

		return row;
	}
	
	@Override
	public void autoSizeColumn(int column) {
		/*
		 * Not applicable for JsonSheet
		 */		
	}
}
