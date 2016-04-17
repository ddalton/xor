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
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.service.DataAccessService;


/**
 * SimpleType have no properties
 * 
 * @author Dilip Dalton
 * 
 */
public class MutableJsonType extends ExternalType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public MutableJsonType(EntityType domainType, Class<?> javaClass) {
		super(domainType, javaClass);
	}
	

	@Override
	public Method getGetterMethod(String targetProperty){
		// Dynamic type does not have getters
		return null;
	}	
	
	@Override
	public String getName() {
		return getDomainType().getName();
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public void setProperty(DataAccessService dataAccessService) {
		if(properties == null) {
			// populate the properties for this type
			properties = new HashMap<String, Property>();	
			for(Property domainProperty: domainType.getProperties()) {
				AbstractProperty abstractProperty = (AbstractProperty) domainProperty;
				Class<?> externalClass = dataAccessService.getTypeMapper().toExternal(domainProperty.getType().getInstanceClass());
				if(externalClass == null)
					throw new RuntimeException("The dynamic type is missing for the following domain class: " + domainProperty.getType().getInstanceClass().getName());

				Type propertyType = dataAccessService.getExternalType(domainProperty.getType().getName());
				if(propertyType == null) {
					Class<?> propertyClass = dataAccessService.getTypeMapper().toExternal(domainProperty.getType().getInstanceClass());
					logger.debug("Name: " + domainProperty.getName() + ", Domain class: " + domainProperty.getType().getInstanceClass().getName() + ", property class: " + propertyClass.getName());
					propertyType = dataAccessService.getType(propertyClass);
				}
				MutableJsonProperty dynamicProperty = new MutableJsonProperty((ExtendedProperty) domainProperty, propertyType, this);
				
				dynamicProperty.init(dataAccessService);
				logger.debug("[" + getName() + "] Domain property name: " + abstractProperty.getName() + ", type name: " + dynamicProperty.getJavaType());
				properties.put(dynamicProperty.getName(), dynamicProperty);
			}			
		} 		
	}	
}
