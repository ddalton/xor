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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.QueryType;
import tools.xor.service.Shape;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.Tree;
import tools.xor.util.graph.TypeGraph;

/**
 * This class is responsible for building a tree of QueryFragment nodes connected by
 * IntraQuery edges.
 *
 * There is a 1:1 correspondence between a QueryTree and a StateTree
 * There is a 1:1 correspondence between a root/child view and a QueryTree
 *
 * A StateTree is composed of QueryType instances
 * The QueryType is basedOn the domain type
 */
public class FragmentBuilder
{
    private static final Logger qtLogger = LogManager.getLogger(Constants.Log.QUERY_TRANSFORMER);

    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;

    public FragmentBuilder(AggregateTree aggregateTree) {
        this.aggregateTree = aggregateTree;
    }

    private void constructQueryTrees (QueryTree<QueryFragment, IntraQuery<QueryFragment>> parent, View childView) {
        // first build the QueryTree for the child
        // The EntityType for the child QueryTree is derived as:
        // 1. If the child view does not have a name or a typename, then it inherits the parent's type
        // 2. If the child view has a typename, that that is the child's entity type
        // 3. If the child view has a name, then it is a property of the parent type and this
        //    property type is used as the child view's entity type

        EntityType entityType = null;
        Property property = null; // Needed to label the edge to the child
        if(childView.getTypeName() != null && !"".equals(childView.getTypeName().trim())) {
            entityType = (EntityType)aggregateTree.getView().getShape().getType(childView.getTypeName());
        } else {
            // derive it from the root
            if(childView.getAnchorPath() != null && !"".equals(childView.getAnchorPath().trim()) ) {

                property = parent.getAggregateType().getProperty(childView.getAnchorPath());
                if(property != null) {
                    if(property.isMany()) {
                        entityType = (EntityType) ((ExtendedProperty)property).getElementType();
                    } else {
                        entityType = (EntityType)property.getType();
                    }
                }
            }
            if(entityType == null) {
                // use the parent's entity type
                entityType = (EntityType)parent.getAggregateType();
            }
        }

        List<QueryTree> queriesFromView = getQueriesFromView(childView, entityType);
        for(QueryTree<QueryFragment, IntraQuery<QueryFragment>> childQueryTree: queriesFromView) {
            build(childQueryTree);

            QueryTree.FragmentAnchor sourceAnchor = parent.findFragment(childView.getAnchorPath());
            QueryFragment sourceFragment = null;
            QueryFragment targetFragment = null;
            if (sourceAnchor != null) {
                sourceFragment = sourceAnchor.fragment;
                targetFragment = childQueryTree.getRoot();
            } else if(parent.getView().isCustom()) {
                sourceFragment = parent.getRoot();
                targetFragment = childQueryTree.getRoot();
            }

            if(sourceFragment != null) {
                addInterQueryEdge(
                    property,
                    parent,
                    childQueryTree,
                    sourceFragment,
                    targetFragment);
            }

            if(childView.getChildren() != null) {
                for (View grandchildView: childView.getChildren()) {
                    constructQueryTrees(childQueryTree, grandchildView);
                }
            }
        }
    }

    private void addInterQueryEdge(Property property,
                                   QueryTree parent,
                                   QueryTree<QueryFragment, IntraQuery<QueryFragment>> childPiece,
                                   QueryFragment sourceFragment,
                                   QueryFragment targetFragment) {

        // The source fragment should fetch the id of the entity, since this id is needed
        // to join with the target QueryTree
        // This is done automatically for system generated QueryTrees - See QueryTree#generateIdFields
        // IMPLICATION 1: For QueryTree of custom views we need to check that the view is indeed fetching
        // this id
        View sourceView = parent.getView();
        if(sourceView.isCustom() && !sourceView.getAttributeList().contains(sourceFragment.getIdPath())) {
            throw new RuntimeException(String.format("Missing %s in parent view. The parent view needs to fetch the owner ids when joining with a child view.", sourceFragment.getIdPath()));
        }

        // IMPLICATION 2: The target EntityType is always the same as the EntityType of the owner of the association
        // This is because the target QueryTree's root fragment is a copy of the sourceFragment from the
        // source QueryTree
        if(sourceFragment.getEntityType() != targetFragment.getEntityType()) {
            throw new RuntimeException("Source and target QueryFragments do not represent the same type - a join is not possible");
        }

        aggregateTree.addEdge(
            new InterQuery(
                property == null ? "" : property.getName(), parent, childPiece, sourceFragment, targetFragment),
            parent,
            childPiece);
    }

