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

package tools.xor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;

/**
 * An OpenType represents a custom type that is a composition of properties from other
 * persistence managed types.
 */
public class JDBCType extends AbstractType {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private String name;
    private JDBCDataModel.TableInfo tableInfo;
    private Property id;
    private Property version;
    private Map<String, String> pathToColumnMap;

    public JDBCType(String name, JDBCDataModel.TableInfo tableInfo) {
        super();

        this.name = name;
        this.tableInfo = tableInfo;
        this.pathToColumnMap = new HashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    public String getTableName() {
        return this.tableInfo.getName();
    }

    public JDBCDataModel.TableInfo getTableInfo() {
        return this.tableInfo;
    }

    public List<String> getPrimaryKeys() {
        return this.tableInfo.getPrimaryKeys();
    }
    
    @Override
    public List <Property> getDeclaredProperties() {
        Map<String, Property> propertyMap = getShape().getDeclaredProperties(this);
        if(propertyMap == null) {
            return null;
        }

        return new ArrayList<>(propertyMap.values());
    }    

    @Override
    public void defineProperties (Shape shape)
    {
        if(!createdSuperTypeProperties()) {
            return;
        }

        JDBCDataModel das = (JDBCDataModel)getShape().getDataModel();
        // Key is column name and value is java type
        for(JDBCDataModel.ColumnInfo column: das.getTable(getTableName()).getBasicColumns()) {
            Type propertyType = getShape().getType(column.getType());
            List<JDBCDataModel.ColumnInfo> columns = new LinkedList<>();
            columns.add(column);
            JDBCProperty property = new JDBCProperty(column.getName(), columns, propertyType, this);

            shape.addProperty(property);
        }

        // For each foreign key add a relationship property
        JDBCDataModel.TableInfo table = das.getTable(getTableName());
        List<JDBCDataModel.ForeignKey> fkeys = table.getForeignKeys();
        JDBCDataModel.ForeignKey parentFK = table.getParentFK();
        if(fkeys != null) {
            for (JDBCDataModel.ForeignKey fkey : fkeys) {
                Type propertyType = getShape().getType(fkey.getReferencedTable().getName());
                List<JDBCDataModel.ColumnInfo> columns = fkey.getReferencingTable().getColumnInfo(fkey.getReferencingColumns());

                if(fkey != parentFK) {
                    JDBCProperty property = new JDBCProperty(
                        fkey.getPropertyName(),
                        columns,
                        propertyType,
                        this,
                        fkey);

                    shape.addProperty(property);
                }
            }
        }

        setIdentifierProperty();
    }

    @Override
    public void initEnd(Shape shape) {
        setColumnMap();
    }

    private void setIdentifierProperty() {
        if (this.tableInfo.getPrimaryKeys() != null) {
            if (this.tableInfo.getPrimaryKeys().size() == 1) {
                String propertyName = this.tableInfo.getPrimaryKeys().get(0);
                this.id = getProperty(propertyName);
                ((AbstractProperty)this.id).setIdentifier(true);
            } else {
                /* Currently we don't support a compound primary key property
                 *
                 * Possible to have multi-column foreign keys subsumed in the primary key
                 * So if that relationship is initialized those fields are populated with values
                 * So setting the compound primary key value needs to consider this
                 * 
                 * primary key: <col1><col2><col3><col4>
                 * fk1: <col1><col2>
                 * fk2: <col3>
                 * 
                 * In the above example, the property representing col4 needs to be set to set the 
                 * primary key, in addition to the properties modeled by fk1 and fk2
                 * 
                 */
                String[] keys = new String[this.tableInfo.getPrimaryKeys().size()];
                setNaturalKey(this.tableInfo.getPrimaryKeys().toArray(keys));
            }
        }
    }

    private void setColumnMap() {
        if(this.tableInfo.getPrimaryKeys() != null) {

            Map<String, Map<String, String>> columnMap = new HashMap<>();
            populateNestedKeyMap(columnMap, "");

            // columnMap should be fully populated and every key has a single value
            for(Map.Entry<String, Map<String, String>> entry: columnMap.entrySet()) {
                this.pathToColumnMap.put(entry.getKey(), entry.getValue().values().iterator().next());
            }

            // Possible to have multi-column foreign keys subsumed in the primary key
            Set<String> primaryKeySet = new HashSet<>(this.tableInfo.getPrimaryKeys());

            if(this.tableInfo.getForeignKeys() != null) {
                Set<String> foreignKeySet = new HashSet<>();
                for (JDBCDataModel.ForeignKey fk : this.tableInfo.getForeignKeys()) {
                    if (primaryKeySet.containsAll(fk.getReferencingColumns())) {
                        foreignKeySet.addAll(fk.getReferencingColumns());
                    }
                }
                if (foreignKeySet.size() > 0) {
                    // Subtract all the columns used up by the foreign keys
                    primaryKeySet.removeAll(foreignKeySet);
                }
            }

            for(String simpleKey: primaryKeySet) {
                this.pathToColumnMap.put(simpleKey, simpleKey);
            }
        }
    }

    public void populateNestedKeyMap(Map<String, Map<String, String>> columnMap, String prefix) {

        Map<String, String> fkMap = new HashMap<>();
        if(columnMap.containsKey(prefix)) {
            fkMap = columnMap.get(prefix);
        }

        List<String> keys = new LinkedList<>();
        if(getNaturalKey() != null) {
            keys.addAll(getNaturalKey());
        } else if(getIdentifierProperty() != null) {
            keys.add(getIdentifierProperty().getName());
        }

        for(String propertyName: keys) {
            JDBCProperty property = (JDBCProperty)ClassUtil.getDelegate(getProperty(propertyName));

            String childPrefix = (prefix.length() > 0 ? (prefix + Settings.PATH_DELIMITER) : "") + propertyName;
            Map<String, String> childFkMap = new HashMap<>();
            columnMap.put(childPrefix, childFkMap);
            if(!property.getType().isDataType()) {
                JDBCDataModel.ForeignKey fk = property.getForeignKey();

                for(int i = 0; i < fk.getReferencingColumns().size(); i++) {
                    // We key by the column at the current depth
                    // the value is the desired column and that does not change
                    if(fkMap.containsKey(fk.getReferencingColumns().get(i))) {
                        childFkMap.put(fk.getReferencedColumns().get(i),
                            fkMap.get(fk.getReferencingColumns().get(i)));
                    } else {
                        childFkMap.put(fk.getReferencedColumns().get(i),
                            fk.getReferencingColumns().get(i));
                    }
                }

                ((JDBCType)property.getType()).populateNestedKeyMap(columnMap, childPrefix);
            } else {
                if(fkMap != null && fkMap.containsKey(propertyName)) {
                    columnMap.put(childPrefix, fkMap);
                } else {
                    childFkMap.put(propertyName, propertyName);
                }
            }
        }
        if(columnMap.containsKey(prefix)) {
            columnMap.remove(prefix);
        }
    }

    public void setOpposite(Shape shape) {
        for(Property property: getProperties()) {
            ((JDBCProperty)ClassUtil.getDelegate(property)).initMappedBy(shape);
        }
    }

    @Override
    public String getEntityName() {
        return getName();
    }

    @Override
    public void defineSubtypes(List<Type> types) {
        subTypes = new HashSet<>();

        for(Type type: types) {
            if(type instanceof EntityType) {
                if(type.isOpen()) {
                    continue;
                }

                EntityType superType = ((EntityType)type).getParentType();
                while(superType != null) {
                    if(superType == this) {
                        subTypes.add(type.getName());
                        break;
                    }
                    superType = superType.getParentType();
                }
            }
        }
    }

    @Override
    public void defineChildTypes() {
        childTypes = new HashSet<>();

        for(EntityType type: getSubtypes()) {
            if(type.isOpen()) {
                continue;
            }
            if(type.getParentType() == this) {
                childTypes.add(type.getName());
            }
        }
    }

    @Override
    public List<Type> getEmbeddableTypes() {
        return new ArrayList<Type>();
    }

    @Override
    public String getURI() {
        return null;
    }

    @Override
    public Class<?> getInstanceClass() {
        return JSONObject.class;
    }

    @Override
    public boolean isInstance(Object object) {
        return Map.class.isAssignableFrom(object.getClass());
    }

    @Override
    public void initPositionProperty(Shape shape) {}

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isSequenced() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public List<?> getAliasNames() {
        return new ArrayList<String>();
    }

    @Override
    public List<?> getInstanceProperties() {
        return new ArrayList<Object>();
    }

    @Override
    public Object get(Property property) {
        return null;
    }

    @Override
    public AccessType getAccessType() {
        throw new UnsupportedOperationException("JDBC type does not define an access mechanism");
    }

    @Override
    public Property getIdentifierProperty() {
        return this.id;
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    public void setIdentifierProperty(String name) {
        this.id = getShape().getProperty(this, name);
    }

    public void setVersionProperty(String name) {
        this.version = getShape().getProperty(this, name);
    }

    @Override
    public Property getVersionProperty() {
        return this.version;
    }

    @Override
    public boolean supportsDynamicUpdate() {
        return true;
    }

    public String getColumn (String path)
    {
        if(!path.contains(Settings.PATH_DELIMITER)) {
            return path;
        }

        return this.pathToColumnMap.get(path);
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(String.format("Entity type: %s", getName()) );
        
        for(Property p: getProperties()) {
            builder.append(String.format("\n     %s", p.toString()));
        }
        
        return builder.toString();        
    }
}
