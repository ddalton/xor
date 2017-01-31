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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.List;

public class CharType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private char min = Character.MIN_VALUE;
	private char max = Character.MAX_VALUE;	

	public char getMin() {
		return min;
	}

	public void setMin(char min) {
		this.min = min;
	}

	public char getMax() {
		return max;
	}

	public void setMax(char max) {
		this.max = max;
	}

	public CharType(Class<?> clazz) {
		super(clazz);
	}	

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom) {

		ExtendedProperty ep = (ExtendedProperty) property;
		if(ep.getGenerator() != null) {
			return ep.getGenerator().getCharValue();
		}

		char range = (char) (getMax() - getMin());
		return (char) (getMin() + (Math.random() * range));
	}		
}
