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
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCProperty;
import tools.xor.JDBCType;
import tools.xor.JSONObjectProperty;
import tools.xor.MutableJsonProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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

    protected Connection connection;

    static {
        translators.put("HSQL DATABASE ENGINE", new HSQLTranslator() );
    }

    protected static final Map<String, Class> SQL_TO_JAVA_TYPE_MAP = new HashMap<>();
    protected static final Map<String, JDBCtoSQLConverter> convertersByDataType = new ConcurrentHashMap<>();

    static {
        SQL_TO_JAVA_TYPE_MAP.put("CHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("VARCHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("LONGVARCHAR", java.lang.String.class);
        SQL_TO_JAVA_TYPE_MAP.put("NUMERIC", java.math.BigDecimal.class);
        SQL_TO_JAVA_TYPE_MAP.put("DECIMAL", java.math.BigDecimal.class);
        SQL_TO_JAVA_TYPE_MAP.put("BIT", Boolean.class);
        SQL_TO_JAVA_TYPE_MAP.put("BOOLEAN", Boolean.class);
        SQL_TO_JAVA_TYPE_MAP.put("TINYINT", Integer.class);
        SQL_TO_JAVA_TYPE_MAP.put("SMALLINT", Integer.class);
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
                DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_DATE);
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return "CAST (df.format(value) AS date)";
                    } else {
                        throw new RuntimeException("Unsupported value type for Date converter");
                    }
                }
            });

        convertersByDataType.put(
            "TIME", new JDBCtoSQLConverter()
            {
                DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_TIME);
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return "CAST (df.format(value) AS time)";
                    } else {
                        throw new RuntimeException("Unsupported value type for Time converter");
                    }
                }
            });

        convertersByDataType.put(
            "TIMESTAMP", new JDBCtoSQLConverter()
            {
                DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT);
                @Override public String toSQLLiteral (Object value)
                {
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return "CAST (df.format(value) AS datetime)";
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

    public static DBTranslator instance(Connection conn) {
        DBTranslator result;
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            String productName = metadata.getDatabaseProductName().toUpperCase();
            result = translators.get(productName);

            if(result == null) {
                throw new RuntimeException("Unable to find DBTranslator for product: " + productName);
            }

            // Always initialized with the active connection
            result.connection = conn;
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
    }

    public String getInsertSql(Settings settings, BusinessObject bo) {

        StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(null, settings, null);

        // Check if the identifier column has been populated
        JDBCType entityType = (JDBCType)bo.getType();
        ExtendedProperty identifierProperty = (ExtendedProperty) entityType.getIdentifierProperty();
        if(identifierProperty != null && !identifierProperty.isGenerated()) {
            Serializable id = (Serializable)identifierProperty.getValue(bo);
            if(id == null || "".equals(id.toString())) {
                Object value = ((BasicType)identifierProperty.getType()).generate(
                    settings,
                    identifierProperty,
                    null,
                    null,
                    visitor);
                bo.set(identifierProperty, value);
            }
        }

        StringBuilder sqlstr = new StringBuilder();
        sqlstr.append("INSERT INTO " + entityType.getTableName() + " (");

        // iterate through the properties
        List<String> columnNames = new LinkedList<>();
        for(Property p: entityType.getProperties()) {
            if(((ExtendedProperty)p).isGenerated()) {
                continue;
            }

            // simple type
            if(p.getType().isDataType() && !p.isMany()) {
                columnNames.add(((JDBCProperty)p).getColumns().get(0).getName());
            }

            // foreign keys
            if(!p.getType().isDataType()) {
                JDBCDAS.ForeignKey fkey = ((JDBCProperty)p).getForeignKey();
                if(fkey == null) {
                    throw new RuntimeException("A TO_ONE relationship should have a foreign key");
                }
                // get the referencing columns
                columnNames.addAll(fkey.getReferencingColumns());
            }
        }
        sqlstr.append(String.join(",", columnNames))
            .append(") VALUES (");

        // get the values
        List<String> values = new LinkedList<>();
        for(Property p: entityType.getProperties()) {
            if(((ExtendedProperty)p).isGenerated()) {
                continue;
            }

            // simple type
            if(p.getType().isDataType() && !p.isMany()) {
                JDBCDAS.ColumnInfo col = ((JDBCProperty)p).getColumns().get(0);
                values.add(getConverter(col.getDataType()).toSQLLiteral(bo.get(col.getName())));
            }

            // foreign keys
            if(!p.getType().isDataType()) {
                List<String> keys = ((EntityType)p.getType()).getExpandedNaturalKey();
                JDBCDAS.ForeignKey fkey = ((JDBCProperty)p).getForeignKey();
                List<JDBCDAS.ColumnInfo> referencingColumns = fkey.getReferencingTable().getColumnInfo(fkey.getReferencingColumns());
                for (int i = 0; i < keys.size(); i++) {
                    String dataType = referencingColumns.get(i).getDataType();
                    values.add(getConverter(dataType).toSQLLiteral(bo.get(keys.get(i))));
                }
            }
        }
        sqlstr.append(String.join(",", values))
            .append(")");

        return sqlstr.toString();
    }

    public abstract JDBCDAS.TableInfo getTable(ForeignKeyEnhancer enhancer, String tableName);

    public abstract List<JDBCDAS.TableInfo> getTables(ForeignKeyEnhancer enhancer);

    public abstract Map<String, List<String>> getPrimaryKeys();

    public abstract JDBCDAS.SequenceInfo getSequence (String sequenceName);

    public abstract List<JDBCDAS.SequenceInfo> getSequences();
}
