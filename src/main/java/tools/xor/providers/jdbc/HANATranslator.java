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

import tools.xor.JSONObjectProperty;
import tools.xor.UnsignedByteType;
import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HANATranslator extends DBTranslator
{
    private static final String FOREIGN_KEY_SQL = "SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME, DELETE_RULE, UPDATE_RULE FROM REFERENTIAL_CONSTRAINTS WHERE SCHEMA_NAME = CURRENT_USER ORDER BY CONSTRAINT_NAME, POSITION";
    private static final String COLUMNS_SQL = "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DATA_TYPE_NAME, GENERATION_TYPE, LENGTH FROM TABLE_COLUMNS WHERE SCHEMA_NAME = CURRENT_USER";
    private static final String PRIMARY_KEY_SQL = "SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, POSITION FROM CONSTRAINTS WHERE SCHEMA_NAME = CURRENT_USER ORDER BY CONSTRAINT_NAME, POSITION";
    private static final String SEQUENCES_SQL = "SELECT sequence_name, max_value, min_value, increment_by, start_number, is_cycled  FROM sequences WHERE SCHEMA_NAME = CURRENT_USER";

    private Map<String, JDBCDAS.TableInfo> tableMap;
    private Map<String, JDBCDAS.SequenceInfo> sequenceMap;

    protected static final Map<String, Class> HANA_SQL_TO_JAVA_TYPE_MAP = new HashMap<>();
    protected static final Map<String, JDBCtoSQLConverter> hanaConvertersByDataType = new ConcurrentHashMap<>();


    static {
        HANA_SQL_TO_JAVA_TYPE_MAP.put("TINYINT", UnsignedByteType.class);
    }

    @Override
    protected Class getJavaClass(String sqlType) {
        if(HANA_SQL_TO_JAVA_TYPE_MAP.containsKey(sqlType)) {
            return HANA_SQL_TO_JAVA_TYPE_MAP.get(sqlType);
        }

        return super.getJavaClass(sqlType);
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

    @Override public JDBCDAS.TableInfo getTable (Connection connection, ForeignKeyEnhancer enhancer, String tableName)
    {
        if(tableMap == null) {
            getTables(connection, enhancer);
        }

        return tableMap.get(tableName);
    }

    @Override public JDBCDAS.SequenceInfo getSequence (Connection connection, String sequenceName)
    {
        if(sequenceMap == null) {
            getSequences(connection);
        }

        return sequenceMap.get(sequenceName);
    }

    @Override public List<JDBCDAS.TableInfo> getTables (Connection connection, ForeignKeyEnhancer enhancer)
    {
        Map<String, List<String>> primaryKeys = getPrimaryKeys(connection);

        Map<String, JDBCDAS.TableInfo> result = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(COLUMNS_SQL);
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDAS.TableInfo table = null;
            List<JDBCDAS.ColumnInfo> columns = new LinkedList<>();
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
                    table = new JDBCDAS.TableInfo(rs.getString(1));
                }

                String columnName = rs.getString(2);
                Boolean nullable = rs.getBoolean(3);
                String columnType = rs.getString(4);
                Boolean generated = (rs.getString(5) != null) ? true : false;
                int length = rs.getInt(6);
                if(getJavaClass(columnType) == null) {
                    throw new RuntimeException("Unknown java mapping for SQL type: " + columnType);
                }
                JDBCDAS.ColumnInfo ci = new JDBCDAS.ColumnInfo(columnName, nullable, getJavaClass(
                    columnType), columnType, generated, length);
                columns.add(ci);
            }
            if(table != null) {
                addTable(result, columns, table, primaryKeys.get(table.getName()));
            }
        }
        catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        List<JDBCDAS.ForeignKey> foreignKeys = getForeignKeys(connection, result);

        // Give a chance to add any additional business logic based relationships
        // not captured by a database foreign key
        foreignKeys = enhancer.process(foreignKeys);
        Map<String, List<JDBCDAS.ForeignKey>> fkMap = new HashMap<>();
        for(JDBCDAS.ForeignKey fk: foreignKeys) {
            fk.init();
            List<JDBCDAS.ForeignKey> fkeys = null;
            if(fkMap.containsKey(fk.getReferencingTable().getName())) {
                fkeys = fkMap.get(fk.getReferencingTable().getName());
            } else {
                fkeys = new LinkedList<>();
                fkMap.put(fk.getReferencingTable().getName(), fkeys);
            }
            fkeys.add(fk);
        }

        List<JDBCDAS.TableInfo> tables = new ArrayList<>(result.values());
        for(JDBCDAS.TableInfo tableInfo: tables) {
            tableInfo.setForeignKeys(fkMap.get(tableInfo.getName()));
        }

        tableMap = result;

        return tables;
    }

    private void addTable(Map<String, JDBCDAS.TableInfo> tables,
                          List<JDBCDAS.ColumnInfo> columns,
                          JDBCDAS.TableInfo table,
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

    private List<JDBCDAS.ForeignKey> getForeignKeys (Connection connection, Map<String, JDBCDAS.TableInfo> tableMap)
    {
        List<JDBCDAS.ForeignKey> result = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(FOREIGN_KEY_SQL);
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDAS.ForeignKey fkey = null;
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
                    JDBCDAS.ForeignKeyRule deleteRule = JDBCDAS.ForeignKeyRule.valueOf(rs.getString(
                            6));
                    JDBCDAS.ForeignKeyRule updateRule = JDBCDAS.ForeignKeyRule.valueOf(rs.getString(7));
                    JDBCDAS.TableInfo referencing = tableMap.get(rs.getString(2));
                    JDBCDAS.TableInfo referenced = tableMap.get(rs.getString(3));
                    fkey = new JDBCDAS.ForeignKey(rs.getString(1),
                        referencing, referenced, deleteRule, updateRule);
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

    @Override public Map<String, List<String>> getPrimaryKeys (Connection connection)
    {
        Map<String, List<String>> result = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(PRIMARY_KEY_SQL);
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

    @Override public List<JDBCDAS.SequenceInfo> getSequences (Connection connection)
    {
        Map<String, JDBCDAS.SequenceInfo> result = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(SEQUENCES_SQL);
            ResultSet rs = ps.executeQuery();
        ) {
            JDBCDAS.SequenceInfo seq = null;
            while(rs.next()) {
                seq = new JDBCDAS.SequenceInfo(
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

    private JDBCDAS.ForeignKeyRule getForeignKeyRule(int value) {
        switch(value) {
        case 1:
            return JDBCDAS.ForeignKeyRule.CASCADE;
        case 2:
            return JDBCDAS.ForeignKeyRule.SET_NULL;
        case 3:
            return JDBCDAS.ForeignKeyRule.SET_DEFAULT;
        case 4:
            return JDBCDAS.ForeignKeyRule.RESTRICT;
        case 5:
            return JDBCDAS.ForeignKeyRule.NO_ACTION;
        }

        throw new RuntimeException("Unknown value for foreign key rule: " + value);
    }
}
