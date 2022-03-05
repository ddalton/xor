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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.AssociationSetting;
import tools.xor.Settings;
import tools.xor.db.dao.TaskDao;
import tools.xor.db.pm.Task;
import tools.xor.db.pm.TaskDetails;
import tools.xor.service.AggregateManager;

public class DefaultUpdate2OneToOne extends AbstractDBTest {

	@Autowired
	protected AggregateManager aggregateService;
	
	@Autowired
	protected TaskDao taskDao;

	// 2 objects
	private Task        A;
	private TaskDetails B;

	/**
	 * Test the update of bi-directional OneToOne relationship involving 2 entities
	 * Corresponds to test case# 1 and 2 in OneToOneTestCombinations.docx document
	 *
	 */

	@BeforeEach
	public void setupData() {
		
		// create Task
		if(A == null) {
			A = new Task();
			A.setName("FIX_DEFECTS");
			A.setDisplayName("Fix defects");
			A.setDescription("Task to track the defect fixing effort");
			A = (Task) aggregateService.create(A, new Settings());	
			A = taskDao.findById(A.getId());
			assert(A.getId() != null);			
		}

		// Create 2nd Task
		if(B == null) {
			B = new TaskDetails();
			B = (TaskDetails) aggregateService.create(B, new Settings());
		}
		
		A = (Task) aggregateService.read(A, getSettings());
		B = (TaskDetails) aggregateService.read(B, getSettings());		
	}

	@AfterEach
	public void resetAssociation() {
		// reset the OneToOne association
		A.setTaskDetails(null);
		B.setTask(null);

		A = (Task) aggregateService.update(A, getSettings());
		
		// Create non-persistence managed objects
		A = (Task) aggregateService.read(A, getSettings());
		B = (TaskDetails) aggregateService.read(B, getSettings());
		
		assert(A.getTaskDetails() == null);
		assert(B.getTask() == null);		
	}		
	
	protected Settings getDetailsSettings() {
		Settings settings = new Settings();
		
		// TaskDetails is a reference association and we need to explicitly mention it 
		settings.expand(new AssociationSetting(TaskDetails.class));
		
		settings.expand(new AssociationSetting(Task.class));

		return settings;
	}	

	public void testCase1() {
		// Setup the bi-directional link
		A.setTaskDetails(B);
		B.setTask(A);

		A = (Task) aggregateService.update(A, getDetailsSettings());

		assert(A.getTaskDetails() != null);
		assert(B.getTask() != null);
		
		A = (Task) aggregateService.read(A, getSettings());
		B = (TaskDetails) aggregateService.read(B, getSettings());		
	}
	
	public void testCase2() {
		// Setup the bi-directional link
		A.setTaskDetails(B);

		A = (Task) aggregateService.update(A, getDetailsSettings());
		A = taskDao.findById(A.getId());
		assert(A.getTaskDetails() != null);
		assert(A.getTaskDetails().getTask() != null);
		
		A = (Task) aggregateService.read(A, getDetailsSettings());
		
		assert(A.getTaskDetails() != null);
		assert(A.getTaskDetails().getTask() != null);
	}	
}
