/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tools.xor.Settings;
import tools.xor.providers.jdbc.JDBCDAS;
import tools.xor.service.AbstractDataAccessService;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class PlainJDBCTest
{
	@Autowired
	protected AggregateManager am;

	@Autowired
	protected DataSource dataSource;

	// Uncomment Before and After methods if running this test independently

	//@Before
	public void init() throws SQLException, ClassNotFoundException, IOException
	{
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
			statement.execute("CREATE TABLE person (id VARCHAR(10) NOT NULL, name VARCHAR(50) NOT NULL,"
					+ "email VARCHAR(50) NOT NULL, PRIMARY KEY (id))");
			connection.commit();
			statement.executeUpdate(
				"INSERT INTO person VALUES ('1001','Jack Wayne', 'jwayne@somewhere.com')");
			statement.executeUpdate("INSERT INTO person VALUES ('1002','John Wayne', 'jowayne@somewhere.com')");
			connection.commit();
		}
	}


	//@After
	public void destroy() throws SQLException, ClassNotFoundException, IOException {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();) {
			statement.executeUpdate("DROP TABLE Person");
			connection.commit();
		}
	}

	@Test
	public void jsonSelect() {
		String jsonString = "{ \"normalized\": false, \"view\" : { \"attributeList\" : [\"count\"], \"nativeQuery\" : { \"selectClause\": \"SELECT count(*) FROM Person\" } } }";

		DataAccessService das = am.getDAS();

		// Rebuild the types
		das.addShape("_DEFAULT_");

		Settings settings = das.settings().json(jsonString).build();

		Object result = am.dml(settings);

		assert(result instanceof List);

		List list = (List) result;
		assert(list.size() == 2);
	}
}
