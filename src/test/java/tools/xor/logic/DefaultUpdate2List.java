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
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.dao.TaskDao;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultUpdate2List extends AbstractDBTest {

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
	
	public void testUniDir() {
		// Create a task
		Task userStory = new Task();
		userStory.setName("DEFECTS");
		userStory.setDisplayName("Defects");
		userStory.setDescription("User story to address product defects");
		
		// Create 2 children
		Task A = new Task();
		A.setName("FIX_DEFECTS");
		A.setDisplayName("Fix defects");
		A.setDescription("Task to track the defect fixing effort");
		A.setTaskParent(userStory);
		
		Task B = new Task();
		B.setName("PRIORITIZE_DEFECTS");
		B.setDisplayName("Prioritize defects");
		B.setDescription("Based upon the effort required for the defects prioritize them");
		B.setTaskParent(userStory);	
		
		Set<Task> children = new HashSet<Task>();
		children.add(A);
		children.add(B);
		
		// Add 2 dependents
		Task D1 = new Task();
		D1.setName("DEPENDANT_1");
		D1.setDisplayName("First dependant");
		D1.setDescription("First task that is dependant upon the current task being completed");
		
		Task D2 = new Task();
		D2.setName("DEPENDANT_2");
		D2.setDisplayName("Second dependant");
		D2.setDescription("Second task that is dependant upon the current task being completed");	
		
		List<Task> dependants = new ArrayList<Task>();
		dependants.add(D1);
		dependants.add(D2);
		
		userStory.setTaskChildren(children);
		userStory.setDependants(dependants);
		Task us = (Task) aggregateService.create(userStory, new Settings());	
		
		Task readTask = (Task) aggregateService.read(us, new Settings());

		assert(readTask.getTaskChildren() != null && readTask.getTaskChildren().size() == 2);
		assert(readTask.getDependants() != null && readTask.getDependants().size() == 2);
		
		Task first = readTask.getDependants().get(0);
		Task second = readTask.getDependants().get(1);
		
		assert(first.getName().equals("DEPENDANT_1"));
		assert(second.getName().equals("DEPENDANT_2"));		
	}		
	
}
