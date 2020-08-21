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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.view.AggregateTree;
import tools.xor.view.View;

public class DefaultViewBranch extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;
	
	/**
	 * Check the branches created for the TASKCHILDREN view
	 */
	@Test
	public void testCreateBranches1() {	
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		View view = aggregateManager.getView("TASKCHILDREN");
		
		// change to accept entity type
		AggregateTree viewBranch = view.getAggregateTree(taskType);
		assert(viewBranch != null);
		assert(viewBranch.getOutEdges(viewBranch.getRoot()).size() == 0); // Ensure no child branches are created
	}

	/**
	 * Check the branches created for the TASKSET view, which has two parallel collections.
	 * One is a set, the other is a list.
	 */
	@Test
	public void testCreateBranches2() {	
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		View view = aggregateManager.getView("TASKSET");
		
		// change to accept entity type
		AggregateTree queryTree = view.getAggregateTree(taskType);
		queryTree.exportToDOT("branches2.dot");

		assert(queryTree != null);

		assert(queryTree.getRoots().size() == 2);
	}	
	
	//@Test
	public void testTaskFull() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		View view = das.getShape().getView((EntityType) taskType);
		
		AggregateTree viewBranch = view.getAggregateTree(taskType);
		assert(viewBranch != null);
		System.out.println("Subbranches: " + viewBranch.getOutEdges(viewBranch.getRoot()).size());
	}
	
	@Test
	public void testValidView() {	
		View view = aggregateManager.getView("VALIDENTITY");
		assert(view.isValid());		
	}	

	@Test
	public void testInValidView() {	
		View view = aggregateManager.getView("INVALIDENTITY");
		assert(!view.isValid());
	}	
	
}
