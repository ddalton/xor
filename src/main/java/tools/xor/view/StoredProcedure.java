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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import tools.xor.AggregateAction;

/*
 * This option is useful if the IN list size is much greater than 1000.
 * If so, a large datatype such as CLOB can be used and the results
 * stored in a temp table.
 * The query can then join with this table.
 */
public class StoredProcedure extends QuerySupport {

	private static final String INLIST_DELIMITER = ","; // default delimiter used to separate the INLIST

	protected String               name;
	protected AggregateAction      action;
	protected List<BindParameter>  parameterList;    // Pass data to the stored procedure
	protected List<OutputLocation> outputLocation;	// Parameterized (non-implicit) SP, which param represents the result
	protected Statement            statement;
	protected String               callString;
	protected String               inListParamName;  // Name of the String parameter containing
	                                                 // a list of root object IDs separated by a delimiter,
	                                                 // used for constructing the IN list
	protected String               inListDelimiter;  // delimiter used to extract the ids from the
	                                                 // inListParam. If not provided the INLIST_DELIMITER is used
	protected boolean              implicit;         // By default a callable statement is created,

	// Set this to true if the code implicitly returns resultsets
	protected boolean              multiple; // flag to denote if it supports multiple resultsets

	@XmlAttribute
	private String maxResults;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getCallString() {
		return callString;
	}

	public void setCallString(String sql) {
		this.callString = sql;
	}	

	public AggregateAction getAction() {
		return action;
	}

	public void setAction(AggregateAction action) {
		this.action = action;
	}

	public List<BindParameter> getParameterList() {
		return parameterList;
	}

	public void setParameterList(List<BindParameter> parameterList) {
		this.parameterList = parameterList;
	}

	public List<OutputLocation> getOutputLocation() {
		return outputLocation;
	}

	public void setOutputLocation(List<OutputLocation> outputLocation) {
		this.outputLocation = outputLocation;
	}
	
	public String getMaxResults() {
		return this.maxResults;
	}

	@XmlTransient
	public Statement getStatement ()
	{
		return statement;
	}

	public void setStatement (Statement statement)
	{
		this.statement = statement;
	}

	private boolean hasReturnValue() {
		boolean result = false;
		for (BindParameter pm : parameterList) {
			if(pm.isReturnType()) {
				result = true;
				break;
			}
		}

		return result;
	}

	public StoredProcedure copy() {
		StoredProcedure result = new StoredProcedure();

		super.copy(result);

		result.setName(name);
		result.setCallString(callString);
		result.setAction(action);
		result.setImplicit(implicit);
		result.setMultiple(multiple);

		if(outputLocation != null) {
			List<OutputLocation> copy = new ArrayList<>();
			for(OutputLocation loc: outputLocation) {
				copy.add(loc.copy());
			}
			result.outputLocation = copy;
		}

		if(parameterList != null) {
			List<BindParameter> paramCopy = new ArrayList<>(parameterList.size());
			for (BindParameter bind : parameterList) {
				paramCopy.add(bind.copy());
			}
			result.parameterList = paramCopy;
		}

		// NOTE: we don't copy Statement as that is specific to the JDBC connection

		return result;
	}

	private String getParamString ()
	{
		StringBuilder result = new StringBuilder();


		int numParams = parameterList == null ? 0 : parameterList.size();
		if (hasReturnValue()) {
			numParams--;
		}

		for (int i = 1; i < numParams; i++) {
			result.append("?,");
		}
		if(numParams > 0) {
			result.append("?");
		}

		return result.toString();
	}

	/**
	 * Creates a JDBC stored procedure string.
	 * e.g.,
	 * 		"{call GET_SP(?, ?)}"
	 * @return SP string
	 */
	public String jdbcCallString() {
		if(getCallString() != null) {
			return getCallString();
		}

		StringBuilder result = new StringBuilder();

		if(hasReturnValue()) {
			result.append("{? = call ");
		} else {
			result.append("{call ");
		}

		result
			.append(name);

		String paramString = getParamString();
		if(paramString.trim().length() > 0) {
			result.append("(")
				.append(getParamString())
				.append(")");
		}
		result.append("}");

		callString = result.toString();
		return callString;
	}

	public boolean isImplicit ()
	{
		return implicit;
	}

	public void setImplicit (boolean implicit)
	{
		this.implicit = implicit;
	}

	public boolean isMultiple ()
	{
		return multiple;
	}

	public void setMultiple (boolean multiple)
	{
		this.multiple = multiple;
	}
}
