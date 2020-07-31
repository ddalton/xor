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
import java.util.Stack;

import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;

/**
 * This strategy needs to be used if needing an INNER JOIN type functionality
 * i.e., if the join needs only those objects participating in all parallel collections.
 *
 * Here anchor refers to the node in the AggregateTree where more than one collection is rooted at.
 * The ancestor path at the root of the new QueryTree is not null and contains the path from
 * the root of the AggregateTree to the anchor node.
 */
public class SplitToAnchor implements TreeMutatorStrategy
{
    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;

    public SplitToAnchor (AggregateTree aggregateTree) {
        this.aggregateTree = aggregateTree;
    }

    /**
     * Initially the AggregateTree has a single QueryTree
     *
     * 1. Compute the parallel collection count on the QueryTree
     * 2. Proceed to step 3 if the root node has a collection count greater than 1.
     *    The root node is added to the stack for processing.
     * 3. Check the stack is not empty and the item has a count greater than 1
     *    if the count less than 2 then pop the item from the stack. Do step 3 until either
     *    the stack is empty or an item is found whose count greater than 1.
     *    If the stack is empty we are done.
     * 4. Do a DFS on the first item in the stack until a node is found that is the anchor of
     *    2 parallel collections and none of whose children have a count greater than 1.
     *    This node represents a CartesianJoin and needs to be split into separate queries.
     *    i.e., a new QueryTree of one of the parallel collection needs to be anchored at this node.
     *    The collection for the new QueryTree can be chosen at random.
     *    The anchor node copy is set as the root for the new QueryTree and also the ancestor path
     *    from the AggregateTree root is set on this node.
     * 5. Keep moving up the ancestor and subtract 1 from the parallel collection count
     *    until the root node. While doing this, find the first node encountered that has
     *    a parallel collection count greater than or equal to 2 during the decrement process.
     *    Push this node on the stack if it is not the same as the current node being processed.
     * 6. Do step 3
     */
    @Override public void execute ()
    {
        List<QueryTree> queryTrees = aggregateTree.getNonCustomVertices();
        for(QueryTree queryTree: queryTrees) {
            processQueryTree(queryTree);
        }
    }

    private void processQueryTree(QueryTree queryTree) {
        queryTree.computeCollectionCount((QueryFragment)queryTree.getRoot());

        Stack<QueryFragment> processing = new Stack<>();
        processing.add((QueryFragment)queryTree.getRoot());

        while(!processing.isEmpty()) {
            if(processing.peek().getParallelCollectionCount() > 1) {
                QueryFragment anchor = dfs(queryTree, processing.peek());
                List<QueryTree> newQueries = processSplit(queryTree, anchor);

                // Go up the ancestor tree and decrement count
                int count = newQueries.size();
                anchor.setParallelCollectionCount(anchor.getParallelCollectionCount()-count);
                QueryFragment fragment = anchor;
                while(queryTree.getParent(fragment) != null) {
                    fragment = (QueryFragment)queryTree.getParent(fragment);
                    anchor.setParallelCollectionCount(anchor.getParallelCollectionCount()-count);
                    if(fragment.getParallelCollectionCount() > 1 && fragment != processing.peek()) {
                        processing.push(fragment);
                    }
                }
            } else {
                processing.pop();
            }
        }
    }

    private QueryFragment dfs(QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree, QueryFragment fragment) {
        assert(fragment.getParallelCollectionCount() > 1);

        for(IntraQuery<QueryFragment> out: queryTree.getOutEdges(fragment)) {
            QueryFragment end = out.getEnd();
            if(end.getParallelCollectionCount() > 1 ) {
                return dfs(queryTree, end);
            }
        }

        // It's children all have atmost 1 collection
        return fragment;
    }

    /**
     * The way this splitter works is we create a clone of the Fragment where the split
     * needs to occur and populate it with the edge that is split from the original QueryFragment
     * We then wrap it in a new QueryTree and connect it with an InterQuery edge.
     *
     * @param originalQT is the QueryTree that needs to be further split into additional queries
     * @param anchor is the node we are splitting the QueryTree
     * @return new queries split from the original QueryTree
     */
    private List<QueryTree> processSplit(QueryTree<QueryFragment, IntraQuery<QueryFragment>> originalQT, QueryFragment anchor) {
        // NOTE: We are not concerned about simple collections as we are doing only INNER JOIN

        List<QueryTree> newQueries = new LinkedList<>();
        if(anchor.getParallelCollectionCount() > 1) {
            // Now identify if that parallel collection occurs across the outgoing edges
            // i.e., check the parallel collection count of the child fragments
            // in addition to the outgoing edge being a collection

            boolean encounteredCollection = false;
            for(IntraQuery<QueryFragment> outgoing: originalQT.getOutEdges(anchor)) {
                // TODO: split all the nodes that have a collection anchored under that node
                QueryFragment child = outgoing.getEnd();
                if(outgoing.getProperty().isMany() || child.getParallelCollectionCount() > 0) {
                    /*
                    if(encounteredCollection) {
                        // Split at this edge
                        QueryTree newQT = split(originalQT, anchor, outgoing);
                        newQueries.add(newQT);
                    } else {
                        encounteredCollection = true;
                    }
                    */
                    // Split at this edge
                    QueryTree newQT = split(originalQT, anchor, outgoing);
                    newQueries.add(newQT);
                }
            }
        }

        return newQueries;
    }

    public QueryTree split(QueryTree<QueryFragment, IntraQuery<QueryFragment>> originalQT, QueryFragment anchor, IntraQuery<QueryFragment> splitAtEdge) {
        String ancestorPath = originalQT.getPathToRoot(anchor);
        QueryFragment clone = new QueryFragment(anchor.getEntityType(), aggregateTree.nextAlias(), ancestorPath);

        QueryTree newQT = new QueryTree(clone.getEntityType(), originalQT.getView());
        newQT.addVertex(clone);

        // Split the original QueryTree
        if(splitAtEdge != null) {
            IntraQuery newEdge = new IntraQuery(splitAtEdge.getName(), clone, splitAtEdge.getEnd(), splitAtEdge.getProperty());
            originalQT.split(splitAtEdge, newEdge, newQT);
        }

        addInterGraphEdge(originalQT, newQT, anchor, clone, splitAtEdge);

        return newQT;
    }

    private void addInterGraphEdge(QueryTree originalQT, QueryTree newQT, QueryFragment original, QueryFragment clone, IntraQuery<QueryFragment> splitAtEdge) {
        InterQuery edge = new InterQuery(splitAtEdge.getProperty().getName(), originalQT, newQT, original, clone);
        aggregateTree.addEdge(edge, originalQT, newQT);
    }
}
