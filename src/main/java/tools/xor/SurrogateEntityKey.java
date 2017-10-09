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
 * Uniquely identifies a persistent entity based on a surrogate key.
 */
public final class SurrogateEntityKey implements EntityKey, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Object key;
	private final String entityTypeName; // For an external entity any name that along with the identifier uniquely identifies the entity
	private final int hashCode;

	public SurrogateEntityKey(Object surrogateKeyValue, String entityTypeName) {
		if ( surrogateKeyValue == null ) {
			throw new IllegalStateException( "null identifier" );
		}
		if ( entityTypeName == null || entityTypeName.trim().equals("")) {
			throw new IllegalStateException( "Entity type name needs to be provided" );
		}
		
		this.key = surrogateKeyValue;
		this.entityTypeName = entityTypeName;
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 17;
		result = 37 * result + key.hashCode();
		result = 37 * result + entityTypeName.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object other) {
		if(other == null)
			return false;

		if (!(other instanceof SurrogateEntityKey))
			return false;

		SurrogateEntityKey otherKey = (SurrogateEntityKey) other;
		return otherKey.entityTypeName.equals(this.entityTypeName) &&
				otherKey.key.equals(this.key);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "SurrogateEntityKey[" + entityTypeName + "," + key.toString() + "]";
	}
}
