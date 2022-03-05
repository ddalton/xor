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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.JDBCType;
import tools.xor.RelationshipType;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.service.AbstractDataModel;
import tools.xor.service.DataModelFactory;
import tools.xor.service.DataStore;
import tools.xor.service.PersistenceProvider;
import tools.xor.service.SchemaExtension;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;

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
public abstract class JDBCDataModel extends AbstractDataModel
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

        public List<ColumnInfo> getColumns() {
            return Collections.unmodifiableList(columns);
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


        public ForeignKey getParentFK() {

            ForeignKey parentFK = null;
            if(this.foreignKeys != null) {
                next: for (ForeignKey fk : foreignKeys) {
                    if(fk.getReferencingColumns().size() == primaryKeys.size()) {
                        for(int i = 0; i < primaryKeys.size(); i++) {
                            String keyPart = primaryKeys.get(i);
                            if(!keyPart.equals(fk.getReferencingColumns().get(i))) {
                                continue next;
                            }
                        }

                        // also check that the referenced end is the primary key of the
                        // referenced table
                        TableInfo rt = fk.getReferencedTable();
                        if(fk.getReferencedColumns().size() == rt.getPrimaryKeys().size()) {
                            for(int i = 0; i < primaryKeys.size(); i++) {
                                String keyPart = rt.getPrimaryKeys().get(i);
                                if(!keyPart.equals(fk.getReferencedColumns().get(i))) {
                                    continue next;
                                }
                            }
                        }
                        parentFK = fk;
                        break;
                    }
                }
            }

            return parentFK;
        }

        /**
         * Return the name of the referenced table where
         * there is a foreign key between the 2 primary keys
         * @return parent table name
         */
        public String getParentTable() {
            ForeignKey parentFK = getParentFK();

            if(parentFK != null) {
                return parentFK.getReferencedTable().getName();
            }

            return null;
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

            // basic columns should always contain the identifier property
            if(primaryKeys != null && primaryKeys.size() == 1) {
                basicColumns.addAll(primaryKeys);
            }
            for(String basicCol: basicColumns) {
                result.add(all.get(basicCol));
            }

            return result;
        }
    }

    public static enum ForeignKeyRule {
        CASCADE,
        RESTRICT,
        NO_ACTION,
        SET_NULL,
        SET_DEFAULT
    }

    public static class ForeignKey {
         /* Delimiter to get inverse relationship name
          * Useful to rename relationships
          * format:
          *   Needs to format the below format if the name contains either _1__1_ or
          *   _1__N_
          *   <unique prefix>_1__1_<relationship name>__<inverse entity relationship name>
          *   <unique prefix>_1__N_<relationship name>__<inverse collection relationship name>
          *
          * unique prefix - A prefix to uniquely identify this foreign key.
          * relationship name - Represents the user facing foreign key relationship name.
          * inverse relationship name - Represents the collection relationship name if _1__N_ else
          *                     represents the entity relationship name (_1__1_)
          * NOTE: all 3 parts are required for a multi-column foreign key relationship
          */
        private static final String DELIM = "__";
        private static final String TO_ONE = "_1__1_";
        private static final String TO_MANY = "_1__N_";

        private String nameInDatabase; // original foreign key name
        private TableInfo referencingTable;      // table representing source of the relationship
        private TableInfo referencedTable;       // table representing target of the relationship
        private String name;                     // name of the foreign key.
        private String inverseName;
        private List<String> referencingColumns;
        private List<String> referencedColumns;
        private RelationshipType type = RelationshipType.TO_MANY; // default
        private ForeignKeyRule deleteRule;
        private ForeignKeyRule updateRule;
        private boolean composition;
        private boolean inheritance;             // Does this foreign key model an inheritance
                                                 //   relationship?

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(String.format("Foreign key: %s [logical name: %s]", nameInDatabase, name) );
            
            builder.append(String.format("\n  ReferencingTable: %s ---> Referenced Table: %s", referencingTable.getName(), referencedTable.getName()));
            for(int i = 0; i < referencingColumns.size(); i++) {
                builder.append(String.format("\n     Key: %s ---> %s", referencingColumns.get(i), referencedColumns.get(i)));
            }
            
            return builder.toString();
        }
        
        public String getName() {
            return this.name;
        }

        public String getInverseRelationshipName() {
            return this.inverseName;
        }

        /**
         * Represents the name of the JDBCProperty representing this foreign key relationship.
         *
         * If the foreign key is comprised of a single column, then the property name is the
         * the name of the column, else the property name is the name of the foreign key,
         * or a name overridden by the user (ForeignKeyEnhancer).
         *
         * @return column name as property name
         */
        public String getPropertyName() {
            if(referencingColumns.size() == 1) {
                return referencingColumns.get(0);
            }

            return name;
        }

        public TableInfo getReferencingTable() {
            return this.referencingTable;
        }

        public TableInfo getReferencedTable() {
            return this.referencedTable;
        }

        public ForeignKey(String name, TableInfo referencing, TableInfo referenced, ForeignKeyRule deleteRule, ForeignKeyRule updateRule) {
            this.nameInDatabase = name;
            this.referencingTable = referencing;
            this.referencedTable = referenced;
            this.deleteRule = deleteRule;
            this.updateRule = updateRule;

            this.name = this.nameInDatabase;
            if(this.nameInDatabase.indexOf(TO_ONE) != -1 || this.nameInDatabase.indexOf(TO_MANY) != -1) {
                if(this.nameInDatabase.indexOf(TO_ONE) != -1) {
                    this.type = RelationshipType.TO_ONE;
                    parseNames(this.nameInDatabase.substring(this.nameInDatabase.indexOf(TO_ONE)+TO_ONE.length()));
                } else {
                    parseNames(this.nameInDatabase.substring(this.nameInDatabase.indexOf(TO_MANY)+TO_MANY.length()));
                }
            }
        }

        private void parseNames(String fkname) {
            if(fkname.indexOf(DELIM) != -1) {
                this.name = fkname.substring(0, fkname.indexOf(DELIM));
                this.inverseName = fkname.substring(fkname.indexOf(DELIM)+DELIM.length());
            } else {
                this.inverseName = fkname;
            }
        }

        public RelationshipType getType() {
            return this.type;
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

        public boolean isComposition() {
            return this.composition;
        }

        public void init() {
            if(this.referencingTable.getPrimaryKeys() != null && this.referencedTable.getPrimaryKeys() != null) {
                if (this.referencingTable.getPrimaryKeys().equals(this.referencingColumns) &&
                    this.referencedTable.getPrimaryKeys().equals(this.referencedColumns)) {

                    // @see initComposition() difference
                    this.inheritance = true;
                }
            }
        }

        /**
         * The primary key is the same between two tables, but they do not participate in
         * a inheritance relationship.
         * So the PK values just needs to be copied to the referencing table from the
         * referenced table instead of it being generated.
         */
        public void makeComposition() {
            this.setReferencingColumns(this.referencingTable.getPrimaryKeys());
            this.setReferencedColumns(this.referencedTable.getPrimaryKeys());

            this.composition = true;
        }
    }

    public JDBCDataModel(DataModelFactory dasFactory, TypeMapper typeMapper) {
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
            return DBTranslator.instance(c).getTable(c, getAggregateManager().getForeignKeyEnhancer(), tableName);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public Map<String, List<String>> getPrimaryKeys() {
        try(Connection c = getDataSource().getConnection()) {
            return DBTranslator.instance(c).getPrimaryKeys(c);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public List<TableInfo> getTables() {
        try(Connection c = getDataSource().getConnection()) {
            List<TableInfo> tables = DBTranslator.instance(c).getTables(c, getAggregateManager().getForeignKeyEnhancer());
            return tables;
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public List<TableInfo> getRelationalTables() {
        try(Connection c = getDataSource().getConnection()) {
            List<TableInfo> tables = DBTranslator.instance(c).getTables(c, getAggregateManager().getForeignKeyEnhancer());

            for(TableInfo table: tables) {
                table.setForeignKeys(null);
            }
            return tables;
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public abstract DataSource getDataSource();

    @Override
    public Type getType(Shape shape, String typeName, Type type) {
        // The typeName is JSONObject, so it is better to use type as fallback
        // as it is more specific
        return type;
    }

    @Override public Shape createShape (String name, SchemaExtension extension, Shape.Inheritance typeInheritance)
    {
        Shape shape = super.createShape(name, extension, typeInheritance);

        List<TableInfo> tables = name.equals(RELATIONAL_SHAPE) ? getRelationalTables() : getTables();

        for(TableInfo table: tables){
            JDBCType dataType = new JDBCType(table.getName(), table);
            shape.addType(dataType.getName(), dataType);
        }

        for(TableInfo table: tables) {
            setSuperType(table, shape);
        }

        // Define the properties for the Types
        // This will end up defining the simple types
        defineProperties(shape, shape.getUniqueTypes());

        postProcess(shape, extension, shape.getUniqueTypes(), false);

        return shape;
    }
    
	private List<TableInfo> defineTypes(Shape shape, Set<String> entityNames) {
		
        List<TableInfo> tables = shape.getName().equals(RELATIONAL_SHAPE) ? getRelationalTables() : getTables();
        
        List<TableInfo> filteredTables = new ArrayList();
		if(entityNames != null && !entityNames.isEmpty()) {

			Map<String, TableInfo> providerEntityMap = new HashMap<>();
			for(TableInfo entityType: tables) {
				providerEntityMap.put(entityType.getName(), entityType);
			}
			
			for(String entityName: entityNames) {
				if(providerEntityMap.containsKey(entityName)) {
					filteredTables.add(providerEntityMap.get(entityName));
				}
			}
		} else {
			filteredTables = tables;
		}
		
        for(TableInfo table: filteredTables){
            JDBCType dataType = new JDBCType(table.getName(), table);
            shape.addType(dataType.getName(), dataType);
        }			
        
        return filteredTables;
	}    
	
	@Override public void processShape(Shape shape, SchemaExtension extension, Set<String> entityNames) {
		
		List<TableInfo> tables = defineTypes(shape, entityNames);
		
		// Only if the table is denormalized do we do this
        for(TableInfo table: tables) {
            setSuperType(table, shape);
        }

        // Define the properties for the Types
        // This will end up defining the simple types
        defineProperties(shape, shape.getUniqueTypes());

        postProcess(shape, extension, shape.getUniqueTypes(), false);		
	}	

    private void setSuperType(TableInfo table, Shape shape) {
        String parentName = table.getParentTable();
        if(parentName != null) {
            JDBCType child = (JDBCType)shape.getType(table.getName());
            JDBCType parent = (JDBCType)shape.getType(parentName);
            child.setParentType(parent);
        }
    }

    public void addNewTypes(Shape shape) {
        String name = shape.getName();

        List<TableInfo> tables = name.equals(RELATIONAL_SHAPE) ? getRelationalTables() : getTables();
        List<TableInfo> newTables = new ArrayList<>();
        List<Type> newTypes = new ArrayList<>();
        for(TableInfo table: tables) {
            if(shape.getType(table.getName()) != null) {
                continue;
            }

            newTables.add(table);
            JDBCType dataType = new JDBCType(table.getName(), table);
            shape.addType(dataType.getName(), dataType);
            newTypes.add(dataType);
        }

        for(TableInfo newTable: newTables) {
            setSuperType(newTable, shape);
        }

        // Create the properties for the new types
        defineProperties(shape, newTypes);

        // We don't do a full post-process as not all steps are necessary
        postProcess(shape, null, newTypes, true);
    }

    protected void defineProperties(Shape shape, Collection<Type> types) {
        for(Type type: types) {
            if(JDBCType.class.isAssignableFrom(type.getClass())) {
                JDBCType jdbcType = (JDBCType) type;
                jdbcType.defineProperties(shape);
            }
        }

        // Create and Link the bi-directional relationship between the properties
        for(Type type: types) {
            if(JDBCType.class.isAssignableFrom(type.getClass())) {
                JDBCType jdbcType = (JDBCType) type;
                jdbcType.setOpposite(shape);
            }
        }
    }
    
    @Override
    public PersistenceProvider getPersistenceProvider() {
        if(super.getPersistenceProvider() == null) {
            this.persistenceProvider = new PersistenceProvider() {
                @Override
                public DataStore createDS(Object sessionContext, Object data) {
                    JDBCDataStore po = new JDBCDataStore((JDBCSessionContext)sessionContext, data);
                    po.setDataSource(getDataSource());

                    return po;
                } 
            };
        }
       
        return this.persistenceProvider;
    }     
}

