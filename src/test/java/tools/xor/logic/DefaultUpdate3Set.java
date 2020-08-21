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

public class DefaultUpdate3Set extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "DEFECT 1";
	public static final String C_NAME = "PERFORMANCE";	

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	// 3 objects
	private Task A;
	private Task B;
	private Task C;	

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

		assert(A.getId() != null);			
		assert(C.getId() != null);		
		assert(B.getId() != null);		

		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
	}

	@AfterEach
	public void resetAssociation() {
		// reset the OneToOne association
		A.setTaskChildren(null);
		A.setAlternateTask(null);
		C.setTaskChildren(null);		
		C.setTaskParent(null);
		B.setTaskParent(null);		

		A = (Task) aggregateService.update(A, getSettings());
		C = (Task) aggregateService.update(C, getSettings());		
		B = (Task) aggregateService.update(B, getSettings());	

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() == null || A.getTaskChildren().size() == 0);
		assert(C.getTaskParent() == null);
		assert(B.getTaskParent() == null);		
	}		

	public void testCase3() {
		Set<Task> children = new HashSet<Task>();
		children.add(C);
		children.add(B);		

		// Setup the bi-directional link
		A.setTaskChildren(children);
		C.setTaskParent(A);
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
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 2);
		assert(C.getTaskParent() != null);
		assert(B.getTaskParent() != null);		
	}

	protected Settings getSettings() {
		// Be default we treat technician to be part of the aggregate
		Settings settings = super.getSettings();

		return settings;
	}	
	
	public void testCase4() {
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
		A.setAlternateTask(C);
		C.setTaskParent(null);
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
				assert(actions.size() == 3);

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
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());		
	}

	public void testCase5() {
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
		A.setAlternateTask(C);
		C.setTaskParent(null);
		B.setTaskParent(null); // We are explicitly not setting the backRef in the input. This should automatically be set by the framework.

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else if(task.getName().equals(B_NAME)) 
					assert(actions.size() == 1);
				else
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

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(B.getTaskParent() != null && B.getTaskParent().getName().equals(A.getName()));
	}

	public void testCase6() {
		// initially set B to be a child of C
		B = taskDao.findById(B.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		C = taskDao.findById(C.getId());		
		C.setTaskChildren(children);
		B.setTaskParent(C);
		taskDao.saveOrUpdate(C);

		C = (Task) aggregateService.read(C, getSettings());
		B = C.getTaskChildren().iterator().next();
		assert(B.getTaskParent() != null);

		// Remove B from C and add to A
		C.getTaskChildren().clear();
		children = new HashSet<Task>();
		children.add(B);
		A.setTaskChildren(children);
		A.setAlternateTask(C);
		B.setTaskParent(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else if(task.getName().equals(B_NAME)) { 
					assert(actions.size() == 3);
				} else if(task.getName().equals(C_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));					
				}
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

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(B.getTaskParent() != null && B.getTaskParent().getName().equals(A.getName()));
	}

	public void testCase7() {
		// initially set B to be a child of C
		B = taskDao.findById(B.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		C = taskDao.findById(C.getId());		
		C.setTaskChildren(children);
		B.setTaskParent(C);
		taskDao.saveOrUpdate(C);

		C = (Task) aggregateService.read(C, getSettings());
		B = C.getTaskChildren().iterator().next();
		assert(B.getTaskParent() != null);

		// Remove B from C and add to A
		C.getTaskChildren().clear();
		children = new HashSet<Task>();
		children.add(B);
		A.setTaskChildren(children);
		A.setAlternateTask(C);
		B.setTaskParent(null); // Set the new backRef to null, this will be populated with the correct value by the framework

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else if(task.getName().equals(B_NAME)) { 
					assert(actions.size() == 3);
				} else if(task.getName().equals(C_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));					
				}
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

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(B.getTaskParent() != null && B.getTaskParent().getName().equals(A.getName()));
	}	

	public void testCase8() {
		// initially set B to be a child of C
		B = taskDao.findById(B.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		C = taskDao.findById(C.getId());		
		C.setTaskChildren(children);
		B.setTaskParent(C);
		taskDao.saveOrUpdate(C);

		C = (Task) aggregateService.read(C, getSettings());
		B = C.getTaskChildren().iterator().next();
		assert(B.getTaskParent() != null);

		// Remove B from C and add to A
		C.getTaskChildren().clear();
		children = new HashSet<Task>();
		children.add(B);
		A.setTaskChildren(children);
		// A.setAlternateTask(C);  C is not part of the input, see test case #6
		B.setTaskParent(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else if(task.getName().equals(B_NAME)) { 
					assert(actions.size() == 2);
				} else if(task.getName().equals(C_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));					
				}
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

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(B.getTaskParent() != null && B.getTaskParent().getName().equals(A.getName()));
	}

	public void testCase9() {
		// initially set B to be a child of C
		B = taskDao.findById(B.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		C = taskDao.findById(C.getId());		
		C.setTaskChildren(children);
		B.setTaskParent(C);
		taskDao.saveOrUpdate(C);

		C = (Task) aggregateService.read(C, getSettings());
		B = C.getTaskChildren().iterator().next();
		assert(B.getTaskParent() != null);

		// Remove B from C and add to A
		C.getTaskChildren().clear();
		children = new HashSet<Task>();
		children.add(B);
		A.setTaskChildren(children);
		// A.setAlternateTask(C);  C is not part of the input, see test case #6
		B.setTaskParent(null); // Set the new backRef to null, this will be populated with the correct value by the framework

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));
				} else if(task.getName().equals(B_NAME)) { 
					assert(actions.size() == 2);
				} else if(task.getName().equals(C_NAME)) {
					assert(actions.size() == 1); // SetUpdateAction
					assert(SetUpdateAction.class.isAssignableFrom(actions.get(0).getClass()));					
				}
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

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(B.getTaskParent() != null && B.getTaskParent().getName().equals(A.getName()));
	}

	public void testCase11() {
		// initially set B to be a child of C
		B = taskDao.findById(B.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		C = taskDao.findById(C.getId());		
		C.setTaskChildren(children);
		B.setTaskParent(C);
		taskDao.saveOrUpdate(C);

		C = (Task) aggregateService.read(C, getSettings());
		assert(C.getTaskChildren().size() == 1);		
		B = C.getTaskChildren().iterator().next();
		assert(B.getTaskParent() != null);

		// Add B to A, it should be automatically removed by the framework from C
		B.setTaskParent(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				assert(actions.size() == 1);					
			}

			@Override
			public void preBiDirActionStage(Map<PropertyKey, List<Executable>> actions) {
				// check the action queue to see if the correct number of actions are present
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		B = (Task) aggregateService.update(B, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(C.getTaskChildren() == null || C.getTaskChildren().size() == 0);
		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());
		assert(B.getTaskParent() != null && B.getTaskParent().getName().equals(A.getName()));
	}

	public void testCase12() {
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
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());		

		assert(C.getTaskParent() == null);
		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());		
	}

	public void testCase13() {
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
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());		

		assert(C.getTaskParent() == null);
		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getId() == A.getTaskChildren().iterator().next().getId());		
	}

	public void testCase20() {
		Set<Task> children = new HashSet<Task>();
		children.add(B);		

		// Setup the bi-directional link
		A.setTaskChildren(children);
		A.setAlternateTask(C);
		C.setTaskParent(A);
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
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		

		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 2);
		assert(C.getTaskParent() != null);
		assert(B.getTaskParent() != null);		
	}

	public void testCase25() {
		B = taskDao.findById(B.getId());		
		Set<Task> children = new HashSet<Task>();
		children.add(B);	

		// Setup the bi-directional link
		A = taskDao.findById(A.getId());		
		A.setTaskChildren(children);
		B.setTaskParent(A);
		taskDao.saveOrUpdate(A);

		A = (Task) aggregateService.read(A, getSettings());
		B = A.getTaskChildren().iterator().next();
		assert(B.getTaskParent() != null);

		// Keep B as A's child. But set the task parent of B as C
		children = new HashSet<Task>();
		children.add(B);
		C.setTaskChildren(children);
		A.setAlternateTask(C);
		B.setTaskParent(C);

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
				assert(actions.size() == 3);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		});		
		A = (Task) aggregateService.update(A, settings); // Should throw the BidirOutOfSyncException	
		
		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());		

		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());			
	}
	
}
