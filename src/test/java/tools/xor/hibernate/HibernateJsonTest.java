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

package tools.xor.hibernate;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.logic.DefaultJson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-JSON-hibernate-test.xml" })
@TransactionConfiguration(defaultRollback = true)
@Transactional
public class HibernateJsonTest extends DefaultJson {

	@Test
	public void checkStringField() throws JSONException {
		super.checkStringField();
	}
	
	@Test
	public void checkDateField() throws JSONException {
		super.checkDateField();
	}	
	
	@Test
	public void checkIntField() throws JSONException {
		super.checkIntField();
	}	
	
	@Test
	public void checkLongField() throws JSONException {
		super.checkLongField();
	}	
	
	@Test
	public void checkEmptyLongField() throws JSONException {
		super.checkEmptyLongField();
	}	
	
	@Test
	public void checkBooleanField() throws JSONException {
		super.checkBooleanField();
	}	
	
	@Test
	public void checkBigDecimalField() throws JSONException {
		super.checkBigDecimalField();
	}	
	
	@Test
	public void checkBigIntegerField() throws JSONException {
		super.checkBigIntegerField();
	}		
	
	@Test
	public void checkEntityField() {
		super.checkEntityField();
	}	
	
	@Test
	public void checkSetField() {
		super.checkSetField();
	}	
}
