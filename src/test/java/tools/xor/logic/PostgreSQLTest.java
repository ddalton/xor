/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2021, Dilip Dalton
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
package tools.xor.logic;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tools.xor.Settings;
import tools.xor.providers.jdbc.DBTranslator;
import tools.xor.providers.jdbc.DBType;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;

@ExtendWith(SpringExtension.class)
//@ContextConfiguration(locations = { "classpath:/spring-psql-jdbc-test.xml" })
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class PostgreSQLTest
{
    @Autowired
    protected AggregateManager am;

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    public void setup() throws SQLException
    {
        am.configure(new Settings());
        JDBCDataStore po = (JDBCDataStore)am.getDataStore();
        JDBCSessionContext sc = po.getSessionContext();
        sc.beginTransaction();

        try {
            Connection c = sc.getConnection();
            DBType dbType = DBTranslator.getDBType(c);
        } finally {
            sc.close();
        }
    }

    @Test
    public void dummy() {

    }
}
