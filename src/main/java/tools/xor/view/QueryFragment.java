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

import org.apache.commons.lang.StringUtils;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCProperty;
import tools.xor.JDBCType;
import tools.xor.Settings;
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
    public static final String ID_PARAMETER_NAME     = "ID_";
    public static final String NEXTTOKEN_PARAM_PREFIX = "ORDER_BY_";
    public static final String PARENT_INLIST = "PARENT_INLIST_";

    public static final Set<String> systemFields = new HashSet<>();

    static {
        systemFields.add(ENTITY_TYPE_ATTRIBUTE);
        systemFields.add(MAP_KEY_ATTRIBUTE);
        systemFields.add(LIST_INDEX_ATTRIBUTE);
        systemFields.add(USERKEY_ATTRIBUTE);
        systemFields.add(ID_PARAMETER_NAME);
        systemFields.add(NEXTTOKEN_PARAM_PREFIX);
    }

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

        clearFields();
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
        if(ancestorPath != null && !"".equals(ancestorPath) && path.startsWith(ancestorPath)) {
            return path.substring(ancestorPath.length()+Settings.PATH_DELIMITER.length());
        } else {
            return path;
        }
    }

    public boolean containsPath(String path) {
        return paths.contains(path) || simpleCollectionPaths.contains(path);
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
        return (ancestorPath == null || "".equals(ancestorPath)) ? path : (ancestorPath + Settings.PATH_DELIMITER) + path;
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
    public int generateFields(int position, Settings settings, QueryTree queryTree, AggregateTree aggregateTree) {
        clearFields();

        // We should not create QueryField instances for fields that are only
        // referenced from functions
        Set<String> attributePaths = new HashSet<>(aggregateTree.getView().getAttributes());

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
        // Add queryFields for all the primary keys fields, if not already present
        String anchorPath = StringUtils.isEmpty(getAncestorPath()) ? "" : (getAncestorPath() + Settings.PATH_DELIMITER);
        if(entityType.getIdentifierProperty() != null) {
            String idPath = anchorPath + entityType.getIdentifierProperty().getName();
            if(!attributePaths.contains(idPath)) {
                position += addField(new QueryField(entityType.getIdentifierProperty().getName(), position++, this, false)) ? 1 : 0;
            }
        } else if(entityType.getNaturalKey() != null) {
            //for(String key: entityType.getNaturalKey()) {
            for(String key: entityType.getExpandedNaturalKey()) {
                String keyPath = anchorPath + key;
                if(!attributePaths.contains(keyPath)) {
                    position += addField(new QueryField(key, position++, this, false)) ? 1 : 0;
                }
            }
        }

        for(QueryField field: queryFields) {
            pathToFieldMap.put(field.getPath(), field);
        }

        // Add the id if we need to share the object in the result
        ObjectResolver.Type type = settings.getResolverType();
        assert type != null : "Except a value for resolver type";

        if(!queryTree.getView().hasUserQuery()) {
            Iterator<IntraQuery<QueryFragment>> iter = queryTree.getInEdges(this).iterator();

            if (iter.hasNext()) {
                IntraQuery<QueryFragment> incomingEdge = iter.next();
                ExtendedProperty property = (ExtendedProperty)incomingEdge.getProperty();
                if (incomingEdge.getProperty().isMany() && !Settings.doSQL(settings.getPersistenceOrchestrator())) {
                    if (property.isList()) {
                        // add INDEX
                        position += addField(
                            new QueryField(
                                LIST_INDEX_ATTRIBUTE,
                                position++,
                                this,
                                true)) ? 1 : 0;
                    }
                    else if (property.isMap()) {
                        // add KEY
                        position += addField(
                            new QueryField(
                                MAP_KEY_ATTRIBUTE,
                                position++,
                                this,
                                true)) ? 1 : 0;
                    }
                    // add COLLECTION_USERKEY
                    if (property.getCollectionKey() != null) {
                        for (String key : property.getCollectionKey()) {
                            position += addField(new QueryField(key, position++, this, true)) ?
                                1 :
                                0;
                        }
                    }
                }
            }
        } else {
            QuerySupport qs = queryTree.getQuerySupport();

            if(qs != null && qs.getAugmenter() != null) {
                Set<String> augmenterSet = new HashSet<>(qs.getAugmenter());

                // If collection
                if(augmenterSet.contains(getFullPath(LIST_INDEX_ATTRIBUTE))) {
                    position += addField(
                        new QueryField(
                            LIST_INDEX_ATTRIBUTE,
                            position++,
                            this,
                            true)) ? 1 : 0;
                }

                if(augmenterSet.contains(getFullPath(MAP_KEY_ATTRIBUTE))) {
                    position += addField(
                        new QueryField(
                            MAP_KEY_ATTRIBUTE,
                            position++,
                            this,
                            true)) ? 1 : 0;
                }

                if(augmenterSet.contains(getFullPath(ENTITY_TYPE_ATTRIBUTE))) {
                    position += addField(
                        new QueryField(
                            ENTITY_TYPE_ATTRIBUTE,
                            position++,
                            this,
                            true)) ? 1 : 0;
                }

                if(augmenterSet.contains(getFullPath(ID_PARAMETER_NAME))) {
                    position += addField(
                        new QueryField(
                            getEntityType().getIdentifierProperty().getName(),
                            position++,
                            this,
                            true)) ? 1 : 0;
                }
            }
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

    public QueryField getIdField() {
        String idPropertyName = getEntityType().getIdentifierProperty().getName();
        return pathToFieldMap.get(idPropertyName);
    }

    private boolean addField(QueryField field) {
        boolean added = false;

        if(!pathToFieldMap.containsKey(field.getPath())) {
            queryFields.add(field);
            pathToFieldMap.put(field.getPath(), field);
            added = true;
        }

        return added;
    }

    /**
     * Add the identifier property if it is not present
     * @param atPosition
     * @return field that was added else return null
     */
    public QueryField checkAndAddId (int atPosition)
    {
        String idPropertyName = getEntityType().getIdentifierProperty().getName();
        QueryField newField = new QueryField(idPropertyName, atPosition, this, true);
        if(addField(newField)) {
            return newField;
        }

        return null;
    }

    public List<String> getPrimaryKeyFieldNames() {
        List<String> result = new LinkedList<>();

        if(entityType.getIdentifierProperty() != null) {
            result.add(entityType.getIdentifierProperty().getName());
        } else {
            if(entityType instanceof JDBCType) {
                result.addAll(((JDBCType)entityType).getPrimaryKeys());
            } else {
                result.addAll(entityType.getExpandedNaturalKey());
            }
        }

        return result;
    }
}
