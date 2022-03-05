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
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.dao.TaskDao;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultPerfUpdate extends AbstractDBTest {

	public static final String ROOT_NAME = "ROOT_TASK";
	public static final String CHILD_PREFIX = "CHILD_";
	public static final String GRAND_CHILD_PREFIX = "GRAND_CHILD_";
	public static final int NUM_CHILD = 10000;

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	protected Task getRootTask() {
		Task ROOT;

		ROOT = new Task();
		ROOT.setName(ROOT_NAME);
		ROOT.setDisplayName("Root task");
		ROOT.setDescription("Task with " + NUM_CHILD + " children");

		addChildren(ROOT, NUM_CHILD, CHILD_PREFIX);
		/*
		for(Task parent: ROOT.getTaskChildren()) {
			addChildren(parent, 3, GRAND_CHILD_PREFIX);
		}*/

		return ROOT;
	}
	
	private void addChildren(Task parent, int count, String prefix) {
		Set<Task> children = new HashSet<Task>();
		for (int i = 0; i < count; i++) {
			Task child = new Task();
			child.setName(prefix + i);
			child.setDisplayName("DISPLAY " + child.getName());
			child.setDescription("Description " + child.getName());
			children.add(child);
		}		
		parent.setTaskChildren(children);
	}

	/**
	 * Update a single child task of a parent having 10000 children. This is
	 * performance test The first test is with baseline set to false and the
	 * second test is with baseline set to true to see if there is a difference
	 * in loading the object in a single query and if attach works.
	 * Each child has 3 children for a total of 10000*3 = 30000 tasks.
	 */
	public void createData() {

		Date start = new Date();
		Task ROOT = getRootTask();
		ROOT = (Task) aggregateService.create(ROOT, new Settings());
		ROOT = taskDao.findById(ROOT.getId());
		System.out.println("DefaultPerfUpdate#createData.create took " + ((new Date().getTime()-start.getTime())) + " milliseconds");

		start = new Date();
		assert (ROOT.getId() != null);
		ROOT = (Task) aggregateService.read(ROOT, getSettings());
		assert (ROOT.getTaskChildren() != null && ROOT.getTaskChildren().size() == NUM_CHILD);
		System.out.println("DefaultPerfUpdate#createData.read took " + ((new Date().getTime()-start.getTime())) + " milliseconds");
	}

	public void testNoBaseline() throws InterruptedException {
		createData();
		// TODO: update a single task with no baseline, and initializing the
		// collection
	}

	public void testBaseline() {
		//createData();
		// TODO: update a single task with baseline
	}
}
