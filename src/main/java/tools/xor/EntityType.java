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
import tools.xor.service.Shape;

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
	 * Gets the list of properties that are directly on this type, i.e.,
	 * it does not include properties not defined on its supertypes
	 * @return list of properties
	 */
	List /*Property*/<Property> getDeclaredProperties();
	
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
	 * This is an optimization step to bring in the properties
	 * from the ancestors into the current type.
	 * @param shape of the type
	 */
	public void unfoldProperties(Shape shape);

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
	 * Add property to this type and its external type
	 * 
	 * @param property that is added
	 */
	public void addProperty(Property property);

	/**
	 * Remove property from this type and its corresponding external type
	 *
	 * @param property that is removed
	 */
	public void removeProperty(Property property);

	/**
	 * Retrieve a property by its alias name
	 * @param name alternative name
	 * @return Property object
	 */
	public Property getPropertyByAlias(String name);

	
	/** 
	 * Initialize the position property for list and map types
	 * @param shape of this type
	 */
	public void initPositionProperty(Shape shape);

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
	 * The higher the number, more entities are dependant upon it.
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
	 * set the shape to which this type belongs. It is an error to reassign a type from a
	 * different shape.
	 * @param shape instance
	 */
	public void setShape(Shape shape);

	/**
	 * Return the shape to which this type belongs
	 * @return shape instance
	 */
	public Shape getShape();

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

	/**
	 * When building OQL queries, some embedded types are not considered to be directly
	 * explorable.
	 * @return true if it can appear in the FROM clause of a query
	 */
	public boolean isExplorable ();

	/**
	 * Checks if this is the root concrete type. That is there is no parent type that
	 * has persisted instances in the database.
	 *
	 * @return true if this is the root concrete type.
	 */
	public boolean isRootConcreteType();

	/**
	 * Allows setting an open property value dynamically.
	 *
	 * @param obj open instance
	 * @param propertyName of the property whose value need to be set
	 * @param value to be set
	 */
	public void setOpenProperty(Object obj, String propertyName, Object value );

	/**
	 * Check to see if the current type is an ancestor type of the given type
	 * @param entityType is a descendant type
	 * @return true if ancestor false otherwise
	 */
	public boolean isSameOrAncestorOf (EntityType entityType);

	/**
	 * Returns the list of the descendant sub types starting from the child
	 * and ending at the parent of the descendant.
	 * i.e., the starting and end are not included in the descendant chain.
	 *
	 * @param entityType descendant type representing the child of the chain end
	 * @return descendant chain
	 */
	public List<EntityType> getDescendantsTo(EntityType entityType);

	/**
	 * Check in which subtypes is the property present.
	 * Ideally this should return only a single item, but there can be cases
	 * where the same property can be defined by unrelated sub types
	 *
	 * @param property to search for
	 * @return subtypes containing that property definition
	 */
	public List<EntityType> findInSubtypes (String property);

	/**
	 * Return the entity specific settings related to data generation
	 * @return
	 */
	public List<EntityGenerator> getGenerators ();

	/**
	 * add the generator settings for the entity
	 * @param generator generator settings object
	 */
	public void addGenerator(EntityGenerator generator);
}
