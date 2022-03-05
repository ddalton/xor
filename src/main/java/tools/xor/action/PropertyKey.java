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

package tools.xor.action;

import java.io.Serializable;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.util.ClassUtil;

/**
 * Uniquely identifies a persistent entity.
 */
public final class PropertyKey implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final BusinessObject dataObject;
	private final Property property;
	private final PersistentAttributeType associationType;
	
	public Property getProperty() {
		return property;
	}

	public BusinessObject getDataObject() {
		return dataObject;
	}	

	public PropertyKey(BusinessObject dataObject, Property property) {
		this.dataObject = dataObject; 
		this.property = property;
		
		this.associationType = ((ExtendedProperty)property).getAssociationType();
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + System.identityHashCode(dataObject);
		result = 37 * result + System.identityHashCode(ClassUtil.getDelegate(property));
		return result;
	}

	@Override
	public boolean equals(Object other) {
		PropertyKey otherKey = (PropertyKey) other;
		
		return otherKey.dataObject == this.dataObject && ClassUtil.isSameDelegate(this.property, otherKey.property);
	}	

	public boolean isOneToOne() {
		return associationType == PersistentAttributeType.ONE_TO_ONE;
	}
}
