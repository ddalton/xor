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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryView;


public class DefaultViewBranch extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;
	
	/**
	 * Check the branches created for the TASKCHILDREN view
	 */
	@Test
	public void testCreateBranches1() {	
		DataAccessService das = aggregateManager.getDAS(); 

		Type taskType = das.getType(Task.class);		
		AggregateView view = aggregateManager.getView("TASKCHILDREN");
		
		// change to accept entity type
		QueryView viewBranch = view.getEntityView(taskType, false); 
		assert(viewBranch != null);
		assert(viewBranch.getSubBranches().size() == 0); // Ensure no child branches are created
	}

	/**
	 * Check the branches created for the TASKSET view, which has two parallel collections.
	 * One is a set, the other is a list.
	 */
	@Test
	public void testCreateBranches2() {	
		DataAccessService das = aggregateManager.getDAS(); 

		Type taskType = das.getType(Task.class);		
		AggregateView view = aggregateManager.getView("TASKSET");
		
		// change to accept entity type
		QueryView viewBranch = view.getEntityView(taskType, false); 
		assert(viewBranch != null);
		assert(viewBranch.getSubBranches().size() == 2);
	}	
	
	//@Test
	public void testTaskFull() {
		DataAccessService das = aggregateManager.getDAS(); 

		Type taskType = das.getType(Task.class);	
		AggregateView view = das.getView((EntityType) taskType);
		
		QueryView viewBranch = view.getEntityView(taskType, false); 
		assert(viewBranch != null);
		System.out.println("Subbranches: " + viewBranch.getSubBranches().size());
	}
	
	@Test
	public void testTaskQueryableRegions() {
		DataAccessService das = aggregateManager.getDAS(); 

		Type taskType = das.getType(Task.class);	
		AggregateView view = das.getView((EntityType) taskType);
		
		List<QueryView> regions = view.getStateGraph((EntityType) taskType).getQueryableRegions();
		System.out.println("Regions: " + regions.size());
	}
	
	/*
	 * TODO tests:
	 * 1. Parallel collection query using set, list and map 
	 * 2. Type - check SQL for any performance impact. Check for toOne and toMany relationship
	 * 3. > 2 levels of retrieval for set, list and map
	 * 4. Implement cross aggregate functionality
	 * 5. Complete MetaModel and associated JAXB classes
	 */
}
