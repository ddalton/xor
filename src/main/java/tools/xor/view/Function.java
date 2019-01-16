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

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import tools.xor.FunctionType;
import tools.xor.Settings;
import tools.xor.util.IntraQuery;
import tools.xor.view.expression.FunctionHandler;
import tools.xor.view.expression.FunctionHandlerFactory;

@XmlAccessorType(XmlAccessType.FIELD)
public class Function implements Comparable<Function> {
	
	@XmlAttribute
	protected int position;

	@XmlAttribute
	protected String name; // Used in comparison and custom functions

	@XmlAttribute(required = true)
	protected FunctionType type;

	protected List<String> args;

	@XmlTransient
	protected FunctionHandler functionHandler;
	
	/**
	 * No-args constructor required for Unmarshalling purpose. Don't use this directly.
	 */
	public Function () {
	}

	public Function (Function f) {
		this(f.name, f.type, f.position, f.args);
	}
	
	public Function (String name, FunctionType type, int position, List<String> args) {
		this.name = name;
		this.type = type;
		this.args = args;
		this.position = position;

		init();
	}	
	
	private void init() {
		this.functionHandler = FunctionHandlerFactory.getFunctionHandler(type, name);
		if(this.functionHandler != null) {
			this.functionHandler.init(this.args);
		}
	}

	public String getName ()
	{
		return name;
	}

	public void setName (String name)
	{
		this.name = name;
	}

	public Function copy() {
		return new Function(this);
	}

	public boolean isOrderBy() {
		return type == FunctionType.ASC || type == FunctionType.DESC;
	}

	public String getQueryString() {
		return functionHandler != null ? functionHandler.getQueryString() : "";
	}

	public String getNormalizedName() {
		return functionHandler.getNormalizedAttributeName();
	}

	public String getAttribute() {
		return functionHandler.getAttributeName();
	}

	/**
	 * Identify the mapping between the entity field path and the query alias name
	 *
	 * @param qp QueryPiece for which the alias name is mapped
	 * @return true if all the names can be normalized, false otherwise
	 */
	public boolean normalize(QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {
		boolean all = true;

		for(String path: functionHandler.getAttributes()) {
			String oqlname = qp.getOQLName(path);
			if(oqlname != null) {
				functionHandler.setNormalizedName(path, oqlname);
			} else {
				all = false;
			}
		}

		return all;
	}


	public Object getNormalizedValue(Object object) {
		return functionHandler.getNormalizedValue(object);
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
		
		for(String parameterName: functionHandler.getParameters()) {

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
	public int compareTo(Function o) {
		return position-o.position;
	}
}
