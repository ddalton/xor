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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.service.DataAccessService;
import tools.xor.service.Shape;

public class ExternalType extends AbstractType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	protected EntityType                 domainType;
	private Class<?>                     javaClass;
	private List<Type>                   baseTypes;	

	public ExternalType(EntityType domainType, Class<?> javaClass) {
		this.domainType = domainType;
		this.javaClass = javaClass;
		
		initMeta();
	}
	
	public void setProperty(DataAccessService dataAccessService, Shape shape) {
		if(properties == null) {
			// populate the properties for this type
			properties = new HashMap<String, Property>();	
			for(Property domainProperty: domainType.getProperties()) {
				ExternalProperty externalProperty = (ExternalProperty) defineProperty(dataAccessService, domainProperty, shape);
				if(externalProperty.getGetterMethod() == null ) {
					logger.warn("Out-of-sync between external and domain types. The following property is not present in the external type: " + domainProperty.getName());
					continue;
				}
				
				externalProperty.init(dataAccessService, shape);
				logger.debug("[" + getName() + "] Domain property name: " + domainProperty.getName() + ", type name: " + externalProperty.getJavaType());
				properties.put(externalProperty.getName(), externalProperty);
			}			
		} 		
	}

	/**
	 * Create and add a new property to this type.
	 *
	 * @param dataAccessService dataAccessService instance
	 * @param domainProperty domain Property object
	 * @param shape of the type
	 * @return Property object
	 */
	public Property defineProperty(DataAccessService dataAccessService, Property domainProperty, Shape shape) {
		Class<?> externalClass = dataAccessService.getTypeMapper().toExternal(domainProperty.getType().getInstanceClass());
		if(externalClass == null)
			throw new RuntimeException("The external type is missing for the following domain class: " + domainProperty.getType().getInstanceClass().getName());

		Type propertyType = shape.getExternalType(externalClass);
		Type elementType = 	null;
		if(((ExtendedProperty)domainProperty).getElementType() != null) {
			elementType = shape.getExternalType(((ExtendedProperty)domainProperty).getElementType().getInstanceClass());
		}
		ExternalProperty externalProperty = null;
		if(domainProperty.isOpenContent()) {
			externalProperty = new ExternalProperty(domainProperty.getName(), (ExtendedProperty) domainProperty, propertyType, this, elementType);
		} else {
			externalProperty = new ExternalProperty((ExtendedProperty) domainProperty, propertyType, this, elementType);
		}

		return externalProperty;
	}

	public void setOpposite(DataAccessService das) {
		for(Property property: properties.values())
			((AbstractProperty)property).initMappedBy(das);
	}

	@Override
	public String getName() {
		return javaClass.getName();
	}

	@Override
	public String getEntityName() {
		return domainType.getName();
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
		return domainType.isDataType();
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
	public List<Type> getBaseTypes() {
		return baseTypes;
	}

	public void setBaseType(DataAccessService dataAccessService) {
		baseTypes = new ArrayList<Type>();

		for(Type baseType: domainType.getBaseTypes()) {
			Class<?> externalClass = getTypeMapper().toExternal(baseType.getInstanceClass());
			Type externalType = dataAccessService.getType(externalClass);

			baseTypes.add(externalType);
		}
	}

	@Override
	public void initRootEntityType(DataAccessService das, Shape shape) {
		if(domainType.getRootEntityType() != null) {
			Class<?> externalClass = das.getTypeMapper().toExternal(domainType.getRootEntityType().getInstanceClass());
			String externalTypeName = das.getTypeMapper().getExternalTypeName(externalClass, domainType);
			rootEntityType = (EntityType) shape.getExternalType(externalTypeName);
		}
	}	

	@Override
	public List<Property> getDeclaredProperties() {
		List<Property> result = new ArrayList<Property>();
		for(Property property: domainType.getDeclaredProperties()) {
			logger.debug("[" + getName() + "] declared domain property name: " + property.getName());
			result.add(getProperty(property.getName()));
		}

		return result;
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
	public EntityType getDomainType() {
		return domainType;
	}

	@Override
	public Property getIdentifierProperty() {
		if(domainType.getIdentifierProperty() != null) {
			Property result = getProperty(domainType.getIdentifierProperty().getName());
			if(result == null) {
				logger.warn("Identifier property " + domainType.getIdentifierProperty().getName() + " is null for Exterrnal type: " + getName() + " from type: " + domainType.getName());
			}
			return result;
		} else {
			return null;
		}
	}

	@Override
	public boolean isEmbedded() {
		return domainType.isEmbedded();
	}

	@Override
	public boolean isEntity() {
		return domainType.isEntity();
	}	
	
	@Override
	public boolean isImmutable() {
		return domainType.isImmutable();
	}	
	
	@Override
	public boolean isAggregate() {
		return domainType.isAggregate();
	}		

	@Override
	public Property getVersionProperty() {
		return getProperty(domainType.getVersionProperty().getName());
	}

	@Override
	public List<String> getNaturalKey() {
		if(domainType.getNaturalKey() != null)
			return domainType.getNaturalKey();
		else
			return null;
	}

	@Override
	public List<String> getExpandedNaturalKey() {
		if(domainType.getExpandedNaturalKey() != null)
			return domainType.getExpandedNaturalKey();
		else
			return null;
	}

	@Override
	public boolean supportsDynamicUpdate() {
		return domainType.supportsDynamicUpdate();
	}

}
