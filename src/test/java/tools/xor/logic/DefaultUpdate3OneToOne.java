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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.BusinessObject;
import tools.xor.Settings;
import tools.xor.action.Executable;
import tools.xor.action.PropertyKey;
import tools.xor.core.Interceptor;
import tools.xor.db.dao.TaskDao;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;

public class DefaultUpdate3OneToOne extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "PRIORITIZE_DEFECTS";
	public static final String C_NAME = "ESTIMATE_DEFECTS";

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	// 2 objects
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
			B.setDisplayName("Prioritize defects");
			B.setDescription("Based upon the effort required for the defects prioritize them");
			B = (Task) aggregateService.create(B, new Settings());
			B = taskDao.findById(B.getId());			
		}

		// Create defect estimation task
		if(C == null) {
			C = new Task();
			C.setName(C_NAME);
			C.setDisplayName("Estimate defects");
			C.setDescription("Estimate the effort required to fix the defects");
			C = (Task) aggregateService.create(C, new Settings());	
			C = taskDao.findById(C.getId());
		}		

		assert(A.getId() != null);			
		assert(B.getId() != null);		
		assert(C.getId() != null);	

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
	}

	@AfterEach
	public void resetAssociation() {
		// reset the OneToOne association
		A.setAuditTask(null);
		B.setAuditTask(null);
		C.setAuditTask(null);
		A.setAuditedTask(null);
		B.setAuditedTask(null);
		C.setAuditedTask(null);		

		A = (Task) aggregateService.update(A, getSettings());	
		B = (Task) aggregateService.update(B, getSettings());
		C = (Task) aggregateService.update(C, getSettings());

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() == null);
		assert(B.getAuditTask() == null);	
		assert(C.getAuditTask() == null);
		assert(A.getAuditedTask() == null);
		assert(B.getAuditedTask() == null);	
		assert(C.getAuditedTask() == null);			
	}		

	public void testCase1() {
		// Setup the bi-directional link
		A.setAuditTask(B);
		B.setAuditedTask(A);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
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

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}

	public void testCase2() {
		// Setup the bi-directional link
		A.setAuditTask(B);
		B.setAuditedTask(null);		

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
		aggregateService.update(A, settings);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}	

	public void testCase3() {
		// Setup the bi-directional link
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		A.setAuditTask(C);
		C.setAuditedTask(A);
		
		aggregateService.update(A, getSettings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(A);	
		C.setAuditedTask(null);
		A.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME))
					assert(actions.size() == 3);
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
	}	

	public void testCase4() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getSettings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditedTask(null);
		A.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME))
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
	}	


	public void testCase5() {
		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, new Settings());

		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());		
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(C.getAuditTask() != null && C.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(C_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(A);	
		C.setAuditTask(null);
		A.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME))
					assert(actions.size() == 3);
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
	}		

	public void testCase6() {
		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, new Settings());

		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());		
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(C.getAuditTask() != null && C.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(C_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditTask(null);
		A.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME))
					assert(actions.size() == 3);
				else if(task.getName().equals(C_NAME))
					assert(actions.size() == 2);
				else
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
	}		

	public void testCase7() {
		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, new Settings());

		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());		
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(C.getAuditTask() != null && C.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(C_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(A);	
		C.setAuditTask(null);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(C_NAME))
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
	}		

	public void testCase8() {
		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, new Settings());

		B = taskDao.findById(B.getId());
		C = taskDao.findById(C.getId());		
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(C.getAuditTask() != null && C.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(C_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditTask(null);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME))
					assert(actions.size() == 2);
				else
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
	}			

	public void testCase9() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getSettings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(A);	

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(C_NAME))
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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);		
	}

	public void testCase17() {
		// Setup the bi-directional link
		A.setAuditTask(B);
		B.setAuditedTask(A);
		C.setAuditedTask(A);
		A.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(C_NAME))
					assert(actions.size() == 1);
				else if(task.getName().equals(B_NAME))
					assert(actions.size() == 2);				
				else 
					assert(actions.size() == 3);			
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

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}


	public void testCase10() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getSettings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(null);	

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

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);		
	}	
}
