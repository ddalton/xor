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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.util.ClassUtil;
import tools.xor.view.QueryViewProperty;

public abstract class AbstractFunctionExpression implements Expression {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	protected String       expression;
	protected String       attributeName;
	protected List<String> parameterName = new ArrayList<String>();
	protected boolean      orderBy;
	protected String       normalizedAttributeName;

	public String getNormalizedAttributeName() {
		return normalizedAttributeName;
	}

	public void setNormalizedAttributeName(String normalizedAttributeName) {
		this.normalizedAttributeName = normalizedAttributeName;
	}

	public boolean isOrderBy() {
		return false;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	protected abstract String getAttributePattern();
	public abstract String getQueryString();

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
				attributeName = matcher.group(2);
			if(groupCount > 2) {
				int i = 2;
				while(i++ < groupCount)
					parameterName.add(matcher.group(i));
			}
		} 
	}

	public void normalize(Map<String, String> normalizedNames) {	
		normalizedAttributeName = normalizedNames.get( QueryViewProperty.qualifyProperty(attributeName) );
	}

	public Object getNormalizedValue(Object object) {
		return object;
	}	
}
