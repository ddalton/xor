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
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.DataAccessService;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.StateTree;
import tools.xor.util.graph.TypeGraph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
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
    private QueryTree<QueryPiece, InterQuery<QueryPiece>> queryTree;
    private DataAccessService das; // optional for denormalized query, needed for creating QueryType instances

    public FragmentBuilder(DataAccessService das, QueryTree queryTree) {
        this.das = das;
        this.queryTree = queryTree;
    }

    private void constructPieces(QueryPiece parent, View childView) {
        // first build the QueryPiece for the child
        EntityType entityType = null;
        if(childView.getTypeName() != null && !"".equals(childView.getTypeName().trim())) {
            entityType = (EntityType)queryTree.getView().getShape().getType(childView.getTypeName());
        } else {
            // derive it from the root
            if(childView.getName() != null && !"".equals(childView.getName().trim()) ) {
                Property property = null;
                for(QueryPiece qp: queryTree.getRoots()) {
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
        QueryPiece<QueryFragment, IntraQuery<QueryFragment>> childPiece = new QueryPiece(entityType, childView);
        build(childPiece);

        QueryPiece.FragmentAnchor sourceAnchor = null;
        for(QueryPiece qp: queryTree.getRoots()) {
            sourceAnchor = qp.findFragment(childView.getName());
            if(sourceAnchor != null) {
                break;
            }
        }
        if(sourceAnchor != null) {
            QueryFragment sourceFragment = sourceAnchor.fragment;
            QueryFragment targetFragment = childPiece.getRoot();

            addInterQueryEdge(parent, childPiece, sourceFragment, targetFragment);
        } else {
            // The QueryTree is a forest
            this.queryTree.addVertex(childPiece);
        }

        if(childView.getChildren() != null) {
            for (View grandchildView: childView.getChildren()) {
                constructPieces(childPiece, grandchildView);
            }
        }
    }

    private void addInterQueryEdge(QueryPiece parent, QueryPiece<QueryFragment, IntraQuery<QueryFragment>> childPiece,
                                   QueryFragment sourceFragment, QueryFragment targetFragment) {
        // TODO: Ensure that the source fragment has id property to help with reconstitution
        queryTree.addEdge(new InterQuery("", parent,
                childPiece, sourceFragment, targetFragment), parent, childPiece);
    }

    /**
     * Builds a QueryPiece.
     * @param entityType of root
     */
    public void build(EntityType entityType)
    {
        View view = queryTree.getView();
        QueryPiece queryPiece = new QueryPiece(entityType, view);

        build(queryPiece);

        if (view.getChildren() != null) {
            for (View childView : view.getChildren()) {
                constructPieces(queryPiece, childView);
            }
        }
    }

    public void build(QueryPiece queryPiece)
    {
        View view = queryPiece.getView();

        this.queryTree.addVertex(queryPiece);

        // We need to handle child views
        List<String> paths = new LinkedList<>(view.getAttributeList());
        if( (paths == null || paths.isEmpty()) && view.getChildren() != null && view.getChildren().size() > 0) {
            return;
        }

        // get the StateTree instance
        TypeGraph st = view.getTypeGraph((EntityType) queryPiece.getAggregateType(), StateGraph.Scope.EDGE);

        /*
         *  Algorithm
         *  1. For each State in the StateTree we create a QueryFragment
         *  2. For each non-inverse relationship property we create a InterQuery edge. Make sure the columns on both sides are included in the select
         *  3. We join all the sub-types of i.e., _PARENT_-1 relationships [Not needed immediately]
         *  4. Add non-fetchable properties (used for query condition etc)
         *  5. Skip QueryPiece for open types
         *  6. If it is not explorable we just add the id or the path
         */




        // Also add the function attributes
        // Add them as NON-fetched properties
        paths.addAll(view.getFunctionAttributes());

        // First create a start fragment
        QueryFragment start = new QueryFragment(
            (EntityType)queryPiece.getAggregateType(),
            queryTree.nextAlias(),
            null);
        queryPiece.addVertex(start);
        pathToFragment.put(QueryFragment.ROOT_NAME, start);

        // We do not construct fragments for open types
        if(queryPiece.getAggregateType().isOpen()) {
            return;
        }

        // TODO: subtype fragments are supported only on JDBC
        // using the special _PARENT_ property

        for(String path: paths) {
            makeFragments(queryPiece, path);
        }

        // write the .dot content to log
        if(qtLogger.isDebugEnabled()) {
            StringWriter sw = new StringWriter();
            BufferedWriter bw = new BufferedWriter(sw);

            try {
                queryPiece.writeDOT(bw);
                qtLogger.debug(sw.toString());
            }
            catch (IOException e) {
                qtLogger.debug("Error in writing .dot content to log: " + e.getMessage());
            }
        }
    }

    private void makeFragments(QueryPiece queryPiece, String path) {

        if(path == null || "".equals(path.trim())) {
            return;
        }

        String[] pathSteps = path.split(Settings.PATH_DELIMITER_REGEX);
        QueryFragment fragment = pathToFragment.get(QueryFragment.ROOT_NAME);
        StringBuilder currentPath = new StringBuilder("");
        for(String step: pathSteps) {
            if (currentPath.length() > 0) {
                currentPath.append(Settings.PATH_DELIMITER);
            }
            currentPath.append(step);

            Property property = fragment.getEntityType().getProperty(step);
            if (property == null) {
                throw new RuntimeException("Unable to resolve property: " + currentPath + ", is it part of a subtype?");
            }

            if(!property.isMany()) {
                // Check if this is a leaf property
                if(property.getType().isDataType()) {
                    // add it to the current fragment
                    fragment.addPath(path);
                } else if( ((EntityType)property.getType()).isEmbedded() ) {
                    // we don't create a new fragment for embedded objects
                    continue;
                } else {
                    if(!isExplorable(queryPiece, currentPath.toString())) {
                        fragment.addPath(path);
                    } else {
                        fragment = addNewFragment(
                            property,
                            step,
                            currentPath.toString(),
                            fragment,
                            queryPiece);
                    }
                }
            }
            // handle collection properties
            else {
                Type elementType = ((ExtendedProperty)property).getElementType();

                // Is this a simple collection
                if(elementType.isDataType()) {
                    fragment.addSimpleCollectionPath(path);
                } else {
                    // create a new fragment
                    fragment = addNewFragment(property, step, currentPath.toString(), fragment, queryPiece);
                }
            }

            // fragment can be null if the property is not queryable
            if(fragment == null) {
                return;
            }
        }
    }

    private QueryFragment addNewFragment(Property property, String step, String currentPath, QueryFragment fragment, QueryPiece queryPiece) {
        EntityType type = property.isMany() ?
            (EntityType)((ExtendedProperty)property).getElementType() : (EntityType)property.getType();

        QueryFragment next = null;
        if(!pathToFragment.containsKey(currentPath)) {
            next = new QueryFragment(type, queryTree.nextAlias(), currentPath);
            pathToFragment.put(currentPath, next);

            // connect this new query fragment
            IntraQuery edge = new IntraQuery(step, fragment, next, property);
            queryPiece.addEdge(edge, fragment, next);
        } else {
            next = pathToFragment.get(currentPath.toString());
        }

        return next;
    }

    /**
     * If the type is not explorable then we just select the property representing that type.
     *
     * @param qp is the QueryPiece containing the path
     * @param currentPath is the property to check
     * @return true if the attribute can be referenced using an alias, i.e., it is explorable
     */
    private boolean isExplorable(QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp, String currentPath) {

        EntityType entityType = qp.getRoot().getEntityType();
        Property property = entityType.getProperty(currentPath);
        if(property != null) {
            Type type = property.getType();
            if(EntityType.class.isAssignableFrom(type.getClass()) && !((EntityType)type).isExplorable()) {
                return false;
            }
        }

        return true;
    }
}
