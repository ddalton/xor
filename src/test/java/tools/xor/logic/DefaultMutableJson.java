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
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.AssociationSetting;
import tools.xor.ImmutableJsonProperty;
import tools.xor.Settings;
import tools.xor.db.base.Employee;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.db.pm.TaskDetails;
import tools.xor.service.AggregateManager;
import tools.xor.view.AggregateView;

public class DefaultMutableJson extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;
	
	protected void checkStringField() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		
		Settings settings = new Settings();
		settings.setEntityClass(Person.class);
		Person person = (Person) aggregateService.create(json, settings);	
		assert(person.getId() != null);
		assert(person.getName().equals("DILIP_DALTON"));
		
		Object jsonObject = aggregateService.read(person, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));

	}

	protected void checkDateField() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		
		// 1/1/15 7:00 PM EST
		final long CREATED_ON = 1420156800000L;
		Date createdOn = new Date(CREATED_ON);
		DateFormat df = new SimpleDateFormat(ImmutableJsonProperty.ISO8601_FORMAT);
		json.put("createdOn", df.format(createdOn));
		
		Settings settings = new Settings();
		settings.setEntityClass(Person.class);
		Person person = (Person) aggregateService.create(json, settings);	
		assert(person.getId() != null);
		assert(person.getName().equals("DILIP_DALTON"));
		assert(person.getCreatedOn().getTime() == CREATED_ON);
		
		Object jsonObject = aggregateService.read(person, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(json.get("name").toString().equals("DILIP_DALTON"));		
		assert((json.get("createdOn")).toString().equals("2015-01-01T16:00:00.000-0800"));
	}
	
	protected void checkIntField() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		json.put("employeeNo", 235);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(json, settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getEmployeeNo() == 235);
		
		Object jsonObject = aggregateService.read(employee, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));		
		assert( Integer.valueOf((json.get("employeeNo")).toString()) == 235);
	}	

	protected void checkLongField() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		json.put("salary", 100000);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(json, settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getSalary() == 100000);
		
		Object jsonObject = aggregateService.read(employee, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));	
		assert(Integer.valueOf((json.get("salary")).toString()) == 100000);
	}	

	protected void checkEmptyLongField() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(json, settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getSalary() == null);
		assert(!employee.getIsCriticalSystemObject());
		
		Object jsonObject = aggregateService.read(employee, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string, empty: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));		
		assert( !json.has("salary") );
	}	
	
	protected void checkBooleanField() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		json.put("isCriticalSystemObject", true);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(json, settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getIsCriticalSystemObject());
		
		Object jsonObject = aggregateService.read(employee, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));		
		assert(json.getBoolean("isCriticalSystemObject"));
	}

	protected void checkBigDecimalField() throws JSONException {
		final BigDecimal largeDecimal = new BigDecimal("12345678998765432100000.123456789987654321");
		
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		json.put("largeDecimal", largeDecimal);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(json, settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getLargeDecimal().equals(largeDecimal) );
		
		Object jsonObject = aggregateService.read(employee, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));	
		assert( new BigDecimal( json.get("largeDecimal").toString() ).equals(largeDecimal));
	}
	
	protected void checkBigIntegerField() throws JSONException {
		final BigInteger largeInteger = new BigInteger("12345678998765432100000123456789987654321");
		
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		json.put("largeInteger", largeInteger);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(json, settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getLargeInteger().equals(largeInteger) );
		
		Object jsonObject = aggregateService.read(employee, settings);
		json = (JSONObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert((json.get("name")).toString().equals("DILIP_DALTON"));			
		assert( new BigInteger( json.get("largeInteger").toString() ).equals(largeInteger));
	}	
	
	protected void checkEntityField() throws JSONException {
		final String TASK_NAME = "SETUP_DSL";
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		json.put("openField", "Success");

		// Create quote
		final BigDecimal price =  new BigDecimal("123456789.987654321");
		JSONObject quote = new JSONObject();
		quote.put("price", price);
		json.put("quote", quote);
		
		Settings settings = getSettings();
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(json, settings);	
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getQuote() != null);
		assert(task.getQuote().getId() != null);
		assert(task.getQuote().getPrice().equals(price));		
		
		Object jsonObject = aggregateService.read(task, settings);
		JSONObject jsonTask = (JSONObject) jsonObject;
		System.out.println("JSON string: " + jsonTask.toString());	
		assert( (jsonTask.get("name")).toString().equals(TASK_NAME));
		JSONObject jsonQuote = jsonTask.getJSONObject("quote");
		assert( new BigDecimal( jsonQuote.get("price").toString() ).equals(price));
	}
	
	protected void checkSetField() throws JSONException {
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		json.put("openField", "Success");

		// Create and add 1 child task
		JSONObject child1 = new JSONObject();
		child1.put("name", CHILD_TASK_NAME);
		child1.put("displayName", "Task 1");
		child1.put("description", "This is the first child task");
		child1.put("openField", "Success");
		
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(child1);
		json.put("taskChildren", jsonArray);
		
		Settings settings = getSettings();
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(json, settings);	
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getTaskChildren() != null);
		System.out.println("Children size: " + task.getTaskChildren().size());
		assert(task.getTaskChildren().size() == 1);
		for(Task child: task.getTaskChildren()) {
			System.out.println("Task name: " + child.getName());
		}
		
		Object jsonObject = aggregateService.read(task, settings);
		JSONObject jsonTask = (JSONObject) jsonObject;
		System.out.println("JSON string for object: " + jsonTask.toString());	
		assert(jsonTask.get("name").toString().equals(TASK_NAME));
		JSONArray jsonChildren = jsonTask.getJSONArray("taskChildren");
		assert(((JSONArray)jsonChildren).length() == 1);
	}

	/**
	 * Add two new fields price and quantity and check to see if it appears in the JSON
	 * string
	 * @throws JSONException
	 */
	protected void checkOpenField() throws JSONException {
		final String TASK_NAME = "SETUP_DSL";

		AggregateView view = new AggregateView();
		List path = new ArrayList();
		path.add("name");
		path.add("displayName");
		path.add("description");
		path.add("quote.price");
		path.add("ItemList"); // open property
		view.setAttributeList(path);

		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		json.put("openField", "Success");

		// Create quote
		final BigDecimal price =  new BigDecimal("123456789.987654321");
		JSONObject quote = new JSONObject();
		quote.put("price", price);
		json.put("quote", quote);

		Settings settings = getSettings();
		settings.setView(view);
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getQuote() != null);
		assert(task.getQuote().getId() != null);
		assert(task.getQuote().getPrice().equals(price));

		Object jsonObject = aggregateService.read(task, settings);
		JSONObject jsonTask = (JSONObject) jsonObject;
		System.out.println("JSON string: " + jsonTask.toString());
		assert( (jsonTask.get("name")).toString().equals(TASK_NAME));
		assert( (jsonTask.get("openField")).toString().equals("Success"));
		JSONObject jsonQuote = jsonTask.getJSONObject("quote");
		assert( new BigDecimal( jsonQuote.get("price").toString() ).equals(price));
	}
	
	protected void checkExcelExport() throws JSONException, IOException {
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		json.put("openField", "Success");

		// Create and add 1 child task
		JSONObject child1 = new JSONObject();
		child1.put("name", CHILD_TASK_NAME);
		child1.put("displayName", "Task 1");
		child1.put("description", "This is the first child task");
		child1.put("openField", "Success");
		
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(child1);
		json.put("taskChildren", jsonArray);
		
		JSONObject childDetails = new JSONObject();
		childDetails.put("version", "0");
		child1.put("taskDetails", childDetails);
		
		Settings settings = getSettings();
		settings.addAssociation( new AssociationSetting(TaskDetails.class));
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(json, settings);	
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getTaskChildren() != null);
		System.out.println("Children size: " + task.getTaskChildren().size());
		assert(task.getTaskChildren().size() == 1);
		for(Task child: task.getTaskChildren()) {
			System.out.println("Task name: " + child.getName());
		}
		for(Task child: task.getTaskChildren()) {
			assert(child.getId() != null);
		}
		
	    FileOutputStream out = new FileOutputStream("taskExcel.xlsx");
		aggregateService.exportAggregate(out, task, settings);
	}	
	
	protected void checkExcelImport() throws JSONException, IOException {
		FileInputStream in = new FileInputStream("taskOneChild.xlsx");
		
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";		
		
		Settings settings = getSettings();
		settings.addAssociation( new AssociationSetting(TaskDetails.class));		
		Task task = (Task) aggregateService.importAggregate(in, settings);
		
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getTaskChildren() != null);
		assert(task.getTaskChildren().size() == 1);
		for(Task child: task.getTaskChildren()) {
			assert(child.getName().equals(CHILD_TASK_NAME));
		}
		for(Task child: task.getTaskChildren()) {
			assert(child.getId() != null);
		}		
	}
	
	protected void checkExcelImport100() throws JSONException, IOException {
		FileInputStream in = new FileInputStream("task100Child.xlsx");
		
		final String TASK_NAME = "SETUP_DSL";
		
		Settings settings = getSettings();
		settings.addAssociation( new AssociationSetting(TaskDetails.class));		
		Task task = (Task) aggregateService.importAggregate(in, settings);
		
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getTaskChildren() != null);
		assert(task.getTaskChildren().size() == 100);
		for(Task child: task.getTaskChildren()) {
			assert(child.getId() != null);
		}		
	}	
	
	
	/**
	 * In this test we will check the save of an entity association
	 * referenced as a primitive (String) field.
	 * 
	 *        subTask (String)
	 *  Task ------------------> Task
	 *  
	 *  The field subTask is a String field, and refers to the id
	 *  of the subTask.
	 *  
	 *  The following steps are involved in this test:
	 *  1. Setup the model to have this association modelled as a Task
	 *     using an open property
	 *  2. Create the test data using JSON object.
	 *     The Open property setter should ensure that we get the id 
	 *     from the subTask field and get the persistence managed object
	 *     If this object is not present, then create it.
	 *     Delegate most of this work to the framework.
	 *  3. Check that the data is saved correctly
	 *  4. Read the data and ensure the data is modelled
	 *     correctly as a Task object referenced by the open property
	 *     name
	 *  
	 */
	protected void checkOpenFieldEntityToOne() {
		final String TASK_NAME = "SETUP_DSL";
		final String SUB_TASK_NAME = "SETUP_WIRING";
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		
		// Create subTask
		JSONObject subTask = new JSONObject();
		subTask.put("name", SUB_TASK_NAME);
		subTask.put("displayName", "Setup Wiring");
		subTask.put("description", "Establish wiring from the external line to the exterior of the home");
		json.put("subTaskObj", subTask);
		
		Settings settings = getSettings();
		settings.setSupportsPostLogic(true);
		settings.addAssociation( new AssociationSetting("subTaskObj"));
		settings.setEntityClass(Task.class);	
		Task task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
		
		// Make sure the subTask contains the id of the subTaskObj object
		assert(task.getSubTask() != null);
		
		// Now read the object and see if the subTaskObj was created
		Object jsonObject = aggregateService.read(task, settings);		
		JSONObject jsonTask = (JSONObject) jsonObject;
		JSONObject subTaskJson = jsonTask.getJSONObject("subTaskObj");
		assert(subTaskJson.get("name").equals(SUB_TASK_NAME));
		//System.out.println("{}{}{}{}{}{}{} JSON string: " + jsonTask.toString());			
	}
	
	protected void checkOpenFieldEntityToOneGrandchild() {
		final String TASK_NAME = "SETUP_DSL";
		final String SUB_TASK_NAME = "SETUP_WIRING";
		final String GC_TASK_NAME = "SCHEDULE_APPT";		
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		
		// Create subTask
		JSONObject subTask = new JSONObject();
		subTask.put("name", SUB_TASK_NAME);
		subTask.put("displayName", "Setup Wiring");
		subTask.put("description", "Establish wiring from the external line to the exterior of the home");
		json.put("subTaskObj", subTask);
		
		// Create Grandchild task
		JSONObject gcTask = new JSONObject();
		gcTask.put("name", GC_TASK_NAME);
		gcTask.put("displayName", "Schedule Appointment");
		gcTask.put("description", "Schedule appointment for the internet installer");
		subTask.put("subTaskObj", gcTask);		
		
		Settings settings = getSettings();
		settings.setSupportsPostLogic(true);
		settings.addAssociation( new AssociationSetting("subTaskObj"));
		settings.setEntityClass(Task.class);	
		Task task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
		
		// Make sure the subTask contains the id of the subTaskObj object
		assert(task.getSubTask() != null);
		
		// Now read the object and see if the subTaskObj was created
		Object jsonObject = aggregateService.read(task, settings);		
		JSONObject jsonTask = (JSONObject) jsonObject;
		JSONObject subTaskJson = jsonTask.getJSONObject("subTaskObj");
		assert(subTaskJson.get("name").equals(SUB_TASK_NAME));
		JSONObject gcJson = subTaskJson.getJSONObject("subTaskObj");
		assert(gcJson.get("name").equals(GC_TASK_NAME));		
		System.out.println("{}{}{}{}{}{}{} JSON string: " + jsonTask.toString());			
	}
	
	protected void checkExternalData() throws JSONException {
		final String TASK_NAME = "SETUP_DSL";
		final String TASK_URI = "http://www.att.com";
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");
		json.put("taskUri", TASK_URI);
		
		Settings settings = getSettings();
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(json, settings);	
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getTaskUri().equals(TASK_URI));
		
		settings.addTag("External Data");
		settings.setExternalData(5);
		Object jsonObject = aggregateService.read(task, settings);
		JSONObject jsonTask = (JSONObject) jsonObject;
		assert( (jsonTask.get("taskUri")).toString().equals(TASK_URI));
	}	
}
