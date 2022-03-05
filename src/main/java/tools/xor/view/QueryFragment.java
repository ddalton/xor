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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.util.IntraQuery;
import tools.xor.util.Vertex;

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
    public static final String INVOCATION_ID_PARAM = "INVOCATION_ID_";
    public static final String PARENT_INVOCATION_ID_PARAM = "PARENT_INVOCATION_ID_";
    public static final String LAST_PARENT_ID_PARAM = "LAST_PARENT_ID_";    

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
        this.queryFields = new LinkedList<>();
        this.pathToFieldMap = new HashMap<>();
    }

    public QueryFragment copy() {
        QueryFragment result = new QueryFragment(this.entityType, this.alias, this.ancestorPath);

        result.paths.addAll(this.paths);
        result.simpleCollectionPaths.addAll(this.simpleCollectionPaths);

        return result;
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
        String anchorPath = getAnchorPath();
        return path.substring(anchorPath.length());
    }

    public boolean containsPath(String path) {
        return paths.contains(path) || simpleCollectionPaths.contains(path);
    }

    public void addPath(String path) {
        this.paths.add(path);
    }

    public void addFullPath(String path) {
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

    @Override public String getDisplayName() {
        return getAlias() + " [" + this.entityType.getName() + "]";
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

    public String getFullPath(String path) {
        return getAnchorPath() + path;
    }

    private boolean isUserAttribute(Set<String> attributePaths, String path) {
        return attributePaths.contains(getFullPath(path));
    }

    public static String extractAnchorPath(String path) {
        if(StringUtils.isEmpty(path)) {
            return path;
        }
        assert(path.contains(Settings.PATH_DELIMITER));

        return path.substring(0, path.lastIndexOf(Settings.PATH_DELIMITER)+1);
    }

    public String getAnchorPath() {
        String anchorPath = getAncestorPath();
        if(StringUtils.isEmpty(anchorPath)) {
            anchorPath = "";
        } else if(!anchorPath.endsWith(Settings.PATH_DELIMITER)) {
            anchorPath += Settings.PATH_DELIMITER;
        }

        return anchorPath;
    }

    /**
     * Create the QueryField instances for this fragment and initialize it starting with
     * the given position.
     *
     * @param position from which the fields are present
     * @param settings describing the result structure (SHARED or DISTINCT)
     * @param queryTree instance that contains this fragment
     * @return the updated position
     */
    public int generateFields(int position, Settings settings, QueryTree queryTree) {

        // Number all the existing fields if any (for e.g., created as part of InterQuery initialization
        // to help with reconstitution)
        for(QueryField field: queryFields) {
            field.setPosition(position++);
        }

        // A QueryTree is per view
        Set<String> attributePaths = new HashSet<>(queryTree.getView().getAttributeList());

        if(queryTree.getView().isCustom()) {
            for (String path : this.paths) {
                position += addField(new QueryField(path, position, this)) ? 1 : 0;
            }
        } else {
            for (String path : this.paths) {
                if (isUserAttribute(attributePaths, path)) {
                    position += addField(new QueryField(path, position, this)) ? 1 : 0;
                }
            }

            for (String path : this.simpleCollectionPaths) {
                if (isUserAttribute(attributePaths, path)) {
                    position += addField(new QueryField(path, position, this)) ? 1 : 0;
                }
            }
        }

        // Add queryFields for all the primary keys fields, if not already present
        String anchorPath = getAnchorPath();
        if(entityType.getIdentifierProperty() != null) {
            String idPath = anchorPath + entityType.getIdentifierProperty().getName();
            if(!attributePaths.contains(idPath)) {
                position += addField(new QueryField(entityType.getIdentifierProperty().getName(), position, this, false)) ? 1 : 0;
            }
        } else if(entityType.getNaturalKey() != null) {
            //for(String key: entityType.getNaturalKey()) {
            for(String key: entityType.getExpandedNaturalKey()) {
                String keyPath = anchorPath + key;
                if(!attributePaths.contains(keyPath)) {
                    position += addField(new QueryField(key, position, this, false)) ? 1 : 0;
                }
            }
        }

        // Add the id if we need to share the object in the result
        ObjectResolver.Type type = settings.getResolverType();
        assert type != null : "Except a value for resolver type";

        if(!queryTree.getView().isCustom()) {
            Iterator<IntraQuery<QueryFragment>> iter = queryTree.getInEdges(this).iterator();

            if (iter.hasNext()) {
                IntraQuery<QueryFragment> incomingEdge = iter.next();
                ExtendedProperty property = (ExtendedProperty)incomingEdge.getProperty();
                if (property != null && property.isMany() && !Settings.doSQL(settings.getDataStore())) {
                    if (property.isList()) {
                        // add INDEX
                        position += addField(
                            new QueryField(
                                LIST_INDEX_ATTRIBUTE,
                                position,
                                this,
                                true)) ? 1 : 0;
                    }
                    else if (property.isMap()) {
                        // add KEY
                        position += addField(
                            new QueryField(
                                MAP_KEY_ATTRIBUTE,
                                position,
                                this,
                                true)) ? 1 : 0;
                    }
                    // add COLLECTION_USERKEY
                    if (property.getCollectionKey() != null) {
                        for (String key : property.getCollectionKey()) {
                            position += addField(new QueryField(key, position, this, true)) ?
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
                            position,
                            this,
                            true)) ? 1 : 0;
                }

                if(augmenterSet.contains(getFullPath(ENTITY_TYPE_ATTRIBUTE))) {
                    position += addField(
                        new QueryField(
                            ENTITY_TYPE_ATTRIBUTE,
                            position,
                            this,
                            true)) ? 1 : 0;
                }

                if(augmenterSet.contains(getFullPath(ID_PARAMETER_NAME))) {
                    position += addField(
                        new QueryField(
                            getEntityType().getIdentifierProperty().getName(),
                            position,
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

    public String getIdPath() {
        return getFullPath(getEntityType().getIdentifierProperty().getName());
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

    // TODO: skip fields that are in the parent QueryFragment in an inheritance hierarchy
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
     * @return field that was added else return null
     */
    public QueryField checkAndAddId ()
    {
        String idPropertyName = getEntityType().getIdentifierProperty().getName();
        if(!paths.contains(idPropertyName)) {
            // The position will be set at the time of field generation
            QueryField newField = new QueryField(idPropertyName, -1, this, true);
            if (addField(newField)) {
                return newField;
            }
        }

        return null;
    }

    public List<String> getPrimaryKeyFieldNames() {
        List<String> result = new LinkedList<>();

        if(entityType.getIdentifierProperty() != null) {
            result.add(entityType.getIdentifierProperty().getName());
        } else {
            if(entityType instanceof JDBCType) {
                if(((JDBCType)entityType).getPrimaryKeys() != null) {
                    result.addAll(((JDBCType)entityType).getPrimaryKeys());
                }
            } else {
                result.addAll(entityType.getExpandedNaturalKey());
            }
        }

        return result;
    }
}
