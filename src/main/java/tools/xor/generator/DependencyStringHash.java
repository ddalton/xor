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

package tools.xor.generator;

import java.util.Random;

import tools.xor.util.graph.StateGraph;

/**
 * This class takes a field of the form
 * [PARENT].attribute.[PARENT].attribute ...
 * attribute.[PARENT].attribute
 * ...
 *
 * Here [_PARENT_] represents the object to which the current object is connected to.
 * If an object is shared, then the connected to object is non-deterministic as the order is
 * currently not enforced.
 * The way we solve the non-deterministic case is to provide multiple paths to resolve the
 * non determinism.
 *
 */
public class DependencyStringHash extends DefaultGenerator
{
    public DependencyStringHash (String[] arguments)
    {
        super(arguments);
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        // Currently support only upto MAX_COLLECTION_ELEMENTS unique ids
        String value = getDependencyValue(visitor);

        if(value == null) {
            return (new Random()).nextInt();
        }

        int intValue = value.hashCode();
        int maxCollectionElements = getMaxCollectionElements();

        int base = intValue - intValue%maxCollectionElements;
        return base + visitor.getSequenceNo()%maxCollectionElements;
    }
}
