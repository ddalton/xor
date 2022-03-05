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

import tools.xor.util.graph.StateGraph;

/**
 * Generators that are depended upon by other generators are strongly recommended
 * to implmement this interface.
 * The benefit is that no explicit ordering needs to be coded if ths is done.
 * For example:
 *   If there are 2 generator classes: G1 and G2
 *   and G2 depends on the value of G1
 *
 *   Then when an entity is generated G1 should be invoked before G2
 *   This is accomplised by sending an event just before a new entity is generated
 *   this prepares the generator with a new value.
 *
 * Another thing to note is that generators that implement this interface
 * should only generate a new value as part of this event.
 *
 * DeferredRangePercent and RangePercent have the same implementation
 * but DeferredRangePercent implements IteratorListener and generates its value
 * only as part of the handleEvent call.
 * Whereas RangePercent generates a new value as part of its get..value calls.
 *
 * So DeferredRangePercent is suitable as a source for a GeneratorRecipient and
 * RangePercent is not
 *
 */
public interface IteratorListener
{
    void handleEvent(int sourceId, StateGraph.ObjectGenerationVisitor visitor);
}
