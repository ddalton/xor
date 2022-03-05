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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import tools.xor.AbstractDBTest;
import tools.xor.AggregateAction;
import tools.xor.AssociationSetting;
import tools.xor.EntitySize;
import tools.xor.EntityType;
import tools.xor.FunctionType;
import tools.xor.ImmutableJsonProperty;
import tools.xor.OpenType;
import tools.xor.Settings;
import tools.xor.db.base.Department;
import tools.xor.db.base.Employee;
import tools.xor.db.base.LocationDetails;
import tools.xor.db.base.Manager;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Project;
import tools.xor.db.pm.Task;
import tools.xor.db.pm.TaskDetails;
import tools.xor.db.sp.P;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.DomainShape;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.TypeGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.OQLQuery;
import tools.xor.view.View;

public abstract class DefaultMutableJson extends AbstractDBTest {
	@Resource(name = "aggregateManager")
	protected AggregateManager aggregateService;

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}
	
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
		final BigDecimal largeDecimal = new BigDecimal("1234567890000.1987654321");
		
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
		final BigInteger largeInteger = new BigInteger("12001239987654321");

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

		AggregateView view = new AggregateView("OPEN_FIELD_VIEW");
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
		settings.expand(new AssociationSetting(TaskDetails.class));
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
		
	    //FileOutputStream out = new FileOutputStream("taskExcel.xlsx");
		//aggregateService.exportAggregate(out, task, settings);
		aggregateService.exportAggregate("taskExcel.xlsx", task, settings);
	}


	protected void checkCSVExport() throws JSONException, IOException {
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
		settings.expand(new AssociationSetting(TaskDetails.class));
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

		// delete existing folder
		String folder = "taskcsv/";
		File directory = new File(folder);
		if(directory.exists()) {
			FileUtils.deleteDirectory(directory);
		}

		aggregateService.exportCSV(folder, task, settings);
	}


	protected void checkExcelExportView() throws JSONException, IOException {
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";
		
		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");

		// Create and add 1 child task
		JSONObject child1 = new JSONObject();
		child1.put("name", CHILD_TASK_NAME);
		child1.put("displayName", "Task 1");
		child1.put("description", "This is the first child task");
		
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(child1);
		json.put("taskChildren", jsonArray);
		
		JSONObject childDetails = new JSONObject();
		childDetails.put("version", "0");
		child1.put("taskDetails", childDetails);
		
		AggregateView view = new AggregateView("VIEW_STREAM");
		List path = new ArrayList();
		path.add("id");
		path.add("name");
		path.add("displayName");
		path.add("description");
		view.setAttributeList(path);
		

		Settings settings = new Settings();
		settings.addFunction(FunctionType.ASC, 1, "name");
		settings.setView(view);		
		settings.expand(new AssociationSetting(TaskDetails.class));
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
		
	    //FileOutputStream out = new FileOutputStream("taskExcelChunking.xlsx");
		//aggregateService.exportAggregate(out, task, settings);
		aggregateService.exportAggregate("taskExcelChunking.xlsx", task, settings);
	}		
	
	protected void checkExcelImport() throws JSONException, IOException {
		//FileInputStream in = new FileInputStream("taskOneChild.xlsx");
		
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";		
		
		Settings settings = getSettings();
		settings.expand(new AssociationSetting(TaskDetails.class));
		//Task task = (Task) aggregateService.importAggregate(in, settings);
		List result = (List)aggregateService.importAggregate("taskOneChild.xlsx", settings);
		Task task = (Task) result.get(0);
		
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
		//FileInputStream in = new FileInputStream("task100Child.xlsx");
		
		final String TASK_NAME = "SETUP_DSL";
		
		Settings settings = getSettings();
		settings.expand(new AssociationSetting(TaskDetails.class));
		//Task task = (Task) aggregateService.importAggregate(in, settings);
		List result = (List) aggregateService.importAggregate("task100Child.xlsx", settings);
		Task task = (Task) result.get(0);
		
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
		settings.setPostFlush(true);
		settings.expand(new AssociationSetting("subTaskObj"));
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
		settings.expand(new AssociationSetting("subTaskObj"));
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
	
	protected void checkOpenFieldQuery() {
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
		settings.expand(new AssociationSetting("subTaskObj"));
		settings.setEntityClass(Task.class);	
		Task task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
		
		AggregateView view = new AggregateView("OPEN_FIELD_QUERY");
		List path = new ArrayList();
		path.add("id");
		path.add("name");
		path.add("displayName");
		path.add("description");
		path.add("subTaskObj.id");
		path.add("subTaskObj.name");
		path.add("subTaskObj.displayName");
		path.add("subTaskObj.description");	
		path.add("subTaskObj.subTaskObj.id");
		path.add("subTaskObj.subTaskObj.name");
		path.add("subTaskObj.subTaskObj.displayName");
		path.add("subTaskObj.subTaskObj.description");			
		view.setAttributeList(path);		
		
		// Make sure the subTask contains the id of the subTaskObj object
		assert(task.getSubTask() != null);
		
		settings = new Settings();
		settings.setView(view);		
		settings.setEntityClass(Task.class);		
		settings.setDenormalized(true);
		List<?> result = aggregateService.query(null, settings);		
		assert(result.size() == 2);
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

	protected void checkOpenFieldPaging() {
		// Task Tree 1
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
		settings.expand(new AssociationSetting("subTaskObj"));
		settings.setEntityClass(Task.class);	
		Task task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);

		// Task Tree 2	

		// Create task
		json = new JSONObject();
		json.put("name", "HOMEWORK");
		json.put("displayName", "Homework");
		json.put("description", "Homework from school");

		// Create subTask
		subTask = new JSONObject();
		subTask.put("name", "MATH_HOMEWORK");
		subTask.put("displayName", "Math's homework");
		subTask.put("description", "Complete math homework");
		json.put("subTaskObj", subTask);

		// Create Grandchild task
		gcTask = new JSONObject();
		gcTask.put("name", "ALGEBRA");
		gcTask.put("displayName", "Algebra problems");
		gcTask.put("description", "Complete the algebra part of the math homework");
		subTask.put("subTaskObj", gcTask);		

		settings = getSettings();
		settings.setSupportsPostLogic(true);
		settings.expand(new AssociationSetting("subTaskObj"));
		settings.setEntityClass(Task.class);	
		task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
		
		// Task tree 3

		// Create task
		json = new JSONObject();
		json.put("name", "ATHLETICS");
		json.put("displayName", "Athletics");
		json.put("description", "The athletics programs the child participates in");

		// Create subTask
		subTask = new JSONObject();
		subTask.put("name", "TRACK_FIELD");
		subTask.put("displayName", "Track & Field");
		subTask.put("description", "Track and field programs");
		json.put("subTaskObj", subTask);

		// Create Grandchild task
		gcTask = new JSONObject();
		gcTask.put("name", "DISTANCE_RUNNING");
		gcTask.put("displayName", "Long distance running");
		gcTask.put("description", "The 1 mile run");
		subTask.put("subTaskObj", gcTask);		

		settings = getSettings();
		settings.setSupportsPostLogic(true);
		settings.expand(new AssociationSetting("subTaskObj"));
		settings.setEntityClass(Task.class);	
		task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
			
		
		AggregateView view = new AggregateView("OPEN_FIELD_QUERY");
		List path = new ArrayList();
		path.add("id");
		path.add("name");
		path.add("displayName");
		path.add("description");
		path.add("subTaskObj.id");
		path.add("subTaskObj.name");
		path.add("subTaskObj.displayName");
		path.add("subTaskObj.description");	
		path.add("subTaskObj.subTaskObj.id");
		path.add("subTaskObj.subTaskObj.name");
		path.add("subTaskObj.subTaskObj.displayName");
		path.add("subTaskObj.subTaskObj.description");	
		view.setAttributeList(path);		


		settings = new Settings();
		settings.addFunction(FunctionType.ASC, 1, "name");
		settings.setView(view);		
		settings.setEntityClass(Task.class);		
		settings.setDenormalized(true);
		settings.setLimit(2);
		List<?> result = aggregateService.query(null, settings);		
		assert(result.size() == 3);	
		
		// We proceed from the nextToken, so we should return the remaining single row
		result = aggregateManager.query(null, settings);
		assert(result.size() == 2);		
	}
	
	/*
		  Table name    Purpose                       Key
		  ------------------------------------------------------
		  S             Suppliers                     (S#)
		  P             Parts                         (P#)
		  SP            Parts supplied by Suppliers   (S#, P#)
		
		The full column set of each table, and the data they typically contain for example uses, are shown below. 
		  S
		  S#  SNAME  STATUS   CITY
		  ----------------------------
		  S1  Smith  20       London
		  S2  Jones  10       Paris
		  S3  Blake  30       Paris
		  S4  Clark  20       London
		  S5  Adams  30       Athens
		
		  P
		  P#  PNAME  COLOR  WEIGHT   CITY
		  ----------------------------------
		  P1  Nut    Red    12.0    London
		  P2  Bolt   Green  17.0    Paris
		  P3  Screw  Blue   17.0    Oslo
		  P4  Screw  Red    14.0    London
		  P5  Cam    Blue   12.0    Paris
		  P6  Cog    Red    19.0    London
		
		  SP
		  S#  P#  QTY
		  ------------
		  S1  P1  300
		  S1  P2  200
		  S1  P3  400
		  S1  P4  200
		  S1  P5  100
		  S1  P6  100
		  S2  P1  300
		  S2  P2  400
		  S3  P2  200
		  S4  P2  200
		  S4  P4  300
		  S4  P5  400
	 */
	
	protected abstract void createSPData();
	
	public void checkOpenPropertyCollection() {
		createSPData();
		
		// We are going to test the following
		// P1.suppliers
		// We should return a collection of {S1, S2}
		
        AggregateView view = new AggregateView("TestSP");
        List path = new ArrayList();
        path.add("supplierParts.supplierNo");
        path.add("supplierParts.partNo");
        path.add("supplierParts.qty");
        path.add("partNo");
        path.add("pname");
        path.add("color");
        path.add("weight");
        path.add("city");
        view.setAttributeList(path);

        Settings settings = new Settings();
        settings.setEntityClass(P.class);
        settings.setView(view);
		settings.expand(new AssociationSetting("supplierParts"));
		settings.setEntityClass(P.class);	

		
		// Now read the object and see if the subTaskObj was created
		P p1 = new P();
		p1.setPartNo("P1");
		Object jsonObject = aggregateService.read(p1, settings);		
		JSONObject jsonP = (JSONObject) jsonObject;
		JSONArray spJson = jsonP.getJSONArray("supplierParts");	
		assert(spJson.length() == 2);
		//System.out.println("{}{}{}{}{}{}{} JSON string: " + jsonTask.toString());			
	}
	
	public void checkOpenPropertyCollectionUpdate() {
		createSPData();
		
		// We are going to test the following
		// P1.suppliers
		// We should return a collection of {S1, S2}
		
        AggregateView view = new AggregateView("TestSP");
        List path = new ArrayList();
        path.add("supplierParts.supplierNo");
        path.add("supplierParts.partNo");
        path.add("supplierParts.qty");
        path.add("partNo");
        path.add("pname");
        path.add("color");
        path.add("weight");
        path.add("city");
        view.setAttributeList(path);

        Settings settings = new Settings();
        settings.setEntityClass(P.class);
        settings.setView(view);
		settings.expand(new AssociationSetting("supplierParts"));
		settings.setEntityClass(P.class);	

		
		// Now read the object and see if the subTaskObj was created
		P p1 = new P();
		p1.setPartNo("P1");
		Object jsonObject = aggregateService.read(p1, settings);		
		JSONObject jsonP = (JSONObject) jsonObject;
		JSONArray spJson = jsonP.getJSONArray("supplierParts");	
		assert(spJson.length() == 2);
		
		settings = new Settings();
		settings.addFunction(FunctionType.ASC, 1, "supplierParts.partNo");
		settings.addFunction(FunctionType.ASC, 2, "supplierParts.supplierNo");
		settings.setView(view);		
		settings.setEntityClass(P.class);		
		List<?> result = aggregateService.query(null, settings);	
		assert(result.size() > 0);
		//System.out.println("{}{}{}{}{}{}{} JSON string: " + jsonTask.toString());		
		
		// Change the quantity value for Supplier parts P1-S1 from 300 to 500
		
	}

	public void checkOpenTypeCrossJoin()
	{
		createSPData();


		Set<String> properties = new HashSet<String>();
		properties.add(tools.xor.db.sp.P.class.getName() + OpenType.DELIM + "partNo");
		properties.add(tools.xor.db.sp.S.class.getName() + OpenType.DELIM + "supplierNo");
		OpenType crossJoin = new OpenType("crossjoin", properties);
		aggregateManager.getTypeMapper().addType(crossJoin);

		Settings settings = new Settings();
		settings.setEntityType(crossJoin);
		AggregateView view = new AggregateView("CROSS");
		view.setShape(new DomainShape("test", null, null));
		settings.setView(view);
		view.setAttributeList(new ArrayList<String>(properties));

		OQLQuery q = new OQLQuery();
		q.setSelectClause("SELECT p.partNo, s.supplierNo FROM S s, P p");
		view.setUserOQLQuery(q);

		List o = aggregateService.query(null, settings);
		assert(o.size() == 30);
		System.out.println("OQL Query Output size : " + o.size());
	}

	public void importCSV() throws Exception
	{
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
		settings.init(aggregateService.getDataModel().getShape());

		aggregateService.importCSV("bulk/", settings);

		// query the task object
		settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
		settings.setDenormalized(true);
		settings.setView(aggregateService.getView("TASKCHILDREN"));
		List<?> result = aggregateService.query(null, settings);

		// Includes header row
		System.out.println("!!!SIZE: " + result.size());
		assert(result.size() == 3);
	}

	protected void generateMediumSizedEntity() throws IOException
	{
		DataModel das = aggregateManager.getDataModel();
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
		Settings settings = new Settings();
		settings.setEntityType(taskType);
		settings.expand(new AssociationSetting(Person.class));
		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(taskType);

		settings.setSparseness(1.0f);
		JSONObject task = (JSONObject) sg.generateObjectGraph(settings);
		System.out.println("Task name: " + task.get("name"));

		/* children could be null as this is a random graph
		Object children = task.get("taskChildren");
		assert(children instanceof JSONArray);

		JSONArray childrenArray = (JSONArray) children;
		assert(childrenArray.length() > 0);
		*/

		// Try and persist this now
		settings.setGraphFileName("Graph_1.0sparseness.dot");
		Task persistedTask = (Task) aggregateManager.create(task, settings);

		aggregateService.exportAggregate("taskRandomMedium.xlsx", persistedTask, settings);
	}

	protected void generateMediumSizedDomainEntity() throws IOException
	{
		DataModel das = aggregateManager.getDataModel();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("DomainValues.xlsx");
		das.initGenerators(inputStream);

		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
		Settings settings = new Settings();
		settings.setEntityType(taskType);
		settings.expand(new AssociationSetting(Person.class));
		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(taskType);
		settings.setGraphFileName("TaskMediumStateGraph.dot");
		sg.generateVisual(settings);
		settings.setGraphFileName(null);

		JSONObject task = (JSONObject) sg.generateObjectGraph(new Settings());
		System.out.println("Task name: " + task.get("name"));

		Object children = task.get("taskChildren");
		assert(children instanceof JSONArray);

		JSONArray childrenArray = (JSONArray) children;
		assert(childrenArray.length() > 0);

		// Try and persist this now
		Task persistedTask = (Task) aggregateManager.create(task, settings);

		aggregateService.exportAggregate("taskRandomMediumDomain.xlsx", persistedTask, settings);
	}

	/**
	 * Create 2 task objects having the same ownedBy object. Natural key is userName.
	 */
	protected void sharedOwnedBy() {

		// Create Person
		JSONObject person = new JSONObject();
		person.put("userName", "tcostner");
		person.put("commonName", "Timothy Costner");

		// Create task1
		JSONObject json1 = new JSONObject();
		json1.put("name", "SETUP_DSL");
		json1.put("displayName", "Setup DSL");
		json1.put("description", "Setup high-speed broadband internet using DSL technology");
		json1.put("ownedBy", person);

		// Create task2
		JSONObject json2 = new JSONObject();
		json2.put("name", "HOMEWORK");
		json2.put("displayName", "Homework");
		json2.put("description", "Homework from school");
		json2.put("ownedBy", person); // Should share the person reference - check

		DataModel das = aggregateManager.getDataModel();
		EntityType personType = (EntityType) das.getShape().getType(Person.class);
		personType.setNaturalKey(new String[] { "userName"});
		Settings settings = new Settings();
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
		settings.setEntityType(taskType);
		settings.expand(new AssociationSetting(Person.class));
		settings.init(das.getShape());
		//settings.setPostFlush(true);

		// Try and persist task 1 now
		List<Object> entityBatch = new LinkedList<>();
		entityBatch.add(json1);
		entityBatch.add(json2);
		//Task persistedTask1 = (Task) aggregateManager.create(json1, settings);
		//Task persistedTask2 = (Task) aggregateManager.create(json2, settings);

		// Ensure both tasks point to the same Person instance
		//Person p = persistedTask1.getOwnedBy();
		Object obj = aggregateManager.create(entityBatch, settings);

		assert(obj instanceof List);
		if(obj instanceof List) {
			List list = (List) obj;
			assert(list.size() == 2);

			Task t1 = (Task) list.get(0);
			Task t2 = (Task) list.get(1);

			assert(t1.getOwnedBy() == t2.getOwnedBy());
		}
	}

	protected void generatePicture() {
		DataModel das = aggregateService.getDataModel();
		EntityType personType = (EntityType) das.getShape().getType(Person.class);
		View view = das.getShape().getBaseView(personType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.setEntityType(personType);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(personType);
		JSONObject jsonObject = sg.generateObjectGraph(settings);

		System.out.println("Generated jsonObject");

		// Try and persist this now
		Person persistedPerson = (Person) aggregateManager.create(jsonObject, settings);

		try {
			assert(persistedPerson.getPhoto() != null);
			Blob blob = persistedPerson.getPhoto();
			byte [] array = blob.getBytes( 1, ( int ) blob.length() );
			File file = new File("PersonPhoto.png");
			FileOutputStream out = new FileOutputStream( file );
			out.write( array );
			out.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(persistedPerson.getUserName());
	}

	protected void checkRegexView() throws JSONException {
		final String TASK_NAME = "SETUP_DSL";

		AggregateView view = new AggregateView("REGEX_VIEW");
		List path = new ArrayList();
		path.add("(name|displayName|description)");
		view.setAttributeList(path);

		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");

		Settings settings = getSettings();
		settings.setView(view);
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(json, settings);
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));

		Object jsonObject = aggregateService.read(task, settings);
		JSONObject jsonTask = (JSONObject) jsonObject;
		System.out.println("JSON string: " + jsonTask.toString());
		assert( (jsonTask.get("name")).toString().equals(TASK_NAME));
	}

	public void testEmployeeType() {
		DataModel das = aggregateService.getDataModel();
		EntityType employeeType = (EntityType)das.getShape().getType(Employee.class);
		View view = das.getShape().getView(employeeType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.setEntityType(employeeType);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(employeeType);
		settings.setGraphFileName("EmployeeAggregateStateGraph.png");
		sg.generateVisual(settings);
	}

	public void exportEmployee () throws IOException
	{

		try {
			DataModel das = aggregateService.getDataModel();

			EntityType employeeType = (EntityType)das.getShape().getType(Employee.class);
			View view = das.getShape().getBaseView(employeeType).copy();
			Settings settings = new Settings();
			settings.setView(view);
			settings.setEntityType(employeeType);
			settings.expand(new AssociationSetting(LocationDetails.class));
			//settings.expand(new AssociationSetting(ParkingSpot.class));

			settings.setEntitySize(EntitySize.LARGE);

			settings.init(das.getShape());
			TypeGraph sg = settings.getView().getTypeGraph(employeeType);
			settings.setGraphFileName("EmployeeStateGraph.png");
			sg.generateVisual(settings);

			settings.setSparseness(0.01f);
			JSONObject employee = (JSONObject)sg.generateObjectGraph(settings);

			// Try and persist this now
			settings.setGraphFileName("EmployeeGraph.png");
			settings.setPostFlush(true);
			Employee persistedEmployee = (Employee)aggregateService.update(employee, settings);

			assert(persistedEmployee.getLocation() != null);

			aggregateService.exportAggregate("src/test/resources/EmployeeRandomMedium.xlsx", persistedEmployee, settings);

			aggregateService.delete(persistedEmployee, settings);

			settings.setPreClear(true);

			// Import the employee
			settings.setAction(AggregateAction.UPDATE);
			Object obj = aggregateService.importAggregate("EmployeeRandomMedium.xlsx", settings);
			assert(obj != null);

			List objects = (List) obj;
			assert(objects.size() == 1);

			Employee e = (Employee)objects.get(0);
			assert(e != null && e.getLocation() != null && e.getLocation().getOfficeNumber() > 0);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateObjectGraph() throws FileNotFoundException
	{
		DataModel das = aggregateManager.getDataModel();
		//InputStream inputStream = new FileInputStream("CoursesValues.xlsx");
		//das.initGenerators(inputStream);

		Settings settings = das.settings().aggregate(Task.class).build();
		settings.setEntitySize(EntitySize.LARGE);
		TypeGraph sg = settings.getView().getTypeGraph((EntityType)settings.getEntityType());

		/*
		EntityType taskType = (EntityType)das.getType(Task.class);
		View view = das.getView(taskType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.setEntityType(taskType);
		settings.setEntitySize(EntitySize.LARGE);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(taskType);
		*/
		settings.setSparseness(0.01f);
		JSONObject task = (JSONObject)sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setGraphFileName("TaskGraph.png");
		settings.setPostFlush(true);
		aggregateManager.update(task, settings);
	}

	public void generatePersonObjectGraph() throws FileNotFoundException
	{
		DataModel das = aggregateManager.getDataModel();
		//InputStream inputStream = new FileInputStream("CoursesValues.xlsx");
		//das.initGenerators(inputStream);

		EntityType taskType = (EntityType)das.getShape().getType(Task.class);
		View view = das.getShape().getView(taskType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.expand(new AssociationSetting(Person.class));
		settings.setEntityType(taskType);
		settings.setEntitySize(EntitySize.MEDIUM);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(taskType);
		settings.setSparseness(0.01f);
		JSONObject task = (JSONObject)sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setGraphFileName("TaskPersonGraph.png");
		settings.setPostFlush(true);
		aggregateManager.update(task, settings);
	}

	public void generateCollectionObjectGraph() throws FileNotFoundException
	{
		DataModel das = aggregateManager.getDataModel();

		EntityType deptType = (EntityType)das.getShape().getType(Department.class);
		View view = das.getShape().getView(deptType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.expand(new AssociationSetting(Employee.class));
		settings.setEntityType(deptType);
		settings.setEntitySize(EntitySize.MEDIUM);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(deptType);
		settings.setGraphFileName("DeptStateGraph.png");
		sg.generateVisual(settings);

		settings.setSparseness(0.1f);
		JSONObject dept = (JSONObject)sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setGraphFileName("DeptCollectionGraph.png");
		settings.setPostFlush(true);
		aggregateManager.update(dept, settings);
	}

	public void readDifferentPersonViews() {
		DataModel das = aggregateManager.getDataModel();

		EntityType deptType = (EntityType)das.getShape().getType(Department.class);
		View view = das.getShape().getView(deptType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.expand(new AssociationSetting(Employee.class));
		settings.setEntityType(deptType);
		settings.setEntitySize(EntitySize.MEDIUM);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(deptType);

		settings.setSparseness(0.1f);
		JSONObject dept = sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setGraphFileName("DeptUpdateCollectionGraph.png");
		settings.setPostFlush(true);
		Department d = (Department)aggregateManager.update(dept, settings);

		AggregateView aggrView = new AggregateView("DIFFERENT_PERSON");
		aggrView.setTypeName(Department.class.getName());
		List<String> attributeList = new ArrayList<>();
		attributeList.add("name");
		attributeList.add("updatedBy.name");
		attributeList.add("createdBy.name");
		attributeList.add("createdBy.description");
		aggrView.setAttributeList(attributeList);

		settings = new Settings();
		settings.setView(aggrView);
		aggrView.setShape(das.getShape());
		Object extDept = aggregateManager.read(d, settings);
		assert(extDept != null);
		System.out.println("Dept: " + extDept);
	}


	public void readEmployeeNumber() {
		DataModel das = aggregateManager.getDataModel();

		EntityType deptType = (EntityType)das.getShape().getType(Department.class);
		View view = das.getShape().getView(deptType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.expand(new AssociationSetting(Employee.class));
		settings.setEntityType(deptType);
		settings.setEntitySize(EntitySize.MEDIUM);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(deptType);

		settings.setSparseness(0.1f);
		JSONObject dept = sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setPostFlush(true);
		Department d = (Department)aggregateManager.update(dept, settings);

		AggregateView employeeView = new AggregateView("EMP");
		employeeView.setTypeName(Manager.class.getName());
		List<String> aList = new ArrayList<>();
		aList.add("description");
		employeeView.setAttributeList(aList);
		List<AggregateView> children = new ArrayList<>();
		children.add(employeeView);

		AggregateView aggrView = new AggregateView("DIFFERENT_PERSON");
		aggrView.setTypeName(Department.class.getName());
		List<String> attributeList = new ArrayList<>();
		attributeList.add("name");
		attributeList.add("updatedBy.name");
		attributeList.add("createdBy.name");
		attributeList.add("createdBy.description");
		attributeList.add("employees.name");
		attributeList.add("employees.[EMP]"); // inheritance
		aggrView.setAttributeList(attributeList);
		aggrView.setChildren(children);

		settings = new Settings();
		settings.setView(aggrView);
		aggrView.setShape(das.getShape());

		sg = settings.getView().getTypeGraph(deptType);
		settings.setGraphFileName("DifferentInheritance.dot");
		sg.generateVisual(settings);

		settings.setGraphFileName(null);
		Object extDept = aggregateManager.read(d, settings);
		assert(extDept != null);
		System.out.println("Dept inheritance: " + extDept);
	}


	public void readNarrowedCollectionObjectGraph() throws FileNotFoundException
	{
		DataModel das = aggregateManager.getDataModel();

		EntityType deptType = (EntityType)das.getShape().getType(Department.class);
		View view = das.getShape().getView(deptType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.expand(new AssociationSetting(Employee.class));
		settings.setEntityType(deptType);
		settings.setEntitySize(EntitySize.MEDIUM);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(deptType);

		settings.setSparseness(0.1f);
		JSONObject dept = sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setGraphFileName("DeptUpdateCollectionGraph.png");
		settings.setPostFlush(true);
		Department d = (Department)aggregateManager.update(dept, settings);

		settings.setGraphFileName("DeptReadCollectionGraph.png");
		Object extDept = aggregateManager.read(d, settings);
		assert(extDept != null);
	}

	public void generateBoundedPersonObjectGraph() throws FileNotFoundException
	{
		DataModel das = aggregateManager.getDataModel();
		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("BoundedPerson.xlsx");
		das.initGenerators(inputStream);

		EntityType taskType = (EntityType)das.getShape().getType(Task.class);
		View view = das.getShape().getView(taskType).copy();
		Settings settings = new Settings();
		settings.setView(view);
		settings.expand(new AssociationSetting(Person.class));
		settings.setEntityType(taskType);
		settings.setEntitySize(EntitySize.MEDIUM);

		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(taskType);
		settings.setSparseness(0.01f);
		JSONObject task = (JSONObject)sg.generateObjectGraph(settings);

		// Try and persist this now
		settings.setGraphFileName("TaskBoundedPersonGraph.png");
		settings.setPostFlush(true);
		aggregateManager.update(task, settings);
	}

	protected void checkReferenceSemantics() throws JSONException {
		final String DESC = "This is dependent task that has many duplicates";

		// First create a natural key for Task based on name
		DataModel das = aggregateManager.getDataModel();
        EntityType domainTask = (EntityType)das.getTypeMapper().getDomainShape().getType(Task.class);
        EntityType externalTask = (EntityType)das.getTypeMapper().getDynamicShape().getType(domainTask.getEntityName());        
        String[] key = { "name" };
        externalTask.setNaturalKey(key);
        domainTask.setNaturalKey(key);		

		try {
			// Create task
			JSONObject json = new JSONObject();
			json.put("name", "ROOT");
			json.put("displayName", "Setup DSL");
			json.put("description", "Setup high-speed broadband internet using DSL technology");

			// Create and add 1 master dependency to the list
			JSONObject dep = new JSONObject();
			dep.put("name", "DEPDUP");
			dep.put("displayName", "Dependency Duplicates");
			dep.put("description", DESC);

			JSONArray jsonArray = new JSONArray();
			for (int i = 1; i < 5; i++) {
				JSONObject duplicate = new JSONObject();
				duplicate.put("name", "DEPDUP");
				jsonArray.put(duplicate);
			}
			// Add the main object in the middle of the list surrounded by references
			jsonArray.put(dep);
			for (int i = 1; i < 5; i++) {
				JSONObject duplicate = new JSONObject();
				duplicate.put("name", "DEPDUP");
				jsonArray.put(duplicate);
			}
			json.put("tasks", jsonArray);

			Settings settings = getSettings();
			settings.setPostFlush(true);
			settings.setEntityClass(Project.class);
			Project p = (Project)aggregateService.create(json, settings);
			assert (p.getId() != null);
			assert (p.getTasks() != null);
			System.out.println("Dependant size: " + p.getTasks().size());
			assert (p.getTasks().size() == 9);

			// Check all the data is present in all the objects.
			// i.e., the objects that had only the natural key populated should be
			// treated as references to the main object
			for (Task d : p.getTasks()) {
				assert (d.getDescription() != null);
				assert (d.getDescription().equals(DESC));
			}

			Object jsonObject = aggregateService.read(p, settings);
			JSONObject jsonTask = (JSONObject)jsonObject;
			JSONArray jsonDependants = jsonTask.getJSONArray("tasks");
			assert (((JSONArray)jsonDependants).length() == 9);


		} finally {

			// reset
			externalTask.setNaturalKey(null);
			domainTask.setNaturalKey(null);
		}
	}
}
