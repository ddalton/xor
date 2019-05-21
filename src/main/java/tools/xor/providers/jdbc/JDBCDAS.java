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
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.service.AbstractDataAccessService;
import tools.xor.service.DASFactory;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.PersistenceType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        private Class javaType;
        private String dataType;
        private boolean nullable;
        private boolean generated; // Does INSERT/UPDATE need to populate this column
        private int length;

        public String getName() {
            return this.name;
        }

        public Class getType() {
            return this.javaType;
        }

        public String getDataType() {
            return this.dataType;
        }

        public boolean isNullable() {
            return this.nullable;
        }

        public ColumnInfo(String name, boolean nullable, Class javaType, String dataType, Boolean generated, int length) {
            this.name = name;
            this.nullable = nullable;
            this.javaType = javaType;
            this.dataType = dataType;
            this.generated = generated;
            this.length = length;
        }

        public boolean isGenerated() {
            return this.generated;
        }

        public int getLength() {
            return this.length;
        }
    }

    public static class SequenceInfo {
        private String name;
        private String dataType;
        private long min;
        private long max;
        private int incrementBy;
        private long startWith;
        private boolean cycle;

        public SequenceInfo(String name,
                            String dataType,
                            long min,
                            long max,
                            int incrementBy,
                            long startWith,
                            boolean cycle) {
            this.name = name;
            this.dataType = dataType;
            this.min = min;
            this.max = max;
            this.incrementBy = incrementBy;
            this.startWith = startWith;
            this.cycle = cycle;
        }

        public String getName() {
            return this.name;
        }
    }

    public static class TableInfo {
        private String name;
        private List<ColumnInfo> columns;
        private List<String> primaryKeys;
        private List<ForeignKey> foreignKeys;

        public String getName() {
            return this.name;
        }

        public void setColumns(List<ColumnInfo> columns) {
            this.columns = columns;
        }

        public List<ColumnInfo> getColumnInfo(List<String> columnssubset) {
            Map<String, ColumnInfo> columnInfoMap = new HashMap<>();
            for(ColumnInfo ci: columns) {
                columnInfoMap.put(ci.getName(), ci);
            }

            List<ColumnInfo> result = new LinkedList<>();
            for(String column: columnssubset) {
                if(columnInfoMap.containsKey(column)) {
                    result.add(columnInfoMap.get(column));
                }
            }

            return result;
        }

        public TableInfo(String name) {
            this.name = name;
        }

        public void setPrimaryKeys(List<String> primaryKeys) {
            this.primaryKeys = primaryKeys;
        }

        public List<ForeignKey> getForeignKeys() {
            return this.foreignKeys;
        }

        public void setForeignKeys(List<ForeignKey> foreignKeys) {
            this.foreignKeys = foreignKeys;
        }

        public List<String> getPrimaryKeys() {
            return this.primaryKeys;
        }

        public void initNoPrimaryKey() {
            // All the fields comprise to form the primary key
            List<String> keys = new LinkedList<>();
            for(ColumnInfo ci: columns) {
                keys.add(ci.getName());
            }

            this.primaryKeys = keys;
        }

        public List<ColumnInfo> getBasicColumns() {
            Map<String, ColumnInfo> all = new HashMap<>();
            for(ColumnInfo ci: columns) {
                all.put(ci.getName(), ci);
            }

            Set<String> fkColumns = new HashSet<>();
            if(foreignKeys != null) {
                for (ForeignKey fk : foreignKeys) {
                    fkColumns.addAll(fk.getReferencingColumns());
                }
            }

            Set<String> basicColumns = new HashSet(all.keySet());
            basicColumns.removeAll(fkColumns);

            List<ColumnInfo> result = new LinkedList<>();
            for(String basicCol: basicColumns) {
                result.add(all.get(basicCol));
            }

            return result;
        }
    }

    public static enum ForeignKeySide {
        REFERENCED,
        REFERENCING,
        UNKNOWN
    }

    public static enum ForeignKeyRule {
        CASCADE,
        RESTRICT,
        NO_ACTION,
        SET_NULL,
        SET_DEFAULT
    }

    public static class ForeignKey {
        private TableInfo referencingTable;      // table representing source of the relationship
        private TableInfo referencedTable;       // table representing target of the relationship
        private String name;                     // name of the foreign key.
        private List<String> referencingColumns;
        private List<String> referencedColumns;
        private ForeignKeyRule deleteRule;
        private ForeignKeyRule updateRule;
        private boolean inheritance;             // Does this foreign key model an inheritance
                                                 //   relationship?

        public String getName() {
            return this.name;
        }

        /**
         * Represents the name of the JDBCProperty representing this foreign key relationship.
         *
         * If the foreign key is comprised of a single column, then the property name is the
         * the name of the column, else the property name is the name of the foreign key,
         * or a name overridden by the user (ForeignKeyEnhancer).
         *
         * @return
         */
        public String getPropertyName() {
            if(referencedColumns.size() == 1) {
                return referencedColumns.get(0);
            }

            return name;
        }

        public TableInfo getReferencingTable() {
            return this.referencingTable;
        }

        public TableInfo getReferencedTable() {
            return this.referencedTable;
        }

        public ForeignKeySide getSide(JDBCType type) {
            if(type.getTableName().equals(this.referencedTable)) {
                return ForeignKeySide.REFERENCED;
            } else if(type.getTableName().equals(this.referencingTable)) {
                return ForeignKeySide.REFERENCING;
            } else {
                return ForeignKeySide.UNKNOWN;
            }
        }

        public ForeignKey(String name, TableInfo referencing, TableInfo referenced, ForeignKeyRule deleteRule, ForeignKeyRule updateRule) {
            this.name = name;
            this.referencingTable = referencing;
            this.referencedTable = referenced;
            this.deleteRule = deleteRule;
            this.updateRule = updateRule;
        }

        public List<String> getReferencingColumns() {
            return this.referencingColumns;
        }

        public void setReferencingColumns(List<String> columns) {
            this.referencingColumns = columns;
        }

        public List<String> getReferencedColumns() {
            return this.referencedColumns;
        }

        public void setReferencedColumns(List<String> columns) {
            this.referencedColumns = columns;
        }

        public boolean isContainment() {
            return deleteRule == ForeignKeyRule.CASCADE;
        }

        public boolean isInheritance() {
            return this.inheritance;
        }

        public void init() {
            if(this.referencingTable.getPrimaryKeys() != null && this.referencedTable.getPrimaryKeys() != null) {
                if (this.referencingTable.getPrimaryKeys().equals(this.referencingColumns) &&
                    this.referencedTable.getPrimaryKeys().equals(this.referencedColumns)) {
                    this.inheritance = true;
                }
            }
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
    public TableInfo getTable(String tableName) {
        try(Connection c = getDataSource().getConnection()) {
            return DBTranslator.instance(c).getTable(getAggregateManager().getForeignKeyEnhancer(), tableName);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public Map<String, List<String>> getPrimaryKeys() {
        try(Connection c = getDataSource().getConnection()) {
            return DBTranslator.instance(c).getPrimaryKeys();
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public List<TableInfo> getTables() {
        try(Connection c = getDataSource().getConnection()) {
            List<TableInfo> tables = DBTranslator.instance(c).getTables(getAggregateManager().getForeignKeyEnhancer());
            return tables;
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public abstract DataSource getDataSource();

    @Override
    public Type getType(String name) {
        // We ignore case
        return getShape().getType(name.toUpperCase());
    }

    @Override public void addShape (String name)
    {
        Shape shape = getOrCreateShape(name);

        for(TableInfo table: getTables()){
            JDBCType dataType = new JDBCType(table.getName(), table);
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

        // Create and Link the bi-directional relationship between the properties
        for(Type type: shape.getUniqueTypes()) {
            if(JDBCType.class.isAssignableFrom(type.getClass())) {
                JDBCType jdbcType = (JDBCType) type;
                jdbcType.setOpposite(shape);
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
        // do nothing
    }

    @Override public Class<?> getNarrowedClass (Class<?> entityClass, String viewName)
    {
        return entityClass;
    }

    @Override public PersistenceOrchestrator createPO (Object sessionContext, Object data)
    {
        if(sessionContext == null) {
            sessionContext = new JDBCSessionContext();
        }
        JDBCPersistenceOrchestrator po = new JDBCPersistenceOrchestrator(sessionContext, data);
        po.setDataSource(getDataSource());

        return po;
    }

}

