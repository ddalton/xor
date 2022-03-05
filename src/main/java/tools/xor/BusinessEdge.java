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

package tools.xor;

import tools.xor.util.Edge;

public final class BusinessEdge<V extends BusinessObject> {
	final V source;         
	final V target;   
	final Property property;

	public BusinessEdge(V source, V target, Property property) {
		this.source = source;
		this.target = target;
		this.property = property;
	}

	public Property getProperty() {
		return property;
	}
	
	public V getStart() {
		return source;
	}

	public V getEnd() {
		return target;
	}

	public boolean isRequired() {
		return AbstractProperty.isRequired( property );
	}

	public boolean isCascaded() {
		return AbstractProperty.isCascadable( property );
	}
	
	/*
	public boolean fromTransient() {
		boolean result = !getStart().isPersistent();
		
		// Check the owner of the collection
		if(property == null) {
			BusinessNode collection = getStart();
			if(collection.getIncoming().size() == 1) {
				BusinessEdge edgeFromOwner = collection.getIncoming().iterator().next();
				result = !edgeFromOwner.source.isPersistent();
			}
		}
		
		return result;
	}
	*/
	public boolean toTransient() {
		return !getEnd().isPersistent();
	}

	@Override
	public String toString() {
		return (property == null) ? Edge.COL_FANOUT : property.getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		result = prime * result + ((source == null) ? 0 : System.identityHashCode(source));
		result = prime * result + ((target == null) ? 0 : System.identityHashCode(target));
		result = prime * result + ((property == null) ? 0 : System.identityHashCode(property));			
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		BusinessEdge other = (BusinessEdge) obj;

		return source == other.source  &&
				target == other.target &&
				property == other.property;
	}

}
