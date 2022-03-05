/**
 * <copyright>
 *
 * THE ARTIFACTS ARE PROVIDED "AS IS" AND THE AUTHORS MAKE NO 
 * REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, REGARDING THE 
 * ARTIFACTS AND THE IMPLEMENTATION OF THEIR CONTENTS, 
 * INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT OR TITLE. 
 * 
 * THE AUTHORS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL, 
 * INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF OR RELATING TO ANY 
 * USE OR DISTRIBUTION OF THE ARTIFACTS.
 * 
 * The name and trademarks of the Authors may NOT be used in any manner, 
 * including advertising or publicity pertaining to the Service Data 
 * Objects Specification or its contents without specific, written prior 
 * permission. Title to copyright in the Service Data Objects 
 * Specification will at all times remain with the Authors.
 * 
 * No other rights are granted by implication, estoppel or otherwise.
 *
 * </copyright>
 * 
 * This artifact is based on the SDO specification which can be found at:
 *   http://www.jcp.org/en/jsr/detail?id=235
 * 
 */

package tools.xor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.xor.generator.Generator;

/**
 * A representation of a Property in the {@link Type type} of a {@link DataObject data object}.
 */
public interface Property
{
  /**
   * Returns the name of the Property.
   * @return the Property name.
   */
  String getName();
  
  /**
   * Returns the type of the Property.
   * @return the Property type.
   */
  Type getType();
  
  /**
   * Returns whether the Property is many-valued.
   * @return <code>true</code> if the Property is many-valued.
   */
  boolean isMany();
  
  /**
   * Returns whether the Property is containment, i.e., whether it represents by-value composition.
   * @return <code>true</code> if the Property is containment.
   */
  default boolean isContainment() { return false; }
  
  /**
   * Returns the containing type of this Property.
   * @return the Property's containing type.
   * @see Type#getProperties()
   */
  default Type getContainingType() { return null; }

  /**
   * Returns the default value this Property will have in a {@link DataObject data object} where the Property hasn't been set.
   * @return the default value.
   */
  Object getDefault();

  /**
   * Returns true if values for this Property cannot be modified using the SDO APIs.
   * When true, DataObject.set(Property property, Object value) throws an exception.
   * Values may change due to other factors, such as services operating on DataObjects.
   * @return true if values for this Property cannot be modified.
   */
  default boolean isReadOnly() { return false; }

  /**
   * Allow the modeller to set a property as read-only. We don't want to delegate this
   * functionality to the underlying ORM/persistence mechanism, since it makes its own
   * judgement on whether a property should be read-only. This may not always be right,
   * for example bi-dir relationship.
   *
   * @param value true if the property is read-only
   */
  default void setReadOnly(boolean value) { return; }

  /**
   * Returns the opposite Property if the Property is bi-directional or null otherwise.
   * @return the opposite Property if the Property is bi-directional or null
   */
  default Property getOpposite() { return null; }

  /**
   * Valid only in the context of a TO_MANY relationship. This represents the property
   * on the many side that captures this relationship.
   *
   * @return Property object
   */
  default public Property getMappedBy() { return null; }

  /**
   * Returns a list of alias names for this Property.
   * @return a list of alias names for this Property.
   */
  default List /*String*/<?> getAliasNames() { return new ArrayList<>(); }

  /**
   * Returns whether or not instances of this property can be set to null. The effect of calling set(null) on a non-nullable
   * property is not specified by SDO.
   * @return true if this property is nullable.
   */
  boolean isNullable();

  /**
   * Returns whether or not this is an open content Property.
   * @return true if this property is an open content Property.
   */
  default boolean isOpenContent() { return false; }

  /**
   * Returns a read-only List of instance Properties available on this Property.
   * <p>
   * This list includes, at a minimum, any open content properties (extensions) added to
   * the object before defining the Property's Type}. Implementations may, but are not required 
   * to in the 2.1 version of SDO, provide additional instance properties.
   * @return the List of instance Properties on this Property.
   */
  default List /*Property*/<?> getInstanceProperties() { return new ArrayList<>(); }

  /**
   * Returns the value of the specified instance property of this Property.
   * @param property one of the properties returned by {@link #getInstanceProperties()}.
   * @return the value of the specified property.
   * @see DataObject#get(Property)
   */
  default Object get(Property property) { return null; }

  /**
   * Returns the expanded property names rooted at this property. 
   * This is typically one entry and can be more than one when dealing with 
   * embedded types.
   * @param examined Avoids processing these types and is useful for avoiding getting stuck in
   *                 a loop
   * @return a list of properties
   */
  default List<String> expand(Set<Type> examined) { return new ArrayList<>(); }

  /**
   * Returns the expanded property names rooted at this property.
   * @param examined Avoids processing these types and is useful for avoiding getting stuck in
   *                 a loop
   * @return a list of properties
   */
  default List<String> expandMigrate(Set<Type> examined) { return new ArrayList<>(); }

  /**
   * Get the constraints that are on this property such as field length etc...
   * @return map of constraints
   */
  default Map<String, Object> getConstraints() { return new HashMap<>(); }

  /**
   * Add a property constraint
   * @param key constraint name
   * @param value constraint value
   */
  default void addConstraint(String key, Object value) {}

  /**
   * The persistence layer specifies the mechanism on retrieving and
   * setting the value of this property. This is done through the
   * PersistenceOrchestrator.
   * @return true if the ORM manages the property access
   */
  default boolean isManaged() { return false; }

  /**
   * Indicates if this property is directly declared on the class or is declared on the superclass.
   * @return true if the property is defined in a superclass
   */
  default boolean isInherited() { return false; }
  

  /**
   * Set the Generator to be used for this property
   * @param generator instance
   */
  default void setGenerator(Generator generator) {}

  /**
   * Set the Generator specific to populating data when coming from incomingProperty
   * @param incomingProperty of the form sourceType:property
   * @param generator instance
   */
  default void setGenerator(String incomingProperty, Generator generator) {}

  /**
   * Get the Generator instance configured on this property
   * @return generator instance
   */
  default Generator getGenerator() { return null; }

  /**
   * Get the Generator instance configured on this property, depending upon the incoming path
   * by which this entity is being created.
   *
   * @param incomingProperty through which this instance is being generated
   * @return generator instance
   */
  default Generator getGenerator(String incomingProperty) { return null; }
}
