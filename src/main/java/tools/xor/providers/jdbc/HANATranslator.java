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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tools.xor.JSONObjectProperty;
import tools.xor.UnsignedByteType;
import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;
import tools.xor.view.BindParameter;

public class HANATranslator extends DBTranslator
{
    private static final String FOREIGN_KEY_SQL = "SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME, DELETE_RULE, UPDATE_RULE FROM REFERENTIAL_CONSTRAINTS WHERE SCHEMA_NAME = CURRENT_USER ORDER BY CONSTRAINT_NAME, POSITION";
    private static final String COLUMNS_SQL = "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DATA_TYPE_NAME, GENERATION_TYPE, LENGTH FROM TABLE_COLUMNS WHERE SCHEMA_NAME = CURRENT_USER ORDER BY TABLE_NAME";
    private static final String PRIMARY_KEY_SQL = "SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, POSITION FROM CONSTRAINTS WHERE IS_PRIMARY_KEY = 'TRUE' AND SCHEMA_NAME = CURRENT_USER ORDER BY CONSTRAINT_NAME, POSITION";
    private static final String SEQUENCES_SQL = "SELECT sequence_name, max_value, min_value, increment_by, start_number, is_cycled  FROM sequences WHERE SCHEMA_NAME = CURRENT_USER";
    private static final String TABLE_EXISTS_SQL = "SELECT count(*) FROM TABLES WHERE TABLE_NAME = '%s' AND SCHEMA_NAME = CURRENT_USER";

    private Map<String, JDBCDataModel.SequenceInfo> sequenceMap;

    protected static final Map<String, Class> HANA_SQL_TO_JAVA_TYPE_MAP = new HashMap<>();
    protected static final Map<String, JDBCtoSQLConverter> hanaConvertersByDataType = new ConcurrentHashMap<>();
    protected static final Map<Integer, BindParameter.SQLConverter> convertersBySQLType = new ConcurrentHashMap<>();

    static {
        HANA_SQL_TO_JAVA_TYPE_MAP.put("TINYINT", UnsignedByteType.class);
    }

    static {
        convertersBySQLType.put(
            Types.TINYINT,

            new BindParameter.SQLConverter() {

                @Override public void javaToSQL (PreparedStatement ps,
                                                 int parameterIndex,
                                                 Object value) throws SQLException
                {
                    Short result = null;
                    if(value instanceof String) {
                        result = Short.valueOf(value.toString());
                    } else if(value instanceof Short) {
                        result = (Short) value;
                    } else if(value instanceof Number) {
                        result = ((Number)value).shortValue();
                    } else {
                        throw new RuntimeException("Unsupported value type for TINYINT converter");
                    }
                    ps.setShort(parameterIndex, result);
                }

                @Override public Object sQLToJava (CallableStatement cs,
                                                   int parameterIndex) throws SQLException
                {
                    return cs.getByte(parameterIndex);
                }

                @Override public Object sQLToJava (ResultSet rs,
                                                   int parameterIndex) throws SQLException
                {
                    return rs.getByte(parameterIndex);
                }
            }
        );
    }

    @Override
    protected Class getJavaClass(String sqlType) {
        if(HANA_SQL_TO_JAVA_TYPE_MAP.containsKey(sqlType)) {
            return HANA_SQL_TO_JAVA_TYPE_MAP.get(sqlType);
        }

        return super.getJavaClass(sqlType);
    }

    public BindParameter.SQLConverter getSQLConverter(int type) {
        if(convertersBySQLType.containsKey(type)) {
            return convertersBySQLType.get(type);
        }

        return super.getSQLConverter(type);
    }

