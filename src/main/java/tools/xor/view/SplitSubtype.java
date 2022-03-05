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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;

/**
 * When a type needs to be downcast, then the query needs to mention this.
 * We support OUTER JOIN with this method.
 * INNER JOIN can be supported using the TREAT operator.
 */
public class SplitSubtype implements TreeMutatorStrategy
{
    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;

    public SplitSubtype (AggregateTree aggregateTree) {
        this.aggregateTree = aggregateTree;
    }

    @Override public void execute ()
    {
        List<QueryTree> queryTrees = aggregateTree.getNonCustomVertices();
        for(QueryTree queryTree: queryTrees) {
            processQueryTree(queryTree);
        }
    }

    private void processQueryTree(QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree) {
        // TODO: document the algorithm with diagrams
        // Algorithm:
        // Step 1: Process each vertex
        // Step 2: Go to the root of the inheritance tree
        // Step 3: Create a separate query for each concrete subtype (OUTER JOIN)
        // NOTE: We do not support QueryFragment on abstract types

        Set<QueryFragment> processed = new HashSet<>();

        // We create a new collection to protect from concurrent modification
        List<QueryFragment> vertices = new LinkedList<>(queryTree.getVertices());
        for(QueryFragment fragment: vertices) {
            if(processed.contains(fragment)) {
                continue;
            }
            processed.add(fragment);

            // ensure we are at the root of the inheritance hierarchy
            while(queryTree.getsuperType(fragment) != null) {
                fragment = queryTree.getsuperType(fragment);
            }
            processInheritanceRoot(queryTree, fragment, processed);
        }
    }

    private void processInheritanceRoot(QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree, QueryFragment fragment, Set<QueryFragment> processed) {
        for(QueryFragment subtype: queryTree.getSubtypes(fragment)) {
            // create a new query tree for the subtype
            // Split at the inheritance edge
            IntraQuery inheritanceEdge = queryTree.getInEdges(subtype).iterator().next();
            QueryTree newQT = split(queryTree, fragment, inheritanceEdge, subtype);

            processed.add(subtype);

            // process the subtype
            processInheritanceRoot(newQT, subtype, processed);
        }
    }

    public QueryTree split(QueryTree<QueryFragment, IntraQuery<QueryFragment>> originalQT, QueryFragment anchor, IntraQuery<QueryFragment> splitAtEdge, QueryFragment subtype) {
        // The root of the new query tree is the subtype
        QueryTree newQT = new QueryTree(subtype.getEntityType(), originalQT.getView());
        newQT.addVertex(subtype);

        // Split the original QueryTree
        if(splitAtEdge != null) {
            originalQT.split(splitAtEdge, null, newQT);
        }

        addInterGraphEdge(originalQT, newQT, anchor, subtype, splitAtEdge);

        return newQT;
    }

    private void addInterGraphEdge(QueryTree originalQT, QueryTree newQT, QueryFragment supertype, QueryFragment subtype, IntraQuery<QueryFragment> splitAtEdge) {
        InterQuery edge = new InterQuery(splitAtEdge.getName(), originalQT, newQT, supertype, subtype);
        aggregateTree.addEdge(edge, originalQT, newQT);
    }
}
