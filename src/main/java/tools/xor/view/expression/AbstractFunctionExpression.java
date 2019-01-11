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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.util.ClassUtil;

public abstract class AbstractFunctionExpression implements Expression {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	protected String       expression;
	protected List<String> parameterName = new ArrayList<String>();

	// Key is the full attribute name and the value is the normalized name
	// If the value is null, then the normalized name has not been initialized
	protected Map<String, String> normalizedNames = new HashMap<>();

	protected abstract String getAttributePattern();
	public abstract String getQueryString();

	public String getNormalizedAttributeName() {
		return normalizedNames.values().iterator().next();
	}

	public boolean isOrderBy() {
		return false;
	}

	public String getAttributeName() {
		return normalizedNames.keySet().iterator().next();
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String value) {
		this.expression = value;
	}

	public AbstractFunctionExpression copy() {
		AbstractFunctionExpression result;
		try {
			result = this.getClass().newInstance();
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
		result.setExpression(this.getExpression());

		return result;
	}

	public List<String> getParameters() {
		return parameterName;
	}

	protected String getParameterName() {
		if(parameterName.size() != 1)
			throw new RuntimeException("Wrong number of parameters for this function expression");

		return parameterName.get(0);
	}

	public void init() {
		// An enhancement would be to return a set of attributes if an expression consists of multiple filter functions			
		Pattern pattern = Pattern.compile( getAttributePattern() ); 
		Matcher matcher = pattern.matcher(getExpression());
		matcher.find();

		if(matcher.matches()) {
			int groupCount = matcher.groupCount();
			if(groupCount > 1)
				normalizedNames.put(matcher.group(2), null);
			if(groupCount > 2) {
				int i = 2;
				while(i++ < groupCount)
					parameterName.add(matcher.group(i));
			}
		} 
	}

	public Set<String> getAttributes() {
		return Collections.unmodifiableSet(normalizedNames.keySet());
	}

	public void setNormalizedName(String path, String name) {
		normalizedNames.put(path, name);
	}

	public Object getNormalizedValue(Object object) {
		return object;
	}	
}
