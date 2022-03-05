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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.util.graph.StateGraph;

public interface Generator
{

    /**
     * Return a byte value
     * @param visitor contains data pertaining to the calling context
     * @return byte value
     */
    Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a char value
     * @param visitor contains data pertaining to the calling context
     * @return char value
     */
    char getCharValue (StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a short value
     * @param visitor contains data pertaining to the calling context
     * @return short value
     */
    Short getShortValue (StateGraph.ObjectGenerationVisitor visitor);

    /**
     * We return an Integer instead of an int since we would like to
     * support null values.
     * Since we use an integer for id generation and we would like
     * to support null parents when generating child elements.
     *
     * @see tools.xor.CollectionOwnerGenerator
     *
     * @param visitor contains data pertaining to the calling context
     * @return int value.
     */
    Integer getIntValue(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Returns a long value
     * @param visitor contains data pertaining to the calling context
     * @return long value
     */
    Long getLongValue(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a date value
     * @param visitor contains data pertaining to the calling context
     * @return date value
     */
    Date getDateValue(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a double value
     * @param visitor contains data pertaining to the calling context
     * @return double value
     */
    Double getDoubleValue(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a float value
     * @param visitor contains data pertaining to the calling context
     * @return float value
     */
    Float getFloatValue(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a BigDecimal instance
     * @param visitor contains data pertaining to the calling context
     * @return BigDecimal value
     */
    BigDecimal getBigDecimal(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Return a BigInteger instance
     * @param visitor contains data pertaining to the calling context
     * @return BigInteger value
     */
    BigInteger getBigInteger(StateGraph.ObjectGenerationVisitor visitor);

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
     * @param stateGraph used to filter the list of subtypes
     * @return the desired descendant state based on generator logic
     */
    EntityType getSubType (EntityType entityType, StateGraph stateGraph);

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
     * @param property collection property
     * @param settings collection sparseness
     * @param path collection sparseness value for this association
     * @param visitor that contains details on the incomingProperty
     * @return collection size
     */
    int getFanout(Property property, Settings settings, String path, StateGraph.ObjectGenerationVisitor visitor);

    /**
     * The generator is only applicable to the collection element and not to the collection
     * itself. The collection element type should be EntityType.
     * @return true if the generator is only applicable to the collection element
     */
    boolean isApplicableToCollectionElement();

    /**
     * Give a chance for the generator to initialize itself
     * @param visitor context
     */
    void init(StateGraph.ObjectGenerationVisitor visitor);

    /**
     * Returns the current value from the generator if applicable
     * It is an undecorated value of the string representation.
     *
     * @param visitor context
     * @return current value
     */
    String getCurrentValue(StateGraph.ObjectGenerationVisitor visitor);
}
