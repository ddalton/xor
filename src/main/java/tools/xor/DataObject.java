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

import java.io.Serializable;
import java.util.List;

/**
 * A data object is a representation of some structured data. 
 * It is the fundamental component in the SDO (Service Data Objects) package.
 * Data objects support reflection, path-based accesss, convenience creation and deletion methods, 
 * and the ability to be part of a data graph.
 * <p>
 * Each data object holds its data as a series of {@link Property Properties}. 
 * Properties can be accessed by name, property index, or using the property meta object itself. 
 * A data object can also contain references to other data objects, through reference-type Properties.
 * <p>
 * A data object has a series of convenience accessors for its Properties. 
 * These methods either use a path (String), 
 * a property index, 
 * or the {@link Property property's meta object} itself, to identify the property.
 * Some examples of the path-based accessors are as follows:
 *<pre>
 * DataObject company = ...;
 * company.get("name");                   is the same as company.get(company.getType().getProperty("name"))
 * company.set("name", "acme");
 * company.get("department.0/name")       is the same as ((DataObject)((List)company.get("department")).get(0)).get("name")
 *                                        .n  indexes from 0 ... implies the name property of the first department
 * company.get("department[1]/name")      [] indexes from 1 ... implies the name property of the first department
 * company.get("department[number=123]")  returns the first department where number=123
 * company.get("..")                      returns the containing data object
 * company.get("/")                       returns the root containing data object
 *</pre> 
 * <p> There are general accessors for Properties, i.e., {@link #get(Property) get} and {@link #set(Property, Object) set}, 
 * as well as specific accessors for the primitive types and commonly used data types like 
 * String, Date, List, BigInteger, and BigDecimal.
 */
public interface DataObject extends Serializable
{
  /**
   * Returns the value of a property of either this object or an object reachable from it, as identified by the
   * specified path.
   * @param path the path to a valid object and property.
   * @return the value of the specified property.
   * @see #get(Property)
   */
  Object get(String path);

  /**
   * Sets a property of either this object or an object reachable from it, as identified by the specified path,
   * to the specified value.
   * @param path the path to a valid object and property.
   * @param value the new value for the property.
   * @see #set(Property, Object)
   */
  void set(String path, Object value);

  /**
   * Returns the value of a <code>DataObject</code> property identified by the specified path.
   * @param path the path to a valid object and property.
   * @return the <code>DataObject</code> value of the specified property.
   * @see #get(String)
   */
  DataObject getExistingDataObject(String path);

  /**
   * Sets the property at the specified index in {@link Type#getProperties property list} of this object's
   * {@link Type type}, to the specified value.
   * @param propertyIndex the index of the property.
   * @param value the new value for the property.
   * @see #set(Property, Object)
   */
  void set(int propertyIndex, Object value);

  /**
   * Returns the value of the given property of this object.
   * <p>
   * If the property is {@link Property#isMany many-valued},
   * the result will be a {@link java.util.List}
   * and each object in the List will be {@link Type#isInstance an instance of} 
   * the property's {@link Property#getType type}.
   * Otherwise the result will directly be an instance of the property's type.
   * @param property the property of the value to fetch.
   * @return the value of the given property of the object.
   * @see #set(Property, Object)
   */
  Object get(Property property);

  /**
   * Returns the value of a String property identified by the specified path.
   * This is an optimization API intended only for use in Export/Import and where
   * the input DataObject is a JSONObject.
   *
   * This optimization API allows us to avoid the creation of DataObject wrappers
   * for embedded objects and helps with performance on Aggregates that use a lot of
   * embedded objects.
   *
   * @param path the path to a valid object and property.
   * @return the String value of the specified property.
   */
  String getString(java.lang.String path);

  /**
   * Sets the value of the given property of the object to the new value.
   * <p>
   * If the property is {@link Property#isMany many-valued},
   * the new value must be a {@link java.util.List}
   * and each object in that list must be {@link Type#isInstance an instance of} 
   * the property's {@link Property#getType type};
   * the existing contents are cleared and the contents of the new value are added.
   * Otherwise the new value directly must be an instance of the property's type
   * and it becomes the new value of the property of the object.
   * @param property the property of the value to set.
   * @param value the new value for the property.
   * @see #get(Property)
   */
  void set(Property property, Object value);

