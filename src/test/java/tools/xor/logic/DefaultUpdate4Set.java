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
import java.util.Iterator;
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

public class DefaultUpdate4Set extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "DEFECT 1";
	public static final String C_NAME = "PERFORMANCE";
	public static final String D_NAME = "DEFECT 2";		

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	// 3 objects
	private Task A;
	private Task B;
	private Task C;
	private Task D;		

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

		if(C == null) {
			C = new Task();
			C.setName(C_NAME);
			C.setDisplayName("Performance related defects");
			C.setDescription("Defects related to the performance of the application");
			C = (Task) aggregateService.create(C, new Settings());
			C = taskDao.findById(C.getId());			
		}

		if(D == null) {
			D = new Task();
			D.setName(D_NAME);
			D.setDisplayName("Defect 2");
			D.setDescription("The second defect to be filed");
			D = (Task) aggregateService.create(D, new Settings());
			D = taskDao.findById(D.getId());			
		}


		assert(A.getId() != null);			
		assert(C.getId() != null);		
		assert(B.getId() != null);
		assert(D.getId() != null);		

		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		
	}

	@AfterEach
	public void resetAssociation() {
		// reset the association
		A.setTaskChildren(null);
		A.setAlternateTask(null);		
		C.setTaskChildren(null);			
		C.setTaskParent(null);		
		B.setTaskParent(null);
		D.setTaskChildren(null);		
		D.setTaskParent(null);
		D.setAlternateTask(null);				

		A = (Task) aggregateService.update(A, new Settings());	
		C = (Task) aggregateService.update(C, new Settings());
		D = (Task) aggregateService.update(D, new Settings());		

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());	
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() == null || A.getTaskChildren().size() == 0);
		assert(B.getTaskParent() == null);		
		assert(C.getTaskParent() == null);
		assert(D.getTaskParent() == null);		
	}		

	public void testCase14() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		D.getTaskChildren().clear();
		A.setAlternateTask(D);
		C.setTaskParent(null);
		B.setTaskParent(A);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME)) {
					System.out.println("B_NAME expected 3 found: " + actions.size());
					for(Executable e: actions) {
						System.out.println("Executable: " + e.toString());
					}
					assert(actions.size() == 3); 
				} else if(task.getName().equals(C_NAME)) {
					System.out.println("C_NAME expected 2 found: " + actions.size());
					for(Executable e: actions) {
						System.out.println("Executable: " + e.toString());
					}					
					assert(actions.size() == 2); 
				} else {
					System.out.println("else expected 1 found: " + actions.size());
					for(Executable e: actions) {
						System.out.println("Executable: " + e.toString());
					}					
					assert(actions.size() == 1);
				}
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(C.getTaskParent() == null);
		assert(D.getTaskChildren() == null || D.getTaskChildren().size() == 0);
	}

	public void testCase15() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		D.getTaskChildren().clear();
		A.setAlternateTask(D);
		C.setTaskParent(null);
		B.setTaskParent(null);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME)) {
					assert(actions.size() == 3); 
				} else if(task.getName().equals(C_NAME)) {
					assert(actions.size() == 2); 
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(C.getTaskParent() == null);
		assert(D.getTaskChildren() == null || D.getTaskChildren().size() == 0);
	}

	public void testCase16() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		children = D.getTaskChildren();
		children.clear();
		children.add(C);

		A.setAlternateTask(D);
		C.setTaskParent(D);
		B.setTaskParent(A);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME) || task.getName().equals(C_NAME)) {
					assert(actions.size() == 3); 
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(D.getTaskChildren() != null || D.getTaskChildren().size() == 1);
		assert(C.getId() == D.getTaskChildren().iterator().next().getId());
	}

	public void testCase17() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		children = D.getTaskChildren();
		children.clear();
		children.add(C);

		A.setAlternateTask(D);
		C.setTaskParent(D);
		B.setTaskParent(null);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME) || task.getName().equals(C_NAME)) {
					assert(actions.size() == 3); 
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(D.getTaskChildren() != null || D.getTaskChildren().size() == 1);
		assert(C.getId() == D.getTaskChildren().iterator().next().getId());
	}

	public void testCase18() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		children = D.getTaskChildren();
		children.clear();
		//children.add(C);

		A.setAlternateTask(D);
		C.setTaskParent(D);
		B.setTaskParent(null);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME) ) {
					assert(actions.size() == 3); 
				} else if (task.getName().equals(C_NAME)) {
					assert(actions.size() == 2); 
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(D.getTaskChildren() != null || D.getTaskChildren().size() == 1);
		assert(C.getId() == D.getTaskChildren().iterator().next().getId());
	}

	public void testCase19() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		children = D.getTaskChildren();
		children.clear();
		//children.add(C);

		A.setAlternateTask(D);
		C.setTaskParent(null);
		B.setTaskParent(null);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME) ) {
					assert(actions.size() == 3); 
				} else if (task.getName().equals(C_NAME)) {
					assert(actions.size() == 2); 
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(D.getTaskChildren() == null || D.getTaskChildren().size() == 0);
		assert(C.getTaskParent() == null);
	}

	public void testCase21() {
		// Initial association between A and C
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		// Initial association between D and B
		B = taskDao.findById(B.getId());		
		children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		D = taskDao.findById(D.getId());		
		D.setTaskChildren(children);
		B.setTaskParent(D);
		taskDao.saveOrUpdate(D);


		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		// D.getTaskChildren().clear(); should automatically clear the children by the framework
		B.setTaskParent(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME)) {
					assert(actions.size() == 2); 
				} else
					assert(actions.size() == 1);				
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(C.getTaskParent() == null);
		assert(D.getTaskChildren() == null || D.getTaskChildren().size() == 0);
	}

	public void testCase22() {
		C = taskDao.findById(C.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(C);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		A = (Task) aggregateService.read(A, getSettings());
		C = A.getTaskChildren().iterator().next();
		assert(C.getTaskParent() != null);

		// Remove child1 and add child2
		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		children.add(D);
		A.setAlternateTask(C);
		C.setTaskParent(null);
		B.setTaskParent(A);
		D.setTaskParent(A);

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
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 2);
	}
	
	public void testCase23() {
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		Set<Task> children = new HashSet<Task>();
		children.add(C);
		children.add(B);			

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		B.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		A = (Task) aggregateService.read(A, getSettings());
		
		assert(A.getTaskChildren().size() == 2);		
		Iterator<Task> iter = A.getTaskChildren().iterator();
		Task child1 = iter.next();
		Task child2 = iter.next();
		if(child1.getName().equals(B_NAME)) {
			B = child1;
			C = child2;
		} else {
			C = child1;
			B = child2;
		}
		assert(B.getName().equals(B_NAME) && C.getName().equals(C_NAME));

		children = A.getTaskChildren();
		children.clear();
		children.add(D);
		A.setAlternateTask(C);
		D.setAlternateTask(B);
		C.setTaskParent(null);
		B.setTaskParent(null);
		D.setTaskParent(A);

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
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(D.getId() == A.getTaskChildren().iterator().next().getId());
	}

	public void testCase24() {
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		Set<Task> children = new HashSet<Task>();
		children.add(C);
		children.add(B);			

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		C.setTaskParent(A);
		B.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		A = (Task) aggregateService.read(A, getSettings());
		
		assert(A.getTaskChildren().size() == 2);		
		Iterator<Task> iter = A.getTaskChildren().iterator();
		Task child1 = iter.next();
		Task child2 = iter.next();
		if(child1.getName().equals(B_NAME)) {
			B = child1;
			C = child2;
		} else {
			C = child1;
			B = child2;
		}
		assert(B.getName().equals(B_NAME) && C.getName().equals(C_NAME));

		children = A.getTaskChildren();
		children.clear();
		children.add(B);
		children.add(D);
		A.setAlternateTask(C);
		C.setTaskParent(null);
		B.setTaskParent(A);
		D.setTaskParent(A);

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
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 2);
	}
	
}
