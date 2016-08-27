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

package tools.xor.jpa;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.AssociationSetting;
import tools.xor.EntityType;
import tools.xor.JPAProperty;
import tools.xor.Property;
import tools.xor.db.pm.Task;
import tools.xor.logic.DefaultMutableJson;
import tools.xor.service.DataAccessService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-mutable-JSON-jpa-test.xml" })
@TransactionConfiguration(defaultRollback = true)
@Transactional
public class JPAMutableJsonTest extends DefaultMutableJson {

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
	public void checkEntityField() throws JSONException {
		super.checkEntityField();
	}

	@Test
	public void checkSetField() throws JSONException {
		super.checkSetField();
	}

	@Test
	public void checkOpenField() throws JSONException {
		DataAccessService das = aggregateService.getDAS();
		EntityType taskType = (EntityType) das.getType(Task.class);
		Property openProperty = new JPAProperty("ItemList", das.getType(Object.class), taskType);
		das.addProperty(taskType, openProperty);

		super.checkOpenField();
	}
	
	@Test
	public void checkExcelExport() throws JSONException, IOException {
		super.checkExcelExport();
	}
	
	@Test
	public void checkExcelImport() throws JSONException, IOException {
		super.checkExcelImport();
	}		
	
	@Test
	public void checkExcelImport100() throws JSONException, IOException {
		super.checkExcelImport100();
	}	
	
	@Test
	public void checkOpenFieldEntityToOne() {
		DataAccessService das = aggregateService.getDAS();
		EntityType taskType = (EntityType) das.getType(Task.class);
		Property openProperty = new JPAProperty("subTaskObj", das.getType(Task.class), taskType);
		das.addProperty(taskType, openProperty);

		super.checkOpenFieldEntityToOne();		
	}
}
