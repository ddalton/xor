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

/**
 * Uniquely identifies a persistent entity based on a surrogate key.
 */
public final class SurrogateEntityKey extends AbstractEntityKey {
	private static final long serialVersionUID = 1L;
	
	private final Object key;
	private final int hashCode;

	public SurrogateEntityKey(Object surrogateKeyValue, String entityTypeName) {
		this(surrogateKeyValue, entityTypeName, null);
	}

	public SurrogateEntityKey(Object surrogateKeyValue, String entityTypeName, String path) {
		super(entityTypeName, path);
		if ( surrogateKeyValue == null) {
			throw new IllegalStateException( "null identifier" );
		}

		this.key = surrogateKeyValue;
		this.hashCode = generateHashCode();
	}

	@Override
	protected int generateHashCode ()
	{
		return (37 * super.generateHashCode()) + (key != null ? key.hashCode() : 0);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SurrogateEntityKey))
			return false;

		SurrogateEntityKey otherKey = (SurrogateEntityKey) other;

		if( this.key == null ^ otherKey.key == null) {
			return false;
		}

		// If both keys are initialized then they should be equal
		if (this.key != null && !this.key.equals(otherKey.key)) {
			return false;
		}

		return super.equals(otherKey);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "SurrogateEntityKey[" + getEntityTypeName() + "," + (key==null ? "null":key.toString()) + "," + (getPath()==null?"null":getPath().toString()) + "]";
	}
}
