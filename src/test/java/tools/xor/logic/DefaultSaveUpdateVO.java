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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.MetaEntity;
import tools.xor.db.base.MetaEntityState;
import tools.xor.db.base.Person;
import tools.xor.db.common.ValueType;
import tools.xor.db.enums.base.MetaEntityStateEnum;
import tools.xor.db.enums.base.MetaEntityTypeEnum;
import tools.xor.db.enums.common.ValueTypeEnum;
import tools.xor.db.pm.Task;
import tools.xor.db.vo.base.MetaEntityStateVO;
import tools.xor.db.vo.base.MetaEntityTypeVO;
import tools.xor.db.vo.base.MetaEntityVO;
import tools.xor.db.vo.base.PersonVO;
import tools.xor.db.vo.common.PropertyVO;
import tools.xor.db.vo.common.ValueTypeVO;
import tools.xor.db.vo.common.ValueVO;
import tools.xor.db.vo.pm.TaskVO;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.view.expression.FunctionHandler;

public class DefaultSaveUpdateVO extends AbstractDBTest {
	private static final String TASK_NAME = "SETUP_DSL";

	@Autowired
	protected AggregateManager aggregateService;

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}

	private MetaEntityStateVO getState(String artifactState) {
		MetaEntityState metaEntityState = new MetaEntityState();
		metaEntityState.setName(artifactState); // ArtifactStateEnum.ACTIVE.name()
		MetaEntityStateVO artifactStateVO = (MetaEntityStateVO) aggregateService.read(metaEntityState, new Settings());
		return artifactStateVO;
	}

	private MetaEntityTypeVO getType(String artifactType) {	
		MetaEntityTypeVO artifactTypeVO = new MetaEntityTypeVO();
		artifactTypeVO.setName(artifactType); // ArtifactTypeEnum.CONTAINER.name() 
		artifactTypeVO = (MetaEntityTypeVO) aggregateService.read(artifactTypeVO, new Settings());
		return artifactTypeVO;
	}

	protected void saveTask() {
		// create person
		PersonVO owner = new PersonVO();
		owner.setName("DILIP_DALTON");
		owner.setDisplayName("Dilip Dalton");
		owner.setDescription("Software engineer in the bay area");
		owner.setUserName("daltond");
		Person managedOwner = (Person) aggregateService.create(owner, new Settings());
		PersonVO person = (PersonVO) aggregateService.read(managedOwner, new Settings());	

		// Create Task
		TaskVO task = new TaskVO();
		task.setName(TASK_NAME);
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		task.setAssignedTo(person);
		Task managedTask = (Task) aggregateService.create(task, getSettings());
		task = (TaskVO) aggregateService.read(managedTask, getSettings());

		assert(task.getId() != null);
		assert(task.getAssignedTo() != null);
		assert(task.getAssignedTo().getId() != null);
	}

	public void readWithVO() {
		// Create Task
		TaskVO task = new TaskVO();
		task.setName(TASK_NAME);
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		Task managedTask = (Task) aggregateService.create(task, new Settings());		

		TaskVO t = new TaskVO();
		t.setId(managedTask.getId());
		TaskVO taskVO = (TaskVO) aggregateService.read(t, new Settings());

		assert(taskVO.getId() != null);
		assert(taskVO.getName().equals(TASK_NAME));
		assert(taskVO.getDisplayName().equals("Setup DSL"));
		assert(taskVO.getDescription().equals("Setup high-speed broadband internet using DSL technology"));
	}

	public void queryProperties() {
		final String VALUE4 = "Value 4";
		final String VALUE5 = "Value 5";

		setupValueTypeVO(aggregateService);
		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);

		ValueType valueType = new ValueType();
		valueType.setName(ValueTypeEnum.STRING.name());
		ValueTypeVO valueTypeVO = (ValueTypeVO) aggregateService.read(valueType, new Settings());

		assert(valueTypeVO != null);
		assert(valueTypeVO.getName().equals("STRING"));

		// Property and values
		Set<ValueVO> p1Values = new HashSet<ValueVO>();
		ValueVO value1 = new ValueVO();
		value1.setValue("Value 1");
		p1Values.add(value1);
		ValueVO value2 = new ValueVO();
		value2.setValue("Value 2");
		p1Values.add(value2);		
		ValueVO value3 = new ValueVO();
		value3.setValue("Value 3");
		p1Values.add(value3);
		PropertyVO property1 = new PropertyVO();
		property1.setName("FIX_DEFECTS");
		property1.setDisplayName("Fix defects");
		property1.setDescription("Task to track the defect fixing effort");
		property1.setValueType(valueTypeVO);
		property1.setValues(p1Values);

		Set<ValueVO> p2Values = new HashSet<ValueVO>();
		ValueVO value4 = new ValueVO();
		value4.setValue(VALUE4);
		p2Values.add(value4);
		ValueVO value5 = new ValueVO();
		value5.setValue(VALUE5);
		p2Values.add(value5);		
		PropertyVO property2 = new PropertyVO();
		property2.setName("PRIORITIZE_DEFECTS");
		property2.setDisplayName("Prioritize defects");
		property2.setDescription("Based upon the effort required for the defects prioritize them");
		property2.setValueType(valueTypeVO);
		property2.setValues(p2Values);		

		// artifact
		Set<PropertyVO> p = new HashSet<PropertyVO>();
		p.add(property1);
		p.add(property2);
		MetaEntityVO artifactVO = new MetaEntityVO();
		artifactVO.setName("ARTIFACT");
		artifactVO.setDisplayName("Defects");
		artifactVO.setDescription("User story to address product defects");
		artifactVO.setProperty(p);
		artifactVO.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		artifactVO.setMetaEntityType(getType(MetaEntityTypeEnum.TASK.name()));		

		MetaEntity managedArtifact = (MetaEntity) aggregateService.create(artifactVO, new Settings());	
		artifactVO = (MetaEntityVO) aggregateService.read(managedArtifact, getSettings());				

		// query the properties
		Settings settings = new Settings();
		settings.setParam("artifactId", artifactVO.getId());
		settings.setParam("propertyName", "PRIORITIZE_DEFECTS");
		settings.setView(aggregateService.getView("PROPERTY_BY_NAME_AND_ARTIFACT"));	
		List<?> toList = aggregateService.query(artifactVO, settings);

		assert(toList.size() == 1);
		Object obj = toList.get(0);
		assert(MetaEntityVO.class.isAssignableFrom(obj.getClass()));

		MetaEntityVO root = (MetaEntityVO) obj;
		assert(root.getName().equals("ARTIFACT"));
		assert(root.getProperty().size() == 1);

		Iterator<PropertyVO> pItr = root.getProperty().iterator();
		PropertyVO property = pItr.next();
		assert( property.getName().equals("PRIORITIZE_DEFECTS") );
		assert( property.getValues().size() == 2);

		Iterator<ValueVO> valIter = property.getValues().iterator();
		ValueVO val1 = valIter.next();
		ValueVO val2 = valIter.next();

		assert(val1.getValue().equals(VALUE4) && val2.getValue().equals(VALUE5) ||
				val1.getValue().equals(VALUE5) && val2.getValue().equals(VALUE4));
	}		

	public void queryTaskChildren() {
		// Create a task
		TaskVO userStory = new TaskVO();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");

		// Create 2 children
		TaskVO A = new TaskVO();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");
		A.setTaskParent(userStory);

		TaskVO B = new TaskVO();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		B.setTaskParent(userStory);	

		Set<TaskVO> children = new HashSet<TaskVO>();
		children.add(A);
		children.add(B);
		userStory.setTaskChildren(children);
		Task persistedTask = (Task) aggregateService.create(userStory, new Settings());	
		assert(persistedTask.getTaskChildren() != null && persistedTask.getTaskChildren().size() == 2);

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKCHILDREN"));
		settings.addFunction(FunctionHandler.NOTNULL, "taskChildren.id");
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(TaskVO.class.isAssignableFrom(obj.getClass()));

		TaskVO root = (TaskVO) obj;
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 2);
	}

	public void queryTaskSetNoDep() {
		// Create a task
		TaskVO userStory = new TaskVO();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");

		// Create 2 children
		TaskVO A = new TaskVO();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");
		A.setTaskParent(userStory);

		TaskVO B = new TaskVO();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		B.setTaskParent(userStory);	

		Set<TaskVO> children = new HashSet<TaskVO>();
		children.add(A);
		children.add(B);
		userStory.setTaskChildren(children);
		aggregateService.create(userStory, new Settings());	

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKSET"));
		settings.addFunction(FunctionHandler.NOTNULL, "taskChildren.id");
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 3);

		int userStoryIndex = 0;
		for(int i = 0; i < toList.size(); i++) {
			TaskVO task = (TaskVO) toList.get(i);
			if(task.getTaskChildren() != null) {
				assert(task.getTaskChildren().size() == 2);
				userStoryIndex = i;
				break;
			}
		}

		TaskVO child1 = (TaskVO) toList.get((userStoryIndex+1)%3);
		TaskVO child2 = (TaskVO) toList.get((userStoryIndex+2)%3);
		assert(child1.getTaskChildren() == null);
		assert(child2.getTaskChildren() == null);
	}	

	public void queryTaskSetWithDep() {
		// Create a task
		TaskVO userStory = new TaskVO();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");

		// Create 2 children
		TaskVO A = new TaskVO();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");
		A.setTaskParent(userStory);

		TaskVO B = new TaskVO();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		B.setTaskParent(userStory);	

		Set<TaskVO> children = new HashSet<TaskVO>();
		children.add(A);
		children.add(B);

		// Add 2 dependents
		TaskVO D1 = new TaskVO();
		D1.setName("DEPENDANT_1");
		D1.setDisplayName("First dependant");
		D1.setDescription("First task that is dependant upon the current task being completed");

		TaskVO D2 = new TaskVO();
		D2.setName("DEPENDANT_2");
		D2.setDisplayName("Second dependant");
		D2.setDescription("Second task that is dependant upon the current task being completed");	

		List<TaskVO> dependants = new ArrayList<TaskVO>();
		dependants.add(D1);
		dependants.add(D2);

		userStory.setTaskChildren(children);
		userStory.setDependants(dependants);
		Task us = (Task) aggregateService.create(userStory, new Settings());	

		TaskVO readTask = (TaskVO) aggregateService.read(us, new Settings());

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("TASKSET"));
		settings.addFunction(FunctionHandler.NOTNULL, "taskChildren.id");
		settings.addFunction(FunctionHandler.NOTNULL, "dependants.id");
		List<?> toList = aggregateService.query(userStory, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(TaskVO.class.isAssignableFrom(obj.getClass()));

		TaskVO root = (TaskVO) obj;
		assert(root.getTaskChildren() != null && root.getTaskChildren().size() == 2);
		assert(root.getDependants() != null && root.getDependants().size() == 2);

		TaskVO first = root.getDependants().get(0);
		TaskVO second = root.getDependants().get(1);

		assert(first.getName().equals("DEPENDANT_1"));
		assert(second.getName().equals("DEPENDANT_2"));
	}	

	public void queryEntity() {

		setupMetaEntityStateVO(aggregateService);
		setupMetaEntityTypeVO(aggregateService);

		// create person
		MetaEntityVO defect = new MetaEntityVO();
		defect.setName("TEST_ARTIFACT");
		defect.setDisplayName("Test artifact");
		defect.setDescription("An artifact created for JUnit tests");
		defect.setState(getState(MetaEntityStateEnum.ACTIVE.name()));
		defect.setMetaEntityType(getType(MetaEntityTypeEnum.TASK.name()));
		aggregateService.create(defect, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("ARTIFACTINFO"));		
		List<?> toList = aggregateService.query(defect, settings);

		assert(toList.size() == 1);

		MetaEntityVO result = null;
		if(MetaEntityVO.class.isAssignableFrom(toList.get(0).getClass()))
			result = (MetaEntityVO) toList.get(0);

		assert(result != null);
		assert(result.getName().equals("TEST_ARTIFACT"));
		assert(result.getDisplayName().equals("Test artifact"));
		assert(result.getDescription().equals("An artifact created for JUnit tests"));
		assert(result.getMetaEntityType() != null && result.getMetaEntityType().getName().equals("TASK"));
		assert(result.getState() != null && result.getState().getName().equals("ACTIVE"));
		assert(result.getOwnedBy() == null);
	}
	
	public void queryOne() {

		// Create 2 tasks
		TaskVO A = new TaskVO();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");
		Task managedTaskA = (Task) aggregateService.create(A, new Settings());
		A = (TaskVO) aggregateService.read(managedTaskA, new Settings());

		TaskVO B = new TaskVO();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		aggregateService.create(B, new Settings());	

		// query the task object
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO"));		
		List<?> toList = aggregateService.query(A, settings);

		assert(toList.size() == 1);		

		Object obj = toList.get(0);
		assert(TaskVO.class.isAssignableFrom(obj.getClass()));

		TaskVO root = (TaskVO) obj;
		assert(root.getName().equals("FIX_DEFECTS"));
	}

	public void queryOneNative() {
		String NAME = "GEORGE_WASHINGTON_2";
		String DISPLAY_NAME = "George Washington";
		String DESCRIPTION = "First President of the United States of America";
		String USER_NAME = "gwashington";	
		
		String JA_NAME = "JOHN_ADAMS";
		String JA_DISPLAY_NAME = "John Adams";
		String JA_DESCRIPTION = "Second President of the United States of America";
		String JA_USER_NAME = "jadams";	
		
		PersonVO gw = new PersonVO();
		gw.setName(NAME);
		gw.setDisplayName(DISPLAY_NAME);
		gw.setDescription(DESCRIPTION);
		gw.setUserName(USER_NAME);		
		Person managedGW = (Person) aggregateService.create(gw, new Settings());
		gw = (PersonVO) aggregateService.read(managedGW, new Settings());
		
		// create person
		PersonVO ja = new PersonVO();
		ja.setName(JA_NAME);
		ja.setDisplayName(JA_DISPLAY_NAME);
		ja.setDescription(JA_DESCRIPTION);
		ja.setUserName(JA_USER_NAME);
		aggregateService.create(ja, new Settings());	

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateService.getView("BASICINFO_NATIVE"));	
		settings.setPreFlush(true);
		List<?> toList = aggregateService.query(gw, settings);

		assert(toList.size() == 1);

		PersonVO result = null;
		if(PersonVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (PersonVO) toList.get(0);
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
	}	
	
}
