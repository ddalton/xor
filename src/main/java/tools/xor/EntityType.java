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

import tools.xor.service.DataAccessService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public interface EntityType extends BasicType, Comparable<EntityType> {
	
	/**
	 * Should the access be based on property (getter method) or 
	 * by field.
	 * 
	 * @return
	 */
	public AccessType getAccessType();
	
	/**
	 * This is used to represent the Java class with meta data (e.g., JPA annotations) and business logic.
	 * 
	 * @return true if domain type, false if external type
	 */
	public boolean isDomainType();
	
	/**
	 * For external types, this will return the domain type from which it is based off.
	 * For domain types, this will return self.
	 * @return
	 */
	public EntityType getDomainType();
	
	/**
	 * Returns the property that refers to the id of the entity
	 * @return
	 */
	public Property getIdentifierProperty();
	
	/**
	 * Returns the property that is marked as the user key
	 * @return
	 */
	public Property getUserKey();	
	
	/**
	 * Returns the property that is marked as the collection user key
	 * @return
	 */
	public Property getCollectionUserKey();	
	
	/**
	 * Returns the property that refers to the version of the entity
	 * @return
	 */
	public Property getVersionProperty();	
	
	/**
	 * Used to get access to the Domain type from external types
	 * @return
	 */
	public TypeMapper getTypeMapper();	

	/**
	 * Get a list of all the types that are embedded in this type
	 * @return
	 */
	public List<Type> getEmbeddableTypes();
	
	/**
	 * Return true if the contents of this object cannot be changed
	 */
	public boolean isImmutable();
	
	/**
	 * Return true if the type is marked as an aggregate using the Aggregate annotation
	 * This is used to control the amount of data using the ContentScope control
	 * @return
	 */
	public boolean isAggregate();	
	
	/**
	 * Returns true if this type is an embedded type
	 */
	public boolean isEmbedded();
	
	/**
	 * Returns the highest ancestor entity in an inheritance hierarchy. Useful in EntityKey
	 */
	public EntityType getRootEntityType();
	
	/**
	 * Executes all the PostLogic annotated methods associated with objects of this type and its ancestor types
	 */
	public void invokePostLogic(Settings settings, Object object);

	/**
	 * Returns the name that the persistence provider uses to map an entity
	 * @see Type#getName()
	 * @return
	 */
	public String getEntityName();

	/** 
	 * Returns true if this type represents an entity
	 * @return
	 */
	public boolean isEntity();

	/**
	 * Find all the types of the subclasses of the instance class of this type 
	 */
	public void defineSubtypes(List<EntityType> entityTypes);
	
	/**
	 * Get a list of all the sub types of this type
	 * @return
	 */
	public Set<EntityType> getSubtypes();
	
	/**
	 * Get a list of all properties for a given apiVersion
	 * 
	 * @param apiVersion
	 * @return
	 */
	public List <Property> getProperties(int apiVersion);

	/**
	 * This method is not synchronized as we do not remove elements from a Map
	 */
	public void addProperty(Property property);

	/**
	 * Retrieve a property by its alias name
	 * @param name
	 * @return
	 */
	public Property getPropertyByAlias(String name);

	
	/** 
	 * Initialize the position property for list and map types
	 */
	public void initPositionProperty();

	/**
	 * Find the annotation object from the instance class
	 * @param annotationClass
	 * @return
	 */
	public Annotation getClassAnnotation(Class<?> annotationClass);

	/**
	 * Return the Method object for the specific property
	 * @param targetProperty
	 * @return
	 */
	public Method getGetterMethod(String targetProperty);

	/**
	 * Return the Field object for the specific property
	 * @param targetProperty
	 * @return
	 */
	public Field getField(String targetProperty);

	/**
	 * Return the setter method object for the property
	 * @param targetProperty
	 * @return
	 */
	public Method getSetterMethod(String targetProperty);

	/**
	 * Get the business logic method that gets invoked when an object
	 * referenced by the property is read
	 * @param targetProperty
	 * @return
	 */
	public Set<MethodInfo> getDataReaders(String targetProperty);

	/**
	 * Get the business logic method that gets invoked when an object
	 * referenced by the property is updated
	 * @param targetProperty
	 * @return
	 */
	public Set<MethodInfo> getDataUpdaters(String targetProperty);
	
	/**
	 * Get the business logic method if available based on viewname
	 * @param viewName
	 * @return
	 */
	public Set<MethodInfo> getDataInvokers(String viewName);

	/**
	 * Returns all the properties that need to be initialized when
	 * the entity is created
	 * @return
	 */
	public Set<String> getInitializedProperties();
	
	/**
	 * Returns the topological sort order for the type
	 * 
	 * @return
	 */
	public int getOrder();
	
	/**
	 * Sets the topological sort order for the type
	 */
	public void setOrder(int value);
	
	/**
	 * If this entity type is configured to support dynamic update
	 * @return
	 */
	public boolean supportsDynamicUpdate();

	/**
	 * Give an ordering of the properties
	 * @param properties
	 * @return
	 */
	public List<Property> sort(List<Property> properties);

	/**
	 * Get the super type for this type
	 * @return
	 */
	public EntityType getSuperType();
	
	/**
	 * Set the super type for this entity type
	 * @param value
	 */
	public void setSuperType(EntityType value);

}
