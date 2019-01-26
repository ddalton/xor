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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tools.xor.Settings;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;

import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jdbc-test.xml" })
public class PlainJDBC
{
	@Autowired
	protected AggregateManager am;

	@Autowired
	protected DataSource dataSource;

	@Test
	public void selectTest() {
		String jsonString = "{ \"normalized\": false, \"view\" : { \"nativeQuery\" : { \"selectClause\": \"SELECT count(*) FROM Person\" } } }";

		DataAccessService das = am.getDAS();
		Settings settings = das.settings().json(jsonString).build();

		Object result = am.dml(settings);
		System.out.println(result);
	}
}
