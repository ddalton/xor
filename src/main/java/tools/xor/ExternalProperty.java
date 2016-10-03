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

public class ExternalProperty extends AbstractProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private ExtendedProperty domainProperty;
	
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
		return (field != null) ? field.getDeclaringClass() : getterMethod.getDeclaringClass();
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
		return domainProperty.isContainment();
	}

	@Override
	public Object getDefault() {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		// Don't make assumptions on external type as this is a separate type
		return false;
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
	
	protected Type getExternalKeyType(DataAccessService das) {
		return das.getExternalType(das.getTypeMapper().toExternal(domainProperty.getKeyType().getInstanceClass()));
	}	
	
	protected Type getExternalElementType(DataAccessService das) {
		return das.getExternalType(das.getTypeMapper().toExternal(domainProperty.getElementType().getInstanceClass()));
	}

	@Override
	public void init(DataAccessService das) {
		if(domainProperty.getKeyType() != null)
			keyType = getExternalKeyType(das);
		if(domainProperty.getElementType() != null) 
			elementType = getExternalElementType(das);
		
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
	public void initMappedBy(DataAccessService das) {
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
	public boolean isTransient() {
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
}
