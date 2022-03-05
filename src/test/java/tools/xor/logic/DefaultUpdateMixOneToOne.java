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

public class DefaultUpdateMixOneToOne extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "PRIORITIZE_DEFECTS";
	public static final String C_NAME = "ESTIMATE_DEFECTS";
	public static final String D_NAME = "RETROSPECTIVE";	

	@Autowired
	protected AggregateManager aggregateService;

	@Autowired
	protected TaskDao taskDao;

	// 2 objects
	private Task a;
	private Task b;
	private Task c;	
	private Task d;	

	/**
	 * Test the update of bi-directional OneToOne relationship involving 3 entities
	 * Corresponds to test case# 3-9, and 17 in OneToOneTestCombinations.docx document
	 *
	 */

	protected Task getA() {
		Task result = new Task();
		result.setName(A_NAME);
		result.setDisplayName("Fix defects");
		result.setDescription("Task to track the defect fixing effort");

		return result;
	}

	protected Task getB() {
		Task result = new Task();
		result.setName(B_NAME);
		result.setDisplayName("Prioritize defects");
		result.setDescription("Based upon the effort required for the defects prioritize them");

		return result;
	}

	protected Task getC() {
		Task result = new Task();
		result.setName(C_NAME);
		result.setDisplayName("Estimate defects");
		result.setDescription("Estimate the effort required to fix the defects");

		return result;
	}

	protected Task getD() {
		Task result = new Task();
		result.setName(D_NAME);
		result.setDisplayName("Retrospective");
		result.setDescription("Look back on what could have been improved");

		return result;
	}		


	@BeforeEach
	public void setupData() {
		// create defect fixing Task
		if(a == null) {
			a = new Task();
			a.setName(A_NAME);
			a.setDisplayName("Fix defects");
			a.setDescription("Task to track the defect fixing effort");
			a = (Task) aggregateService.create(a, getEmptySettings());
			a = taskDao.findById(a.getId());			
		}

		// Create defect priority task
		if(b == null) {
			b = new Task();
			b.setName(B_NAME);
			b.setDisplayName("Prioritize defects");
			b.setDescription("Based upon the effort required for the defects prioritize them");
			b = (Task) aggregateService.create(b, getEmptySettings());
			b = taskDao.findById(b.getId());			
		}

		// Create defect estimation task
		if(c == null) {
			c = new Task();
			c.setName(C_NAME);
			c.setDisplayName("Estimate defects");
			c.setDescription("Estimate the effort required to fix the defects");
			c = (Task) aggregateService.create(c, getEmptySettings());	
			c = taskDao.findById(c.getId());
		}		

		// Create retrospective task
		if(d == null) {
			d = new Task();
			d.setName(D_NAME);
			d.setDisplayName("Retrospective");
			d.setDescription("Look back on what could have been improved");
			d = (Task) aggregateService.create(d, getEmptySettings());	
			d = taskDao.findById(d.getId());
		}		


		assert(a.getId() != null);			
		assert(b.getId() != null);		
		assert(c.getId() != null);	
		assert(d.getId() != null);			

		a = (Task) aggregateService.read(a, getSettings());
		b = (Task) aggregateService.read(b, getSettings());
		c = (Task) aggregateService.read(c, getSettings());
		d = (Task) aggregateService.read(d, getSettings());		
	}

	@AfterEach
	public void resetAssociation() {
		// reset the OneToOne association
		a.setAuditTask(null);
		b.setAuditTask(null);
		c.setAuditTask(null);
		d.setAuditTask(null);		
		a.setAuditedTask(null);
		b.setAuditedTask(null);
		c.setAuditedTask(null);		
		d.setAuditedTask(null);				

		a = (Task) aggregateService.update(a, getSettings());	
		b = (Task) aggregateService.update(b, getSettings());
		c = (Task) aggregateService.update(c, getSettings());
		d = (Task) aggregateService.update(d, getSettings());		

		a = taskDao.findById(a.getId());
		b = taskDao.findById(b.getId());
		c = taskDao.findById(c.getId());
		d = taskDao.findById(d.getId());		

		a = (Task) aggregateService.read(a, getSettings());
		b = (Task) aggregateService.read(b, getSettings());
		c = (Task) aggregateService.read(c, getSettings());
		d = (Task) aggregateService.read(d, getSettings());		

		assert(a.getAuditTask() == null);
		assert(b.getAuditTask() == null);	
		assert(c.getAuditTask() == null);
		assert(d.getAuditTask() == null);		
		assert(a.getAuditedTask() == null);
		assert(b.getAuditedTask() == null);	
		assert(c.getAuditedTask() == null);			
		assert(d.getAuditedTask() == null);					
	}		

	public void testCase19() {
		Task A = getA(); // A is transient instance
		Task B = b;

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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings); // Should persist A

		assert(A.getId() != null);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}

	public void testCase20() {
		Task A = getA(); // A is a transient instance
		Task B = getB(); // B is a transient instance

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
		A = (Task) aggregateService.update(A, settings);
		B = A.getAuditTask();

		assert(A.getId() != null);
		assert(B.getId() != null);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}	

	public void testCase21() {
		Task A = a; 
		Task B = getB(); // B is a transient instance

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
		settings.setPostFlush(true); // We need to flush since an auto flush is not during an update
		A = (Task) aggregateService.update(A, settings);
		B = A.getAuditTask();

		assert(A.getId() != null);
		assert(B.getId() != null);

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}	

	public void testCase22() {
		Task A = a;
		Task B = getB(); // B is transient
		Task C = c;

		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		// DON'T read, as that will overwrite with data from DB
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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings);	
		B = A.getAuditTask();

		assert(B.getId() != null);

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

	public void testCase23() {
		Task A = a;
		Task B = getB(); // B is transient
		Task C = c;

		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getEmptySettings());

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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings);	
		B = A.getAuditTask();

		assert(B.getId() != null);

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

	public void testCase24() {
		Task A = getA();
		Task B = b;
		Task C = c;

		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, getEmptySettings());

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

	public void testCase25() {
		Task A = getA();
		Task B = b;
		Task C = c;

		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, getEmptySettings());

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

	public void testCase26() {
		Task A = getA();
		Task B = b;
		Task C = c;

		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, getEmptySettings());

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

	public void testCase27() {
		Task A = getA();
		Task B = b;
		Task C = c;

		// Setup the bi-directional link
		C.setAuditTask(B);
		B.setAuditedTask(C);

		aggregateService.update(C, getEmptySettings());

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

	public void testCase28() {
		Task A = a;
		Task B = getB();
		Task C = c;

		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getEmptySettings());

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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings);
		B = A.getAuditTask();
		assert(B.getId() != null);

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

	public void testCase29() {
		Task A = a;
		Task B = getB();
		Task C = c;

		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getEmptySettings());

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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings);
		B = A.getAuditTask();
		assert(B.getId() != null);

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

	public void testCase30() {
		Task A = a;
		Task B = getB();
		Task C = c;

		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getEmptySettings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(A);
		B.setAuditTask(C);
		C.setAuditedTask(B);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(B_NAME)) // twice, one for audited and the other for audit
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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings);
		B = A.getAuditTask();
		assert(B.getId() != null);

		// Now read all three tasks
		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());		
		C = (Task) aggregateService.read(C, getSettings());		

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(B_NAME));
		assert(B.getAuditedTask() != null && B.getAuditedTask().getName().equals(A_NAME));
		assert(B.getAuditTask() != null && B.getAuditTask().getName().equals(C_NAME));		
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(B_NAME));		
	}

	public void testCase31() {
		Task A = a;
		Task B = getB(); // B is transient
		Task C = c;
		Task D = getD(); // D is transient

		// Setup the bi-directional link
		A.setAuditTask(C);
		C.setAuditedTask(A);

		//A = (Task) aggregateService.read(A, getSettings());
		aggregateService.update(A, getEmptySettings());

		A = taskDao.findById(A.getId());
		C = taskDao.findById(C.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		C = (Task) aggregateService.read(C, getSettings());

		assert(A.getAuditTask() != null && A.getAuditTask().getName().equals(C_NAME));
		assert(C.getAuditedTask() != null && C.getAuditedTask().getName().equals(A_NAME));

		A.setAuditTask(B);
		B.setAuditedTask(A);
		D.setAuditTask(C);
		C.setAuditedTask(D);
		A.setAlternateTask(C);

		Settings settings = getSettings();
		settings.setInterceptor(new Interceptor() {
			// check the number of actions in each object
			private void checkNumber(BusinessObject dataObject, List<Executable> actions) {
				Task task = (Task) dataObject.getInstance();
				if(task.getName().equals(A_NAME) || task.getName().equals(C_NAME))
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
		settings.setPostFlush(true);
		A = (Task) aggregateService.update(A, settings);	
		B = A.getAuditTask();
		C = A.getAlternateTask();
		D = C.getAuditedTask();

		assert(B.getId() != null);
		assert(D.getId() != null);

		// Now read all three tasks
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
	
	public void testCase32() {
		Task A = getA();
		Task B = getB(); // B is transient
		Task C = getC();
		
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
		B = A.getAuditTask();
		C = A.getAlternateTask();

		A = taskDao.findById(A.getId());
		B = taskDao.findById(B.getId());		
		A = (Task) aggregateService.read(A, getSettings());
		B = (Task) aggregateService.read(B, getSettings());

		assert(A.getAuditTask() != null);
		assert(B.getAuditedTask() != null);
	}
	
}
