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

import java.util.LinkedList;
import java.util.List;

import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;

/**
 * This strategy needs to be used if needing an OUTER JOIN type functionality
 * i.e., if the join needs to be included in the result, irrespective of whether
 */
public class SplitToRoot implements TreeMutatorStrategy
{
    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;

    public SplitToRoot (AggregateTree aggregateTree) {
        this.aggregateTree = aggregateTree;
    }

    /**
     * 1. Loop through every QueryTree
     * 2. For each QueryTree, loop through every QueryFragment
     * 3. Rollup all the collections found in the descendants. At the end of this
     *    process, the root node will have a max of parallel collections in the tree.
     * 4. Do a BFS and if any node has more than 1 parallel collection, it represents a
     *    CartesianJoin and the QueryTree needs to be split into separate queries.
     *    Randomly choose a collection to split and create a new QueryTree out of it and
     *    add it to the end of the list of QueryTree to process.
     */
    @Override public void execute ()
    {
        List<QueryTree> pieces = aggregateTree.getNonCustomVertices();
        while(!pieces.isEmpty()) {
            QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree = pieces.remove(0);

            for(QueryFragment fragment: queryTree.getVertices()) {
                if(queryTree.getSubtypes(fragment).size() > 0) {
                    throw new RuntimeException("SplitToRoot not supported for fields on subtypes, use SplitToAnchor instead.");
                }
            }

            QueryFragment root = queryTree.getRoot();
            queryTree.computeCollectionCount(root);

            if(root.getParallelCollectionCount() > 1) {
                // There is a minimum of one feasible split
                List<QueryTree> newQueries = processSplit(queryTree, root);

                // processSplit splits at the first occurrences of a parallel collection
                // The new queries could have further parallel collections so we need
                // to process them
                pieces.addAll(newQueries);
            }
        }
    }

    /**
     * The way this splitter works is we create a clone of the Fragment where the split
     * needs to occur and populate it with the edge that is split from the original QueryFragment
     * We then wrap it in a new QueryTree and connect it with an InterQuery edge.
     *
     * @param originalQT is the QueryTree that needs to be further split into additional queries
     * @param fragment is the root of the QueryTree
     * @return new queries split from the original QueryTree
     */
    private List<QueryTree> processSplit(QueryTree<QueryFragment, IntraQuery<QueryFragment>> originalQT, QueryFragment fragment) {
        // first check if there are simple collections
        // if so, put each in a separate query

        List<QueryTree> newQueries = new LinkedList<>();

        if(fragment.getSimpleCollectionCount() > 0) {
            // Find the simple collections
            for(String simpleColl: fragment.getSimpleCollectionPaths()) {
                QueryTree<QueryFragment, IntraQuery<QueryFragment>> newQP = split(
                    originalQT,
                    fragment,
                    null);
                newQueries.add(newQP);
                fragment.removeSimpleCollectionPath(simpleColl);
                newQP.getRoot().addSimpleCollectionPath(simpleColl);
            }
        }

        if(fragment.getParallelCollectionCount() > 1) {
            // Now identify if that parallel collection occurs across the outgoing edges
            // i.e., check the parallel collection count of the child fragments
            // in addition to the outgoing edge being a collection

            boolean encounteredCollection = false;
            for(IntraQuery<QueryFragment> outgoing: originalQT.getOutEdges(fragment)) {
                QueryFragment child = outgoing.getEnd();
                if(outgoing.getProperty().isMany() || child.getParallelCollectionCount() > 0) {
                    if(encounteredCollection) {
                        // Split at this edge
                        QueryTree newQT = split(originalQT, fragment, outgoing);
                        newQueries.add(newQT);
                    } else {
                        encounteredCollection = true;
                    }
                }
            }
        }

        return newQueries;
    }

    private QueryTree split(QueryTree<QueryFragment, IntraQuery<QueryFragment>> originalQT, QueryFragment fragment, IntraQuery<QueryFragment> splitAtEdge) {
        QueryFragment clone = originalQT.copyRoot(this.aggregateTree);
        QueryTree newQT = new QueryTree(clone.getEntityType(), originalQT.getView());
        newQT.addVertex(clone);

        // Split the original QueryTree
        if(splitAtEdge != null) {
            IntraQuery newEdge = new IntraQuery(splitAtEdge.getName(), clone, splitAtEdge.getEnd(), splitAtEdge.getProperty());
            originalQT.split(splitAtEdge, newEdge, newQT);
        }

        // The aggregateTree is now a forest
        aggregateTree.addVertex(newQT);

        return newQT;
    }
}
