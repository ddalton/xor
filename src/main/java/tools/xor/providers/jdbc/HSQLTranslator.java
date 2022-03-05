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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.xor.service.ForeignKeyEnhancer;
import tools.xor.util.ClassUtil;

public class HSQLTranslator extends DBTranslator
{
    private static final String FOREIGN_KEY_SQL = "SELECT FK_NAME, FKTABLE_NAME, PKTABLE_NAME, FKCOLUMN_NAME, PKCOLUMN_NAME, DELETE_RULE, UPDATE_RULE FROM INFORMATION_SCHEMA.SYSTEM_CROSSREFERENCE WHERE FKTABLE_SCHEM = CURRENT_SCHEMA ORDER BY FK_NAME, KEY_SEQ";
    private static final String COLUMNS_SQL = "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DTD_IDENTIFIER, is_identity, character_maximum_length FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME NOT LIKE 'SYSTEM_%' AND TABLE_SCHEMA = CURRENT_SCHEMA";
    private static final String PRIMARY_KEY_SQL = "SELECT table_name, pk_name, column_name, key_seq FROM INFORMATION_SCHEMA.SYSTEM_PRIMARYKEYS WHERE table_schem = CURRENT_SCHEMA ORDER BY pk_name, key_seq";
    private static final String SEQUENCES_SQL = "SELECT sequence_name, data_type, maximum_value, minimum_value, increment, start_with, cycle_option  FROM information_schema.sequences WHERE SEQUENCE_SCHEMA = CURRENT_SCHEMA";
    private static final String TABLE_EXISTS_SQL = "SELECT count(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '%s' AND TABLE_SCHEMA = CURRENT_SCHEMA";

    private Map<String, JDBCDataModel.SequenceInfo> sequenceMap;

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
        Boolean nullable = "NO".equals(rs.getString(3)) ? false : true;
        String columnType = rs.getString(4);
        if(columnType.contains("(")) {
            columnType = columnType.substring(0, columnType.indexOf("("));
        }
        Boolean generated = "YES".equals(rs.getString(5)) ? true : false;
        int length = rs.getInt(6);
        if(getJavaClass(columnType) == null) {
            throw new RuntimeException("Unknown java mapping for SQL type: " + columnType);
        }
        JDBCDataModel.ColumnInfo ci = new JDBCDataModel.ColumnInfo(columnName, nullable, getJavaClass(columnType), columnType,
            generated, length);

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
        JDBCDataModel.ForeignKeyRule deleteRule = getForeignKeyRule(rs.getInt(6));
        JDBCDataModel.ForeignKeyRule updateRule = getForeignKeyRule(rs.getInt(7));
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
                    rs.getString(2),
                    rs.getLong(3),
                    rs.getLong(4),
                    rs.getInt(5),
                    rs.getLong(6),
                    rs.getBoolean(7));
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

    private JDBCDataModel.ForeignKeyRule getForeignKeyRule(int value) {
        switch(value) {
        case 1:
            return JDBCDataModel.ForeignKeyRule.CASCADE;
        case 2:
            return JDBCDataModel.ForeignKeyRule.SET_NULL;
        case 3:
            return JDBCDataModel.ForeignKeyRule.SET_DEFAULT;
        case 4:
            return JDBCDataModel.ForeignKeyRule.RESTRICT;
        case 5:
            return JDBCDataModel.ForeignKeyRule.NO_ACTION;
        }

        throw new RuntimeException("Unknown value for foreign key rule: " + value);
    }
}
