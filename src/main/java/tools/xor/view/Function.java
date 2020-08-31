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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import tools.xor.FunctionScope;
import tools.xor.FunctionType;
import tools.xor.Settings;
import tools.xor.StringType;
import tools.xor.service.DataStore;
import tools.xor.util.IntraQuery;
import tools.xor.view.expression.FreestyleHandler;
import tools.xor.view.expression.FunctionHandler;
import tools.xor.view.expression.FunctionHandlerFactory;

@XmlAccessorType(XmlAccessType.FIELD)
public class Function implements Comparable<Function> {
	
	@XmlAttribute
	protected int position;

	@XmlAttribute
	protected String name; // Used in comparison and custom functions
	
    @XmlAttribute
    protected String include; // refers to a user provided parameter. If present, this function is included	

	@XmlAttribute(required = true)
	protected FunctionType type;

	@XmlAttribute
	protected FunctionScope scope = FunctionScope.ANY;

	protected List<String> args;

	@XmlTransient
	protected FunctionHandler functionHandler;
	
	/**
	 * No-args constructor required for Unmarshalling purpose. Don't use this directly.
	 */
	public Function () {
	}

	public Function (Function f) {
		this(f.name, f.type, f.scope, f.position, f.args, f.include);
	}
	
	public Function (String name, FunctionType type, FunctionScope scope, int position, List<String> args, String include) {
		this.name = name;
		this.type = type;
		this.scope = scope;
		this.args = args;
		this.position = position;
		this.include = include;

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

	public boolean isRelevant() {
		return functionHandler.getNormalizedAttributeName() != null;
	}

	public String getNormalizedName() {
		return functionHandler.getNormalizedAttributeName();
	}

	public String getAttribute() {
		return functionHandler.getAttributeName();
	}

	public Set<String> getAttributes() {
		return functionHandler.getAttributes();
	}

	public FunctionScope getScope() {
		return this.scope;
	}

	public void setScope(FunctionScope scope) {
		this.scope = scope;
	}

	/**
	 * Returns the number of positional parameters in a freestyle function
	 * @return count
	 */
	public int getPositionalParamCount() {
		if(this.type == FunctionType.FREESTYLE) {
			return ((FreestyleHandler)functionHandler).getPositionalParamCount();
		}

		// Not supported for other functions
		return -1;
	}

	/**
	 * Identify the mapping between the entity field path and the query alias name
	 *
	 * @param queryTree QueryTree for which the alias name is mapped
	 * @param po persistenceOrchestrator
	 * @return true if all the names can be normalized, false otherwise
	 */
	public boolean normalize(QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree, DataStore po) {
		boolean all = true;

		for(String path: functionHandler.getAttributes()) {
			String oqlname = queryTree.getOQLName(path, po);;

			// For JDBC a property could resolve to multiple columns. Do we need to support
			// this? or expect the user to provide the fully qualified path in the view
			/*if(Settings.doSQL(po)) {
				QueryField field = queryTree.findField(path);
				if(field != null) {
					oqlname = field.getSQL();
				}
			} else {
				oqlname = queryTree.getOQLName(path, po);
			}
			*/
			if(oqlname != null) {
				functionHandler.setNormalizedName(path, oqlname);
			} else {
				all = false;
			}
		}

		return all;
	}
	
    /**
     * Checks to see if a filter is relevant for this query based on the input parameters
     * 
     * @param userParams user supplied parameter values
     * @return true if filter is included
     */	
	private boolean isFilterIncluded(Set<String> inputParams) {
	    inputParams = new HashSet<>(inputParams);
	    inputParams.add(QueryFragment.PARENT_INVOCATION_ID_PARAM);
	    inputParams.add(QueryFragment.LAST_PARENT_ID_PARAM);
	    
        for(String parameterName: functionHandler.getParameters()) {
            if(!inputParams.contains(Settings.encodeParam(parameterName)) ) { // parameter is not set
                return false;
            }
        }
        
        if(this.include != null && !inputParams.contains(this.include)) {
            return false;
        }
        
        return true;
	}

	/**
	 * Enhances the user supplied parameter with new values based on the result of the transformation
	 * @param userParams user provided parameters, that may be modified 
	 */
	private void enhanceUserParams(Map<String, Object> userParams) {
		
		for(String parameterName: functionHandler.getParameters()) {
			String key = Settings.encodeParam(parameterName);
			Object transformedValue = functionHandler.getTransformation(userParams.get(Settings.encodeParam(key)));
			if(transformedValue != null) {
				// since the value is changed, we need to refer to it using a new parameter
				String uniqueSuffix = StringType.randomAlphanumeric(5).toUpperCase();
				key = key + Settings.URI_PATH_DELIMITER + uniqueSuffix;
				userParams.put(key, transformedValue);

				// We now have to update the parameter name with the modified parameter name
				// so the function can use the modfied value
				functionHandler.updateParamName(parameterName, key);
			}
		}
	}
	
	public static boolean doProcess(Function function, Settings settings) {
        // Filter is skipped
        if(!function.isFilterIncluded(settings.getAllParameters())) {
            return false;
        }
        
        // Enhance user provided parameters if necessary
        function.enhanceUserParams(settings.getParams());
        
        return true;
	}

	public FunctionHandler getHandler() {
		return this.functionHandler;
	}

	@Override
	public int compareTo(Function o) {
		return position-o.position;
	}
}
