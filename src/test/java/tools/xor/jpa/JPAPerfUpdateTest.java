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

package tools.xor.jpa;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.db.pm.Task;
import tools.xor.logic.DefaultPerfUpdate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-perf-test.xml" })
@Transactional
public class JPAPerfUpdateTest extends DefaultPerfUpdate {
	@PersistenceContext
	private EntityManager entityManager;	

	@Test
	public void testNoBaseline() throws InterruptedException {
		super.testNoBaseline();
	}	
	
	@Test
	public void testBaseline() {
		super.testBaseline();
	}	
	
	@Test
	public void createDataORM() {

		Date start = new Date();
		Task ROOT = getRootTask();
		entityManager.persist(ROOT);
		System.out.println("JPAPerfUpdateTest#createDataORM.persist took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");

		start = new Date();
		assert (ROOT.getId() != null);
		ROOT = (Task) entityManager.find(Task.class, ROOT.getId());
		assert (ROOT.getTaskChildren() != null && ROOT.getTaskChildren().size() == NUM_CHILD);
		System.out.println("JPAPerfUpdateTest#createDataORM.find took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
	}	
}
