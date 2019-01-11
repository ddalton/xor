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
import tools.xor.util.IntraQuery;
import tools.xor.view.expression.AbstractFunctionExpression;
import tools.xor.view.expression.ExpressionFactory;

public class Filter implements Comparable<Filter> {

	protected String expression;
	
	@XmlAttribute
	protected int position;

	@XmlAttribute
	protected String name;

	@XmlAttribute
	protected String alias;
	
	protected AbstractFunctionExpression functionExpression;
	
	/**
	 * No-args constructor required for Unmarshalling purpose. Don't use this directly.
	 */
	public Filter() {
	}

	public Filter(Filter f) {
		this(f.expression, f.position);
		this.name = f.name;
		this.alias = f.alias;
	}
	
	public Filter(String expression, int position) {
		init(expression);
		this.position = position;
	}	
	
	private void init(String expression) {
		this.expression = expression;
		if(expression != null) {
			this.functionExpression = ExpressionFactory.getFunctionExpression(expression);
			if(this.functionExpression != null) {
				this.functionExpression.init();
			}
		} else {
			this.functionExpression = null;
		}
	}

	public Filter copy() {
		return new Filter(this);
	}

	public boolean isOrderBy() {
		return functionExpression.isOrderBy();
	}

	public boolean isAliasFilter() {
		return alias != null && !"".equals(alias.trim());
	}

	public String getQueryString() {
		if(isAliasFilter()) {
			return null;
		}
		return functionExpression != null ? functionExpression.getQueryString() : expression;
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

	/**
	 * Identify the mapping between the entity field path and the query alias name
	 *
	 * @param qp QueryPiece for which the alias name is mapped
	 * @return true if all the names can be normalized, false otherwise
	 */
	public boolean normalize(QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {
		boolean all = true;

		for(String path: functionExpression.getAttributes()) {
			String oqlname = qp.getOQLName(path);
			if(oqlname != null) {
				functionExpression.setNormalizedName(path, oqlname);
			} else {
				all = false;
			}
		}

		return all;
	}


	public Object getNormalizedValue(Object object) {
		return functionExpression.getNormalizedValue(object);
	}	

	/**
	 * Checks to see if a filter is relevant for this query based on the input parameters
	 * 
	 * @param userParams user supplied parameter values
	 * @param normParam normalized users parameter values. This is populated by this code.
	 * @param parameterMap parameter map used by filters
	 * @return true if filter is included
	 */
	public boolean isFilterIncluded(Map<String, Object> userParams, Map<String, Object> normParam, Map<String, Parameter> parameterMap) {
		boolean result = false;
		
		for(String parameterName: functionExpression.getParameters()) {

			Parameter param =  parameterMap.get(parameterName);
			if(!userParams.containsKey(Settings.encodeParam(parameterName)) ) { // parameter is not set
				// check if the parameter has a default value
				if(param != null)
					normParam.put( Settings.encodeParam(parameterName), getNormalizedValue(param.defaultValue));
				else
					return result;
			} else {
				String key = Settings.encodeParam(parameterName);
				if(param != null)
					key = param.filterName;
				Object normalizedValue = getNormalizedValue(userParams.get(Settings.encodeParam(key)));
				normParam.put( Settings.encodeParam(parameterName), normalizedValue);
			}
		}

		return true;
	}

	@Override
	public int compareTo(Filter o) {
		return position-o.position;
	}
}
