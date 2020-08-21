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

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.db.base.Technician;
import tools.xor.db.pm.Task;
import tools.xor.db.vo.base.PersonVO;
import tools.xor.db.vo.base.TechnicianVO;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.TypeGraph;
import tools.xor.view.View;

public class DefaultQueryInheritance extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}

	final String NAME = "GEORGE_HADE";
	final String DISPLAY_NAME = "George Hade";
	final String DESCRIPTION = "Technician to fix HVAC issues";
	final String USER_NAME = "ghade";
	final String SKILL = "HVAC";
		
	public void queryTechnician() {
		
		// create person
		Technician technician = new Technician();
		technician.setName(NAME);
		technician.setDisplayName(DISPLAY_NAME);
		technician.setDescription(DESCRIPTION);
		technician.setUserName(USER_NAME);
		technician.setSkill(SKILL);

		technician = (Technician) aggregateManager.create(technician, new Settings());
		PersonVO person = new PersonVO();
		person.setId(technician.getId());

		// read the person object using a DataObject
		Settings settings = new Settings();
		View view = aggregateManager.getView("TECHNICIANINFO");
		view = view.copy();
		view.setSplitToRoot(false);
		settings.setView(view);
		//settings.setView(aggregateManager.getView("TECHNICIANINFO"));
		List<?> toList = aggregateManager.query(person, settings);

		assert(toList.size() == 1);

		TechnicianVO result = null;
		if(TechnicianVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (TechnicianVO) toList.get(0);
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
		assert(result.getSkill().equals(SKILL));
	}

	/*
	 *
     *               owner
     *  T A S K  ————————————————> P E R S O N
     *                                  ^
     *                                  |
     *                                  |
     *                         T E C H N I C I A N — skill
     *
     *
     *  Should produce the following query:
     *  SELECT p.skill FROM task t left join TREAT(t.owner AS Technician) p
	 *
	 */
	 public void queryTaskSkill() {

		 // create person
		 Technician technician = new Technician();
		 technician.setName(NAME);
		 technician.setDisplayName(DISPLAY_NAME);
		 technician.setDescription(DESCRIPTION);
		 technician.setUserName(USER_NAME);
		 technician.setSkill(SKILL);
		 technician = (Technician) aggregateManager.create(technician, new Settings());

		 // create task
		 Task task = new Task();
		 task.setName("ROOT");
		 task.setDisplayName("Setup DSL");
		 task.setDescription("Setup high-speed broadband internet using DSL technology");
		 task.setOwnedBy(technician);
		 Settings settings = new Settings();
		 settings.doPostFlush();
		 task = (Task) aggregateManager.create(task, new Settings());

		 // Now read the task object
		 settings = new Settings();
		 View view = aggregateManager.getView("TASKSKILL");

		 Type taskType = aggregateManager.getDataModel().getShape().getType(Task.class);
		 view = view.copy();
		 view.setSplitToRoot(false);
		 TypeGraph tg = view.getTypeGraph((EntityType)taskType, StateGraph.Scope.EDGE);

		 // TODO: why does technician not show up
		 tg.exportToDOT("TaskSkill.dot");

		 settings.setView(view);
		 List<?> toList = aggregateManager.query(task, settings);

		 assert(toList.size() == 1);
	 }
}
