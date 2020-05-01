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

package tools.xor.view.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class FunctionHandler
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public static final String ILIKE = "ILIKE";
	public static final String IN = "IN";
	public static final String EQUAL = "EQUAL";
	public static final String GE = "GE";
	public static final String GT = "GT";
	public static final String LE = "LE";
	public static final String LT = "LT";
	public static final String NE = "NE";
	public static final String BETWEEN = "BETWEEN";
	public static final String NULL = "NULL";
	public static final String NOTNULL = "NOTNULL";

	protected List<String> parameterName = new ArrayList<String>();

	// Key is the full attribute name and the value is the normalized name
	// If the value is null, then the normalized name has not been initialized
	protected Map<String, String> normalizedNames = new HashMap<>();


	public abstract void init(List<String> args);

	public String getQueryString() {
		return "";
	}

	public String getNormalizedAttributeName() {
		return normalizedNames.values().iterator().next();
	}

	public String getAttributeName() {
		return normalizedNames.keySet().iterator().next();
	}

	public List<String> getParameters() {
		return parameterName;
	}

	protected String getParameterName() {
		if(parameterName.size() != 1)
			throw new RuntimeException("Wrong number of parameters for this function expression");

		return parameterName.get(0);
	}

	public Set<String> getAttributes() {
		return Collections.unmodifiableSet(normalizedNames.keySet());
	}

	public void setNormalizedName(String path, String name) {
		normalizedNames.put(path, name);
	}

	/**
	 * Return null if the value is not going to be transformed
	 * @param object input
	 * @return transformed object if there is a transformation else return null
	 */
	public Object getTransformation (Object object) {
		return null;
	}

	public void updateParamName(String oldName, String newName) {
		List<String> paramNames = new ArrayList<>(this.parameterName.size());

		for(String paramName: this.parameterName) {
			if(oldName.equals(paramName)) {
				paramNames.add(newName);
			} else {
				paramNames.add(paramName);
			}
		}

		this.parameterName = paramNames;
	}
}
