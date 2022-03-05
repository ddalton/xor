/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

import java.sql.Connection;

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

/**
 * A generator implementing this interface signals that that generator is the
 * main driver for that type.
 * The generator for other fields on the table can be powered of by the driving generator.
 *
 * There are multiple generators that can be chosen as a driver. Some examples are:
 *  1. CounterGenerator         - A simple driver that can be used to initialize an id
 *  2. CollectionOwnerGenerator - Useful to populate a table that has both element and owner id in the same table
 *  3. HierarchGenerator        - Useful to populate a table that describes a hierarchy relationship
 *  4. QueryGenerator           - If a table needs to get its values from other table(s)
 */
public interface GeneratorDriver
{
    /**
     * Gives an opportunity for the generator driver to keep the visitor
     * up to date with the current generated value
     * 
     * @param connection Used by QueryGenerator to generate value from a database query
     * @param visitor object that is updated with the current generated value
     */
    void init(Connection connection, StateGraph.ObjectGenerationVisitor visitor);

    /**
     * All the GeneratorVisit instances are processed when this is invoked.
     * This allows a GeneratorDriver to dynamically change the generators associated
     * with a property.
     */
    public void processVisitors();

    /**
     * Allows to control dynamically the generator that is associated with a property
     * @param visit object modelling relationship between generator and property
     */
    void addVisit(DefaultGenerator.GeneratorVisit visit);

    /**
     * Allows other generators to be notified when there is a change in generated value
     * @param listener generator that registers for this generator's events
     */
    void addListener(IteratorListener listener);

    /**
     * Indicates if this generator driver is also directly responsible for generating the data
     * for the type's properties.
     * If true, then the Iterator#next method will return JSONObject instance
     * relating the property name to its value.
     * NOTE:
     * 1. The values are restricted to only the types supported in json. For e.g., Date
     *    objects need to be converted to String.
     * 2. The json key name should be normalized using CSVLoader.CSVState#normalize
     *
     * @return true if generates property values false otherwise
     */
    default boolean isDirect() {
        return false;
    }
}
