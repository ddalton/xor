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

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.db.base.Technician;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultCloneOneToOne extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;
	
	private static final String TASK_NAME = "SETUP_DSL";
	
	protected Task saveTask() {
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
		
		return task;
	}

	protected void cloneTask() {
		Task task = saveTask();
		
		Task clonedTask = (Task) aggregateService.clone(task, new Settings());
		
		assert(task != clonedTask);
		assert(clonedTask.getId() != null);	
		assert(clonedTask.getAssignedTo() != null);
		assert(clonedTask.getAssignedTo().getId() != null);		
	}
}