  /**
   * Returns the value of the specified <code>DataObject</code> property.
   * @param property the property to get.
   * @return the <code>DataObject</code> value of the specified property.
   * @see #get(Property)
   */
  DataObject getExistingDataObject(Property property);

  /**
   * Returns the value of the specified <code>List</code> property.
   * The List returned contains the current values.
   * Updates through the List interface operate on the current values of the DataObject.
   * Each access returns the same List object.
   * @param property the property to get.
   * @return the <code>List</code> value of the specified property.
   * @see #get(Property)
   */
  List<?> getList(Property property);

  /**
   * Returns a new {@link DataObject data object} contained by this object using the specified property,
   * which must be a {@link Property#isContainment containment property}.
   * The type of the created object is the {@link Property#getType declared type} of the specified property.
   * @param propertyName the name of the specified containment property.
   * @return the created data object.
   * @see #createDataObject(String, String, String)
   */
  DataObject createDataObject(String propertyName);

  /**
   * Returns a new {@link DataObject data object} contained by this object using the specified property,
   * which must be a {@link Property#isContainment containment property}.
   * The type of the created object is the {@link Property#getType declared type} of the specified property.
   * @param propertyIndex the index of the specified containment property.
   * @return the created data object.
   * @see #createDataObject(int, String, String)
   */
  DataObject createDataObject(int propertyIndex);

  /**
   * Returns a new {@link DataObject data object} contained by this object using the specified property,
   * which must be a {@link Property#isContainment containment property}.
   * The type of the created object is the {@link Property#getType declared type} of the specified property.
   * @param property the specified containment property.
   * @return the created data object.
   * @see #createDataObject(Property, Type)
   */
  DataObject createDataObject(Property property);

  /**
   * Returns a new {@link DataObject data object} contained by this object using the specified property,
   * which must be a {@link Property#isContainment containment property}.
   * The type of the created object is specified by the packageURI and typeName arguments.
   * The specified type must be a compatible target for the property identified by propertyName.
   * @param propertyName the name of the specified containment property.
   * @param namespaceURI the namespace URI of the package containing the type of object to be created.
   * @param typeName the name of a type in the specified package.
   * @return the created data object.
   * @see #createDataObject(String)
   */
  DataObject createDataObject(String propertyName, String namespaceURI, String typeName);

  /**
   * Returns a new {@link DataObject data object} contained by this object using the specified property,
   * which must be a {@link Property#isContainment containment property}.
   * The type of the created object is specified by the packageURI and typeName arguments.
   * The specified type must be a compatible target for the property identified by propertyIndex.
   * @param propertyIndex the index of the specified containment property.
   * @param namespaceURI the namespace URI of the package containing the type of object to be created.
   * @param typeName the name of a type in the specified package.
   * @return the created data object.
   * @see #createDataObject(int)
   */
  DataObject createDataObject(int propertyIndex, String namespaceURI, String typeName);

  /**
   * Returns a new {@link DataObject data object} contained by this object using the specified property,
   * which must be of {@link Property#isContainment containment type}.
   * The type of the created object is specified by the type argument,
   * which must be a compatible target for the speicifed property.
   * @param property a containment property of this object.
   * @param type the type of object to be created.
   * @return the created data object.
   * @see #createDataObject(int)
   */
  DataObject createDataObject(Property property, Type type);

  /**
   * Returns the containing {@link DataObject data object}
   * or <code>null</code> if there is no container.
   * @return the containing data object or <code>null</code>.
   */
  DataObject getContainer();

  /**
   * Return the Property of the {@link DataObject data object} containing this data object
   * or <code>null</code> if there is no container.
   * @return the property containing this data object.
   */
  Property getContainmentProperty();

  /**
   * Returns the data object's type.
   * <p>
   * The type defines the Properties available for reflective access.
   * @return the type.
   */
  Type getType();

  /**
   * Returns the named Property from the current instance properties,
   * or null if not found.  The instance properties are getInstanceProperties().  
   * @param propertyName the name of the Property
   * @return the named Property from the DataObject's current instance properties, or null.
   */
  Property getInstanceProperty(String propertyName);

  /**
   * @deprecated replaced by {@link #getInstanceProperty(String)} in 2.1.0
   * @param propertyName the name of the Property
   * @return the Property object
   */
  Property getProperty(String propertyName);

  /**
   * Returns the root {@link DataObject data object}.
   * @return the root data object.
   */
  DataObject getRootObject();
}
