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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.xor.service.Shape;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoRE;

public interface EntityType extends BasicType {
	static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	/**
	 * Should the access be based on property (getter method) or 
	 * by field.
	 * 
	 * @return method or field access
	 */
	default public AccessType getAccessType() { return AccessType.USERDEFINED; }
	
	/**
	 * This is used to represent the Java class with meta data (e.g., JPA annotations) and business logic.
	 * 
	 * @return true if domain type, false if external type
	 */
	default public boolean isDomainType() { return false; }
	
	/**
	 * For external types, this will return the domain type name from which it is based off. Optional.
	 * @return Domain Type for this type, null otherwise
	 */
	default public String getDomainTypeName() { return null; }
	
	/**
	 * Returns the property that refers to the id of the entity
	 * @return Property object
	 */
	default public Property getIdentifierProperty() { return null; }
	
	/**
	 * Returns the key that the entity is uniquely referred by the user
	 * @return the fields in the natural key
	 */
	default List<String> getNaturalKey() { return new ArrayList<>(); }

	/**
	 * Returns the key that the entity is uniquely referred by the user
	 * In terms of simple fields.
	 * This can have a performance impact.
	 * @return the fields in the natural key
	 */
	default List<String> getExpandedNaturalKey() { return new ArrayList<>(); }
	
	/**
	 * Set the natural key for this type
	 * @param keys fields of the natural key
	 */
	default void setNaturalKey(String[] keys) { }
	
	/**
	 * List of all candidate keys supported by the entity.
	 * Includes the natural key if one is designated as such.
	 * 
	 * @return the candidate keys.
	 */
	default List<Set<String>> getCandidateKeys() { return new ArrayList<>(); }
	
	/**
	 * Returns the property that refers to the version of the entity
	 * @return Property object
	 */
	default Property getVersionProperty() { return null; }

	/**
	 * Gets the list of properties that are directly on this type, i.e.,
	 * it does not include properties not defined on its supertypes
	 * @return list of properties
	 */
	List /*Property*/<Property> getDeclaredProperties();

	/**
	 * Get a list of all the types that are embedded in this type
	 * @return list of embedded types
	 */
	default List<Type> getEmbeddableTypes() { return new ArrayList<>(); }
	
	/**
	 * Return true if the contents of this object cannot be changed
	 * @return true if immutable
	 */
	default boolean isImmutable() { return false; }
	
	/**
	 * Returns true if this type is an embedded type
	 * @return true if embedded type
	 */
	default boolean isEmbedded() { return false; }
	
	/**
	 * Returns the highest ancestor entity in an inheritance hierarchy. 
	 * Useful in EntityKey formation since we want find the same entity
	 * but it might be associated against a parent type.
	 * 
	 * If a type has multiple super types, then we don't rely on this 
	 * method but use the entity type directly.
	 *  
	 * @return EntityType
	 */
	default EntityType getRootEntityType() { return null; }
	
	/**
	 * Executes all the PostLogic annotated methods associated with objects of this type and its ancestor types
	 * 
	 * @param settings passed by user
	 * @param instance on which the postlogic methods are invoked
	 */
	default void invokePostLogic(Settings settings, Object instance) {}

	/**
	 * Returns the name that the persistence provider uses to map an entity
	 * @see Type#getName()
	 * @return entityName
	 */
	String getEntityName();

	/** 
	 * Returns true if this type represents an entity
	 * @return boolean value
	 */
	default boolean isEntity() { return false; }

	/**
	 * This is an optimization step to bring in the properties
	 * from the ancestors into the current type.
	 * @param shape of the type
	 */
	default void unfoldProperties(Shape shape) {}

	/**
	 * Find all the types of the subclasses of the instance class of this type 
	 * 
	 * @param types of all types   
	 */
	default void defineSubtypes(List<Type> types) {}

	/**
	 * Find all the immediate subType entities of this type
	 */
	default void defineChildTypes() {}
	
