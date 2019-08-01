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

import org.json.JSONObject;
import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.DataGenerator;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ImmutableJsonProperty;
import tools.xor.JDBCProperty;
import tools.xor.JDBCType;
import tools.xor.JSONObjectProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.BindParameter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for interacting with various RDBMS implementations.
 *
 * Use the following to test:
 * Oracle
 * <bean id="dataSource" class="oracle.jdbc.pool.OracleDataSource">
 *   <property name="dataSourceName" value="ds"/>
 *   <property name="URL" value="jdbc:oracle:thin:@<hostname>:<port_num>:<SID>"/>
 *   <property name="user" value="dummy_user"/>
 *   <property name="password" value="dummy_pwd"/>
 * </bean>
 *
 * HANA
 *	<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
 *    <property name="driverClassName" value="com.sap.db.jdbc.Driver" />
 *    <property name="url" value="jdbc:sap://<hostname>:<port_num>" />
 *    <property name="username" value="user" />
 *    <property name="password" value="password" />
 *  </bean>
 */
public abstract class DBTranslator
{
    private final static Map<String, DBTranslator> translators = new ConcurrentHashMap<>();

    static {
        translators.put("HSQL DATABASE ENGINE", new HSQLTranslator() );
        translators.put("HDB", new HANATranslator() );
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
        SQL_TO_JAVA_TYPE_MAP.put("LONGVARBINARY", byte[].class);
        SQL_TO_JAVA_TYPE_MAP.put("DATE", java.sql.Date.class);
        SQL_TO_JAVA_TYPE_MAP.put("TIME", java.sql.Time.class);
        SQL_TO_JAVA_TYPE_MAP.put("TIMESTAMP", java.sql.Timestamp.class);
        SQL_TO_JAVA_TYPE_MAP.put("BLOB", java.sql.Blob.class);
        SQL_TO_JAVA_TYPE_MAP.put("CLOB", java.sql.Clob.class);
    }

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

    public static DBTranslator getTranslator(Statement statement) {
        try {
            DatabaseMetaData metadata = statement.getConnection().getMetaData();
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
                    containerProperty = (JDBCProperty)bo.getContainer().getContainmentProperty();
                    container = (BusinessObject)bo.getContainer().getContainer();
                }
            }
        } else {
            containerProperty = (JDBCProperty)bo.getContainmentProperty();
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
        List<Property> properties = entityType.getProperties();
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

    public String getInsertSqlFragment(BusinessObject bo, JDBCType entityType, boolean isBindParameters, DataGenerator dataGenerator) {
        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append("INSERT INTO " + entityType.getTableName() + " (");

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
                JDBCDAS.ForeignKey fkey = ((JDBCProperty)p).getForeignKey();
                if(fkey == null) {
                    throw new RuntimeException("A TO_ONE relationship should have a foreign key");
                } else {
                    // get the referencing columns
                    columnNames.addAll(fkey.getReferencingColumns());
                }
            }
        }

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

    public String setValues(PreparedStatement ps, BusinessObject bo, JDBCType entityType, boolean isCSV, DataGenerator dataGenerator) {
        // get the values
        List<String> values = new LinkedList<>();
        int position = 1;
        for(Property p: getProperties(entityType, dataGenerator)) {
            if(shouldSkip(bo, p, dataGenerator)) {
                continue;
            }

            // simple type
            if(p.getType().isDataType() && !p.isMany()) {
                JDBCDAS.ColumnInfo col = ((JDBCProperty)p).getColumns().get(0);
                JDBCtoSQLConverter c = isCSV ? getCSVConverter(col.getDataType()) : getConverter(col.getDataType());
                values.add(c.toSQLLiteral(bo.get(col.getName())));

                if(ps != null) {
                    addBindParameter(ps, col.getDataType(), position++, bo.get(col.getName()));
                }
            }

            // foreign keys
            if(!p.getType().isDataType() && p.getMappedBy() == null) {
                JDBCDAS.ForeignKey fkey = ((JDBCProperty)p).getForeignKey();
                JSONObject entity = (JSONObject)bo.get(p);
                List<JDBCDAS.ColumnInfo> referencedColumns = fkey.getReferencedTable().getColumnInfo(
                    fkey.getReferencedColumns());
                for (int i = 0; i < referencedColumns.size(); i++) {
                    String dataType = referencedColumns.get(i).getDataType();
                    String columnName = referencedColumns.get(i).getName();
                    JDBCtoSQLConverter c = isCSV ? getCSVConverter(dataType) : getConverter(dataType);
                    values.add(c.toSQLLiteral(entity.get(columnName)));

                    if(ps != null) {
                        addBindParameter(ps, dataType, position++, entity.get(columnName));
                    }
                }
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

    private void addBindParameter(PreparedStatement ps, String type, int position, Object value) {
        BindParameter bp = BindParameter.instance(position, null);
        bp.setType(type);
        bp.setValue(ps, this, value);
    }

    public String getInsertSql(BusinessObject bo, JDBCType entityType, DataGenerator dataGenerator) {

        StringBuilder sqlstr = new StringBuilder(getInsertSqlFragment(bo, entityType, false, dataGenerator));
        sqlstr.append("(")
            .append(setValues(null, bo, entityType, false, dataGenerator))
            .append(")");

        return sqlstr.toString();
    }

    public String getCSV(BusinessObject bo, JDBCType entityType, DataGenerator dataGenerator) {

        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append(setValues(null, bo, entityType, true, dataGenerator));

        return sqlstr.toString();
    }


    public abstract JDBCDAS.TableInfo getTable(Connection connection, ForeignKeyEnhancer enhancer, String tableName);

    public abstract List<JDBCDAS.TableInfo> getTables(Connection connection ,ForeignKeyEnhancer enhancer);

    public abstract Map<String, List<String>> getPrimaryKeys(Connection connection);

    public abstract JDBCDAS.SequenceInfo getSequence (Connection connection, String sequenceName);

    public abstract List<JDBCDAS.SequenceInfo> getSequences(Connection connection);
}
