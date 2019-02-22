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

import tools.xor.util.ClassUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HSQLTranslator extends DBTranslator
{

    private static final String TABLE_SQL = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME NOT LIKE 'SYSTEM_%' AND TABLE_SCHEMA = 'PUBLIC'";
    private static final String COLUMNS_SQL = "SELECT COLUMN_NAME, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s' AND TABLE_SCHEMA = 'PUBLIC'";
    private static final String COLUMN_TYPES_SQL = "SELECT * FROM %s LIMIT 1";

    @Override public List<JDBCDAS.ColumnInfo> getColumns (String tableName)
    {
        List<JDBCDAS.ColumnInfo> result = new ArrayList<>();
        Map<String, Class> columnTypes = new HashMap<>();

        String sql = String.format(COLUMN_TYPES_SQL, tableName);
        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
        ) {
            ResultSetMetaData rsmd=rs.getMetaData();
            for(int i = 0; i < rsmd.getColumnCount(); i++) {
                String javaClassName = rsmd.getColumnClassName(i+1);
                Class javaClass = Class.forName(javaClassName);
                columnTypes.put(rsmd.getColumnName(i+1), javaClass);
            }
        }
        catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        sql = String.format(COLUMNS_SQL, tableName);
        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
        ) {
            while(rs.next()) {
                String name = rs.getString(1);
                Boolean nullable = "NO".equals(rs.getString(2)) ? false : true;
                JDBCDAS.ColumnInfo ci = new JDBCDAS.ColumnInfo(name, nullable, columnTypes.get(name));
                result.add(ci);
            }
        }
        catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
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

    @Override public List<String> getTables ()
    {
        List<String> result = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(TABLE_SQL);
            ResultSet rs = ps.executeQuery();
        ) {
            while(rs.next()) {
                result.add(rs.getString(1));
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }

        return result;
    }
}