	/**
	 * Get a list of all the sub types of this type
	 * @return subtypes
	 */
	default Set<EntityType> getSubtypes() { return new HashSet<>(); }

	/**
	 * Get a list of all the immediate sub types of this type
	 * @return subtypes
	 */
	default Set<EntityType> getChildTypes() { return new HashSet<>(); }

	/**
	 * Add property to this type and its external type
	 * 
	 * @param property that is added
	 */
	default void addProperty(Property property) {}

	/**
	 * Remove property from this type and its corresponding external type
	 *
	 * @param property that is removed
	 */
	default void removeProperty(Property property) {}

	/**
	 * Retrieve a property by its alias name
	 * @param name alternative name
	 * @return Property object
	 */
	default Property getPropertyByAlias(String name) { return null; }


	@Override
	default List<Property> getProperties() {
		Map<String, Property> propertyMap = getShape().getProperties(this);
		// Returning null is significant as there is logic around it
		if(propertyMap == null) {
			return null;
		}

		// Make the map mutable since we need to add to it
		propertyMap = new HashMap<>(propertyMap);

		EntityType parentType = getParentType();
		while(parentType != null) {
			Map<String, Property> parentProperties = getShape().getProperties(parentType);
			for(Map.Entry<String, Property> entry: parentProperties.entrySet()) {
				if(!propertyMap.containsKey(entry.getKey())) {
					propertyMap.put(entry.getKey(), entry.getValue());
				}
			}

			// Walk up the super types
			parentType = parentType.getParentType();
		}

		return new ArrayList<>(propertyMap.values());
	}

	@Override
	default Property getProperty(String path) {
		int delim = path.indexOf(Settings.PATH_DELIMITER);

		Property result = null;
		if(delim == -1) {
			if(path.startsWith(Constants.XOR.IDREF)) {
				path = path.substring(Constants.XOR.IDREF.length());
			}
			result = getShape().getProperty(this, path);
		} else {
			Property property = getProperty(path.substring(0, delim));
			if(property == null) {
				if(path.contains(DFAtoRE.RECURSE_SYMBOL)) {
					throw new RuntimeException("Recursive references currently not supported");
				} else {
					logger.info("Property " + path + " not found. If this is an open property, ensure it is added to the type");
					return null;
				}
			}
			Type propertyType = property.getType();
			if(property.isMany())
				propertyType = ((ExtendedProperty)property).getElementType();
			result = propertyType.getProperty(path.substring(delim+1));
		}

		return result;
	}
	
	/** 
	 * Initialize the position property for list and map types
	 * @param shape of this type
	 */
	default void initPositionProperty(Shape shape) {}

	/**
	 * Find the annotation object from the instance class
	 * @param annotationClass on this type's java class
	 * @return annotation object if present
	 */
	default Annotation getClassAnnotation(Class<?> annotationClass) { return null; }

	/**
	 * Return the Method object for the specific property
	 * @param targetProperty property name
	 * @return Method object if present, null otherwise
	 */
	default Method getGetterMethod(String targetProperty) { return null; }

	/**
	 * Return the Field object for the specific property
	 * @param targetProperty property name
	 * @return Field object if present, null otherwise
	 */
	default Field getField(String targetProperty) { return null; }

	/**
	 * Return the setter method object for the property
	 * @param targetProperty property name
	 * @return Method object if present, null otherwise
	 */
	default Method getSetterMethod(String targetProperty) { return null; }

	/**
	 * Get the lambdas associated with a property
	 * @param targetProperty property name
	 * @return list of lambdas
	 */
	default List<MethodInfo> getLambdas(String targetProperty) { return new ArrayList<>(); }

	/**
	 * Returns all the properties that need to be initialized when
	 * the entity is created
	 * @return a set of properties
	 */
	default Set<String> getInitializedProperties() { return new HashSet<>(); }
	
	/**
	 * Returns the topological sort order for the type
	 * The higher the number, more entities are dependant upon it.
	 * 
	 * @return order number
	 */
	default int getOrder() { return -1; }
	
