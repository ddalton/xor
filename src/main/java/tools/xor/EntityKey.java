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

import java.io.Serializable;

/**
 * Uniquely identifies a persistent entity.
 */
public final class EntityKey implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Object identifier;
	private final String referenceTypeName;
	private final String derivedTypeName;
	private final int hashCode;

	public EntityKey(Object id, String referenceTypeName) {
		this(id, referenceTypeName, "");
	}

	public EntityKey(Object id, String referenceTypeName, String derivedTypeName) {
		if ( id == null ) {
			throw new IllegalStateException( "null identifier" );
		}
		this.identifier = id; 
		this.referenceTypeName = referenceTypeName;
		this.derivedTypeName = derivedTypeName;
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		result = 37 * result + referenceTypeName.hashCode();
		result = 37 * result + derivedTypeName.hashCode();
		result = 37 * result + identifier.hashCode();
		return result;
	}

	public Object getIdentifier() {
		return identifier;
	}

	@Override
	public boolean equals(Object other) {
		if(other == null)
			return false;

		if (!(other instanceof EntityKey))
			return false;

		EntityKey otherKey = (EntityKey) other;
		return otherKey.referenceTypeName.equals(this.referenceTypeName) &&
				otherKey.derivedTypeName.equals(this.derivedTypeName) &&
				otherKey.identifier.equals(this.identifier);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

}
