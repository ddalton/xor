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

/**
 * A sequence is a heterogeneous list of {@link Property properties} and corresponding values.
 * It represents an ordered arbitrary mixture of data values from more than one property of a {@link DataObject data object}.
 */
public interface Sequence
{
  /**
   * Returns the number of entries in the sequence.
   * @return the number of entries.
   */
  int size();

  /**
   * Returns the property for the given entry index.
   * Returns <code>null</code> for mixed text entries.
   * @param index the index of the entry.
   * @return the property or <code>null</code> for the given entry index.
   */
  Property getProperty(int index);
  
  /**
   * Returns the property value for the given entry index.
   * @param index the index of the entry.
   * @return the value for the given entry index.
   */
  Object getValue(int index);
  
  /**
   * Sets the entry at a specified index to the new value.
   * @param index the index of the entry.
   * @param value the new value for the entry.
   */
  Object setValue(int index, Object value);

  /**
   * Adds a new entry with the specified property name and value
   * to the end of the entries.
   * @param propertyName the name of the entry's property.
   * @param value the value for the entry.
   */
  boolean add(String propertyName, Object value);

  /**
   * Adds a new entry with the specified property index and value
   * to the end of the entries.
   * @param propertyIndex the index of the entry's property.
   * @param value the value for the entry.
   */
  boolean add(int propertyIndex, Object value);

  /**
   * Adds a new entry with the specified property and value
   * to the end of the entries.
   * @param property the property of the entry.
   * @param value the value for the entry.
   */
  boolean add(Property property, Object value);

  /**
   * Adds a new entry with the specified property name and value
   * at the specified entry index.
   * @param index the index at which to add the entry.
   * @param propertyName the name of the entry's property.
   * @param value the value for the entry.
   */
  void add(int index, String propertyName, Object value);

  /**
   * Adds a new entry with the specified property index and value
   * at the specified entry index.
   * @param index the index at which to add the entry.
   * @param propertyIndex the index of the entry's property.
   * @param value the value for the entry.
   */
  void add(int index, int propertyIndex, Object value);

  /**
   * Adds a new entry with the specified property and value
   * at the specified entry index.
   * @param index the index at which to add the entry.
   * @param property the property of the entry.
   * @param value the value for the entry.
   */
  void add(int index, Property property, Object value);
 
  /**
   * Removes the entry at the given entry index.
   * @param index the index of the entry.
   */
  void remove(int index);

  /**
   * Moves the entry at <code>fromIndex</code> to <code>toIndex</code>.
   * @param toIndex the index of the entry destination.
   * @param fromIndex the index of the entry to move.
   */
  void move(int toIndex, int fromIndex);
  
  /**
   * @deprecated replaced by {@link #addText(String)} in 2.1.0
   */
  void add(String text);

  /**
   * @deprecated replaced by {@link #addText(int, String)} in 2.1.0
   */
  void add(int index, String text);
  
  /**
   * Adds a new text entry to the end of the Sequence.
   * @param text value of the entry.
   */
  void addText(String text);

  /**
   * Adds a new text entry at the given index.
   * @param index the index at which to add the entry.
   * @param text value of the entry.
   */
  void addText(int index, String text);
  
}
