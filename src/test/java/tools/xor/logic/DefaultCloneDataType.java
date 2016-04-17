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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.db.base.Technician;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultCloneDataType extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;	

	public void cloneDataType() {

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
		Date finish = new Date();
		task.setScheduledFinish(finish);
		task = (Task) aggregateService.create(task, getSettings());
		task = (Task) aggregateService.read(task, getSettings());		
		
		// Ensure task is persisted
		assert(task.getId() != null);
		assert(task.getAssignedTo() != null);
		assert(task.getAssignedTo().getId() != null);
		
		// ensure change the date in the from instance does not affect the date object in the to instance
		// We need to make sure a clone is being made and the same object is not referenced
		finish.setTime(finish.getTime() + TimeUnit.DAYS.toMillis( 1 ));
		assert(finish.getTime() != task.getScheduledFinish().getTime());
	}
}