    private List<QueryTree> getQueriesFromView(View view, EntityType entityType) {
        List<QueryTree> rootQueries = new ArrayList<>();
        if(view.isCompositionView()) {
            // A composition view only contains view references as its attributes
            // Create a querytree for each of them
            for(View child: getCompositionViews(view, entityType.getShape())) {
                // Make a copy of the child view
                // and populate the anchor path
                View childView = child.copy();
                childView.setAnchorPath(view.getAnchorPath());
                rootQueries.add(new QueryTree(entityType, child.copy()));
            }
        } else {
            rootQueries.add(new QueryTree(entityType, view));
        }

        return rootQueries;
    }

    /**
     * Builds a QueryTree.
     * @param entityType of root
     */
    public void build(EntityType entityType)
    {
        View view = aggregateTree.getView();

        List<QueryTree> rootQueries = getQueriesFromView(view, entityType);
        for(QueryTree queryTree: rootQueries) {
            // If the view has direct attributes or no children then build a query for this view
            if (view.getAttributeList().size() > 0 || (view.getChildren() == null
                || view.getChildren().size() == 0)) {
                build(queryTree);
            }
            if (view.getChildren() != null) {
                for (View childView : view.getChildren()) {
                    constructQueryTrees(queryTree, childView);
                }
            }
        }
    }

    private Set<View> getCompositionViews(View view, Shape shape) {

        Set<View> views = new HashSet<>();
        for(String attribute: view.getAttributeList()) {
            if(TraversalView.isCompositionReference(attribute)) {
                String viewname = TraversalView.getViewReference(attribute);
                views.add(shape.getView(viewname));
            }
        }

        return views;
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

        // Add the fragments
        Map<State, QueryFragment> stateToFragmentMap = new HashMap<>();
        if(view.isCustom()) {
            // Custom views only have a single fragment as the query is user provided and
            // does not need to be constructed
            // The fragment here is just used to map the columns selected by the custom query
            Collection<State> roots = st.getRoots();
            assert roots.size() == 1 : "Custom query is supported only for a single root state tree";

            State root = roots.iterator().next();
            QueryType qt = (QueryType)root.getType();
            QueryFragment fragment = new QueryFragment(
                qt.getBasedOn() == null ? qt : qt.getBasedOn(), // DQOR QueryTypes do not have basedOn type
                aggregateTree.nextAlias(),
                view.getAnchorPath());
            queryTree.addVertex(fragment);

            // We need to fetch the columns in the order specified by the user query
            for (String path : paths) {
                fragment.addPath(path);
            }
        } else {

            // Function attributes
            for(String attr: view.getFunctionAttributes()) {
                st.extend(attr, st.getRootState(), false);
            }	
        	
            // TODO: Since this is a tree data structure, we can do a BFS traversal
            // through the tree
            // That was we can avoid adding duplicate attributes from the subtypes, if it has
            // already been selected on the supertype
            // Only the id property needs to be duplicated both on the supertype and subtype
            for (State state : st.getVertices()) {

                QueryType qt = (QueryType)state.getType();
                QueryFragment fragment = new QueryFragment(
                    qt.getBasedOn(),  // Need the actual type, since the query is built using the actual type
                    aggregateTree.nextAlias(),
                    ((Tree)st).getPathToRoot(state));
                stateToFragmentMap.put(state, fragment);

                // collect them to later add them in the correct order
                for (String attr : state.getAttributes()) {
                    fragment.addPath(attr);
                }

                queryTree.addVertex(fragment);
            }

            // Add the edges
            // handle subtypes, joins etc
            for (Edge<State> edge : st.getEdges()) {
                // DQOR QueryTypes do not have edges in StateTree
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
