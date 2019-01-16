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
import tools.xor.util.Constants;
import tools.xor.util.IntraQuery;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
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
    private QueryTree queryTree;

    public FragmentBuilder(QueryTree queryTree) {
        this.queryTree = queryTree;
    }

    /**
     * Builds a QueryPiece.
     * @param entityType of root
     */
    public void build(EntityType entityType) {
        QueryPiece queryPiece = new QueryPiece(entityType);
        build(queryPiece);
    }

    public void build(QueryPiece queryPiece) {
        List<String> paths = queryTree.getView().getAttributeList();

        // Also add the function attributes
        paths.addAll(queryTree.getView().getFunctionAttributes());

        // First create a start fragment
        QueryFragment start = new QueryFragment(
            (EntityType)queryPiece.getAggregateType(),
            queryTree.nextAlias(),
            null);
        queryPiece.addVertex(start);
        pathToFragment.put(QueryFragment.ROOT_NAME, start);

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

        this.queryTree.addVertex(queryPiece);
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
                throw new RuntimeException("Unable to resolve property: " + currentPath);
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
