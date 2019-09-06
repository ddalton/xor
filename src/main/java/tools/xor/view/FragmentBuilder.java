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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.QueryType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.DataAccessService;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.Tree;
import tools.xor.util.graph.TypeGraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for building a tree of QueryFragment nodes connected by
 * InterQuery edges.
 */
public class FragmentBuilder
{
    private static final Logger qtLogger = LogManager.getLogger(Constants.Log.QUERY_TRANSFORMER);

    private Map<String, QueryFragment> pathToFragment = new HashMap<>();
    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;
    private DataAccessService das; // optional for denormalized query, needed for creating QueryType instances

    public FragmentBuilder(DataAccessService das, AggregateTree aggregateTree) {
        this.das = das;
        this.aggregateTree = aggregateTree;
    }

    private void constructQueryTrees (QueryTree parent, View childView) {
        // first build the QueryTree for the child
        EntityType entityType = null;
        if(childView.getTypeName() != null && !"".equals(childView.getTypeName().trim())) {
            entityType = (EntityType)aggregateTree.getView().getShape().getType(childView.getTypeName());
        } else {
            // derive it from the root
            if(childView.getName() != null && !"".equals(childView.getName().trim()) ) {
                Property property = null;
                for(QueryTree qp: aggregateTree.getRoots()) {
                    property = qp.getAggregateType().getProperty(childView.getName());
                    if(property != null) {
                        break;
                    }
                }
                if(property != null) {
                    entityType = (EntityType) property.getType();
                }
            }
            if(entityType == null) {
                // use the parent's entity type
                entityType = (EntityType)parent.getAggregateType();
            }
        }
        QueryTree<QueryFragment, IntraQuery<QueryFragment>> childPiece = new QueryTree(entityType, childView);
        build(childPiece);

        QueryTree.FragmentAnchor sourceAnchor = null;
        for(QueryTree qp: aggregateTree.getRoots()) {
            sourceAnchor = qp.findFragment(childView.getName());
            if(sourceAnchor != null) {
                break;
            }
        }
        if(sourceAnchor != null) {
            QueryFragment sourceFragment = sourceAnchor.fragment;
            QueryFragment targetFragment = childPiece.getRoot();

            addInterQueryEdge(parent, childPiece, sourceFragment, targetFragment);
        }

        if(childView.getChildren() != null) {
            for (View grandchildView: childView.getChildren()) {
                constructQueryTrees(childPiece, grandchildView);
            }
        }
    }

    private void addInterQueryEdge(QueryTree parent, QueryTree<QueryFragment, IntraQuery<QueryFragment>> childPiece,
                                   QueryFragment sourceFragment, QueryFragment targetFragment) {
        // TODO: Ensure that the source fragment has id property to help with reconstitution
        aggregateTree.addEdge(
            new InterQuery(
                "", parent,
                childPiece, sourceFragment, targetFragment), parent, childPiece);
    }

    /**
     * Builds a QueryTree.
     * @param entityType of root
     */
    public void build(EntityType entityType)
    {
        View view = aggregateTree.getView();

        QueryTree queryTree = new QueryTree(entityType, view);
        // If the view has direct attributes or no children then build a query for this view
        if(view.getAttributeList().size() > 0 || (view.getChildren() == null || view.getChildren().size() == 0)) {
            build(queryTree);
        }

        if (view.getChildren() != null) {
            for (View childView : view.getChildren()) {
                constructQueryTrees(queryTree, childView);
            }
        }
    }

    public void build(QueryTree queryTree)
    {
        View view = queryTree.getView();

        this.aggregateTree.addVertex(queryTree);

        // We need to handle child views
        List<String> paths = new LinkedList<>(view.getAttributeList());
        if( (paths == null || paths.isEmpty()) && view.getChildren() != null && view.getChildren().size() > 0) {
            return;
        }

        // get the StateTree instance
        TypeGraph<State, Edge<State>> st = view.getTypeGraph((EntityType) queryTree.getAggregateType(), StateGraph.Scope.EDGE);

        /*
         *  Algorithm
         *  1. For each State in the StateTree we create a QueryFragment
         *  2. For each non-inverse relationship property we create a InterQuery edge. Make sure the columns on both sides are included in the select
         *  3. We join all the sub-types of i.e., _PARENT_-1 relationships [Not needed immediately]
         *  4. Add non-fetchable properties (used for query condition etc)
         *  5. Skip QueryTree for open types
         *  6. If it is not explorable we just add the id or the path
         */

        // Function attributes
        for(String attr: view.getFunctionAttributes()) {
            st.extend(attr, st.getRootState(), false);
        }

        // Add the fragments
        Map<State, QueryFragment> stateToFragmentMap = new HashMap<>();
        Map<String, QueryFragment> pathToFragmentMap = new HashMap<>();

        // TODO: Since this is a tree data structure, we can do a BFS traversal
        // through the tree
        // That was we can avoid adding duplicate attributes from the subtypes, if it has
        // already been selected on the supertype
        // Only the id property needs to be duplicated both on the supertype and subtype
        for(State state: st.getVertices()) {

            QueryType qt = (QueryType)state.getType();
            QueryFragment fragment = new QueryFragment(
                qt.getBasedOn(),
                aggregateTree.nextAlias(),
                ((Tree)st).getPathToRoot(state));
            stateToFragmentMap.put(state, fragment);

            // collect them to later add them in the correct order
            for(String attr: state.getAttributes()) {
                if(!view.hasUserQuery()) {
                    fragment.addPath(attr);
                } else {
                    pathToFragmentMap.put(fragment.getFullPath(attr), fragment);
                }
            }

            queryTree.addVertex(fragment);
        }

        // We need to fetch the columns in the order specified by the user query
        if(view.hasUserQuery()) {
            // Add the attributes in the correct order
            for (String path : paths) {
                if (pathToFragmentMap.containsKey(path)) {
                    QueryFragment qf = pathToFragmentMap.remove(path);
                    qf.addFullPath(path);
                }
            }
            // Add the remaining attributes - order is irrelevant
            for (String path : new HashSet<>(pathToFragmentMap.keySet())) {
                QueryFragment qf = pathToFragmentMap.remove(path);
                qf.addFullPath(path);
            }
        }

        // Add the edges
        // handle subtypes, joins etc
        for(Edge<State> edge: st.getEdges()) {
            EntityType startType = ((QueryType)edge.getStart().getType()).getBasedOn();
            Property p = startType.getProperty(edge.getName());

            QueryFragment start = stateToFragmentMap.get(edge.getStart());
            QueryFragment end = stateToFragmentMap.get(edge.getEnd());
            IntraQuery queryEdge = new IntraQuery(
                edge.getName(),
                start,
                end,
                p);

            queryTree.addEdge(queryEdge, start, end);
        }

        // We do not construct fragments for open types
        if(queryTree.getAggregateType().isOpen()) {
            return;
        }

        // write the .dot content to log
        if(qtLogger.isDebugEnabled()) {
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);

            try {
                queryTree.writeDOT(bw);
                qtLogger.debug(sw.toString());
            }
            catch (IOException e) {
                qtLogger.debug("Error in writing .dot content to log: " + e.getMessage());
            }
        }
    }
}
