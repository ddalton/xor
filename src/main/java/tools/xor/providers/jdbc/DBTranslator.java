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

package tools.xor.providers.jdbc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.DataGenerator;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCProperty;
import tools.xor.JDBCType;
import tools.xor.JSONObjectProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.service.AbstractDataStore;
import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.BindParameter;
import tools.xor.view.QueryJoinAction;

/**
 * Responsible for interacting with various RDBMS implementations.
 * Useful to convert values between a specific DB type and JAVA.
 */
public abstract class DBTranslator
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    
    private final static Map<String, DBTranslator> translators = new ConcurrentHashMap<>();
    private final static Map<String, DBType> dbTypeByProductName = new ConcurrentHashMap<>();

    private static final String H2_PRODUCT_NAME = "H2";
    private static final String HANA_PRODUCT_NAME = "HDB";
    private static final String HSQLDB_PRODUCT_NAME = "HSQL DATABASE ENGINE";
    private static final String POSTGRESQL_PRODUCT_NAME = "POSTGRESQL";

    static {
        translators.put(H2_PRODUCT_NAME, new H2Translator() );        
        translators.put(HANA_PRODUCT_NAME, new HANATranslator() );
        translators.put(HSQLDB_PRODUCT_NAME, new HSQLTranslator() );
        translators.put(POSTGRESQL_PRODUCT_NAME, new PGTranslator() );

        dbTypeByProductName.put(H2_PRODUCT_NAME, DBType.H2);
        dbTypeByProductName.put(HANA_PRODUCT_NAME, DBType.HANA);
        dbTypeByProductName.put(HSQLDB_PRODUCT_NAME, DBType.HSQLDB);
        dbTypeByProductName.put(POSTGRESQL_PRODUCT_NAME, DBType.POSTGRESQL);
    }

    protected static final Map<String, Class> SQL_TO_JAVA_TYPE_MAP = new HashMap<>();
    protected static final Map<String, JDBCtoSQLConverter> convertersByDataType = new ConcurrentHashMap<>();
    protected static final Map<String, JDBCtoSQLConverter> csvConvertersByDataType = new ConcurrentHashMap<>();

    static {
        SQL_TO_JAVA_TYPE_MAP.put("CHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("VARCHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("NVARCHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("LONGVARCHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("NUMERIC", java.math.BigDecimal.class);
        SQL_TO_JAVA_TYPE_MAP.put("DECIMAL", java.math.BigDecimal.class);
        SQL_TO_JAVA_TYPE_MAP.put("BIT", Boolean.class);
        SQL_TO_JAVA_TYPE_MAP.put("BOOLEAN", Boolean.class);
        SQL_TO_JAVA_TYPE_MAP.put("TINYINT", Byte.class);
        SQL_TO_JAVA_TYPE_MAP.put("SMALLINT", Short.class);
        SQL_TO_JAVA_TYPE_MAP.put("INTEGER", Integer.class);
        SQL_TO_JAVA_TYPE_MAP.put("BIGINT", Long.class);
        SQL_TO_JAVA_TYPE_MAP.put("REAL", Float.class);
        SQL_TO_JAVA_TYPE_MAP.put("DOUBLE", Double.class);
        SQL_TO_JAVA_TYPE_MAP.put("BINARY", byte[].class);
        SQL_TO_JAVA_TYPE_MAP.put("VARBINARY", byte[].class);
        SQL_TO_JAVA_TYPE_MAP.put("BYTEA", byte[].class);
        SQL_TO_JAVA_TYPE_MAP.put("LONGVARBINARY", byte[].class);
        SQL_TO_JAVA_TYPE_MAP.put("DATE", java.sql.Date.class);
        SQL_TO_JAVA_TYPE_MAP.put("TIME", java.sql.Time.class);
        SQL_TO_JAVA_TYPE_MAP.put("TIMESTAMP", java.sql.Timestamp.class);
        SQL_TO_JAVA_TYPE_MAP.put("BLOB", java.sql.Blob.class);
        SQL_TO_JAVA_TYPE_MAP.put("CLOB", java.sql.Clob.class);
        SQL_TO_JAVA_TYPE_MAP.put("SQLXML", java.sql.SQLXML.class);
    }

    protected Map<String, JDBCDataModel.TableInfo> tableMap;

    public static DBTranslator getTranslator(String name) {
        return translators.get(name);
    }

    protected Class getJavaClass (String sqlType) {
        return SQL_TO_JAVA_TYPE_MAP.get(sqlType);
    }

    public BindParameter.SQLConverter getSQLConverter(int type) {

        // TODO: Move from BindParameter to here
        return null;
    }

    public interface JDBCtoSQLConverter {
        public String toSQLLiteral (Object value);
    }

    static {

        JDBCtoSQLConverter charConverter = new JDBCtoSQLConverter()
        {
            @Override public String toSQLLiteral (Object value)
            {
                if(value == null) return "NULL";
                return "'" + value.toString().replace("'", "''") + "'";
            }
        };
        convertersByDataType.put("CHAR", charConverter);
        convertersByDataType.put("VARCHAR", charConverter);
        convertersByDataType.put("NVARCHAR", charConverter);
        convertersByDataType.put("LONGVARCHAR", charConverter);
        convertersByDataType.put("CLOB", charConverter);

        JDBCtoSQLConverter bigDecimalConverter = new JDBCtoSQLConverter()
        {
            @Override public String toSQLLiteral (Object value)
            {
                if(value == null) return "NULL";
                if(value instanceof String) {
                    return (String) value;
                } else if(value instanceof BigDecimal) {
                    return value.toString();
                } else {
                    throw new RuntimeException("Unsupported value type for BigDecimal converter");
                }
            }
        };
        convertersByDataType.put("NUMERIC", bigDecimalConverter);
        convertersByDataType.put("DECIMAL", bigDecimalConverter);

        JDBCtoSQLConverter booleanConverter = new JDBCtoSQLConverter()
        {
            @Override public String toSQLLiteral (Object value)
            {
                if(value == null) return "NULL";
                if(value instanceof String) {
                    return (String) value;
                } else if(value instanceof Boolean) {
                    return value.toString();
                } else {
                    throw new RuntimeException("Unsupported value type for boolean converter");
                }
            }
        };
        convertersByDataType.put("BIT", booleanConverter);
        convertersByDataType.put("BOOLEAN", booleanConverter);

        convertersByDataType.put(
            "TINYINT", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof String) {
                        return (String) value;
                    } else if(value instanceof Byte || value instanceof Number) {
                        return value.toString();
                    } else {
                        throw new RuntimeException("Unsupported value type for TINYINT converter");
                    }
                }
            });

        JDBCtoSQLConverter integerConverter = new JDBCtoSQLConverter()
        {
            @Override public String toSQLLiteral (Object value)
            {
                if(value == null) return "NULL";
                if(value instanceof String) {
                    return (String) value;
                } else if(value instanceof Integer || value instanceof Number) {
                    return value.toString();
                } else {
                    throw new RuntimeException("Unsupported value type for Integer converter");
                }
            }
        };
        convertersByDataType.put("SMALLINT", integerConverter);
        convertersByDataType.put("INTEGER", integerConverter);

        convertersByDataType.put(
            "BIGINT", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof String) {
                        return (String) value;
                    } else if (value instanceof Long || value instanceof Number) {
                        return value.toString();
                    } else {
                        throw new RuntimeException("Unsupported value type for BIGINT converter");
                    }
                }
            });

        convertersByDataType.put(
            "REAL", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof String) {
                        return (String) value;
                    } else if(value instanceof Float || value instanceof Number) {
                        return value.toString();
                    } else {
                        throw new RuntimeException("Unsupported value type for Float converter");
                    }
                }
            });

        convertersByDataType.put(
            "DOUBLE", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof String) {
                        return (String) value;
                    } else if(value instanceof Double || value instanceof Number) {
                        return value.toString();
                    } else {
                        throw new RuntimeException("Unsupported value type for Double converter");
                    }
                }
            });

        // Convert to string -> byte array -> hex
        JDBCtoSQLConverter binaryConverter = new JDBCtoSQLConverter()
        {
            @Override public String toSQLLiteral (Object value)
            {
                if(value == null) return "NULL";
                if(value instanceof String) {
                    value = ((String)value).getBytes();
                }
                if(value instanceof byte[]) {
                    return String.format("%x", (byte[])value);
                } else {
                    throw new RuntimeException("Unsupported value type for binary converter");
                }
            }
        };
        convertersByDataType.put("BINARY", binaryConverter);
        convertersByDataType.put("VARBINARY", binaryConverter);
        convertersByDataType.put("LONGVARBINARY", binaryConverter);
        convertersByDataType.put("BLOB", binaryConverter);

        convertersByDataType.put(
            "DATE", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_DATE);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return String.format("CAST ('%s' AS date)", df.format(value));
                    } else {
                        throw new RuntimeException("Unsupported value type for Date converter");
                    }
                }
            });

        convertersByDataType.put(
            "TIME", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_TIME);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return String.format("CAST ('%s' AS time)", df.format(value));
                    } else {
                        throw new RuntimeException("Unsupported value type for Time converter");
                    }
                }
            });

        convertersByDataType.put(
            "TIMESTAMP", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ANSI_FORMAT_DATETIME);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return String.format("CAST ('%s' AS datetime)", df.format(value));
                    } else {
                        throw new RuntimeException("Unsupported value type for Timestamp converter");
                    }
                }
            });

        convertersByDataType.put(
            "SQLXML", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    return value.toString();
                }
            });
    }

    static {
        csvConvertersByDataType.put(
            "DATE", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_DATE);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return df.format(value);
                    } else {
                        throw new RuntimeException("Unsupported value type for Date converter");
                    }
                }
            });

        csvConvertersByDataType.put(
            "TIME", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_TIME);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return df.format(value);
                    } else {
                        throw new RuntimeException("Unsupported value type for Time converter");
                    }
                }
            });

        csvConvertersByDataType.put(
            "TIMESTAMP", new JDBCtoSQLConverter()
            {
                @Override public String toSQLLiteral (Object value)
                {
                    SimpleDateFormat df = new SimpleDateFormat(JSONObjectProperty.ANSI_FORMAT_DATETIME);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return df.format(value);
                    } else {
                        throw new RuntimeException("Unsupported value type for Timestamp converter");
                    }
                }
            });
    }

    /**
     * Can be overridden as necessary
     * @param dataType for which the appropriate converter is to be found
     * @return converter
     */
    protected JDBCtoSQLConverter getConverter(String dataType) {
        return convertersByDataType.get(dataType);
    }

    protected JDBCtoSQLConverter getCSVConverter(String dataType) {
        if(csvConvertersByDataType.containsKey(dataType)) {
            return csvConvertersByDataType.get(dataType);
        }

        return getConverter(dataType);
    }

    public static DBTranslator instance(Connection conn) {
        DBTranslator result;
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            String productName = metadata.getDatabaseProductName().toUpperCase();
            result = getTranslator(productName);

            if(result == null) {
                throw new RuntimeException("Unable to find DBTranslator for product: " + productName);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
    }

    public static DBType getDBType(Connection conn) {
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            String productName = metadata.getDatabaseProductName().toUpperCase();
            logger.info(String.format("Connected to DB: %s", productName));
            return dbTypeByProductName.get(productName);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public static DBTranslator getTranslator(Statement stmt) {
        try {
            return getTranslator(stmt.getConnection());
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public static DBTranslator getTranslator(Connection conn) {
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            String productName = metadata.getDatabaseProductName().toUpperCase();
            return DBTranslator.getTranslator(productName);
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public void setIdentifier(Settings settings, BusinessObject bo, JDBCType entityType) {

        // Check if the business object is a collection element
        JDBCProperty containerProperty = null;
        BusinessObject container = null;
        if(bo.getContainmentProperty() == null) {
            // Go to the parent if possible
            if(bo.getContainer() != null) {
                if (bo.getContainer() != null) {
                    containerProperty = (JDBCProperty)ClassUtil.getDelegate(bo.getContainer().getContainmentProperty());
                    container = (BusinessObject)bo.getContainer().getContainer();
                }
            }
        } else {
            containerProperty = (JDBCProperty)ClassUtil.getDelegate(bo.getContainmentProperty());
            container = (BusinessObject)bo.getContainer();
        }

        // Case 1: This is not a containment object, so generate an id
        if(!(container != null && containerProperty != null && containerProperty.doPropagateId())) {
            StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(
                null,
                settings,
                null);

            // Check if the identifier column has been populated
            ExtendedProperty identifierProperty = (ExtendedProperty)entityType.getIdentifierProperty();

            if (identifierProperty != null && !identifierProperty.isGenerated()) {
                Serializable id = (Serializable)identifierProperty.getValue(bo);
                if (id == null || "".equals(id.toString())) {
                    Object value = ((BasicType)identifierProperty.getType()).generate(
                        settings,
                        identifierProperty,
                        null,
                        null,
                        visitor);
                    bo.set(identifierProperty, value);
                }
            }
        }
        // Case 2: Propagate the id
        else {
            containerProperty.propagateId(container, bo);
        }
    }

    private List<Property> getProperties(EntityType entityType, DataGenerator dataGenerator) {
        List<Property> properties = entityType.getDeclaredProperties();
        if(dataGenerator != null) {
            properties = dataGenerator.getGeneratedFields(entityType);
        }

        return properties;
    }

    private boolean shouldSkip(BusinessObject bo, Property p, DataGenerator dataGenerator) {
        if(((ExtendedProperty)p).isGenerated()) {
            return true;
        }

        if(bo.get(p) == null && dataGenerator == null) {
            return true;
        }

        if(!((ExtendedProperty)p).isUpdatable()) {
            return true;
        }

        return false;
    }

    /**
     * Gets a list of all the properties
     * @param entityType containing the properties, can be a supertype
     * @param bo for which the relevant properties need to be extracted
     * @return properties to udate
     */
    private List<Property> getPropertiesToUpdate(JDBCType entityType, BusinessObject bo) {

        // Excludes the identifier
        // add version property at end if type supports this property

        Property identifier = entityType.getIdentifierProperty();
        Property version = entityType.getVersionProperty();

        if(identifier == null) {
            throw new RuntimeException("Only update of entities containing identifier property is currently supported");
        }

        List<Property> result = new LinkedList<>();

        // Needed for performance optimization for objects containing a large number of fields in the type
        JSONObject jsonObject = (JSONObject)bo.getInstance();
        Iterator<String> propertyNames = jsonObject.keys();
        Map<String, Property> directProperties = entityType.getShape().getDeclaredProperties(entityType);
        while(propertyNames.hasNext()) {
            Property property = directProperties.get(propertyNames.next());
            if(property == null) {
                continue;
            }

            if(property == identifier || (version != null && property == version)) {
                continue;
            }
            result.add(property);
        }
        if(version != null) {
            result.add(version);
        }

        return result;
    }

    /**
     * Generate an update statement that works with optimistic concurrency control and no
     * @param entityType type
     * @param bo Object containing the values to be updated
     * @param isBindParameters false if a literal SQL fragment needs to be produced
     * @return sql string without values
     *
     * For example:
     *   UPDATE user SET name = "new_value" WHERE name = "old_value" and id = "id_value"
     */
    public String getUpdateSqlFragment(JDBCType entityType, BusinessObject bo, boolean isBindParameters)
    {
        StringBuilder sqlstr = new StringBuilder("UPDATE ");
        sqlstr.append(bo.getType().getName()).append(" SET ");

        if(isBindParameters) {

            List<Property> properties = getPropertiesToUpdate(entityType, bo);
            List<String> propToSet = new LinkedList<>();
            for(Property p: properties) {
                propToSet.add(p.getName() + " = ?");
            }
            sqlstr.append(String.join(",", propToSet));

            sqlstr.append(" WHERE ").append(String.join(" AND ", propToSet));
            Property identifier = entityType.getIdentifierProperty();
            sqlstr.append(String.format(" AND %s = ?", identifier.getName()));
        }

        return sqlstr.toString();
    }
    
    public String getUpdateSqlFragment(JDBCType entityType, BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys)
    {
        StringBuilder sqlstr = new StringBuilder("UPDATE ");
        sqlstr.append(bo.getType().getName()).append(" SET ");

        List<String> columnsToSetSQL = new LinkedList<>();
        for(String column: columnsToSet) {
            columnsToSetSQL.add(column + " = ?");
        }
        sqlstr.append(String.join(",", columnsToSetSQL));

        // lookup keys
        List<String> lookupClause = new LinkedList<>();
        for(String column: lookupKeys.keySet()) {
            lookupClause.add(column + " = ?");
        }            
        sqlstr.append(" WHERE ").append(String.join(" AND ", lookupClause));

        return sqlstr.toString();
    }    
    
    public String getSelectStmt(JDBCType entityType, String primaryKeyColumn, Map<String, String> lookupKeys) {
        StringBuilder sqlstr = new StringBuilder("SELECT ");
        sqlstr.append(primaryKeyColumn)
        .append(" FROM ")
        .append(entityType.getName());

        // lookup keys
        List<String> lookupClause = new LinkedList<>();
        for(String column: lookupKeys.keySet()) {
            lookupClause.add(column + " = ?");
        }            
        sqlstr.append(" WHERE ").append(String.join(" AND ", lookupClause));

        return sqlstr.toString();        
    }
    
    public String setSelectValues(JDBCType entityType, PreparedStatement ps, BusinessObject bo, String primaryKeyColumn, Map<String, String> lookupKeys) {

        // set the lookup values in the WHERE predicate list
        int position = 1;
        List<String> lookupValues = new LinkedList<>();        
        for(Map.Entry<String, String> entry: lookupKeys.entrySet()) {
            Property p = entityType.getProperty(entry.getKey());
            position = setSimpleValue(ps, lookupValues, p, bo, position, false, true);            
        }        

        return null;
    }       

    private List<Property> getPropertiesForDelete(JDBCType entityType, BusinessObject bo) {
        JSONObject jsonObject = (JSONObject)bo.getInstance();
        Iterator iter = jsonObject.keys();

        List<Property> result = new LinkedList<>();
        while(iter.hasNext()) {
            String propertyName = (String)iter.next();
            Property p = entityType.getProperty(propertyName);

            if(p != null) {
                result.add(p);
            }
        }

        return result;
    }

    public String getDeleteSqlFragment(JDBCType entityType, BusinessObject bo) {
        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append("DELETE FROM " + entityType.getTableName() + " WHERE ");

        List<Property> properties = getPropertiesForDelete(entityType, bo);
        List<String> propToSet = new LinkedList<>();
        for(Property p: properties) {
            propToSet.add(p.getName() + " = ?");
        }
        sqlstr.append(String.join(" AND ", propToSet));

        return sqlstr.toString();
    }

    public String getInsertSqlFragment(JDBCType entityType, BusinessObject bo, boolean isBindParameters, DataGenerator dataGenerator) {
        // iterate through the properties
        List<String> columnNames = new LinkedList<>();
        for(Property p: getProperties(entityType, dataGenerator)) {
            if(shouldSkip(bo, p, dataGenerator)) {
                continue;
            }

            // simple type
            if(p.getType().isDataType() && !p.isMany()) {
                columnNames.add(((JDBCProperty)p).getColumns().get(0).getName());
            }

            // foreign keys
            if(!p.getType().isDataType() && p.getMappedBy() == null) {
                JDBCDataModel.ForeignKey fkey = ((JDBCProperty)p).getForeignKey();
                if(fkey == null) {
                    throw new RuntimeException("A TO_ONE relationship should have a foreign key");
                } else {
                    // get the referencing columns
                    columnNames.addAll(fkey.getReferencingColumns());
                }
            }
        }
        
        return getInsertSqlFragment(entityType, isBindParameters, columnNames);
    }
    
    public String getInsertSqlFragment(JDBCType entityType, boolean isBindParameters, List<String> columnNames) {
        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append("INSERT INTO " + entityType.getTableName() + " (");

        sqlstr.append(String.join(",", columnNames));
        sqlstr.append(") VALUES ");

        if (isBindParameters) {
            List<String> placeHolders = new LinkedList<>();
            for(int i = 0; i < columnNames.size(); i++) {
                placeHolders.add("?");
            }
            sqlstr.append("(")
                .append(String.join(",", placeHolders))
                .append(")");
        }

        return sqlstr.toString();        
    }
    

    public void setDeletePredicate(JDBCType entityType,
                                   PreparedStatement ps,
                                   BusinessObject bo) {
        int position = 1;
        for(Property p: getPropertiesForDelete(entityType, bo)) {
            position = setValue(ps, new LinkedList<>(), p, bo, position, false);
        }
    }

    public String setInsertValues (JDBCType entityType,
                                   PreparedStatement ps,
                                   BusinessObject bo,
                                   boolean isCSV,
                                   DataGenerator dataGenerator) {

        List<Property> properties = new ArrayList<>();
        for(Property p: getProperties(entityType, dataGenerator)) {
            if(shouldSkip(bo, p, dataGenerator)) {
                continue;
            }
            properties.add(p);
        }

        return setInsertValues(entityType, ps, bo, isCSV, properties, true);
    }
    
    public String setInsertValues (JDBCType entityType,
            PreparedStatement ps,
            BusinessObject bo,
            boolean isCSV,
            List<Property> properties,
            boolean modelsRelationships) {
        
        // get the values
        List<String> values = new LinkedList<>();
        int position = 1;
        for(Property p: properties) {
            if(modelsRelationships) {
                position = setValue(ps, values, p, bo, position, isCSV);
            } else {
                position = setSimpleValue(ps, values, p, bo, position, isCSV, false);                
            }
        }        
        
        if(ps == null) {
            StringBuilder sqlstr = new StringBuilder();
            sqlstr.append(String.join(",", values));

            return sqlstr.toString();
        } else {
            return null;
        }        
    }

    private int setValue(PreparedStatement ps, List<String> values, Property p, BusinessObject bo, int position, boolean isCSV) {
        return setValue(ps, values, p, bo, position, isCSV, false);
    }

    private int setValue(PreparedStatement ps, List<String> values, Property p, BusinessObject bo, int position, boolean isCSV, boolean isUpdate) {
        // simple type
        if (p.getType().isDataType() && !p.isMany()) {
            position = setSimpleValue(ps, values, p, bo, position, isCSV, isUpdate);
        }

        // foreign keys
        if(!p.getType().isDataType() && p.getMappedBy() == null) {
            JDBCDataModel.ForeignKey fkey = ((JDBCProperty)p).getForeignKey();
            JSONObject entity = (JSONObject)bo.get(p);
            List<JDBCDataModel.ColumnInfo> referencedColumns = fkey.getReferencedTable().getColumnInfo(
                fkey.getReferencedColumns());
            for (int i = 0; i < referencedColumns.size(); i++) {
                String dataType = referencedColumns.get(i).getDataType();
                JDBCDataModel.ColumnInfo col = referencedColumns.get(i);
                JDBCtoSQLConverter c = isCSV ? getCSVConverter(dataType) : getConverter(dataType);
                values.add(getColumnString(entity.get(col.getName()), isUpdate, col.getName(), c));

                if(ps != null) {
                    addBindParameter(ps, dataType, position++, entity.get(col.getName()));
                }
            }
        }

        return position;
    }
    
    private int setSimpleValue(PreparedStatement ps, List<String> values, Property p, BusinessObject bo, int position, boolean isCSV, boolean isUpdate) {
        JDBCDataModel.ColumnInfo col = ((JDBCProperty)p).getColumns().get(0);
        JDBCtoSQLConverter c = isCSV ? getCSVConverter(col.getDataType()) : getConverter(col.getDataType());
        
        Object value = bo.get(col.getName());

        // check the length and trim if necessary
        if(value != null && value instanceof String) {
            if(value.toString().length() >= col.getLength()) {
                value = value.toString().substring(0, col.getLength());
            }
            logger.debug(String.format("Setting String value '%s' on column '%s'", value.toString(), col.getName()));
        }
        values.add(getColumnString(value, isUpdate, col.getName(), c));

        if(ps != null) {
            addBindParameter(ps, col.getDataType(), position++, value);
        }
         
        return position;
    }
    
    

    private String getColumnString(Object value, boolean isUpdate, String columnName, JDBCtoSQLConverter c) {
        if(isUpdate) {
            return String.format(
                    "%s = %s",
                    columnName,
                    c.toSQLLiteral(value));
        } else {
            return c.toSQLLiteral(value);
        }
    }

    public String setUpdateValues (JDBCType entityType,
                                   PreparedStatement ps,
                                   BusinessObject bo,
                                   BusinessObject dbBO) {
        // set modified values
        Property version = entityType.getVersionProperty();
        if(version != null) {
            if(bo.get(version.getName()) == null) {
                throw new RuntimeException("Version is a required field for update");
            }
            // Ensure we increment this value in the modified object
            bo.set(version.getName(), Integer.valueOf(bo.get(version.getName()).toString()) + 1);
        }
        List<String> modifiedValues = new LinkedList<>();
        int position = 1;
        for(Property p: getPropertiesToUpdate(entityType, bo)) {
            position = setValue(ps, modifiedValues, p, bo, position, false, true);
        }

        // set original values in the WHERE predicate list
        List<String> originalValues = new LinkedList<>();
        List<Property> persistentBOproperties = getPropertiesToUpdate(entityType, dbBO);
        persistentBOproperties.add(entityType.getIdentifierProperty());
        for(Property p: persistentBOproperties) {
            position = setValue(ps, originalValues, p, dbBO, position, false, true);
        }


        if(ps == null) {
            StringBuilder sqlstr = new StringBuilder();
            sqlstr.append(String.join(", ", modifiedValues))
                .append(" WHERE ")
                .append(String.join(" AND ", originalValues));

            return sqlstr.toString();
        } else {
            return null;
        }
    }
    
    public String setUpdateValues(JDBCType entityType, PreparedStatement ps, BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys) {

        int position = 1;
        List<String> modifiedValues = new LinkedList<>();
        for(String columnToSet: columnsToSet) {
            Property p = entityType.getProperty(columnToSet);
            position = setSimpleValue(ps, modifiedValues, p, bo, position, false, true);
        }

        // set the lookup values in the WHERE predicate list
        List<String> lookupValues = new LinkedList<>();        
        for(Map.Entry<String, Object> entry: lookupKeys.entrySet()) {
            Property p = entityType.getProperty(entry.getKey());
            JDBCDataModel.ColumnInfo col = ((JDBCProperty)p).getColumns().get(0);
            position = setSimpleValue(ps, lookupValues, p, bo, position, false, true);            
        }        
        
        if(ps == null) {
            StringBuilder sqlstr = new StringBuilder();
            sqlstr.append(String.join(", ", modifiedValues))
                .append(" WHERE ")
                .append(String.join(" AND ", lookupValues));

            return sqlstr.toString();
        } else {
            return null;
        }
    }    

    private void addBindParameter(PreparedStatement ps, String type, int position, Object value) {
        BindParameter bp = BindParameter.instance(position, null);
        bp.setType(type);
        bp.setValue(ps, this, value);
    }

    public String getInsertSql(JDBCType entityType, BusinessObject bo, DataGenerator dataGenerator) {

        StringBuilder sqlstr = new StringBuilder(getInsertSqlFragment(entityType, bo, false, dataGenerator));
        sqlstr.append("(")
            .append(setInsertValues(entityType, null, bo, false, dataGenerator))
            .append(")");

        return sqlstr.toString();
    }
    
    public String getInsertSql(JDBCType entityType, BusinessObject bo, List<String> columns, List<Property> properties) {

        StringBuilder sqlstr = new StringBuilder(getInsertSqlFragment(entityType, false, columns));
        sqlstr.append("(")
            .append(setInsertValues(entityType, null, bo, false, properties, false))
            .append(")");

        return sqlstr.toString();
    }    

    public String getCSV(JDBCType entityType, BusinessObject bo, DataGenerator dataGenerator) {

        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append(setInsertValues(entityType, null, bo, true, dataGenerator));

        return sqlstr.toString();
    }
    
    public String getCSV(JDBCType entityType, BusinessObject bo, List<Property> properties) {

        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append(setInsertValues(entityType, null, bo, true, properties, false));

        return sqlstr.toString();
    }    

    public String getUpdateSql(JDBCType entityType, BusinessObject bo, BusinessObject dbBO) {

        StringBuilder sqlstr = new StringBuilder(getUpdateSqlFragment(entityType, bo, false));
        sqlstr.append(setUpdateValues(entityType, null, bo, dbBO));

        return sqlstr.toString();
    }
    
    public String getUpdateSql(JDBCType entityType, BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys) {

        StringBuilder sqlstr = new StringBuilder(getUpdateSqlFragment(entityType, bo, columnsToSet, lookupKeys));
        sqlstr.append(setUpdateValues(entityType, null, bo, columnsToSet, lookupKeys));

        return sqlstr.toString();
    }

    public String getCreateQueryJoinTableSQL (Integer stringKeyLen)
    {
        StringBuilder builder = new StringBuilder(String.format("CREATE GLOBAL TEMPORARY TABLE %s (", QueryJoinAction.JOIN_TABLE_NAME));
        // We use the DECIMAL data type to store numbers as it is a ANSI type
        // 20 digits is sufficient to store a long value
        builder.append(String.format(" %s DECIMAL(20), ", AbstractDataStore.QUERYJOIN_ID_INT_COL));

        // 36 is sufficient to store a GUID value
        if(stringKeyLen == null) {
            stringKeyLen = 36;
        }
        builder.append(String.format(" %s VARCHAR(%s), ", AbstractDataStore.QUERYJOIN_ID_STR_COL, stringKeyLen));

        // 36 is sufficient to store a GUID value
        // We control the population of this column
        builder.append(String.format(" %s VARCHAR(32) ", AbstractDataStore.QUERYJOIN_INVOC_COL));

        builder.append(" ) ");

        // Optionally we can create an index on the ID_INT and ID_STR columns

        return builder.toString();
    }

    public boolean tableExists (Connection connection, String tableName)
    {
        boolean result = false;

        try (PreparedStatement ps = connection.prepareStatement(String.format(getTableExistsSQL(), tableName));
            ResultSet rs = ps.executeQuery();
        ) {
            if(rs.next()) {
                if(rs.getInt(1) == 1) {
                    result = true;
                }
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
    }

    public List<JDBCDataModel.TableInfo> getTables (Connection connection, ForeignKeyEnhancer enhancer)
    {
        Map<String, List<String>> primaryKeys = getPrimaryKeys(connection);

        Map<String, JDBCDataModel.TableInfo> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(getTableColumnsSQL());
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDataModel.TableInfo table = null;
            List<JDBCDataModel.ColumnInfo> columns = new LinkedList<>();
            while(rs.next()) {
                if(table != null) {
                    // check if we need to reset
                    if(!table.getName().equals(rs.getString(1))) {
                        addTable(result, columns, table, primaryKeys.get(table.getName()));

                        table = null;
                        columns = new LinkedList<>();
                    }
                }

                if(table == null) {
                    table = new JDBCDataModel.TableInfo(rs.getString(1));
                }

                JDBCDataModel.ColumnInfo ci = createColumnInfo(rs);
                columns.add(ci);
            }
            if(table != null) {
                addTable(result, columns, table, primaryKeys.get(table.getName()));
            }
        }
        catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        List<JDBCDataModel.ForeignKey> foreignKeys = getForeignKeys(connection, result);

        // Give a chance to add any additional business logic based relationships
        // not captured by a database foreign key
        foreignKeys = enhancer.process(foreignKeys);
        Map<String, List<JDBCDataModel.ForeignKey>> fkMap = new HashMap<>();
        for(JDBCDataModel.ForeignKey fk: foreignKeys) {
            fk.init();
            List<JDBCDataModel.ForeignKey> fkeys = null;
            if(fkMap.containsKey(fk.getReferencingTable().getName())) {
                fkeys = fkMap.get(fk.getReferencingTable().getName());
            } else {
                fkeys = new LinkedList<>();
                fkMap.put(fk.getReferencingTable().getName(), fkeys);
            }
            fkeys.add(fk);
        }

        List<JDBCDataModel.TableInfo> tables = new ArrayList<>(result.values());
        for(JDBCDataModel.TableInfo tableInfo: tables) {
            tableInfo.setForeignKeys(fkMap.get(tableInfo.getName()));
        }

        tableMap = result;

        return tables;
    }

    private List<JDBCDataModel.ForeignKey> getForeignKeys (Connection connection, Map<String, JDBCDataModel.TableInfo> tableMap)
    {
        List<JDBCDataModel.ForeignKey> result = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(getForeignKeysSQL());
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDataModel.ForeignKey fkey = null;
            List<String> referencingColumns = new LinkedList<>();
            List<String> referencedColumns = new LinkedList<>();
            while(rs.next()) {
                if(fkey != null) {
                    // check if we need to reset
                    if(!fkey.getName().equals(rs.getString(1))) {
                        fkey.setReferencingColumns(referencingColumns);
                        fkey.setReferencedColumns(referencedColumns);
                        result.add(fkey);

                        fkey = null;
                        referencingColumns = new LinkedList<>();
                        referencedColumns = new LinkedList<>();
                    }
                }

                if(fkey == null) {
                    fkey = createForeignKey(rs, tableMap);
                }
                referencingColumns.add(rs.getString(4));
                referencedColumns.add(rs.getString(5));
            }
            if(fkey != null) {
                fkey.setReferencingColumns(referencingColumns);
                fkey.setReferencedColumns(referencedColumns);
                result.add(fkey);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
    }

    private void addTable(Map<String, JDBCDataModel.TableInfo> tables,
                          List<JDBCDataModel.ColumnInfo> columns,
                          JDBCDataModel.TableInfo table,
                          List<String> primaryKeys) {
        table.setColumns(columns);
        tables.put(table.getName(), table);

        if(primaryKeys != null && primaryKeys.size() > 0) {
            table.setPrimaryKeys(primaryKeys);
        } else {
            // all the fields of the table become the primary key
            table.initNoPrimaryKey();
        }
    }

    public Map<String, List<String>> getPrimaryKeys (Connection connection)
    {
        Map<String, List<String>> result = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(getPrimaryKeySQL());
            ResultSet rs = ps.executeQuery();
        ) {
            String tableName = null;
            List<String> columns = new LinkedList<>();
            while(rs.next()) {
                if(columns.size() > 0) {
                    // check if we need to reset
                    if(!tableName.equals(rs.getString(1))) {
                        result.put(tableName, columns);

                        columns = new LinkedList<>();
                    }
                }

                if(columns.size() == 0) {
                    tableName = rs.getString(1);
                }
                columns.add(rs.getString(3));
            }
            if(tableName != null) {
                result.put(tableName, columns);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
    }

    public abstract JDBCDataModel.TableInfo getTable(Connection connection, ForeignKeyEnhancer enhancer, String tableName);

    protected abstract JDBCDataModel.ForeignKey createForeignKey(ResultSet rs, Map<String, JDBCDataModel.TableInfo> tableMap) throws SQLException;

    protected abstract JDBCDataModel.ColumnInfo createColumnInfo(ResultSet rs) throws SQLException;

    public abstract JDBCDataModel.SequenceInfo getSequence (Connection connection, String sequenceName);

    public abstract List<JDBCDataModel.SequenceInfo> getSequences(Connection connection);

    public abstract String getTableColumnsSQL();

    public abstract String getPrimaryKeySQL();

    public abstract String getForeignKeysSQL();

    public abstract String getTableExistsSQL();

}
