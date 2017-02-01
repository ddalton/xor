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

package tools.xor.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.AbstractType;
import tools.xor.AbstractTypeNarrower;
import tools.xor.Settings;
import tools.xor.Type;

/**
 * In the context of a state graph, a state A that is dependent on state B
 * comes earlier in the ordering
 *
 */
public class State implements Vertex {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private Type                    type;
	private boolean                 startState;
	private boolean                 finishState;
	private boolean                 inScope;
	private Set<String>             attributes;

	public State(Type type, boolean startState) {
		this.type = type;
		this.setStartState(startState);
	}
	
	public void setAttributes(Set<String> value) {
		this.attributes = value;
	}
	
	public Set<String> getAttributes() {
		return this.attributes == null ? new HashSet<String>() : Collections.unmodifiableSet(this.attributes);
	}
	
	public State copy() {
		State result = new State(this.type, this.isStartState());
		
		if(this.attributes != null) {
			result.attributes = new HashSet<String>(this.attributes);
		}
		
		return result;
	}

	public void initDataTypes() {
		if(attributes == null) {
			attributes = new HashSet<String>();
		}
		attributes.addAll(AbstractTypeNarrower.getDataTypes(this.type));
	}

	@Override
	public String getName() {
		return this.type.getName();
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + type.hashCode();			
		return result;
	}

	@Override
	public boolean equals(Object other) {
		State otherState = (State) other;
		return otherState.type == this.type;
	}

	public boolean isFinishState() {
		return finishState;
	}

	public void setFinishState(boolean finishState) {
		this.finishState = finishState;
	}

	public boolean isStartState() {
		return startState;
	}

	public void setStartState(boolean startState) {
		this.startState = startState;
	}		

	public boolean isInScope() {
		return inScope;
	}

	public void setInScope(boolean inScope) {
		this.inScope = inScope;
	}

	public static String getNextAttr(String attrPath) {
		int pathDelim = attrPath.indexOf(Settings.PATH_DELIMITER);
		if(pathDelim == -1) {
			return attrPath;
		}

		return attrPath.substring(0, pathDelim);
	}
	
	public static String getRemaining(String attrPath) {
		if(attrPath.indexOf(Settings.PATH_DELIMITER) == -1) {
			return null;
		}

		return attrPath.substring(attrPath.indexOf(Settings.PATH_DELIMITER)+1);
	}

	public void addAttribute(String attr) {
		if(attributes == null) {
			attributes = new HashSet<String>();
		}

		attributes.add(attr);
	}
	
	@Override
	public String toString() {

		return getName();

		// Use simple name for now so it is easier to view in large object graphs
		// Might need to go bar to FQDN for accuracy reasons
		//return AbstractType.getBaseName(getType());
	}
}