	/**
	 * Sets the topological sort order for the type
	 * 
	 * @param value of order
	 */
	default void setOrder(int value) {}
	
	/**
	 * If this entity type is configured to support dynamic update
	 * @return boolean value
	 */
	default boolean supportsDynamicUpdate() { return false; }

	/**
	 * Get the immediate super type for this type
	 * @return EntityType
	 */
	default EntityType getParentType() { return null; }
	
	/**
	 * Set the immediate super type for this entity type
	 * @param value of EntityType
	 */
	default void setParentType(EntityType value) {}

	/**
	 * set the shape to which this type belongs. It is an error to reassign a type from a
	 * different shape.
	 * @param shape instance
	 */
	void setShape(Shape shape);

	/**
	 * Return the shape to which this type belongs
	 * @return shape instance
	 */
	Shape getShape();

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
	default boolean isNullable(String propertyPath) { return false; }

	/**
	 * When building OQL queries, some embedded types are not considered to be directly
	 * explorable.
	 * @return true if it can appear in the FROM clause of a query
	 */
	default boolean isExplorable () { return false; }

	/**
	 * Checks if this is the root concrete type. That is there is no parent type that
	 * has persisted instances in the database.
	 *
	 * @return true if this is the root concrete type.
	 */
	default boolean isRootConcreteType() { return false; }

	/**
	 * Allows setting an open property value dynamically.
	 *
	 * @param obj open instance
	 * @param propertyName of the property whose value need to be set
	 * @param value to be set
	 */
	default void setOpenProperty(Object obj, String propertyName, Object value ) {}

	/**
	 * Check to see if the current type is an ancestor type of the given type
	 * @param entityType is a descendant type
	 * @return true if ancestor false otherwise
	 */
	default boolean isSameOrSupertypeOf (EntityType entityType) { return false; }

	/**
	 * Check if the current type is a descendant of the given type
	 * @param entityType given type
	 * @return true if the current type is a subtype
	 */
	default boolean isSubtypeOf (EntityType entityType) { return false; }

	/**
	 * Returns the list of the descendant sub types starting from the child
	 * and ending at the parent of the descendant.
	 * i.e., the starting and end are not included in the descendant chain.
	 *
	 * @param entityType descendant type representing the child of the chain end
	 * @return descendant chain
	 */
	default List<EntityType> getDescendantsTo(EntityType entityType) { return new ArrayList<>(); }

	/**
	 * Check in which subtypes is the property present.
	 * Ideally this should return only a single item, but there can be cases
	 * where the same property can be defined by unrelated sub types
	 *
	 * @param property to search for
	 * @return subtypes containing that property definition
	 */
	default List<EntityType> findInSubtypes (String property) { return new ArrayList<>(); }

	/**
	 * Return the entity specific settings related to data generation
	 * @return all the generators for this type
	 */
	default List<GeneratorDriver> getGenerators () { return new ArrayList<>(); }

	/**
	 * add the generator settings for the entity
	 * @param generator generator settings object
	 */
	default void addGenerator(GeneratorDriver generator) {}

	/**
	 * Clear the generators set on this type
	 */
	default void clearGenerators() {}

	/**
	 * Set the value for the root entity type
	 * @param value entity type name
	 */
    default void setRootEntityType(String value) {}
    
    /**
     * Create the properties for this type on 
     * the provided shape instance
     * @param shape containing the properties for this type
     */
    default void defineProperties(Shape shape) {}
    
    @Override
    default String getJsonType() { return MutableJsonType.JSONSCHEMA_OBJECT_TYPE; }
    
    /**
     * Mainly used to capture different generator drivers for a type 
     * 
     * When the copy is done, the correct shape for which it is needed
     * is passed
     * 
     * @param shape new shape containing the type
     * @return copy of the type instance
     */
    default EntityType copy(Shape shape) { return null; }


	@Override
	default TypeKind getKind() {
		if(isAbstract()) {
			return TypeKind.INTERFACE;
		} else {
			return TypeKind.OBJECT;
		}
	}
}
