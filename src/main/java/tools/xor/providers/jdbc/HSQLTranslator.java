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

import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HSQLTranslator extends DBTranslator
{
    private static final String FOREIGN_KEY_SQL = "SELECT FK_NAME, FKTABLE_NAME, PKTABLE_NAME, FKCOLUMN_NAME, PKCOLUMN_NAME, DELETE_RULE, UPDATE_RULE FROM INFORMATION_SCHEMA.SYSTEM_CROSSREFERENCE WHERE FKTABLE_SCHEM = 'PUBLIC' ORDER BY FK_NAME, KEY_SEQ";
    private static final String COLUMNS_SQL = "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DTD_IDENTIFIER FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME NOT LIKE 'SYSTEM_%' AND TABLE_SCHEMA = 'PUBLIC'";
    private static final String PRIMARY_KEY_SQL = "SELECT table_name, pk_name, column_name, key_seq FROM INFORMATION_SCHEMA.SYSTEM_PRIMARYKEYS WHERE table_schem = 'PUBLIC' ORDER BY pk_name, key_seq";

    private Map<String, JDBCDAS.TableInfo> tableMap;

    @Override public JDBCDAS.TableInfo getTable (ForeignKeyEnhancer enhancer, String tableName)
    {
        if(tableMap == null) {
            getTables(enhancer);
        }

        return tableMap.get(tableName);
    }

    @Override public List<JDBCDAS.TableInfo> getTables (ForeignKeyEnhancer enhancer)
    {
        Map<String, List<String>> primaryKeys = getPrimaryKeys();

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
                Boolean nullable = "NO".equals(rs.getString(3)) ? false : true;
                String columnType = rs.getString(4);
                if(columnType.contains("(")) {
                    columnType = columnType.substring(0, columnType.indexOf("("));
                }
                if(!SQL_TO_JAVA_TYPE_MAP.containsKey(columnType)) {
                    throw new RuntimeException("Unknown java mapping for SQL type: " + columnType);
                }
                JDBCDAS.ColumnInfo ci = new JDBCDAS.ColumnInfo(columnName, nullable, SQL_TO_JAVA_TYPE_MAP.get(columnType), columnType);
                columns.add(ci);
            }
            if(table != null) {
                addTable(result, columns, table, primaryKeys.get(table.getName()));
            }
        }
        catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        List<JDBCDAS.ForeignKey> foreignKeys = getForeignKeys(result);

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

        if(primaryKeys != null) {
            table.setPrimaryKeys(primaryKeys);
        } else {
            // all the fields of the table become the primary key
            table.initNoPrimaryKey();
        }
    }

    private void printResult(String sql) {
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

    private List<JDBCDAS.ForeignKey> getForeignKeys (Map<String, JDBCDAS.TableInfo> tableMap)
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
                    JDBCDAS.ForeignKeyRule deleteRule = getForeignKeyRule(rs.getInt(6));
                    JDBCDAS.ForeignKeyRule updateRule = getForeignKeyRule(rs.getInt(7));
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

    @Override public Map<String, List<String>> getPrimaryKeys ()
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
