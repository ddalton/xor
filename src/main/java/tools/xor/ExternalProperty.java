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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.Shape;

public class ExternalProperty extends AbstractProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	protected boolean isContainment; 
	protected boolean isMany;
	protected boolean isNullable;
	protected String  mappedByName;
	protected PersistentAttributeType associationType;
	protected boolean isMap;
	protected boolean isList;
	protected boolean isSet;
	protected String  domainName;
	
	public ExternalProperty(ExtendedProperty domainProperty, Type type, ExternalType parentType, Type elementType) {
		super(type, parentType);
		setElementType(elementType);		
		processProperty(domainProperty);
		
		init();
	}

	public ExternalProperty(String name, ExtendedProperty domainProperty, Type type, EntityType parentType, Type elementType) {
		super(name, type, parentType);
		setElementType(elementType);
	    processProperty(domainProperty);
	}
	
    public ExternalProperty(String name, Type type, EntityType parentType, Type elementType) {
        super(name, type, parentType);
        setElementType(elementType);

        this.domainName = name;
        this.isMany = elementType != null;
        this.associationType = isMany ? PersistentAttributeType.ONE_TO_MANY : (type.isDataType() ? null : PersistentAttributeType.MANY_TO_ONE);
        this.isList = elementType != null; // Support list by default
    }	
	
	private void processProperty(ExtendedProperty domainProperty) {
	    // We cannot use name, as that would signal an open property
	    this.domainName = domainProperty.getName();
	    this.isContainment = domainProperty.isContainment();
	    this.isMany = domainProperty.isMany();
	    this.readOnly = domainProperty.isReadOnly();
	    this.mappedByName = domainProperty.getMappedByName();
	    this.associationType = domainProperty.getAssociationType();
	    this.alwaysInitialized = domainProperty.isAlwaysInitialized();
	    this.isMap = domainProperty.isMap();
	    this.isList = domainProperty.isList();
	    this.isSet = domainProperty.isSet();
	    this.isNullable = domainProperty.isNullable();
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

		setType(wrapType(getType(), this.elementType));
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
		return this.domainName;
	}

	@Override
	public boolean isMany() {
		return this.isMany;
	}

	@Override
	public boolean isContainment() {
		return this.isContainment;
	}

	@Override
	public Object getDefault() {
		return null;
	}

	@Override
	public boolean isNullable() {
		return this.isNullable;
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}

    public void init(ExtendedProperty domainProperty, Shape dynamicShape) {
        if (domainProperty.getKeyType() != null) {
            String keyTypeName = domainProperty.getKeyType().getName();
            if (domainProperty.getKeyType() instanceof EntityType) {
                keyTypeName = ((EntityType) domainProperty.getKeyType()).getEntityName();
            }
            keyType = dynamicShape.getType(keyTypeName);
        }
        if (domainProperty.getElementType() != null) {
            String entityTypeName = domainProperty.getElementType().getName();
            if (domainProperty.getElementType() instanceof EntityType) {
                entityTypeName = ((EntityType) domainProperty.getElementType()).getEntityName();
            }
            elementType = dynamicShape.getType(entityTypeName);
        }

        if ((field != null && AbstractType.isWrapperType(field.getType()))
                || (getterMethod != null && AbstractType.isWrapperType(getterMethod.getReturnType())))
            logger.info("Primitive type found: " + getContainingType().getInstanceClass().getName() + "::"
                    + field.getName() + ", use a wrapper class instead.");

        /*
         * Not sure if this is needed for an external Type
         * if(domainProperty.getPositionProperty() != null) { positionProperty =
         * (ExtendedProperty)getElementType().getProperty(domainProperty.
         * getPositionProperty().getName()); }
         */
    }

	@Override
	public void initMappedBy(Shape shape) {
		Type type = isMany() ? getElementType() : getType();
		setMappedBy( mappedByName != null ? type.getProperty(mappedByName) : null, mappedByName);

		if(getMappedBy() != null)
			logger.debug("Opposite of property '" + getContainingType().getName() + "." + getName() + "' is '" + mappedByName + "'");
	}

	@Override
	public PersistentAttributeType getAssociationType() {
		return this.associationType;
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
		return this.isMap;
	}

	@Override
	public boolean isList() {
		return this.isList;
	}

	@Override
	public boolean isSet() {
		return this.isSet;
	}

	@Override public boolean isCollectionOfReferences ()
	{
		return false;
	}
}
