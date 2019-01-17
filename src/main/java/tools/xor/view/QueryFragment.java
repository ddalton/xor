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
import tools.xor.Settings;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.util.Vertex;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryFragment implements Vertex
{
    public static final String ROOT_NAME = "<ROOT>";
    public static final String ENTITY_TYPE_ATTRIBUTE  = "TYPE_";
    public static final String MAP_KEY_ATTRIBUTE     = "KEY_";
    public static final String LIST_INDEX_ATTRIBUTE  = "INDEX_";
    public static final String USERKEY_ATTRIBUTE     = "USERKEY_";
    public static final String ID_PARAMETER_NAME     = "id_";
    public static final String NEXTTOKEN_PARAM_PREFIX = "orderBy_";

    EntityType entityType;               // Entity type for this fragment
    String ancestorPath;                 // path from root of the QueryTree
    String alias;                        // alias used in the query string
    List<String> paths;                  // attributes of simple types
    List<String> simpleCollectionPaths;  // attributes of collections of simple types
    List<QueryField> queryFields;        // refers to fields that are retrieved from database
    Map<String, QueryField> pathToFieldMap; // optimization
    int simpleCollectionCount;   // count of all simple collections within a fragment and its
                                 //  descendants
    int parallelCollectionCount; // max parallel collections within a fragment in the descendants,
                                 // this count includes simple collections

    public EntityType getEntityType ()
    {
        return entityType;
    }

    public String getAlias ()
    {
        return alias;
    }

    public QueryFragment(EntityType type, String alias, String ancestorPath) {
        this.entityType = type;
        this.alias = alias;
        this.ancestorPath = ancestorPath;
        this.paths = new LinkedList<>();
        this.simpleCollectionPaths = new LinkedList<>();
        this.queryFields = new LinkedList<>();
    }

    public int getSimpleCollectionCount() {
        return this.simpleCollectionCount;
    }

    public void setSimpleCollectionCount (int simpleCollectionCount)
    {
        this.simpleCollectionCount = simpleCollectionCount;
    }

    public String getAncestorPath() {
        return this.ancestorPath;
    }

    @Override public String getName ()
    {
        return this.ancestorPath == null ? ROOT_NAME : this.ancestorPath;
    }

    private String stripAncestor(String path) {
        if(ancestorPath != null && path.startsWith(ancestorPath)) {
            return path.substring(ancestorPath.length()+Settings.PATH_DELIMITER.length());
        } else {
            return path;
        }
    }

    public void addPath(String path) {
        this.paths.add(stripAncestor(path));
    }

    public void addSimpleCollectionPath (String path) {
        this.paths.add(stripAncestor(path));
    }

    public void removeSimpleCollectionPath (String path) {
        this.paths.remove(path);
    }

    @Override public String toString() {
        return getAlias();
    }

    public List<String> getPaths() {
        return Collections.unmodifiableList(this.paths);
    }

    public List<String> getSimpleCollectionPaths() {
        return Collections.unmodifiableList(this.simpleCollectionPaths);
    }

    public int getParallelCollectionCount ()
    {
        return parallelCollectionCount;
    }

    public void setParallelCollectionCount (int parallelCollectionCount)
    {
        this.parallelCollectionCount = parallelCollectionCount;
    }

    public List<QueryField> getQueryFields() {
        return Collections.unmodifiableList(this.queryFields);
    }

    private void clearFields() {
        this.queryFields = new LinkedList<>();
        this.pathToFieldMap = new HashMap<>();
    }

    public String getFullPath(String path) {
        return ((ancestorPath == null) ? "" : (ancestorPath + Settings.PATH_DELIMITER)) + path;
    }

    private boolean isUserAttribute(Set<String> attributePaths, String path) {
        return attributePaths.contains(getFullPath(path));
    }

    /**
     * Create the QueryField instances for this fragment and initialize it starting with
     * the given position.
     *
     * @param position from which the fields are present
     * @param settings describing the result structure (SHARED or DISTINCT)
     * @return the updated position
     */
    public int generateFields(int position, Settings settings, QueryPiece queryPiece, QueryTree queryTree) {
        clearFields();

        // We should not create QueryField instances for fields that are only
        // referenced from functions
        Set<String> attributePaths = new HashSet<>(queryTree.getAttributes(queryTree.getView()));

        for(String path: this.paths) {
            if(isUserAttribute(attributePaths, path)) {
                queryFields.add(new QueryField(path, position++, this));
            }
        }
        for(String path: this.simpleCollectionPaths) {
            if(isUserAttribute(attributePaths, path)) {
                queryFields.add(new QueryField(path, position++, this));
            }
        }

        // Add the id if we need to share the object in the result
        ObjectResolver.Type type = settings.getResolverType();
        assert type != null : "Except a value for resolver type";

        if(type == ObjectResolver.Type.SHARED) {
            // add surrogate key
            String idName = getEntityType().getIdentifierProperty().getName();
            if(!paths.contains(idName)) {
                queryFields.add(new QueryField(idName, position++, this, true));
            }

            // add USERKEY
            if( getEntityType().getNaturalKey() != null ) {
                for(String key: getEntityType().getExpandedNaturalKey()) {
                    if(!paths.contains(key)) {
                        queryFields.add(new QueryField(key, position++, this, true));
                    }
                }
            }
        }

        Iterator<IntraQuery<QueryFragment>> iter = queryPiece.getInEdges(this).iterator();

        if(iter.hasNext()) {
            IntraQuery<QueryFragment> incomingEdge = iter.next();
            ExtendedProperty property = (ExtendedProperty)incomingEdge.getProperty();
            if (incomingEdge.getProperty().isMany()) {
                if (property.isList()) {
                    // add INDEX
                    queryFields.add(new QueryField(LIST_INDEX_ATTRIBUTE, position++, this, true));
                }
                else if (property.isMap()) {
                    // add KEY
                    queryFields.add(new QueryField(MAP_KEY_ATTRIBUTE, position++, this, true));
                }
                // add COLLECTION_USERKEY
                if (property.getCollectionKey() != null) {
                    for (String key : property.getCollectionKey()) {
                        if (!paths.contains(key)) {
                            queryFields.add(new QueryField(key, position++, this, true));
                        }
                    }
                }
            }
        }

        for(QueryField field: queryFields) {
            pathToFieldMap.put(field.getPath(), field);
        }

        return position;
    }

    public String getId() {
        return getAlias() + Settings.PATH_DELIMITER + getEntityType().getIdentifierProperty().getName();
    }

    public QueryField getField(String path) {
        if(pathToFieldMap.containsKey(path)) {
            return pathToFieldMap.get(path);
        }

        return null;
    }
}
