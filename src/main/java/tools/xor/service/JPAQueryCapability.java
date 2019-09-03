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

package tools.xor.service;

import tools.xor.util.IntraQuery;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;

public class JPAQueryCapability extends AbstractQueryCapability {

    @Override public String getDowncastClause (QueryTree queryTree, IntraQuery<QueryFragment> joinEdge)
    {
        // We need to get the association edge that points to the root of the inheritance hierarchy
        IntraQuery<QueryFragment> association = joinEdge.getAssociationEdge(queryTree);

        return String.format("TREAT(%s AS %s) ", association.getNormalizedName(), joinEdge.getEnd().getEntityType().getName());
    }
}