    static {
        hanaConvertersByDataType.put(
            "DATE", new JDBCtoSQLConverter()
            {
                final String hanadateFmt = "YYYY-MM-DD";
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_DATE);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return String.format("TO_DATE('%s', '%s')", df.format(value), hanadateFmt);
                    } else {
                        throw new RuntimeException("Unsupported value type for Date converter");
                    }
                }
            });

        hanaConvertersByDataType.put(
            "TIME", new JDBCtoSQLConverter()
            {
                final String hanaTimeFmt = "HH24:MI:SS";
                @Override public String toSQLLiteral (Object value)
                {
                    DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_TIME);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return String.format("TO_TIME('%s', '%s')", df.format(value), hanaTimeFmt);
                    } else {
                        throw new RuntimeException("Unsupported value type for Time converter");
                    }
                }
            });

        hanaConvertersByDataType.put(
            "TIMESTAMP", new JDBCtoSQLConverter()
            {
                final String hanaTimestampFmt = "YYYY-MM-DD HH24:MI:SS";
                @Override public String toSQLLiteral (Object value)
                {
                    SimpleDateFormat df = new SimpleDateFormat(JSONObjectProperty.ANSI_FORMAT_DATETIME);
                    if(value == null) return "NULL";
                    if(value instanceof java.util.Date) {
                        return String.format("TO_TIMESTAMP('%s', '%s')", df.format(value), hanaTimestampFmt);
                    } else {
                        throw new RuntimeException("Unsupported value type for Timestamp converter");
                    }
                }
            });
    }

    @Override
    protected JDBCtoSQLConverter getConverter(String dataType) {
        if(hanaConvertersByDataType.containsKey(dataType)) {
            return hanaConvertersByDataType.get(dataType);
        }

        return super.getConverter(dataType);
    }

    @Override public JDBCDataModel.TableInfo getTable (Connection connection, ForeignKeyEnhancer enhancer, String tableName)
    {
        if(tableMap == null) {
            getTables(connection, enhancer);
        }

        return tableMap.get(tableName);
    }

    @Override public JDBCDataModel.SequenceInfo getSequence (Connection connection, String sequenceName)
    {
        if(sequenceMap == null) {
            getSequences(connection);
        }

        return sequenceMap.get(sequenceName);
    }

    @Override
    protected JDBCDataModel.ColumnInfo createColumnInfo(ResultSet rs) throws SQLException
    {
        String columnName = rs.getString(2);
        Boolean nullable = rs.getBoolean(3);
        String columnType = rs.getString(4);
        Boolean generated = (rs.getString(5) != null) ? true : false;
        int length = rs.getInt(6);
        if(getJavaClass(columnType) == null) {
            throw new RuntimeException("Unknown java mapping for SQL type: " + columnType);
        }
        JDBCDataModel.ColumnInfo ci = new JDBCDataModel.ColumnInfo(columnName, nullable, getJavaClass(
            columnType), columnType, generated, length);

        return ci;
    }

    private void printResult(Connection connection, String sql) {
        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
        ) {
            ResultSetMetaData rsmd=rs.getMetaData();
            int count = rsmd.getColumnCount();

            for(int i = 0; i < count; i++) {
                System.out.println(rsmd.getColumnName(i+1));
            }

            System.out.println("============== SQL =======================");
            System.out.println(sql);
            System.out.println("============== SQL =======================");
            while(rs.next()) {
                StringBuilder row = new StringBuilder();
                for(int i = 0; i < count; i++) {
                    if (row.length() > 0) {
                        row.append(", ");
                    }

                    // JDBC column starts from 1
                    row.append(rs.getString(i+1));
                }
                System.out.println(row.toString());
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override
    protected JDBCDataModel.ForeignKey createForeignKey(ResultSet rs, Map<String, JDBCDataModel.TableInfo> tableMap) throws SQLException
    {
        JDBCDataModel.ForeignKeyRule deleteRule = JDBCDataModel.ForeignKeyRule.valueOf(rs.getString(
            6));
        JDBCDataModel.ForeignKeyRule updateRule = JDBCDataModel.ForeignKeyRule.valueOf(rs.getString(7));
        JDBCDataModel.TableInfo referencing = tableMap.get(rs.getString(2));
        JDBCDataModel.TableInfo referenced = tableMap.get(rs.getString(3));
        JDBCDataModel.ForeignKey fkey = new JDBCDataModel.ForeignKey(rs.getString(1),
            referencing, referenced, deleteRule, updateRule);

        return fkey;
    }

    @Override public List<JDBCDataModel.SequenceInfo> getSequences (Connection connection)
    {
        Map<String, JDBCDataModel.SequenceInfo> result = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SEQUENCES_SQL);
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDataModel.SequenceInfo seq = null;
            while(rs.next()) {
                seq = new JDBCDataModel.SequenceInfo(
                    rs.getString(1),
                    "DECIMAL",
                    rs.getLong(2),
                    rs.getLong(3),
                    rs.getInt(4),
                    rs.getLong(5),
                    rs.getBoolean(6));
                result.put(seq.getName(), seq);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        this.sequenceMap = result;

        return new ArrayList<>(this.sequenceMap.values());
    }

    @Override public String getTableColumnsSQL ()
    {
        return COLUMNS_SQL;
    }

    @Override public String getPrimaryKeySQL ()
    {
        return PRIMARY_KEY_SQL;
    }

    @Override public String getForeignKeysSQL ()
    {
        return FOREIGN_KEY_SQL;
    }

    @Override public String getTableExistsSQL ()
    {
        return TABLE_EXISTS_SQL;
    }
}
