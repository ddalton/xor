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

package tools.xor.view;

import tools.xor.BusinessObject;
import tools.xor.Settings;
import tools.xor.util.InterQuery;

public interface ObjectResolver
{
    /**
     * 1. Shared - Objects with same ids are shared. The result will be a graph of information.
     *    To allow the identification of shareable objects, additional information will need to
     *    be fetched such as the id of the objects.
     *    Typically used where the objects are fully populated.
     * 2. Distinct - Objects with same ids are not shared. This is faithful to what the customer requested.
     *    The data for the same object might occur multiple times in the result.
     *    The result becomes a tree of information.
     *    Typically used where the same objects are populated differently depending upon how
     *    they are accessed
     */
    public enum Type {
        SHARED,
        DISTINCT
    }

    /**
     * We set the runtime parameters before the query is executed
     *
     * @param queryTree query piece for the query
     * @param settings user settings
     * @param qti QueryTreeInvocation object
     * @param parentEdge not null if this is a child query
     */
    void preProcess(QueryTree queryTree, Settings settings, QueryTreeInvocation qti, InterQuery parentEdge);

    /**
     * Notify the resolver of the reconstituted object
     *
     * @param businessObject reconstituted object
     * @param isRoot true if the object is created from a root query tree
     */
    void notify(BusinessObject businessObject, boolean isRoot);

    /**
     * Extract the root entities and return to the user.
     */
    void postProcess();
}
