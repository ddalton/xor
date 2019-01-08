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

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.IntraQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentBuilder
{
    Map<String, QueryFragment> pathToFragment = new HashMap<>();
    int aliasCounter = 0;

    /**
     * Builds a QueryPiece.
     * @param entityType of root
     * @param paths of all that data that needs to be queried
     */
    public QueryPiece build(EntityType entityType, List<String> paths) {
        QueryPiece queryPiece = new QueryPiece(entityType);

        // First create a start fragment
        QueryFragment start = new QueryFragment(entityType, nextAlias(), null);
        queryPiece.addVertex(start);
        pathToFragment.put(QueryFragment.ROOT_NAME, start);

        for(String path: paths) {
            makeFragments(queryPiece, path);
        }

        return queryPiece;
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
                    fragment = addNewFragment(property, step, currentPath.toString(), fragment, queryPiece);
                }
            }
            // handle collection properties
            else {
                Type elementType = ((ExtendedProperty)property).getElementType();

                // Is this a simple collection
                if(elementType.isDataType()) {
                    fragment.addPath(path);
                    fragment.incrementSimpleCollectionCount();
                } else {
                    // create a new fragment
                    fragment = addNewFragment(property, step, currentPath.toString(), fragment, queryPiece);
                }
            }
        }
    }

    private QueryFragment addNewFragment(Property property, String step, String currentPath, QueryFragment fragment, QueryPiece queryPiece) {
        EntityType type = property.isMany() ?
            (EntityType)((ExtendedProperty)property).getElementType() : (EntityType)property.getType();

        QueryFragment next = null;
        if(!pathToFragment.containsKey(currentPath)) {
            next = new QueryFragment(type, nextAlias(), currentPath);
            pathToFragment.put(currentPath, next);

            // connect this new query fragment
            IntraQuery edge = new IntraQuery(step, fragment, next, property);
            queryPiece.addEdge(edge, fragment, next);
        } else {
            next = pathToFragment.get(currentPath.toString());
        }

        return next;
    }

    private String nextAlias() {
        return QueryTree.ENTITY_ALIAS_PREFIX + aliasCounter++;
    }
}
