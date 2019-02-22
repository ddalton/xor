/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
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

package tools.xor.providers.jdbc;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.JDBCType;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.service.AbstractDataAccessService;
import tools.xor.service.AggregateManager;
import tools.xor.service.DASFactory;
import tools.xor.service.DataAccessService;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.PersistenceType;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryTransformer;
import tools.xor.view.View;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the Data Access Service framework.
 * It allows DB specific build of the object structure based on the tables, columns
 * and foreign keys
 *
 * dotted notation join syntax has two containment specific behavior (CASCADE DELETE)
 * 1. If the foreign key is between 2 tables connecting their primary keys
 *    then that relationship represents an inheritance relationship
 *    NOTE: A foreign key between two primary keys is supported on both
 *          Oracle and HANA
 * 2. Else it represents a containment relationship between 2 entities
 *
 * @author Dilip Dalton
 */
public abstract class JDBCDAS extends AbstractDataAccessService
{

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public static class ColumnInfo {
        private String name;
        private Class type;
        private boolean nullable;

        public String getName() {
            return this.name;
        }

        public Class getType() {
            return this.type;
        }

        public boolean isNullable() {
            return this.nullable;
        }

        public ColumnInfo(String name, boolean nullable, Class type) {
            this.name = name;
            this.nullable = nullable;
            this.type = type;
        }
    }

    public JDBCDAS(DASFactory dasFactory, TypeMapper typeMapper) {
        super(dasFactory, typeMapper);
    }

    /**
     * Return the columns and their corresponding JAVA types from the database
     *
     * @param tableName RDBMS table name
     * @return map of columns and their types
     */
    public List<ColumnInfo> getColumns(String tableName) {
        try(Connection c = getDataSource().getConnection()) {
            return DBTranslator.instance(c).getColumns(tableName);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public List<String> getTables() {
        try(Connection c = getDataSource().getConnection()) {
            return DBTranslator.instance(c).getTables();
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public abstract DataSource getDataSource();

    @Override public void addShape (String name)
    {
        Shape shape = getOrCreateShape(name);

        for(String tableName: getTables()){
            JDBCType dataType = new JDBCType(tableName, tableName);
            shape.addType(dataType.getName(), dataType);
        }

        // TODO: supertypes can be defined as foreign keys between the primary keys of
        // TODO: 2 tables

        // Define the properties for the Types
        // This will end up defining the simple types
        defineProperties(shape);

        postProcess(shape);
    }

    protected void defineProperties(Shape shape) {
        for(Type type: shape.getUniqueTypes()) {
            if(JDBCType.class.isAssignableFrom(type.getClass())) {
                JDBCType jdbcType = (JDBCType) type;
                jdbcType.defineProperties(shape);
            }
        }
    }

    @Override
    public PersistenceType getAccessType ()
    {
        return PersistenceType.JDBC;
    }

    @Override public void populateNarrowedClass (Class<?> entityClass, TypeNarrower typeNarrower)
    {
        throw new UnsupportedOperationException("A JDBCType does not have an associated Java class");
    }

    @Override public Class<?> getNarrowedClass (Class<?> entityClass, String viewName)
    {
        throw new UnsupportedOperationException("A JDBCType does not have an associated Java class");
    }

    @Override public PersistenceOrchestrator createPO (Object sessionContext, Object data)
    {
        JDBCPersistenceOrchestrator po = new JDBCPersistenceOrchestrator(sessionContext, data);
        po.setDataSource(getDataSource());

        return po;
    }

}

