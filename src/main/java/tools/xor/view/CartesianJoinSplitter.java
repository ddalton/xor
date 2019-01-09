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

import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CartesianJoinSplitter implements QuerySplitter
{
    private QueryTree queryTree;

    public CartesianJoinSplitter(QueryTree queryTree) {
        this.queryTree = queryTree;
    }

    /**
     * 1. Loop through every QueryPiece
     * 2. For each QueryPiece loop through every QueryFragment
     * 3. Rollup all the collections found in the descendants. At the end of this
     *    process, the root node will have a max of parallel collections in the tree.
     * 4. Do a BFS and if any node has more than 1 parallel collection, it represents a
     *    CartesianJoin and the QueryPiece needs to be split into separate queries.
     *    Randomly choose a collection to split and create a new QueryPiece out of it and
     *    add it to the end of the list of QueryPieces to process.
     */
    @Override public void execute ()
    {
        Collection<QueryPiece> vertices = queryTree.getVertices();

        List<QueryPiece> pieces = new LinkedList<>(vertices);
        while(!pieces.isEmpty()) {
            QueryPiece qp = pieces.remove(0);
            QueryFragment root = (QueryFragment)qp.getRoot();
            qp.computeCollectionCount(root);

            if(root.getParallelCollectionCount() > 1) {
                // There is a minimum of one feasible split
                List<QueryPiece> newQueries = processSplit(qp, root);

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
     * We then wrap it in a new QueryPiece and connect it with an InterQuery edge.
     *
     * @param originalQP is the QueryPiece that needs to be further split into additional queries
     * @param fragment is the root of the QueryPiece
     * @return new queries split from the original QueryPiece
     */
    private List<QueryPiece> processSplit(QueryPiece<QueryFragment, IntraQuery<QueryFragment>> originalQP, QueryFragment fragment) {
        // first check if there are simple collections
        // if so, put each in a separate query

        List<QueryPiece> newQueries = new LinkedList<>();

        if(fragment.getSimpleCollectionCount() > 0) {
            // Find the simple collections
            for(String simpleColl: fragment.getSimpleCollectionPaths()) {
                QueryPiece<QueryFragment, IntraQuery<QueryFragment>> newQP = split(
                    originalQP,
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
            for(IntraQuery<QueryFragment> outgoing: originalQP.getOutEdges(fragment)) {
                QueryFragment child = outgoing.getEnd();
                if(outgoing.getProperty().isMany() || child.getParallelCollectionCount() > 0) {
                    if(encounteredCollection) {
                        // Split at this edge
                        QueryPiece newQP = split(originalQP, fragment, outgoing);
                        newQueries.add(newQP);
                    } else {
                        encounteredCollection = true;
                    }
                }
            }
        }

        return newQueries;
    }

    private QueryPiece split(QueryPiece<QueryFragment, IntraQuery<QueryFragment>> originalQP, QueryFragment fragment, IntraQuery<QueryFragment> splitAtEdge) {
        QueryFragment clone = originalQP.copyRoot(this.queryTree);
        QueryPiece newQP = new QueryPiece(clone.getEntityType());
        newQP.addVertex(clone);

        // Split the original QueryPiece
        if(splitAtEdge != null) {
            IntraQuery newEdge = new IntraQuery(splitAtEdge.getName(), clone, splitAtEdge.getEnd(), splitAtEdge.getProperty());
            originalQP.split(splitAtEdge, newEdge, newQP);
        }

        addInterGraphEdge(originalQP, newQP, fragment, clone, splitAtEdge);

        return newQP;
    }

    private void addInterGraphEdge(QueryPiece originalQP, QueryPiece newQP, QueryFragment original, QueryFragment clone, IntraQuery<QueryFragment> splitAtEdge) {
        InterQuery edge = new InterQuery(splitAtEdge.getProperty().getName(), originalQP, newQP, original, clone);
        queryTree.addEdge(edge, originalQP, newQP);
    }
}
