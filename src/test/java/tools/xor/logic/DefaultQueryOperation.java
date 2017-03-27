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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Employee;
import tools.xor.db.base.MetaEntity;
import tools.xor.db.base.MetaEntityState;
import tools.xor.db.base.MetaEntityType;
import tools.xor.db.base.Patent;
import tools.xor.db.base.Person;
import tools.xor.db.catalog.Catalog;
import tools.xor.db.catalog.CatalogItem;
import tools.xor.db.common.Property;
import tools.xor.db.common.Value;
import tools.xor.db.common.ValueType;
import tools.xor.db.dao.TaskDao;
import tools.xor.db.enums.base.MetaEntityStateEnum;
import tools.xor.db.enums.base.MetaEntityTypeEnum;
import tools.xor.db.enums.common.ValueTypeEnum;
import tools.xor.db.pm.Project;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.view.AggregateView;

public class DefaultQueryOperation extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	final String JA_NAME = "JOHN_ADAMS";
	final String JA_DISPLAY_NAME = "John Adams";
	final String JA_DESCRIPTION = "Second President of the United States of America";
	final String JA_USER_NAME = "jadams";		

	final String NAME = "GEORGE_WASHINGTON";
	final String DISPLAY_NAME = "George Washington";
	final String DESCRIPTION = "First President of the United States of America";
	final String USER_NAME = "gwashington";	

	private ValueType getValueType(String type) {
		ValueType valueType = new ValueType();
		valueType.setName(type); 
		valueType = (ValueType) aggregateService.read(valueType, new Settings());
		return valueType;
	}	

	private MetaEntityState getState(String state) {
		MetaEntityState artifactState = new MetaEntityState();
		artifactState.setName(state); // ArtifactStateEnum.ACTIVE.name()
		artifactState = (MetaEntityState) aggregateService.read(artifactState, new Settings());
		return artifactState;
	}

	private MetaEntityType getType(String type) {	
		MetaEntityType artifactType = new MetaEntityType();
		artifactType.setName(type); // ArtifactTypeEnum.CONTAINER.name() 
		artifactType = (MetaEntityType) aggregateService.read(artifactType, new Settings());
		return artifactType;
	}

	public void queryPerson() {

		// create person
		Person person = new Person();
		person.setName(NAME);
		person.setDisplayName(DISPLAY_NAME);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		person = (Person) aggregateService.create(person, new Settings());	

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO"));		
		List<?> toList = aggregateService.query(person, settings);

		assert(toList.size() == 1);

		Person result = null;
		if(Person.class.isAssignableFrom(toList.get(0).getClass()))
			result = (Person) toList.get(0);

		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
	}

	public void queryPersonNative() {

		// create person
		Person person = new Person();
		person.setName(NAME);
		person.setDisplayName(DISPLAY_NAME);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		person = (Person) aggregateService.create(person, new Settings());	

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO_NATIVE"));	
		settings.setPreFlush(true);
		List<?> toList = aggregateService.query(person, settings);

		assert(toList.size() == 1);

		Person result = null;
		if(Person.class.isAssignableFrom(toList.get(0).getClass()))
			result = (Person) toList.get(0);

		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
	}	
	
	public void queryPersonStoredProcedure() {

		// create person
		Person person = new Person();
		person.setName(NAME);
		person.setDisplayName(DISPLAY_NAME);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		person = (Person) aggregateService.create(person, new Settings());	

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO_SP"));	
		settings.setPreFlush(true);
		List<?> toList = aggregateService.query(person, settings);

		// This requires the presence of a stored procedure in the DB. So we comment
		// this test for now
		assert(toList.size() == 1);

		Person result = null;
		if(Person.class.isAssignableFrom(toList.get(0).getClass()))
			result = (Person) toList.get(0);

		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
	}		

	public void queryPersonType() {
		// create Employee
		Employee john = new Employee();
		john.setName(JA_NAME);
		john.setDisplayName(JA_DISPLAY_NAME);
		john.setDescription(JA_DESCRIPTION);
		john.setUserName(JA_USER_NAME);

		john = (Employee) aggregateService.create(john, new Settings());

		// create Manager
		Person george = new Person();
		george.setName(NAME);
		george.setDisplayName(DISPLAY_NAME);
		george.setDescription(DESCRIPTION);
		george.setUserName(USER_NAME);

		george = (Person) aggregateService.create(george, new Settings());		

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO"));	
		settings.setPreFlush(true);
		List<?> toList = aggregateService.query(new Person(), settings);

		assert(toList.size() == 2);		
	}

	public void queryNarrowPersonType() {
		// create Employee
		Employee john = new Employee();
		john.setName(JA_NAME);
		john.setDisplayName(JA_DISPLAY_NAME);
		john.setDescription(JA_DESCRIPTION);
		john.setUserName(JA_USER_NAME);

		john = (Employee) aggregateService.create(john, new Settings());

		// create Manager
		Person george = new Person();
		george.setName(NAME);
		george.setDisplayName(DISPLAY_NAME);
		george.setDescription(DESCRIPTION);
		george.setUserName(USER_NAME);

		george = (Person) aggregateService.create(george, new Settings());		

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO_NATIVE_TYPE"));	
		settings.setPreFlush(true);
		settings.setNarrow(true);
		List<?> toList = aggregateService.query(george, settings);

		assert(toList.size() == 1);
		Person result = null;
		if(Person.class.isAssignableFrom(toList.get(0).getClass()))
			result = (Person) toList.get(0);

		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));

	}	

	public void queryTaskChildren() {
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
		settings.setView(aggregateService.getView("TASKCHILDREN"));		
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Task.class.isAssignableFrom(obj.getClass()));

		Task root = (Task) obj;
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 2);
	}
	
	public void queryTaskGrandChildren() {
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

		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		A.setTaskChildren(c);
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
		settings.setView(aggregateService.getView("TASKGRANDCHILDREN"));		
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Task.class.isAssignableFrom(obj.getClass()));

		Task root = (Task) obj;
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 2);
		
		Iterator<Task> iter = root.getTaskChildren().iterator();
		Task child1 = iter.next();
		Task child2 = iter.next();
		
		Task fixDefects = child1.getName().equals("FIX_DEFECTS") ? child1 : child2;
		Task prioritize = child1.getName().equals("FIX_DEFECTS") ? child2 : child1;
		assert(fixDefects.getTaskChildren() != null && fixDefects.getTaskChildren().size() == 2);
		assert(prioritize.getTaskChildren() == null || prioritize.getTaskChildren().size() == 0);
	}

	public void queryTaskGrandChildrenRegEx()
	{
		// Create a task
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");
		userStory = (Task)aggregateService.create(userStory, new Settings());

		// Create 2 children
		Task A = new Task();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");

		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		A.setTaskChildren(c);
		A = (Task)aggregateService.create(A, new Settings());
		A.setTaskParent(userStory);

		Task B = new Task();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		B = (Task)aggregateService.create(B, new Settings());
		B.setTaskParent(userStory);

		Set<Task> children = new HashSet<Task>();
		children.add(A);
		children.add(B);
		userStory.setTaskChildren(children);

		// query the task object
		Settings settings = new Settings();
		List<String> paths = new ArrayList<String>();

		// In this REGEX we fetch tasks with no children or children 1 level deep
		paths.add("(auditTask.|taskChildren.|dependants.){0,1}(description|isCriticalSystemObject|id|iconUrl|version|displayName|detailedDescription|name)");
		AggregateView view = new AggregateView("REGEX_TGC");
		view.setAttributeList(paths);
		settings.setView(view);
		userStory = (Task)aggregateService.read(userStory, settings);

		Task root = (Task) userStory;
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 2);

		Iterator<Task> iter = root.getTaskChildren().iterator();
		Task child1 = iter.next();
		Task child2 = iter.next();

	}
	
	public void queryTaskGrandChildrenSkip() {
		// Create a task
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");
		userStory = (Task) aggregateService.create(userStory, new Settings());	

		// Create 1 child
		Task A = new Task();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");

		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		A.setTaskChildren(c);
		A = (Task) aggregateService.create(A, new Settings());
		A.setTaskParent(userStory);		

		Set<Task> children = new HashSet<Task>();
		children.add(A);
		userStory.setTaskChildren(children);
		userStory = (Task) aggregateService.read(userStory, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKGRANDCHILDRENSKIP"));		
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Task.class.isAssignableFrom(obj.getClass()));

		Task root = (Task) obj;
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 1);
		
		Iterator<Task> iter = root.getTaskChildren().iterator();
		Task child1 = iter.next();	
		assert(child1.getId() != null);
		assert(child1.getTaskChildren() != null && child1.getTaskChildren().size() == 2);
	}
	
	public void queryTaskGrandChildSetList() {
		// Create a task
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");
		userStory = (Task) aggregateService.create(userStory, new Settings());	

		// Create 1 child
		Task A = new Task();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");

		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		A.setTaskChildren(c);
		
		// Dependants
		Task dep1 = new Task();
		dep1.setName("DEP_1");
		dep1.setDisplayName("Dependency 1");
		dep1.setDescription("First dependency");
		Task dep2 = new Task();
		dep2.setName("DEP_2");
		dep2.setDisplayName("Dependency 2");
		dep2.setDescription("Second dependency");
		List<Task> l = new ArrayList<Task>();
		l.add(dep1);
		l.add(dep2);
		A.setDependants(l);
		
		A = (Task) aggregateService.create(A, new Settings());
		A.setTaskParent(userStory);		

		Set<Task> children = new HashSet<Task>();
		children.add(A);
		userStory.setTaskChildren(children);
		userStory = (Task) aggregateService.read(userStory, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKGRANDCHILDSETLIST"));		
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Task.class.isAssignableFrom(obj.getClass()));

		Task root = (Task) obj;
		assert(root.getName() != null && root.getName().equals("DEFECTS"));
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 1);
		
		Iterator<Task> iter = root.getTaskChildren().iterator();
		Task child1 = iter.next();	
		assert(child1.getId() != null);
		assert(child1.getName() != null && child1.getName().equals("FIX_DEFECTS"));
		assert(child1.getTaskChildren() != null && child1.getTaskChildren().size() == 2);
		assert(child1.getDependants() != null && child1.getDependants().size() == 2);		
	}	
	
	public void queryTaskDependencies() {
		// Create a task
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");
		userStory = (Task) aggregateService.create(userStory, new Settings());	

		// Create 2 dependents
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

		List<Task> dependents = new ArrayList<Task>();
		dependents.add(A);
		dependents.add(B);
		userStory.setDependants(dependents);
		userStory = (Task) aggregateService.read(userStory, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKDEP"));		
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Task.class.isAssignableFrom(obj.getClass()));

		Task root = (Task) obj;
		assert(root.getDependants() != null && root.getDependants().size() == 2);
		assert(root.getDependants().get(0).getName().equals("FIX_DEFECTS"));
		assert(root.getDependants().get(1).getName().equals("PRIORITIZE_DEFECTS"));		
	}

	/**
	 * Uni-directional query Map test
	 */
	public void querySubProjects() {
		// Create a task
		Project master = new Project();
		master.setName("INFRASTRUCTURE");
		master.setDisplayName("Infrastructure");
		master.setDescription("Project to setup the new infrastructure");
		master = (Project) aggregateService.create(master, new Settings());	

		// Create 2 dependents
		Project A = new Project();
		A.setName("SETUP_NETWORK");
		A.setDisplayName("Setup network");
		A.setDescription("Project to lay the network cables and connect to routers");
		A = (Project) aggregateService.create(A, new Settings());

		Project B = new Project();
		B.setName("SETUP_TELEPHONE");
		B.setDisplayName("Setup telephone");
		B.setDescription("Setup the telephone at each employee desk");
		B = (Project) aggregateService.create(B, new Settings());

		Map<String, Project> subProjects = new HashMap<String, Project>();
		subProjects.put(A.getName(), A);
		subProjects.put(B.getName(), B);
		master.setSubProjects(subProjects);
		master = (Project) aggregateService.read(master, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("SUBPROJECTS"));		
		List<?> toList = aggregateService.query(master, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Project.class.isAssignableFrom(obj.getClass()));

		Project root = (Project) obj;
		assert(root.getSubProjects() != null && root.getSubProjects().size() == 2);
		assert(root.getSubProjects().get("SETUP_NETWORK").getName().equals("SETUP_NETWORK"));
		assert(root.getSubProjects().get("SETUP_TELEPHONE").getName().equals("SETUP_TELEPHONE"));		
	}	

	public void querySetListMap() {
		// Create a task
		Project master = new Project();
		master.setName("INFRASTRUCTURE");
		master.setDisplayName("Infrastructure");
		master.setDescription("Project to setup the new infrastructure");

		// Create 2 dependents
		Project A = new Project();
		A.setName("SETUP_NETWORK");
		A.setDisplayName("Setup network");
		A.setDescription("Project to lay the network cables and connect to routers");

		Project B = new Project();
		B.setName("SETUP_TELEPHONE");
		B.setDisplayName("Setup telephone");
		B.setDescription("Setup the telephone at each employee desk");

		Map<String, Project> subProjects = new HashMap<String, Project>();
		subProjects.put(A.getName(), A);
		subProjects.put(B.getName(), B);
		master.setSubProjects(subProjects);
		
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");	

		// Create 1 child
		Task child1 = new Task();
		child1.setName("FIX_DEFECTS");
		child1.setDisplayName("Fix defects");
		child1.setDescription("Task to track the defect fixing effort");

		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		child1.setTaskChildren(c);
		
		// Dependants
		Task dep1 = new Task();
		dep1.setName("DEP_1");
		dep1.setDisplayName("Dependency 1");
		dep1.setDescription("First dependency");
		Task dep2 = new Task();
		dep2.setName("DEP_2");
		dep2.setDisplayName("Dependency 2");
		dep2.setDescription("Second dependency");
		List<Task> l = new ArrayList<Task>();
		l.add(dep1);
		l.add(dep2);
		child1.setDependants(l);
		
		Set<Task> children = new HashSet<Task>();
		children.add(child1);
		userStory.setTaskChildren(children);		
		
		master.setRootTask(userStory);	
		Settings createSettings = getSettings();	
		master = (Project) aggregateService.create(master, createSettings);	
		
		Settings readSettings = getSettings();		
		master = (Project) aggregateService.read(master, readSettings);	
		Task c1 = master.getRootTask().getTaskChildren().iterator().next();
		assert(c1 != null && c1.getId() != null && c1.getTaskChildren().size() == 2);
		
		Project prj = (Project) aggregateService.getDasFactory().getPersistenceOrchestrator(null).findById(Project.class, master.getId());

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("SETLISTMAP"));	
		settings.setPreFlush(true);
		List<?> toList = aggregateService.query(master, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(Project.class.isAssignableFrom(obj.getClass()));

		Project project = (Project) obj;
		assert(project.getSubProjects() != null && project.getSubProjects().size() == 2);
		assert(project.getSubProjects().get("SETUP_NETWORK").getName().equals("SETUP_NETWORK"));
		assert(project.getSubProjects().get("SETUP_TELEPHONE").getName().equals("SETUP_TELEPHONE"));
		
		Task rootTask = project.getRootTask();
		assert(rootTask != null );
		assert(rootTask.getTaskChildren() != null && rootTask.getTaskChildren().size() == 1);
		
		Iterator<Task> iter = rootTask.getTaskChildren().iterator();
		c1 = iter.next();	
		assert(c1.getId() != null);
		assert(c1.getTaskChildren() != null && c1.getTaskChildren().size() == 2);
		assert(c1.getDependants() != null && c1.getDependants().size() == 2);			
	}		
	
	public void queryTaskEmptyFilter() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		task1 = (Task) aggregateService.create(task1, new Settings());	

		// task 2
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		task2 = (Task) aggregateService.create(task2, new Settings());

		// task 3
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		task3 = (Task) aggregateService.create(task3, new Settings());

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKFILTER"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 3);		
	}

	public void queryTaskNameFilter() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		task1 = (Task) aggregateService.create(task1, new Settings());	

		// task 2
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		task2 = (Task) aggregateService.create(task2, new Settings());

		// task 3
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		task3 = (Task) aggregateService.create(task3, new Settings());

		task1 = (Task) aggregateService.read(task1, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.addFilter("name", "FIX_DEFECTS");
		settings.setView(aggregateService.getView("TASKFILTER"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 1);
		Object obj = toList.get(0);
		assert(Task.class.isAssignableFrom(obj.getClass()));

		Task root = (Task) obj;
		assert(root.getName().equals("FIX_DEFECTS"));
	}

	public void queryTaskUnionNameFilter() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		task1 = (Task) aggregateService.create(task1, new Settings());	

		// task 2
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		task2 = (Task) aggregateService.create(task2, new Settings());

		// task 3
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		task3 = (Task) aggregateService.create(task3, new Settings());

		task1 = (Task) aggregateService.read(task1, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.addFilter("name1", "FIX_DEFECTS");
		settings.addFilter("name2", "PRIORITIZE_DEFECTS");		
		settings.setView(aggregateService.getView("TASKUNIONFILTER"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 2);

		Object obj1 = toList.get(0);
		Object obj2 = toList.get(1);

		assert(Task.class.isAssignableFrom(obj1.getClass()));
		assert(Task.class.isAssignableFrom(obj2.getClass()));
		
		Task t1 = (Task) obj1;
		Task t2 = (Task) obj2;		
		
		assert( (t1.getName().equals("FIX_DEFECTS") && t2.getName().equals("PRIORITIZE_DEFECTS")) ||
				(t2.getName().equals("FIX_DEFECTS") && t1.getName().equals("PRIORITIZE_DEFECTS")) );
	}		

	/**
	 * Union query with empty collection
	 */
	public void queryTaskUnionSet1() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		task1 = (Task) aggregateService.create(task1, new Settings());	

		// task 2
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		task2 = (Task) aggregateService.create(task2, new Settings());

		// task 3
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		task3 = (Task) aggregateService.create(task3, new Settings());

		task1 = (Task) aggregateService.read(task1, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.addFilter("name1", "FIX_DEFECTS");
		settings.addFilter("name2", "PRIORITIZE_DEFECTS");		
		settings.setView(aggregateService.getView("TASKUNIONSET"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 2);

		Object obj1 = toList.get(0);
		Object obj2 = toList.get(1);

		assert(Task.class.isAssignableFrom(obj1.getClass()));
		assert(Task.class.isAssignableFrom(obj2.getClass()));
		
		Task t1 = (Task) obj1;
		Task t2 = (Task) obj2;		
		
		assert( (t1.getName().equals("FIX_DEFECTS") && t2.getName().equals("PRIORITIZE_DEFECTS")) ||
				(t2.getName().equals("FIX_DEFECTS") && t1.getName().equals("PRIORITIZE_DEFECTS")) );
	}		

	/**
	 * Union query with children collection
	 */
	public void queryTaskUnionSet2() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		
		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		task1.setTaskChildren(c);
		task1 = (Task) aggregateService.create(task1, new Settings());	

		// task 2
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		task2 = (Task) aggregateService.create(task2, new Settings());

		// task 3
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		task3 = (Task) aggregateService.create(task3, new Settings());

		task1 = (Task) aggregateService.read(task1, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.addFilter("name1", "DEFECTS");
		settings.addFilter("name2", "PRIORITIZE_DEFECTS");		
		settings.setView(aggregateService.getView("TASKUNIONSET"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 2);

		Object obj1 = toList.get(0);
		Object obj2 = toList.get(1);

		assert(Task.class.isAssignableFrom(obj1.getClass()));
		assert(Task.class.isAssignableFrom(obj2.getClass()));
		
		Task t1 = (Task) obj1;
		Task t2 = (Task) obj2;		
		
		assert( (t1.getName().equals("DEFECTS") && t2.getName().equals("PRIORITIZE_DEFECTS")) ||
				(t2.getName().equals("DEFECTS") && t1.getName().equals("PRIORITIZE_DEFECTS")) );
		
		Task d = t1.getName().equals("DEFECTS") ? t1 : t2;
		assert(d.getTaskChildren() != null && d.getTaskChildren().size() == 2);
	}		

	public void queryTaskParallel() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		
		Task d1 = new Task();
		d1.setName("DEFECT 1");
		d1.setDisplayName("Defect 1");
		d1.setDescription("First defect");
		Task d2 = new Task();
		d2.setName("DEFECT 2");
		d2.setDisplayName("Defect 2");
		d2.setDescription("Second defect");
		Set<Task> c = new HashSet<Task>();
		c.add(d1);
		c.add(d2);
		task1.setTaskChildren(c);
		
		// Dependants
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		List<Task> l = new ArrayList<Task>();
		l.add(task2);
		l.add(task3);
		task1.setDependants(l);
		
		task1 = (Task) aggregateService.create(task1, new Settings());	
		task1 = (Task) aggregateService.read(task1, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKPARALLEL"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 1);
		Object obj1 = toList.get(0);
		assert(Task.class.isAssignableFrom(obj1.getClass()));
		Task t1 = (Task) obj1;
		
		assert( t1.getName().equals("DEFECTS"));
		assert( t1.getTaskChildren() != null && t1.getTaskChildren().size() == 2);		
		assert( t1.getDependants() != null && t1.getDependants().size() == 2);
		assert( t1.getDependants().get(0).getName().equals("FIX_DEFECTS") && t1.getDependants().get(1).getName().equals("PRIORITIZE_DEFECTS"));
		
	}		
		
	public void queryTaskUnionOverlap() {
		// task 1
		Task task1 = new Task();
		task1.setName("DEFECTS");
		task1.setDisplayName("Defects");
		task1.setDescription("User story to address product defects");
		task1 = (Task) aggregateService.create(task1, new Settings());	

		// task 2
		Task task2 = new Task();
		task2.setName("FIX_DEFECTS");
		task2.setDisplayName("Fix defects");
		task2.setDescription("Task to track the defect fixing effort");
		task2 = (Task) aggregateService.create(task2, new Settings());

		// task 3
		Task task3 = new Task();
		task3.setName("PRIORITIZE_DEFECTS");
		task3.setDisplayName("Prioritize defects");
		task3.setDescription("Based upon the effort required for the defects prioritize them");
		task3 = (Task) aggregateService.create(task3, new Settings());

		task1 = (Task) aggregateService.read(task1, getSettings());		

		// query the task object
		Settings settings = new Settings();
		settings.addFilter("name1", "FIX_DEFECTS");
		settings.setView(aggregateService.getView("TASKUNIONOVERLAP"));		
		List<?> toList = aggregateService.query(new Task(), settings);

		assert(toList.size() == 3);

		Object obj1 = toList.get(0);
		Object obj2 = toList.get(1);
		Object obj3 = toList.get(2);

		assert(Task.class.isAssignableFrom(obj1.getClass()));
		assert(Task.class.isAssignableFrom(obj2.getClass()));
		assert(Task.class.isAssignableFrom(obj3.getClass()));		
		
		Task t1 = (Task) obj1;
		Task t2 = (Task) obj2;
		Task t3 = (Task) obj3;		
		
		assert( t1.getName().equals("DEFECTS") && t2.getName().equals("FIX_DEFECTS") && t3.getName().equals("PRIORITIZE_DEFECTS"));
	}	

	public void queryEntityProperties() {
		final String VALUE4 = "Value 4";
		final String VALUE5 = "Value 5";

		setupValueType(aggregateService);
		setupMetaEntityState(aggregateService);
		setupMetaEntityType(aggregateService);

		// Property and values
		Set<Value> p1Values = new HashSet<Value>();
		Value value1 = new Value();
		value1.setValue("Value 1");
		p1Values.add(value1);
		Value value2 = new Value();
		value2.setValue("Value 2");
		p1Values.add(value2);		
		Value value3 = new Value();
		value3.setValue("Value 3");
		p1Values.add(value3);
		Property property1 = new Property();
		property1.setName("FIX_DEFECTS");
		property1.setDisplayName("Fix defects");
		property1.setDescription("Task to track the defect fixing effort");
		property1.setValueType(getValueType(ValueTypeEnum.STRING.name()));
		property1.setValues(p1Values);

		Set<Value> p2Values = new HashSet<Value>();
		Value value4 = new Value();
		value4.setValue(VALUE4);
		p2Values.add(value4);
		Value value5 = new Value();
		value5.setValue(VALUE5);
		p2Values.add(value5);		
		Property property2 = new Property();
		property2.setName("PRIORITIZE_DEFECTS");
		property2.setDisplayName("Prioritize defects");
		property2.setDescription("Based upon the effort required for the defects prioritize them");
		property2.setValueType(getValueType(ValueTypeEnum.STRING.name()));
		property2.setValues(p2Values);		

		// artifact
		Set<Property> p = new HashSet<Property>();
		p.add(property1);
		p.add(property2);
		MetaEntity artifact = new MetaEntity();
		artifact.setName("ARTIFACT");
		artifact.setDisplayName("Defects");
		artifact.setDescription("User story to address product defects");
		artifact.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		artifact.setMetaEntityType(getType(MetaEntityTypeEnum.TASK.name()));	
		artifact.setProperty(p);		
		artifact = (MetaEntity) aggregateService.create(artifact, new Settings());				
		artifact = (MetaEntity) aggregateService.read(artifact, getSettings());		

		System.out.println("****************START**********");
		try {
		// query the task object
		Settings settings = new Settings();
		settings.addFilter("artifactId", artifact.getId());
		settings.addFilter("propertyName", "PRIORITIZE_DEFECTS");
		settings.setView(aggregateService.getView("PROPERTY_BY_NAME_AND_ARTIFACT"));		
		List<?> toList = aggregateService.query(artifact, settings);
		assert(toList.size() == 1);
		Object obj = toList.get(0);
		assert(MetaEntity.class.isAssignableFrom(obj.getClass()));

		MetaEntity root = (MetaEntity) obj;
		assert(root.getName().equals("ARTIFACT"));
		
		/*
		 * Since we are getting the cached object, the
		 * number of properties will be 2 even though we filter
		 * only on one property
		 */
		assert(root.getProperty().size() == 2);
		Iterator<Property> pItr = root.getProperty().iterator();
		Property property = pItr.next();
		if(!property.getName().equals("PRIORITIZE_DEFECTS")) {
			property = pItr.next();
		}
		assert( property.getName().equals("PRIORITIZE_DEFECTS") );
		assert( property.getValues().size() == 2);

		Iterator<Value> valIter = property.getValues().iterator();
		Value val1 = valIter.next();
		Value val2 = valIter.next();

		assert(val1.getValue().equals(VALUE4) && val2.getValue().equals(VALUE5) ||
				val1.getValue().equals(VALUE5) && val2.getValue().equals(VALUE4));
		} finally {
		System.out.println("****************END**********");
		}
	}

	public void listPatents() {
		setupMetaEntityState(aggregateService);
		setupMetaEntityType(aggregateService);

		// Create first patent
		Patent patent1 = new Patent();
		patent1.setName("ARTIFACT1");
		patent1.setDisplayName("Defects");
		patent1.setDescription("User story to address product defects");
		patent1.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent1 = (Patent) aggregateService.create(patent1, new Settings());				

		// Create second patent
		Patent patent2 = new Patent();
		patent2.setName("ARTIFACT2");
		patent2.setDisplayName("Enhancements");
		patent2.setDescription("User story to address product enhancements");
		patent2.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent2.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent2 = (Patent) aggregateService.create(patent2, new Settings());		

		// query the task object
		Settings settings = new Settings();
		settings.addFunctionFilter("ilike(name, :name)");
		settings.addFunctionFilter("in(state, :state)");
		settings.addFunctionFilter("equal(ownedBy.name, :owner)");
		settings.addFunctionFilter("ge(createdOn, :createdSince)");
		settings.addFunctionFilter("ge(updatedOn, :updatedSince)");
		settings.addFunctionFilter("asc(name)", 1);
		settings.setView(aggregateService.getView("ARTIFACTINFO"));	
		MetaEntity input = new MetaEntity();
		input.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		List<?> toList = aggregateService.query(input, settings);

		assert(toList.size() == 2);		

		Object obj = toList.get(0);
		assert(MetaEntity.class.isAssignableFrom(obj.getClass()));
	}

	public void listCatalogOfPatents() {
		setupMetaEntityState(aggregateService);
		setupMetaEntityType(aggregateService);

		// Create first artifact
		Patent patent1 = new Patent();
		patent1.setName("PATENT1");
		patent1.setDisplayName("Offering 1");
		patent1.setDescription("My first service offering");
		patent1.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		patent1.setMetaEntityType(getType(MetaEntityTypeEnum.PATENT.name()));		
		patent1 = (Patent) aggregateService.create(patent1, new Settings());	
		patent1 = (Patent) aggregateService.read(patent1, getSettings());	

		Catalog catalog = new Catalog();
		catalog.setName("CATALOG");
		catalog.setDisplayName("Global catalog");
		catalog.setDescription("My first catalog");
		catalog.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		catalog.setMetaEntityType(getType(MetaEntityTypeEnum.CATALOG.name()));
		catalog = (Catalog) aggregateService.create(catalog, new Settings());
		catalog = (Catalog) aggregateService.read(catalog, getSettings());	

		CatalogItem item = new CatalogItem();
		item.setContext(patent1);
		item.setCatalog(catalog);
		item = (CatalogItem) aggregateService.create(item, new Settings());	

		// query the offerings
		Settings settings = new Settings();
		settings.addFilter("catalogId", catalog.getId());
		settings.setView(aggregateService.getView("SERVICESINFO"));	
		MetaEntity input = new MetaEntity();		
		List<?> toList = aggregateService.query(input, settings);	

		assert(toList.size() == 1);
	}
}
