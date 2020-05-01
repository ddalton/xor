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

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.Shape;

/**
 * SimpleType have no properties
 * 
 * @author Dilip Dalton
 * 
 */
public class ImmutableJsonType extends ExternalType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public ImmutableJsonType(EntityType domainType, Class<?> javaClass) {
		super(domainType, javaClass);
	}
	

	@Override
	public Method getGetterMethod(String targetProperty){
		// Dynamic type does not have getters
		return null;
	}	
	
    @Override
    public String getName() {
        return getEntityName();
    }	
	
	@Override
	public boolean isOpen() {
		return true;
	}
	

    @Override
    public void setProperty (Shape domainShape, Shape dynamicShape, TypeMapper typeMapper)
    {
        // populate the properties for this type
        EntityType domainType = (EntityType) domainShape.getType(getEntityName());
        for (Property domainProperty : domainShape.getProperties(domainType).values()) {
            ImmutableJsonProperty dynamicProperty = (ImmutableJsonProperty)defineProperty(
                domainProperty,
                dynamicShape,
                typeMapper);

            dynamicProperty.init((ExtendedProperty) domainProperty, dynamicShape);
            logger.debug(
                "[" + getName() + "] Domain property name: " + domainProperty.getName()
                    + ", type name: " + dynamicProperty.getJavaType());
            dynamicShape.addProperty(dynamicProperty);
        }
    }	
	
    @Override
    public Property defineProperty(Property domainProperty, Shape dynamicShape, TypeMapper typeMapper) {
        
        Class<?> externalClass = typeMapper.toExternal(domainProperty.getType());
        if(externalClass == null) {
            throw new RuntimeException("The dynamic type is missing for the following domain class: " + domainProperty.getType().getInstanceClass().getName());
        }

        String typeName = domainProperty.getType().getName();
        if(domainProperty.getType() instanceof EntityType) {
            typeName = ((EntityType)domainProperty.getType()).getEntityName();
        }
        Type propertyType = dynamicShape.getType(typeName);
        Type elementType = null;
        if(((ExtendedProperty)domainProperty).getElementType() != null) {
            String elementTypeName = ((ExtendedProperty)domainProperty).getElementType().getName();
            if(((ExtendedProperty)domainProperty).getElementType() instanceof EntityType) {
                elementTypeName = ((EntityType)((ExtendedProperty)domainProperty).getElementType()).getEntityName();
            }                    
            elementType = dynamicShape.getType(elementTypeName);
        }       
        if(propertyType == null) {
            Class<?> propertyClass = typeMapper.toExternal(domainProperty.getType());
            logger.debug("Name: " + domainProperty.getName() + ", Domain class: " + domainProperty.getType().getInstanceClass().getName() + ", property class: " + propertyClass.getName());
            propertyType = dynamicShape.getType(propertyClass);
        }
        ImmutableJsonProperty dynamicProperty = new ImmutableJsonProperty(
                (ExtendedProperty)domainProperty,
                propertyType,
                this,
                elementType);
        dynamicProperty.setDomainTypeName(domainProperty.getType().getInstanceClass().getName());
        
        return dynamicProperty;
    }	
}
