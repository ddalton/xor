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

import tools.xor.generator.Generator;
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
	 * @return method or field access
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
	 * @return Domain Type for this type
	 */
	public EntityType getDomainType();
	
	/**
	 * Returns the property that refers to the id of the entity
	 * @return Property object
	 */
	public Property getIdentifierProperty();
	
	/**
	 * Returns the key that the entity is uniquely referred by the user
	 * @return the fields in the natural key
	 */
	public List<String> getNaturalKey();

	/**
	 * Returns the key that the entity is uniquely referred by the user
	 * In terms of simple fields.
	 * This can have a performance impact.
	 * @return the fields in the natural key
	 */
	public List<String> getExpandedNaturalKey();
	
	/**
	 * Set the natural key for this type
	 * @param keys fields of the natural key
	 */
	public void setNaturalKey(String[] keys);	
	
	/**
	 * List of all candidate keys supported by the entity.
	 * Includes the natural key if one is designated as such.
	 * 
	 * @return the candidate keys.
	 */
	public List<Set<String>> getCandidateKeys();	
	
	/**
	 * Returns the property that refers to the version of the entity
	 * @return Property object
	 */
	public Property getVersionProperty();	
	
	/**
	 * Used to get access to the Domain type from external types
	 * @return TypeMapper
	 */
	public TypeMapper getTypeMapper();	

	/**
	 * Get a list of all the types that are embedded in this type
	 * @return list of embedded types
	 */
	public List<Type> getEmbeddableTypes();
	
	/**
	 * Return true if the contents of this object cannot be changed
	 * @return true if immutable
	 */
	public boolean isImmutable();
	
	/**
	 * Return true if the type is marked as an aggregate using the Aggregate annotation
	 * This is used to control the amount of data using the ContentScope control
	 * @return true if aggregate
	 */
	public boolean isAggregate();	
	
	/**
	 * Returns true if this type is an embedded type
	 * @return true if embedded type
	 */
	public boolean isEmbedded();
	
	/**
	 * Returns the highest ancestor entity in an inheritance hierarchy. Useful in EntityKey
	 * @return EntityType
	 */
	public EntityType getRootEntityType();
	
	/**
	 * Executes all the PostLogic annotated methods associated with objects of this type and its ancestor types
	 * 
	 * @param settings passed by user
	 * @param instance on which the postlogic methods are invoked
	 */
	public void invokePostLogic(Settings settings, Object instance);

	/**
	 * Returns the name that the persistence provider uses to map an entity
	 * @see Type#getName()
	 * @return entityName
	 */
	public String getEntityName();

	/** 
	 * Returns true if this type represents an entity
	 * @return boolean value
	 */
	public boolean isEntity();

	/**
	 * Find all the types of the subclasses of the instance class of this type 
	 * 
	 * @param types of all types   
	 */
	public void defineSubtypes(List<Type> types);

	/**
	 * Find all the immediate subType entities of this type
	 */
	public void defineChildSubtypes();
	
	/**
	 * Get a list of all the sub types of this type
	 * @return subtypes
	 */
	public Set<EntityType> getSubtypes();

	/**
	 * Get a list of all the sub types that are the immediate children of this type
	 * @return subtypes
	 */
	public Set<EntityType> getChildSubtypes();
	
	/**
	 * Get a list of all properties for a given apiVersion
	 * 
	 * @param apiVersion apiVersion number
	 * @return list of properties
	 */
	public List <Property> getProperties(int apiVersion);

	/**
	 * Add property to this type
	 * This method is not synchronized as we do not remove elements from a Map
	 * 
	 * @param property that is added
	 */
	public void addProperty(Property property);

	/**
	 * Retrieve a property by its alias name
	 * @param name alternative name
	 * @return Property object
	 */
	public Property getPropertyByAlias(String name);

	
	/** 
	 * Initialize the position property for list and map types
	 */
	public void initPositionProperty();

	/**
	 * Find the annotation object from the instance class
	 * @param annotationClass on this type's java class
	 * @return annotation object if present
	 */
	public Annotation getClassAnnotation(Class<?> annotationClass);

	/**
	 * Return the Method object for the specific property
	 * @param targetProperty property name
	 * @return Method object if present, null otherwise
	 */
	public Method getGetterMethod(String targetProperty);

	/**
	 * Return the Field object for the specific property
	 * @param targetProperty property name
	 * @return Field object if present, null otherwise
	 */
	public Field getField(String targetProperty);

	/**
	 * Return the setter method object for the property
	 * @param targetProperty property name
	 * @return Method object if present, null otherwise
	 */
	public Method getSetterMethod(String targetProperty);

	/**
	 * Get the lambdas associated with a property
	 * @param targetProperty property name
	 * @return list of lambdas
	 */
	public List<MethodInfo> getLambdas(String targetProperty);

	/**
	 * Returns all the properties that need to be initialized when
	 * the entity is created
	 * @return a set of properties
	 */
	public Set<String> getInitializedProperties();
	
	/**
	 * Returns the topological sort order for the type
	 * 
	 * @return order number
	 */
	public int getOrder();
	
	/**
	 * Sets the topological sort order for the type
	 * 
	 * @param value of order
	 */
	public void setOrder(int value);
	
	/**
	 * If this entity type is configured to support dynamic update
	 * @return boolean value
	 */
	public boolean supportsDynamicUpdate();

	/**
	 * Get the super type for this type
	 * @return EntityType
	 */
	public EntityType getSuperType();
	
	/**
	 * Set the super type for this entity type
	 * @param value of EntityType
	 */
	public void setSuperType(EntityType value);

	/**
	 * The DAS object is needed when doing lazy initializing on certain functionality
	 * @param das DataAccessService for this type
	 */
	public void setDAS(DataAccessService das);

	/**
	 * Returns whether or not all a property represented by
	 * property is not specified by SDO.
	 * @return true if this property is nullable.
	 */
	/**
	 * Returns whether or any of the property in the propertyPath chain is nullable
	 * @param propertyPath to a property. This can refer to an indirect property i.e., property path
	 * @return true if any property in the property path is nullable or if the property is not found
	 */
	public boolean isNullable(String propertyPath);

}
