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

package tools.xor.generator;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface Generator
{

    /**
     * Return a byte value
     * @return byte value
     */
    byte getByteValue ();

    /**
     * Return a char value
     * @param visitor object
     * @return char value
     */
    char getCharValue (StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a short value
     * @return short value
     */
    short getShortValue ();

    /**
     * Returns an int value.
     * @param visitor object
     * @return int value.
     */
    int getIntValue(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Returns a long value
     * @return long value
     */
    long getLongValue();

    /**
     * Return a date value
     * @return date value
     */
    Date getDateValue();

    /**
     * Return a double value
     * @return double value
     */
    Double getDoubleValue();

    /**
     * Return a float value
     * @return float value
     */
    Float getFloatValue();

    /**
     * Return a BigDecimal instance
     * @return BigDecimal value
     */
    BigDecimal getBigDecimal();

    /**
     * Return a BigInteger instance
     * @return BigInteger value
     */
    BigInteger getBigInteger();

    /**
     * Return a string value
     * @param property whose value needs to be generated
     * @param visitor containing call stack specific information
     * @return string value
     */
    String getStringValue(Property property, StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Returns the desired descendant State
     * @param entityType root type of the inheritance hierarchy
     * @return the desired descendant state based on generator logic
     */
    EntityType getSubType (EntityType entityType);

    /**
     * Validate this generator against the property for which it is going to
     * generate the values.
     *
     * @param property for which this generator is being configured
     */
    void validate(ExtendedProperty property);

    /**
     * Get the collection size for a toMany association.
     * @see Generator#isApplicableToCollectionElement
     * @param settings collection sparseness
     * @param path collection sparseness value for this association
     * @return collection size
     */
    int getFanout(Settings settings, String path);

    /**
     * The generator is only applicable to the collection element and not to the collection
     * itself. The collection element type should be EntityType.
     * @return true if the generator is only applicable to the collection element
     */
    boolean isApplicableToCollectionElement();
}
