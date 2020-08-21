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

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.db.base.Technician;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;

public class DefaultQueryOneToOne extends AbstractDBTest {
	
	@Autowired
	protected AggregateManager aggregateService;	
	
	private static final String TASK_NAME = "SETUP_DSL";

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}
	
	protected void query() {
		// create person
		Technician owner = new Technician();
		owner.setName("DILIP_DALTON");
		owner.setDisplayName("Dilip Dalton");
		owner.setDescription("Software engineer in the bay area");
		owner.setUserName("daltond");
		owner.setSkill("Network, electric, telephone");
		owner = (Technician) aggregateService.create(owner, new Settings());	
		Person person = (Person) aggregateService.read(owner, new Settings());		
		
		// Create Task
		Task task = new Task();
		task.setName(TASK_NAME);
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		task.setAssignedTo(person);
		task = (Task) aggregateService.create(task, getSettings());
		task = (Task) aggregateService.read(task, getSettings());
		
		assert(task.getId() != null);
		assert(task.getAssignedTo() != null);
		assert(task.getAssignedTo().getId() != null);
		
		readTask(task);
	}
	
	protected void readTask(Task task) {
		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO"));
		List<?> toList = aggregateService.query(task, settings);

		assert(toList.size() == 1);

		Task result = null;
		if(Task.class.isAssignableFrom(toList.get(0).getClass()))
				result = (Task) toList.get(0);
		
		assert(result != null && result.getName().equals(TASK_NAME));		
	}

}
