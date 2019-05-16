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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import tools.xor.providers.jdbc.JDBCDAS;
import tools.xor.service.Shape;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An OpenType represents a custom type that is a composition of properties from other
 * persistence managed types.
 */
public class JDBCType extends AbstractType {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private String name;
    private JDBCDAS.TableInfo tableInfo;
    private Property id;

    public JDBCType(String name, JDBCDAS.TableInfo tableInfo) {
        super();

        this.name = name;
        this.tableInfo = tableInfo;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getTableName() {
        return this.tableInfo.getName();
    }

    public void defineProperties (Shape shape)
    {
        // If the properties are already defined then return
        if (shape.getProperties(this) != null) {
            return;
        }

        JDBCDAS das = (JDBCDAS)getDAS();
        // Key is column name and value is java type
        for(JDBCDAS.ColumnInfo column: das.getTable(getTableName()).getBasicColumns()) {
            Type propertyType = getDAS().getType(column.getType());
            List<JDBCDAS.ColumnInfo> columns = new LinkedList<>();
            columns.add(column);
            JDBCProperty property = new JDBCProperty(column.getName(), columns, propertyType, this);

            shape.addProperty(property);
        }

        // For each foreign key add a relationship property
        List<JDBCDAS.ForeignKey> fkeys = das.getTable(getTableName()).getForeignKeys();
        if(fkeys != null) {
            for (JDBCDAS.ForeignKey fkey : fkeys) {
                Type propertyType = getDAS().getType(fkey.getReferencedTable().getName());
                List<JDBCDAS.ColumnInfo> columns = fkey.getReferencingTable().getColumnInfo(fkey.getReferencingColumns());
                JDBCProperty property = new JDBCProperty(
                    fkey.getPropertyName(),
                    columns,
                    propertyType,
                    this);

                shape.addProperty(property);
            }
        }


        if(this.tableInfo.getPrimaryKeys() != null) {
            if(this.tableInfo.getPrimaryKeys().size() == 1) {
                String propertyName = this.tableInfo.getPrimaryKeys().get(0);
                this.id = getProperty(propertyName);
            } else {
                String[] keys = this.tableInfo.getPrimaryKeys().stream().toArray(String[]::new);
                setNaturalKey(keys);
            }
        }
    }

    public void setOpposite(Shape shape) {
        for(Property property: getProperties()) {
            ((JDBCProperty)property).initMappedBy(shape);
        }
    }

    @Override
    public String getEntityName() {
        return getName();
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
    public Property getProperty(String path) {
        return getDAS().getShape().getProperty(this, path);
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
    public List<Type> getBaseTypes() {
        return new ArrayList<Type>();
    }

    @Override
    public List<Property> getDeclaredProperties() {
        return getProperties();
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

    @Override
    public Property getVersionProperty() {
        return null;
    }

    @Override
    public boolean supportsDynamicUpdate() {
        return true;
    }

}
