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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Uniquely identifies a persistent entity based on a surrogate key.
 */
public final class NaturalEntityKey implements EntityKey, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final Map<String, Object> key;
	private final String entityTypeName; // For an external entity any name that along with the identifier uniquely identifies the entity
	private final int hashCode;

	public NaturalEntityKey(Map<String, Object> naturalKey, String entityTypeName) {
		if ( naturalKey == null || naturalKey.size() == 0 ) {
			throw new IllegalStateException( "null identifier" );
		}
		if ( entityTypeName == null || entityTypeName.trim().equals("")) {
			throw new IllegalStateException( "Entity type name needs to be provided" );
		}
		
		Map<String, Object> aMap = new HashMap<String, Object>(naturalKey);
		
		this.key = Collections.unmodifiableMap(aMap);
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

		if (!(other instanceof NaturalEntityKey))
			return false;
		
		if(key.size() == 0) {
			return false;
		}

		// don't consider equality of an empty key
		boolean hasValue = false;
		for(Object keypart: key.values()) {
			if(keypart == null || "".equals(keypart.toString())) {
				continue;
			}
			hasValue = true;
			break;
		}
		if(!hasValue) {
			return false;
		}

		NaturalEntityKey otherKey = (NaturalEntityKey) other;
		return otherKey.entityTypeName.equals(this.entityTypeName) &&
				otherKey.key.equals(this.key);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

}
