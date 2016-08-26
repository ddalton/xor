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

/**
 * Represents an Excel row or a JSON row used in REST call
 * 
 * @author Dilip Dalton
 *
 */
public class JsonCell implements ICell {
	private String value;

	@Override
	public String getStringCellValue() {
		return value;
	}

	@Override
	public void setCellValue(String value) {
		this.value = value;
	}
	
}
