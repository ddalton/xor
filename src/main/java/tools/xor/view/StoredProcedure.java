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

import java.sql.CallableStatement;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import tools.xor.AggregateAction;

/**
 * TODO: some common functionality between NativeQuery and StoredProcedure?
 * something like CallInterface (SQL or StoredProcedure)
 * Where/How is this invoked? THis should influence the interface
 */
public class StoredProcedure {

	protected String                 name;
	protected String                 callString;
	protected AggregateAction        action;
	protected List<ParameterMapping> parameterList;
	protected List<OutputLocation>   outputLocation;	// List to represent different locations for multiple results
	protected List<String>           resultList;	
	protected CallableStatement      callableStatement;
	
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

	public List<ParameterMapping> getParameterList() {
		return parameterList;
	}

	public void setParameterList(List<ParameterMapping> parameterList) {
		this.parameterList = parameterList;
	}

	public List<OutputLocation> getOutputLocation() {
		return outputLocation;
	}

	public void setOutputLocation(List<OutputLocation> outputLocation) {
		this.outputLocation = outputLocation;
	}

	public List<String> getResultList() {
		return resultList;
	}

	public void setResultList(List<String> resultList) {
		this.resultList = resultList;
	}
	
	public String getMaxResults() {
		return this.maxResults;
	}
	
	public void setCallableStatement(CallableStatement cs) {
		this.callableStatement = cs;
	}
	
	@XmlTransient
	public CallableStatement getCallableStatement() {
		return this.callableStatement;
	}
	
	public String jdbcCallString() {
		if(getCallString() != null) {
			return getCallString();
		}
		
		// TODO: Compute the call string
		return null;
	}
}