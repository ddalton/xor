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

import java.util.List;

/**
 * A representation of the type of a {@link Property property} of a {@link DataObject data object}.
 */
public interface Type
{
  /**
   * Returns the name of the type.
   * @return the type name.
   */
  String getName();
  
  /**
   * Returns the namespace URI of the type or <code>null</code> if the type has no URI
   * (for example it was generated from a Schema with no target namespace).
   * @return the namespace URI.
   */
  String getURI();

  /**
   * Returns the Java class that this type represents.
   * Does not make sense for an open type as it can be a composition of properties from multiple types.
   * @return the Java class and null if open type
   */
  Class<?> getInstanceClass();

  /**
   * Returns whether the specified object is an instance of this type.
   * @param object the object in question.
   * @return <code>true</code> if the object is an instance.
   * @see Class#isInstance
   */
  boolean isInstance(Object object);

  /**
   * Returns the List of the {@link Property Properties} of this type.
   * <p>
   * The expression
   *<pre>
   *   type.getProperties().indexOf(property)
   *</pre>
   * yields the property's index relative to this type.
   * As such, these expressions are equivalent:
   *<pre>
   *    dataObject.{@link DataObject#get(Property) get}((Property)dataObject.getType().getProperties().get(i));
   *</pre>
   * <p>
   * @return the Properties of the type.
   * @see Property#getContainingType
   */
  List /*Property*/<Property> getProperties();
  
  /**
   * Returns from {@link #getProperties all the Properties} of this type, the one with the specified name.
   * As such, these expressions are equivalent:
   *<pre>
   *    dataObject.{@link DataObject#get(String) get}("name")
   *    dataObject.{@link DataObject#get(Property) get}(dataObject.getType().getProperty("name"))
   *</pre>
   * <p>
   * @param propertyName property name
   * @return the Property with the specified name.
   * @see #getProperties
   */
  Property getProperty(String propertyName);
  
  /*
   * Indicates if this Type specifies DataTypes (true) or DataObjects (false).
   * When false, any object that is an instance of this type
   * also implements the DataObject interface.
   * True for simple types such as Strings and numbers.
   * For any object:
   *  <pre>
   *   isInstance(object) && !isDataType() implies
   *    DataObject.class.isInstance(object) returns true. 
   *  </pre>
   * @return true if Type specifies DataTypes, false for DataObjects.
   */
  boolean isDataType();

  /**
   * Indicates if this Type specifies a DataType in the context of the provided object
   * For e.g., a DataType instance might refer to an abstract class, where the object
   * refers to the concrete instance and we can infer the correct type from it. So this method
   * is preferred over Type#isDataType() when the object information is available as if provides
   * more accurate information on the data type
   *
   * Usually overridden in subclasses, as the behavior is persistence type specific
   *
   * @param object The object whose type we are trying to check. If it is null, then
   *               we fallback to checking the meta information. The meta information may not
   *               always be correct since it could refer to an abstract class that might be
   *               marked as a DataType.
   * @return false if the provided object is not a DataType
   */
  boolean isDataType(Object object);
  
  /**
   * Indicates if this Type allows any form of open content.  If false,
   * dataObject.getInstanceProperties() must be the same as 
   * dataObject.getType().getProperties() for any DataObject dataObject of this Type.
   * @return true if this Type allows open content.
   */
  boolean isOpen();

  /**
   * Indicates if this Type specifies Sequenced DataObjects.
   * Sequenced DataObjects are used when the order of values 
   * between Properties must be preserved.
   * When true, a DataObject will return a Sequence. 
   * @return true if this Type specifies Sequenced DataObjects.
   */
  boolean isSequenced();

  /**
   * Indicates if this Type is abstract.  If true, this Type cannot be
   * instantiated.  Abstract types cannot be used in DataObject or 
   * DataFactory create methods.
   * @return true if this Type is abstract.
   */
  boolean isAbstract();

  /**
   * Returns the List of base Types for this Type.  The List is empty
   * if there are no base Types. 
   * @return the List of base Types for this Type.
   */
  List /*Type*/<Type> getBaseTypes();
  
  /**
   * Returns the Properties declared in this Type as opposed to
   * those declared in base Types.
   * @return the Properties declared in this Type.
   */
  List /*Property*/<Property> getDeclaredProperties();

  /**
   * Return a list of alias names for this Type.
   * @return a list of alias names for this Type.
   */
  List /*String*/<?> getAliasNames();

  /**
   * Returns a read-only List of instance Properties available on this Type.
   * <p>
   * This list includes, at a minimum, any open content properties (extensions) added to
   * the object before defining the Type's Type}. Implementations may, but are not required 
   * to in the 2.1 version of SDO, provide additional instance properties.
   * @return the List of instance Properties on this Type.
   */
  List /*Property*/<?> getInstanceProperties();

  /**
   * Returns the value of the specified instance property of this Type.
   * @param property one of the properties returned by {@link #getInstanceProperties()}.
   * @return the value of the specified property.
   * @see DataObject#get(Property)
   */
  Object get(Property property);

}
