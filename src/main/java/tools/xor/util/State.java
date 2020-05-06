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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AbstractTypeNarrower;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;

/**
 * In the context of a state graph, a state A that is dependent on state B
 * comes earlier in the ordering
 *
 */
public class State implements Vertex {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private Type        type;
	private boolean     startState;
	private boolean     finishState;
	private boolean     inScope;
	private Set<String> attributes;
	private boolean     reference;

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
		
		copyData(result);
		
		return result;
	}

	protected void copyData(State copy) {
		if(this.attributes != null) {
			copy.attributes = new HashSet<>(this.attributes);
		}

		copy.setFinishState(this.finishState);
		copy.setInScope(this.inScope);
		copy.setReference(this.reference);
	}

	public void initDataTypes() {
		if(attributes == null) {
			attributes = new HashSet<>();
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

	/**
	 * Get the type name on which this State is based.
	 * Overridden by subclasses.
	 *
	 * @return type name
	 */
	public String getTypeName() {
		return getType().getName();
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
		StringBuilder result = new StringBuilder(getName());

		if(type instanceof EntityType) {
			// first check if the graph has been topologically sorted
			int order = ((EntityType)type).getOrder();
			if(order >= Constants.XOR.TOPO_ORDERING_START) {
				result = new StringBuilder("[").append(order).append("]").append(result);
			}
		}

		return result.toString();

		// Use simple name for now so it is easier to view in large object graphs
		// Might need to go to FQDN for accuracy reasons
		//return AbstractType.getBaseName(getType());
	}
	public boolean isReference ()
	{
		return reference;
	}

	public void setReference (boolean reference)
	{
		this.reference = reference;
	}

}

