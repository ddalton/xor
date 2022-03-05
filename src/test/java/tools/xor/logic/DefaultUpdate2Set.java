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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.BusinessObject;
import tools.xor.Settings;
import tools.xor.action.Executable;
import tools.xor.action.PropertyKey;
import tools.xor.action.SetUpdateAction;
import tools.xor.core.Interceptor;
import tools.xor.db.dao.TaskDao;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultUpdate2Set extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "DEFECT 1";

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	// 2 objects
	private Task A;
	private Task B;


	/**
	 * Test the update of bi-directional OneToOne relationship involving 3 entities
	 * Corresponds to test case# 3-9, and 17 in OneToOneTestCombinations.docx document
	 *
	 */

	@BeforeEach
	public void setupData() {
		// create defect fixing Task
		if(A == null) {
			A = new Task();
			A.setName(A_NAME);
			A.setDisplayName("Fix defects");
			A.setDescription("Task to track the defect fixing effort");
			A = (Task) aggregateService.create(A, new Settings());
			A = taskDao.findById(A.getId());			
		}

		// Create defect priority task
		if(B == null) {
			B = new Task();
			B.setName(B_NAME);
			B.setDisplayName("Defect 1");
			B.setDescription("The first defect to be filed");
			B = (Task) aggregateService.create(B, new Settings());
			B = taskDao.findById(B.getId());			
		}	

		assert(A.getId() != null);			
		assert(B.getId() != null);		

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
	}

	@AfterEach
	public void resetAssociation() {
		// reset the OneToOne association
		A.setTaskChildren(null);
		B.setTaskParent(null);

		A = (Task) aggregateService.update(A, new Settings());	

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getTaskChildren() == null || A.getTaskChildren().size() == 0);
		assert(B.getTaskParent() == null);	
	}		

	public void testCase1() {
		Set<Task> children = new HashSet<Task>();
		children.add(B);

		// Setup the bi-directional link
		A.setTaskChildren(children);
		B.setTaskParent(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else
					assert(actions.size() == 2);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 2);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getTaskParent() != null);
	}

	public void testCase2() {
		Set<Task> children = new HashSet<Task>();
		children.add(B);

		// Setup the bi-directional link
		A.setTaskChildren(children);
		B.setTaskParent(null);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				assert(actions.size() == 1);
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 2);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getTaskParent() != null);
	}

	public void testCase10() {
		// Setup only the backRef
		B.setTaskParent(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 2);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});
		settings.setPostFlush(true);
		B = (Task) aggregateService.update(B, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getTaskParent() != null);
	}
	
}
