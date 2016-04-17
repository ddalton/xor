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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.ImmutableJsonProperty;
import tools.xor.Settings;
import tools.xor.db.base.Employee;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultJson extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;
	
	protected void checkStringField() throws JSONException {
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		
		Settings settings = new Settings();
		settings.setEntityClass(Person.class);
		Person person = (Person) aggregateService.create(jsonBuilder.build(), settings);	
		assert(person.getId() != null);
		assert(person.getName().equals("DILIP_DALTON"));
		
		Object jsonObject = aggregateService.read(person, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));

	}

	protected void checkDateField() throws JSONException {
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		
		// 1/1/15 7:00 PM EST
		final long CREATED_ON = 1420156800000L;
		Date createdOn = new Date(CREATED_ON);
		DateFormat df = new SimpleDateFormat(ImmutableJsonProperty.ISO8601_FORMAT);
		jsonBuilder.add("createdOn", df.format(createdOn));
		
		Settings settings = new Settings();
		settings.setEntityClass(Person.class);
		Person person = (Person) aggregateService.create(jsonBuilder.build(), settings);	
		assert(person.getId() != null);
		assert(person.getName().equals("DILIP_DALTON"));
		assert(person.getCreatedOn().getTime() == CREATED_ON);
		
		Object jsonObject = aggregateService.read(person, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert(((JsonString)json.get("createdOn")).getString().equals("2015-01-01T16:00:00.000-0800"));
	}
	
	protected void checkIntField() throws JSONException {
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		jsonBuilder.add("employeeNo", 235);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(jsonBuilder.build(), settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getEmployeeNo() == 235);
		
		Object jsonObject = aggregateService.read(employee, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert(((JsonNumber)json.get("employeeNo")).intValue() == 235);
	}	
	
	protected void checkLongField() throws JSONException {
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		jsonBuilder.add("salary", 100000);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(jsonBuilder.build(), settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getSalary() == 100000);
		
		Object jsonObject = aggregateService.read(employee, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert(((JsonNumber)json.get("salary")).intValue() == 100000);
	}	
	
	protected void checkEmptyLongField() throws JSONException {
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(jsonBuilder.build(), settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getSalary() == null);
		assert(!employee.getIsCriticalSystemObject());
		
		Object jsonObject = aggregateService.read(employee, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert( !json.containsKey("salary") );
	}	
	
	protected void checkBooleanField() throws JSONException {
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		jsonBuilder.add("isCriticalSystemObject", true);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(jsonBuilder.build(), settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getIsCriticalSystemObject());
		
		Object jsonObject = aggregateService.read(employee, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert(json.getBoolean("isCriticalSystemObject"));
	}
	
	protected void checkBigDecimalField() throws JSONException {
		final BigDecimal largeDecimal = new BigDecimal("12345678998765432100000.123456789987654321");
		
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		jsonBuilder.add("largeDecimal", largeDecimal);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(jsonBuilder.build(), settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getLargeDecimal().equals(largeDecimal) );
		
		Object jsonObject = aggregateService.read(employee, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert(((JsonNumber)json.get("largeDecimal")).bigDecimalValue().equals(largeDecimal));
	}
	
	
	protected void checkBigIntegerField() throws JSONException {
		final BigInteger largeInteger = new BigInteger("12345678998765432100000123456789987654321");
		
		// create person
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", "DILIP_DALTON");
		jsonBuilder.add("displayName", "Dilip Dalton");
		jsonBuilder.add("description", "Software engineer in the bay area");
		jsonBuilder.add("userName", "daltond");
		jsonBuilder.add("largeInteger", largeInteger);
		
		Settings settings = new Settings();
		settings.setEntityClass(Employee.class);
		Employee employee = (Employee) aggregateService.create(jsonBuilder.build(), settings);	
		assert(employee.getId() != null);
		assert(employee.getName().equals("DILIP_DALTON"));
		assert(employee.getLargeInteger().equals(largeInteger) );
		
		Object jsonObject = aggregateService.read(employee, settings);
		JsonObject json = (JsonObject) jsonObject;
		System.out.println("JSON string: " + json.toString());	
		assert(((JsonString)json.get("name")).getString().equals("DILIP_DALTON"));		
		assert(((JsonNumber)json.get("largeInteger")).bigIntegerValue().equals(largeInteger));
	}	
	
	protected void checkEntityField() {
		final String TASK_NAME = "SETUP_DSL";
		
		// Create task
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", TASK_NAME);
		jsonBuilder.add("displayName", "Setup DSL");
		jsonBuilder.add("description", "Setup high-speed broadband internet using DSL technology");

		// Create quote
		final BigDecimal price =  new BigDecimal("123456789.987654321");
		jsonBuilder.add("quote", Json.createObjectBuilder().add("price", price));
		
		Settings settings = getSettings();
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(jsonBuilder.build(), settings);	
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getQuote() != null);
		assert(task.getQuote().getId() != null);
		assert(task.getQuote().getPrice().equals(price));		
		
		Object jsonObject = aggregateService.read(task, settings);
		JsonObject jsonTask = (JsonObject) jsonObject;
		System.out.println("JSON string: " + jsonTask.toString());	
		assert(((JsonString)jsonTask.get("name")).getString().equals(TASK_NAME));
		JsonObject jsonQuote = jsonTask.getJsonObject("quote");
		assert(((JsonNumber)jsonQuote.get("price")).bigDecimalValue().equals(price));
	}
	
	protected void checkSetField() {
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";
		
		// Create task
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add("name", TASK_NAME);
		jsonBuilder.add("displayName", "Setup DSL");
		jsonBuilder.add("description", "Setup high-speed broadband internet using DSL technology");

		// Create and add 1 child task
		jsonBuilder.add("taskChildren", Json.createArrayBuilder()
				.add(Json.createObjectBuilder()
						.add("name", CHILD_TASK_NAME)
						.add("displayName", "Task 1")
						.add("description", "This is the first child task")));
		
		Settings settings = getSettings();
		settings.setEntityClass(Task.class);
		Task task = (Task) aggregateService.create(jsonBuilder.build(), settings);	
		assert(task.getId() != null);
		assert(task.getName().equals(TASK_NAME));
		assert(task.getTaskChildren() != null);
		System.out.println("Children size: " + task.getTaskChildren().size());
		assert(task.getTaskChildren().size() == 1);
		for(Task child: task.getTaskChildren()) {
			System.out.println("Task name: " + child.getName());
		}
		
		Object jsonObject = aggregateService.read(task, settings);
		JsonObject jsonTask = (JsonObject) jsonObject;
		System.out.println("JSON string for object: " + jsonTask.toString());	
		assert(((JsonString)jsonTask.get("name")).getString().equals(TASK_NAME));
		JsonArray jsonChildren = jsonTask.getJsonArray("taskChildren");
		assert(((JsonArray)jsonChildren).size() == 1);
	}	
}
