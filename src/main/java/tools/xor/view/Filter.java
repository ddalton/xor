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

package tools.xor.view;

import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;

import tools.xor.Settings;
import tools.xor.view.expression.AbstractFunctionExpression;
import tools.xor.view.expression.ExpressionFactory;

public class Filter implements Comparable<Filter> {

	protected String expression;
	
	@XmlAttribute
	protected int position;
	
	protected AbstractFunctionExpression functionExpression;
	
	public Filter() {
		
	}
	
	public Filter(String expression) {
		init(expression);
	}
	
	public void init(String expression) {
		this.expression = expression;
		if(expression != null) {
			this.functionExpression = ExpressionFactory.getExpression(expression);
			this.functionExpression.init();
		} else {
			this.functionExpression = null;
		}
	}

	public Filter narrow() {
		return new Filter(expression);
	}

	public boolean isOrderBy() {
		return functionExpression.isOrderBy();
	}

	public String getQueryString() {
		return functionExpression.getQueryString();
	}

	public String getNormalizedName() {
		return functionExpression.getNormalizedAttributeName();
	}

	public String getExpression() {
		return expression;
	}
	
	public void setExpression(String expression) {
		init(expression);
	}

	public String getAttribute() {
		return functionExpression.getAttributeName();
	}

	public void normalize(Map<String, String> normalizedNames) {
		functionExpression.normalize(normalizedNames);
	}

	public Object getNormalizedValue(Object object) {
		return functionExpression.getNormalizedValue(object);
	}	

	/**
	 * Checks to see if a filter is relevant for this query based on the input parameters
	 * 
	 * @param originalFilters user supplied filters
	 * @param filters normalized filters
	 * @param parameterMap parameter map used by filters
	 * @return true if filter is included
	 */
	public boolean isFilterIncluded(Map<String, Object> originalFilters, Map<String, Object> filters, Map<String, Parameter> parameterMap) {
		boolean result = false;
		
		for(String parameterName: functionExpression.getParameters()) {

			Parameter param =  parameterMap.get(parameterName);
			if(!originalFilters.containsKey(Settings.encodeParam(parameterName)) ) { // parameter is not set
				// check if the parameter has a default value
				if(param != null)
					filters.put( Settings.encodeParam(parameterName), getNormalizedValue(param.defaultValue));
				else
					return result;
			} else {
				String key = Settings.encodeParam(parameterName);
				if(param != null)
					key = param.filterName;
				Object normalizedValue = getNormalizedValue(originalFilters.get(Settings.encodeParam(key)));
				filters.put( Settings.encodeParam(parameterName), normalizedValue);
			}
		}

		return true;
	}

	@Override
	public int compareTo(Filter o) {
		return position-o.position;
	}
}
