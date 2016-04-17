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

package tools.xor.hibernate;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.db.pm.Task;
import tools.xor.logic.DefaultPerfUpdate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-hibernate-perf-test.xml" })
@TransactionConfiguration(defaultRollback = true)
@Transactional
public class HibernatePerfUpdateTest extends DefaultPerfUpdate {
	
	@Autowired
	private SessionFactory sessionFactory;
	
	protected Session getSession() {
		if (this.sessionFactory.getCurrentSession() == null)
			throw new IllegalStateException("Session has not been set on DAO before usage");
		return this.sessionFactory.getCurrentSession();
	}    	
	
	@Test
	public void testNoBaseline() throws InterruptedException {
		//System.out.println("Sleeping thread for 30 secs");
		//Thread.sleep(20000);
		//System.out.println("Thread woken");		
		
		super.testNoBaseline();
		
		//System.out.println("Sleeping thread for 30 secs");
		//Thread.sleep(20000);
		//System.out.println("Thread woken");			
	}	
	
	//@Test
	public void testBaseline() {
		super.testBaseline();
	}		
	
	//@Test
	public void createDataORM() {
		
		Date start = new Date();
		Task ROOT = getRootTask();
		Serializable id = getSession().save(ROOT);
		System.out.println("HibernatePerfUpdateTest#createDataORM.save took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		
		start = new Date();
		assert (id != null);
		ROOT = (Task) getSession().get(Task.class, id);
		assert (ROOT.getTaskChildren() != null && ROOT.getTaskChildren().size() == NUM_CHILD);
		System.out.println("HibernatePerfUpdateTest#createDataORM.get took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
	}	
}
