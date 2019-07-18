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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tools.xor.exception.MultipleClassForPropertyException;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;

public abstract class AbstractTypeNarrower implements TypeNarrower {

	private static Map<Class<?>, Map<String, Object>> subTypeProperties = new ConcurrentHashMap<Class<?>, Map<String, Object>>();

	private AggregateManager aggregateManager;

	public AggregateManager getAggregateManager() {
		return aggregateManager;
	}

	public void setAggregateManager(AggregateManager aggregateManager) {
		this.aggregateManager = aggregateManager;
	}
	
	public static Set<String> getDataTypes(Type type) {
		Set<String> result = new HashSet<>();

		for(Property property: type.getProperties()) {
			if( ((ExtendedProperty)property).isDataType()) {
				result.add(property.getName());
			}
		}
		
		return result;
	}

	/**
	 * This method is used to perform dynamic type narrowing. Subclasses override this method
	 * to provide custom behavior.
	 */
	@Override
	public Class<?> narrow(Object entity, String viewName) {
		Class<?> entityClass = ClassUtil.getUnEnhanced(entity.getClass());
				
		getAggregateManager().getDAS().refresh(this);
		getAggregateManager().getDAS().populateNarrowedClass(entityClass, this);		

		return getAggregateManager().getDAS().getNarrowedClass(entityClass, viewName);
	}

	/**
	 * This method provides static type narrowing. Subclasses generally don't override
	 * this method.
	 */
	public Class<?> narrow(Class<?> entityClass, String propertyName) {
		Type entityType = null;

		TypeMapper typeMapper = aggregateManager.getTypeMapper();
		DataAccessService das = aggregateManager.getDAS();

		Class<?> referenceClass = entityClass;
		if(typeMapper.isExternal(entityClass))
			referenceClass = typeMapper.toDomain(entityClass);

		entityType = das.getType(referenceClass);
		if(SimpleType.class.isAssignableFrom(entityType.getClass()))
			return entityType.getInstanceClass();

		return narrow(entityType, propertyName);
	}

	private Class<?> narrow(Type entityType, String propertyName) {
		if(entityType.getProperty(propertyName) != null) {
			return entityType.getInstanceClass();
		} else {
			return findSubclass(entityType, propertyName);
		}
	}

	private void populate(Type entityType) {
		if(!EntityType.class.isAssignableFrom(entityType.getClass()))
			return;

		EntityType type = (EntityType) entityType;
		TypeMapper typeMapper = aggregateManager.getTypeMapper();		

		// Find the property in the sub-class
		Map<String, Object> allEntityProperties = new HashMap<String, Object>();
		for(EntityType subType: type.getSubtypes()) {
			for(Property property: subType.getProperties()) {
				if(allEntityProperties.get(property.getName()) == null)
					allEntityProperties.put(property.getName(), subType.getInstanceClass());
				else { // multiple matches
					Object object = allEntityProperties.get(property.getName());
					if(Set.class.isAssignableFrom(object.getClass()))
						((Set) object).add(subType.getInstanceClass());
					else { // create a collection object and add to it the existing and the new class
						Set entityWithProperty = new HashSet();
						entityWithProperty.add(object);
						entityWithProperty.add(subType.getInstanceClass());
						allEntityProperties.put(property.getName(), entityWithProperty);
					}
				}
			}	
		}
		subTypeProperties.put(entityType.getInstanceClass(), allEntityProperties);
	}

	private Class<?> findSubclass(Type entityType, String propertyName) {
		if(subTypeProperties.get(entityType.getInstanceClass()) == null)
			populate(entityType);

		Map<String, Object> allEntityProperties = subTypeProperties.get(entityType.getInstanceClass());
		if(allEntityProperties.get(propertyName) == null)
			return null; // No sub-class has this property

		// Find the subclass
		Object object = allEntityProperties.get(propertyName);
		if(Class.class.isAssignableFrom(object.getClass()))
			return (Class<?>) object;

		// There is more than one match for the property throw an exception
		throw new MultipleClassForPropertyException(entityType, propertyName, object);
	}
}
