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
import tools.xor.AssociationSetting;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.db.base.Technician;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultReadOperation extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;

	public void readPerson() {
		// create person
		Person person = new Person();
		person.setName("DILIP_DALTON");
		person.setDisplayName("Dilip Dalton");
		person.setDescription("Software engineer in the bay area");
		person.setUserName("daltond");

		person = (Person) aggregateService.create(person, new Settings());	
		person = (Person) aggregateService.read(person, new Settings());
		
		assert(person.getId() != null);
	}

	public void readTask() {
		// create person
		Technician owner = new Technician();
		owner.setName("TOMMY_HILFIGHER");
		owner.setDisplayName("Tommy Hilfigher");
		owner.setDescription("A famous fashion designer");
		owner.setUserName("thilf");
		owner.setSkill("fashion design");
		owner = (Technician) aggregateService.create(owner, new Settings());	
		Person person = (Person) aggregateService.read(owner, new Settings());
		
		// Create Task
		Task task = new Task();
		task.setName("CREATE_GOWN");
		task.setDisplayName("Create wedding gown");
		task.setDescription("Design a wedding gown");
		task.setAssignedTo(person);
		task = (Task) aggregateService.create(task, getSettings());

		assert(task.getId() != null);
		assert(task.getAssignedTo() != null);
		assert(task.getAssignedTo().getId() != null);

		Settings settings = getSettings();
		settings.expand(new AssociationSetting("assignedTo.name"));
		task = (Task) aggregateService.read(task, settings);  // treat Technician association as "part of"	

		assert(task.getId() != null);
		assert(task.getName().equals("CREATE_GOWN"));
		
		person = task.getAssignedTo();
		assert(person != null);
		assert(person.getId() != null);
		assert(person.getName().equals("TOMMY_HILFIGHER"));
	}	
}
