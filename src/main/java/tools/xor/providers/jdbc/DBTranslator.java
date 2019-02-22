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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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

    public abstract List<JDBCDAS.ColumnInfo> getColumns(String tableName);

    public abstract List<String> getTables();
}
