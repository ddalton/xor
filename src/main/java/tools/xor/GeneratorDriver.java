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

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

import java.sql.Connection;

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
    void init(Connection connection, StateGraph.ObjectGenerationVisitor visitor);

    public void processVisitors();

    void addVisit(DefaultGenerator.GeneratorVisit visit);

    void addListener(IteratorListener listener);
}
