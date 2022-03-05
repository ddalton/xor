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

public class DefaultUpdate4OneToOne extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "PRIORITIZE_DEFECTS";
	public static final String C_NAME = "ESTIMATE_DEFECTS";
	public static final String D_NAME = "RETROSPECTIVE";	

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	// 2 objects
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

		// Create retrospective task
		if(D == null) {
			D = new Task();
			D.setName(D_NAME);
			D.setDisplayName("Retrospective");
			D.setDescription("Look back on what could have been improved");
			D = (Task) aggregateService.create(D, new Settings());	
			D = taskDao.findById(D.getId());
		}		


		assert(A.getId() != null);			
		assert(B.getId() != null);		
		assert(C.getId() != null);	
		assert(D.getId() != null);			

		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		
	}

	@AfterEach
	public void resetAssociation() {
		// reset the OneToOne association
		A.setAuditTask(null);
		B.setAuditTask(null);
		C.setAuditTask(null);
		D.setAuditTask(null);		
		A.setAuditedTask(null);
		B.setAuditedTask(null);
		C.setAuditedTask(null);		
		D.setAuditedTask(null);				

		A = (Task) aggregateService.update(A, new Settings());	
		B = (Task) aggregateService.update(B, new Settings());
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

		assert(A.getAuditTask() == null);
		assert(B.getAuditTask() == null);	
		assert(C.getAuditTask() == null);
		assert(D.getAuditTask() == null);		
		assert(A.getAuditedTask() == null);
		assert(B.getAuditedTask() == null);	
		assert(C.getAuditedTask() == null);			
		assert(D.getAuditedTask() == null);					
	}		


	public void testCase11() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);
		D.setAuditTask(B);
		B.setAuditedTask(D);		

		aggregateService.update(A, new Settings());
		aggregateService.update(D, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(D_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(A);	
		C.setAuditedTask(null);
		D.setAuditTask(null);
		A.setAlternateTask(C);
		B.setAlternateTask(D);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME) || task.getName().equals(B_NAME))
					assert(actions.size() == 3);
				else 
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

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
		assert(D.getAuditTask() == null && D.getAuditedTask() == null);		
	}	

	public void testCase12() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);
		D.setAuditTask(B);
		B.setAuditedTask(D);		

		aggregateService.update(A, new Settings());
		aggregateService.update(D, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(D_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditedTask(null);
		D.setAuditTask(null);
		A.setAlternateTask(C);
		B.setAlternateTask(D);

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
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		}); 		
		A = (Task) aggregateService.update(A, settings);	

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null && C.getAuditTask() == null);
		assert(D.getAuditTask() == null && D.getAuditedTask() == null);		
	}

	public void testCase13() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);
		D.setAuditTask(B);
		B.setAuditedTask(D);		

		aggregateService.update(A, new Settings());
		aggregateService.update(D, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(D_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(A);	
		C.setAuditedTask(D);
		D.setAuditTask(C);
		A.setAlternateTask(D);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				assert(actions.size() == 3);			
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

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(D_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(C_NAME));		
	}

	public void testCase14() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);
		D.setAuditTask(B);
		B.setAuditedTask(D);		

		aggregateService.update(A, new Settings());
		aggregateService.update(D, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(D_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditedTask(D);
		D.setAuditTask(C);
		A.setAlternateTask(D);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME))
					assert(actions.size() == 2);
				else 
					assert(actions.size() == 3);			
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

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(D_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(C_NAME));		
	}

	public void testCase15() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);
		D.setAuditTask(B);
		B.setAuditedTask(D);		

		aggregateService.update(A, new Settings());
		aggregateService.update(D, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(D_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditedTask(D);
		D.setAuditTask(null);
		A.setAlternateTask(D);
		B.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME) || task.getName().equals(C_NAME))
					assert(actions.size() == 2);
				else 
					assert(actions.size() == 3);			
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

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(D_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(C_NAME));		
	}

	public void testCase16() {
		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);
		D.setAuditTask(B);
		B.setAuditedTask(D);		

		aggregateService.update(A, new Settings());
		aggregateService.update(D, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));
		assert(D.getAuditTask() != null && D.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(D_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(null);	
		C.setAuditedTask(null);
		D.setAuditTask(null);
		A.setAlternateTask(D);
		B.setAlternateTask(C);

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
				assert(actions.size() == 4);

				for(Map.Entry<PropertyKey, List<Executable>> entry : actions.entrySet())
					checkNumber(entry.getKey().getDataObject(), entry.getValue());
			}
		}); 		
		A = (Task) aggregateService.update(A, settings);	

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(C.getAuditedTask() == null);
		assert(D.getAuditTask() == null);		
	}

	public void testCase18() {
		// Setup the bi-directional link
		A.setAuditTask(D);
		D.setAuditedTask(A);
		C.setAuditTask(B);
		B.setAuditedTask(C);		

		aggregateService.update(A, new Settings());
		aggregateService.update(C, new Settings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());
		B = taskDao.findById(B.getId());
		D = taskDao.findById(D.getId());				
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());
		B = (Task) aggregateService.read(B, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(D_NAME));
		assert(D.getAuditedTask() != null && D.getAuditedTask().getName().equals(A_NAME));
		assert(C.getAuditTask() != null && C.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(C_NAME));		

		A.setAuditTask(B);
		B.setAuditedTask(A);	

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(C_NAME) || task.getName().equals(D_NAME))
					assert(actions.size() == 1);
				else 
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

		// Now read all four tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());
		D = taskDao.findById(D.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());
		D = (Task) aggregateService.read(D, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));		
		assert(D.getAuditedTask() == null);
		assert(C.getAuditTask() == null);		
	}
	
}
