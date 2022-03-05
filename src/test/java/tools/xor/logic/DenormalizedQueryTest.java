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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DenormalizedQueryTest extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;

	final String NAME = "GEORGE_WASHINGTON_1";
	final String DISPLAY_NAME = "George Washington";
	final String DESCRIPTION = "First President of the United States of America";
	final String USER_NAME = "gwashington";	

	/**
	 * Should get an empty excel file with a header containing
	 * the field names
	 * @throws FileNotFoundException 
	 */
	public void queryEmpty() throws FileNotFoundException {

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Person.class));
		settings.setDenormalized(true);
		settings.setView(aggregateService.getView("BASICINFO"));
		
		FileOutputStream out = new FileOutputStream("queryEmpty.xlsx");
		aggregateService.exportDenormalized(out, settings);
	}	

	public void queryOne() throws FileNotFoundException {

		// create person
		Person person = new Person();
		person.setName(NAME);
		person.setDisplayName(DISPLAY_NAME);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		person = (Person) aggregateService.create(person, new Settings());	

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Person.class));
		settings.setDenormalized(true);
		settings.setView(aggregateService.getView("BASICINFO"));
		
		FileOutputStream out = new FileOutputStream("queryOne.xlsx");
		aggregateService.exportDenormalized(out, settings);
	}

	public void queryPersonNative() throws FileNotFoundException {

		// create person
		Person person = new Person();
		person.setName(NAME);
		person.setDisplayName(DISPLAY_NAME);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		person = (Person) aggregateService.create(person, new Settings());	

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Person.class));
		settings.setDenormalized(true);
		settings.setView(aggregateService.getView("BASICINFO_NATIVE"));
		
		FileOutputStream out = new FileOutputStream("queryOneNative.xlsx");
		aggregateService.exportDenormalized(out, settings);
	}	

	public void queryTaskChildren() throws FileNotFoundException {
		// Create a task
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");
		userStory = (Task) aggregateService.create(userStory, new Settings());	

		// Create 2 children
		Task A = new Task();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");
		A = (Task) aggregateService.create(A, new Settings());
		A.setTaskParent(userStory);

		Task B = new Task();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		B = (Task) aggregateService.create(B, new Settings());
		B.setTaskParent(userStory);		

		Set<Task> children = new HashSet<Task>();
		children.add(A);
		children.add(B);
		userStory.setTaskChildren(children);
		userStory = (Task) aggregateService.read(userStory, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
		settings.setDenormalized(true);		
		settings.setView(aggregateService.getView("TASKCHILDREN"));
		
		FileOutputStream out = new FileOutputStream("queryTaskChildren.xlsx");
		aggregateService.exportDenormalized(out, settings);
	}

	public void updateTaskChildren() throws IOException
	{
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
		settings.setView(aggregateService.getView("TASKCHILDREN"));

		FileInputStream is = new FileInputStream("queryTaskChildren.xlsx");
		aggregateService.importDenormalized(is, settings);
	}
}
