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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.service.DataAccessService;
import tools.xor.service.Shape;

public class ExternalProperty extends AbstractProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private ExtendedProperty domainProperty;
	private Boolean containment; // Note: this should be an instance to allow 3VL.
	
	public ExternalProperty(ExtendedProperty domainProperty, Type type, ExternalType parentType, Type elementType) {
		super(type, parentType);
		this.domainProperty = domainProperty;
		setElementType(elementType);			
		
		init();
	}

	public ExternalProperty(String name, ExtendedProperty domainProperty, Type type, EntityType parentType, Type elementType) {
		super(name, type, parentType);
		this.domainProperty = domainProperty;
		setElementType(elementType);
	}

	@Override
	public Property refine (String name, Type type, EntityType parentType) {
		throw new UnsupportedOperationException("Refine of external property is not allowed. Refine a domain property instead and create an external property from it");
	}

	@Override
	protected void initBusinessLogicAnnotations() {
		// Business logic not supported on external types
	}
	
	private void setElementType(Type eType) {
		this.elementType = eType;
		if(this.elementType != null) {
			this.relType = RelationshipType.TO_MANY;
		}			
	}
	
	@Override
	public Property getDomainProperty() {
		return domainProperty;
	}
	
	public Class<?> getJavaType() {
		if (field != null) {
			return field.getDeclaringClass();
		} else {
			if(getterMethod != null) {
				return getterMethod.getDeclaringClass();
			} else {
				throw new RuntimeException("Unable to obtain Java type for external property. Maybe a JSON type mapper needs to be configured.");
			}
		}
	}
	
	@Override
	public String getName() {
		return domainProperty.getName();
	}

	@Override
	public boolean isMany() {
		return domainProperty.isMany();
	}

	@Override
	public boolean isContainment() {
		// First check if the field has been initialized,
		// if not, fallback to the domain value
		if(containment != null) {
			return containment;
		}
		return domainProperty.isContainment();
	}

	/**
	 * It is possible to mark an external property as a containment property for
	 * data generation reasons.
	 * This allows us to enhance the model without touching
	 * the domain model. By setting it as containment, these fields will be given
	 * priority for data generation, and hence make them available for linking with
	 * non-containment relationships.
	 *
	 * @param containment value to set
	 */
	public void setContainment(boolean containment) {
		this.containment = containment;
	}

	@Override
	public Object getDefault() {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return getDomainProperty().isReadOnly();
	}

	@Override
	public boolean isNullable() {
		return domainProperty.isNullable();
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}

	/*
	protected Type getExternalKeyType(DataAccessService das, Shape shape) {
		//return shape.getExternalType(das.getTypeMapper().toExternal(domainProperty.getKeyType().getInstanceClass()));
		return
	}	
	
	protected Type getExternalElementType(DataAccessService das, Shape shape) {
		return shape.getExternalType(das.getTypeMapper().toExternal(domainProperty.getElementType().getInstanceClass()));
	}*/

	protected Type getExternalKeyType(Shape shape) {
		return shape.getExternalType(((ExtendedProperty)getDomainProperty()).getKeyType().getName());
	}

	protected Type getExternalElementType(Shape shape) {
		return shape.getExternalType(((ExtendedProperty)getDomainProperty()).getElementType().getName());
	}

	@Override
	public void init(Shape shape) {
		if(domainProperty.getKeyType() != null)
			keyType = getExternalKeyType(shape);
		if(domainProperty.getElementType() != null) 
			elementType = getExternalElementType(shape);
		
		if( (field != null && AbstractType.isWrapperType(field.getType())) || 
				(getterMethod != null && AbstractType.isWrapperType(getterMethod.getReturnType()))
			)
			logger.info("Primitive type found: " + getContainingType().getInstanceClass().getName() + "::" + field.getName() + ", use a wrapper class instead.");
		
		if(domainProperty.getPositionProperty() != null)
			positionProperty = (ExtendedProperty) getElementType().getProperty(domainProperty.getPositionProperty().getName());		
	}
	
	@Override
	public boolean isAlwaysInitialized() {
		return domainProperty.isAlwaysInitialized();
	}

	@Override
	public void initMappedBy(Shape shape) {
		String mappedBy = domainProperty.getMappedByName();

		Type type = isMany() ? getElementType() : getType();
		setMappedBy( mappedBy != null ? type.getProperty(mappedBy) : null, mappedBy);

		if(getMappedBy() != null)
			logger.debug("Opposite of property '" + domainProperty.getContainingType().getName() + "." + getName() + "' is '" + mappedBy + "'");
	}

	@Override
	public PersistentAttributeType getAssociationType() {
		return domainProperty.getAssociationType();
	}

	@Override
	public boolean isGenerated () {
		// check if the annotation is on the getter method
		if(getterMethod != null && getterMethod.getAnnotation(XmlTransient.class) != null)
			return true;	
		
		// check if the annotation is on the field			
		if(field != null && field.getAnnotation(XmlTransient.class) != null)
			return true;
		
		return false;	
	}

	@Override
	public boolean isMap() {
		return domainProperty.isMap();
	}

	@Override
	public boolean isList() {
		return domainProperty.isList();
	}

	@Override
	public boolean isSet() {
		return domainProperty.isSet();
	}

	@Override public boolean isCollectionOfReferences ()
	{
		return false;
	}
}
