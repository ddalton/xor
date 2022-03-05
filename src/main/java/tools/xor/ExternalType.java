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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.Shape;

public class ExternalType extends AbstractType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private Class<?>           javaClass;
	private String             entityName;
	protected boolean          isDataType;
	protected List<ExternalType> parentTypes;	
	protected String           idPropertyName;
	protected String           versionPropertyName;
	protected boolean          isEmbedded;
	protected boolean          isEntity;
	protected boolean          isImmutable;

    public ExternalType(EntityType domainType, Class<?> javaClass) {
        this(domainType.getEntityName(), javaClass);

        this.isDataType = domainType.isDataType();
        this.idPropertyName = domainType.getIdentifierProperty() != null ? domainType.getIdentifierProperty().getName() : null;
        this.versionPropertyName = domainType.getVersionProperty() != null ? domainType.getVersionProperty().getName() : null;
        this.isEmbedded = domainType.isEmbedded();
        this.isEntity = domainType.isEntity();

        if (domainType.getNaturalKey() != null) {
            this.naturalKey = new ArrayList<String>(domainType.getNaturalKey());
            this.expandedNaturalKey = new ArrayList<String>(domainType.getExpandedNaturalKey());
        }

        initMeta();
    }
    
    public ExternalType(String entityName, Class<?> javaClass) {
        this.entityName = entityName;
        this.javaClass = javaClass;
        this.isDataType = false;
        this.parentTypes = new ArrayList<>();
    }

	public void setProperty (Shape domainShape, Shape dynamicShape, TypeMapper typeMapper)
	{
		setProperty(domainShape, dynamicShape, typeMapper, true);
	}

	public void setProperty (Shape domainShape, Shape dynamicShape, TypeMapper typeMapper, boolean add) {
		// populate the properties for this type
        EntityType domainType = (EntityType) domainShape.getType(getEntityName());
		for (Property domainProperty : domainShape.getProperties(domainType).values()) {
			ExternalProperty externalProperty = (ExternalProperty)defineProperty(
				domainProperty,
				dynamicShape,
				typeMapper);

			externalProperty.init((ExtendedProperty) domainProperty, dynamicShape);
			if(add) {
				if (externalProperty.getGetterMethod() == null && !(domainType instanceof JDBCType) ) {
					logger.warn(
						"Out-of-sync between external and domain types. The following property is not present in the external type: "
							+ domainProperty.getName());
					continue;
				}

				logger.debug(
					"[" + getName() + "] Domain property name: " + domainProperty.getName()
						+ ", type name: " + externalProperty.getJavaType());
				dynamicShape.addProperty(externalProperty);
			}
		}
	}

	/**
	 * Create and add a new property to this type.
	 *
	 * @param domainProperty domain Property object
	 * @param dynamicShape shape of the type
     * @param typeMapper instance
	 * @return Property object
	 */
	public Property defineProperty(Property domainProperty, Shape dynamicShape, TypeMapper typeMapper) {
        
		Class<?> externalClass = typeMapper.toExternal(domainProperty.getType());
		if(externalClass == null)
			throw new RuntimeException("The external type is missing for the following domain class: " + domainProperty.getType().getInstanceClass().getName());

		Type propertyType = dynamicShape.getType(externalClass);
		Type elementType = 	null;
		if(((ExtendedProperty)domainProperty).getElementType() != null) {
			elementType = dynamicShape.getType(((ExtendedProperty)domainProperty).getElementType().getInstanceClass());
		}
		ExternalProperty externalProperty = null;
		if(domainProperty.isOpenContent()) {
			externalProperty = new ExternalProperty(domainProperty.getName(), (ExtendedProperty) domainProperty, propertyType, this, elementType);
		} else {
			externalProperty = new ExternalProperty((ExtendedProperty) domainProperty, propertyType, this, elementType);
		}

		return externalProperty;
	}

	public void setOpposite(Shape shape) {
		for(Property property: getProperties())
			((AbstractProperty)property).initMappedBy(shape);
	}

	@Override
	public String getName() {
		return javaClass.getName();
	}

	@Override
	public String getEntityName() {
		return this.entityName;
	}	

	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Class<?> getInstanceClass() {
		return this.javaClass;
	}

	@Override
	public boolean isInstance(Object object) {
		return getInstanceClass().isAssignableFrom(object.getClass());
	}

	@Override
	public boolean isDataType() {
		return this.isDataType;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isSequenced() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public List<ExternalType> getParentTypes() {
		return this.parentTypes;
	}

	public void initParentTypes(EntityType domainType, TypeMapper typeMapper) {
        if (domainType.getParentTypes() != null) {
            for (Type parentType : domainType.getParentTypes()) {
                String parentEntityName = ((EntityType)parentType).getEntityName();
                Type externalType = getShape().getType(parentEntityName);

                this.parentTypes.add((ExternalType) externalType);
            }
        }
	}

	@Override
	public List<?> getAliasNames() {
		return new ArrayList<String>();
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}

	@Override
	public AccessType getAccessType() {
		// Prefer property access for external types
		return AccessType.PROPERTY;
	}

	@Override
	public boolean isDomainType() {
		return false;
	}

	@Override
	public Property getIdentifierProperty() {
	    if(this.idPropertyName != null) {
	        Property result = getProperty(idPropertyName);

			if(result == null) {
				logger.warn("Identifier property " + idPropertyName + " is null for Exterrnal type: " + getName());
			}
			return result;
		} else {
			return null;
		}
	}

	@Override
	public boolean isEmbedded() {
		return this.isEmbedded;
	}

	@Override
	public boolean isEntity() {
		return this.isEntity;
	}	
	
	@Override
	public boolean isImmutable() {
		return this.isImmutable;
	}

	@Override
	public Property getVersionProperty() {
		return getProperty(this.versionPropertyName);
	}

	@Override
	public boolean supportsDynamicUpdate() {
	    // This is not a persistence managed entity
		return false;
	}
	
    @Override
    public void setParentType(EntityType value) {
        super.setParentType(value);
        
        this.parentTypes.add((ExternalType) value);
    }	
    
    @Override
    public EntityType getParentType() {

        if(this.parentTypes.size() == 1) {
            return this.parentTypes.get(0);
        }

        return super.getParentType();
    }

    public void setOfType(Type type) { }
}
