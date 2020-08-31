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
import java.util.HashMap;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.AggregateAction;
import tools.xor.AssociationSetting;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.MapperSide;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.StringType;
import tools.xor.TypeMapper;
import tools.xor.custom.TestAssociationStrategy;
import tools.xor.db.base.Chapter;
import tools.xor.db.base.ChapterType;
import tools.xor.db.base.Facet;
import tools.xor.db.base.Person;
import tools.xor.db.base.Technician;
import tools.xor.db.pm.AddressEntity;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;

public class DefaultCheckType extends AbstractDBTest {
	
	@Autowired
	protected AggregateManager aggregateManager;
	
	private static final String TASK_NAME = "SETUP_DSL";
	
	protected void checkPath() {
		DataModel das = aggregateManager.getDataModel();
		
		// create person
		Technician owner = new Technician();
		owner.setName("DILIP_DALTON");
		owner.setDisplayName("Dilip Dalton");
		owner.setDescription("Software engineer in the bay area");
		owner.setUserName("daltond");
		owner.setSkill("Network, electric, telephone");
		owner = (Technician) aggregateManager.create(owner, new Settings());	
		Person person = (Person) aggregateManager.read(owner, new Settings());	
		
		// Create Task
		Task task = new Task();
		task.setName(TASK_NAME);
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		task.setAssignedTo(person);
		
		Settings settings = new Settings();
		// Need to enhance the default aggregate view with this association to persist the technician relationship
		settings.expand(new AssociationSetting("assignedTo"));
		
		task = (Task) aggregateManager.create(task, settings);
		task = (Task) aggregateManager.read(task, getSettings());
		
		assert(task.getId() != null);
		assert(task.getAssignedTo() != null);
		assert(task.getAssignedTo().getId() != null);
		
		// read the person object using a DataObject
		settings = getSettings();
        TypeMapper typeMapper = das.getTypeMapper().newInstance(MapperSide.DOMAIN);
		ObjectCreator oc = new ObjectCreator(settings, aggregateManager.getDataStore(), typeMapper);
		settings.expand(new AssociationSetting("assignedTo.name")); // enhance the view to get the technician name
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
		settings.setEntityType(taskType);
		settings.init(das.getShape());
		
		BusinessObject from = oc.createDataObject(task, taskType, null, null);
		settings.setAssociationStrategy(new TestAssociationStrategy()); // Explicitly set the association strategy if not going through the AggregateManager
		settings.setAction(AggregateAction.READ);
		BusinessObject to = (BusinessObject) from.read(settings);

		BusinessObject technician = (BusinessObject) to.getExistingDataObject("/assignedTo");
		owner = (Technician) technician.getInstance();
		assert(owner.getId() != null);
		
		// When expanding "assignedTo.name", the Technician type was included and hence the
		// name should not be null
		assert(owner.getName() != null);
		
		// Now let us get the task from DB and check it
		// NOTE: we use the same settings, but use the read API from the AggregateManager that gets the DB object
		task = (Task) aggregateManager.read(task, settings);
		assert(task.getId() != null);
		assert(task.getAssignedTo() != null);
		assert(task.getAssignedTo().getId() != null);
		assert(task.getAssignedTo().getName().equals("DILIP_DALTON"));
	}

	protected void checkOrder() {
		DataModel das = aggregateManager.getDataModel();
		
		EntityType chapterT = (EntityType) das.getShape().getType(Chapter.class);
		EntityType chapterTT = (EntityType) das.getShape().getType(ChapterType.class);
		EntityType facetT = (EntityType) das.getShape().getType(Facet.class);
		
		assert(chapterT.getOrder() < chapterTT.getOrder());
		assert(chapterTT.getOrder() < facetT.getOrder());
	}
	
	protected void checkReflectionGetter() {
		DataModel das = aggregateManager.getDataModel();
		
		// Create Task
		Task task = new Task();
		task.setName(TASK_NAME);
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
        TypeMapper typeMapper = das.getTypeMapper().newInstance(MapperSide.DOMAIN);
        ObjectCreator oc = new ObjectCreator(new Settings(), aggregateManager.getDataStore(), typeMapper);
		BusinessObject from = oc.createDataObject(task, taskType, null, null);
		
		Property property = taskType.getProperty("name");
		assert(property != null);
		
		Object name = from.get(property);
		//assert(name != null && name.toString().equals(TASK_NAME));
		Date start = new Date();
		for(int i = 0; i < 1000000; i++) {
			from.get(property);
		}
		System.out.println("checkReflectionGetter[Reflection call] took " + ((new Date()).getTime() - start.getTime()) + " milliseconds");
		
		start = new Date();
		for(int i = 0; i < 1000000; i++) {
			task.getName();
		}
		System.out.println("checkReflectionGetter[Direct call] took " + ((new Date()).getTime() - start.getTime()) + " milliseconds");		
	}
	
	protected void checkReflectionSetter() {
		DataModel das = aggregateManager.getDataModel();
		
		// Create Task
		Task task = new Task();
		task.setName(TASK_NAME);
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
        TypeMapper typeMapper = das.getTypeMapper().newInstance(MapperSide.DOMAIN);
        ObjectCreator oc = new ObjectCreator(new Settings(), aggregateManager.getDataStore(), typeMapper);		
		BusinessObject from = oc.createDataObject(task, taskType, null, null);
		
		Property property = taskType.getProperty("name");
		assert(property != null);
		
		from.set(property, "New Name"); 
		Date start = new Date();
		for(int i = 0; i < 2000000; i++) {
			from.set(property, "New Name"); 
		}
		System.out.println("checkReflectionSetter[Reflection call] took " + ((new Date()).getTime() - start.getTime()) + " milliseconds");
		
		start = new Date();
		for(int i = 0; i < 2000000; i++) {
			task.setName("New Name");
		}
		System.out.println("checkReflectionSetter[Direct call] took " + ((new Date()).getTime() - start.getTime()) + " milliseconds");		
	}	
	
	protected void generateSimple() {
		DataModel das = aggregateManager.getDataModel();
		EntityType addressType = (EntityType) das.getShape().getType(AddressEntity.class);
		
		JSONObject address = (JSONObject) addressType.generate(new Settings(), null, null, null, new StateGraph.ObjectGenerationVisitor(new HashMap<JSONObject, State>(), new Settings(), null));
		System.out.println("Address street: " + address.get("street"));
		assert(address.get("street").toString().length() > 0);
	}
	
	protected void testRandomString() {
	    String randomStr = StringType.randomAlphanumeric(10);
	    System.out.println("Random str: " + randomStr);
	    assert(randomStr.length() == 10);
	}
}
