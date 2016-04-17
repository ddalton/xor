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

package tools.xor;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Dilip Dalton
 * 
 */
public class DateType extends SimpleType {

	public DateType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {

		// Date
		if (instance instanceof Date) {
			Date date = (Date) instance;
			return new Date(date.getTime());
		} else if (instance instanceof Timestamp) {
			Timestamp ts = (Timestamp) instance;
			return new Timestamp(ts.getTime());
		}		
		
		return null;
	}
}
