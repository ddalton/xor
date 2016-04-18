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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.logic.DefaultQueryOperation;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@TransactionConfiguration(defaultRollback = true)
@Transactional
public class JPAQueryOperationTest extends DefaultQueryOperation {

	@Test
	public void queryPerson() {
		super.queryPerson();
	}
	
	@Test
	public void queryPersonNative() {
		super.queryPersonNative();
	}	
	
	//@Test
	public void queryPersonStoredProcedure() {
		super.queryPersonStoredProcedure();
	}		
	
	@Test
	public void queryPersonType() {
		super.queryPersonType();
	}
	
	@Test
	public void queryNarrowPersonType() {
		super.queryNarrowPersonType();
	}	
	
	@Test
	public void queryTaskChildren() {
		super.queryTaskChildren();
	}
	
	@Test
	public void queryTaskGrandChildren() {
		super.queryTaskGrandChildren();
	}			
	
	@Test
	public void queryTaskGrandChildrenSkip() {
		super.queryTaskGrandChildrenSkip();
	}		
	
	@Test
	public void queryTaskGrandChildSetList() {
		super.queryTaskGrandChildSetList();
	}		
	
	@Test
	public void queryTaskDependencies() {
		super.queryTaskDependencies();
	}	
	
	@Test
	public void querySubProjects() {
		super.querySubProjects();
	}
	
	@Test
	public void querySetListMap() {
		super.querySetListMap();
	}		
	
	@Test
	public void queryTaskEmptyFilter() {
		super.queryTaskEmptyFilter();
	}		
	
	@Test
	public void queryTaskNameFilter() {
		super.queryTaskNameFilter();
	}		
	
	@Test
	public void queryTaskUnionNameFilter() {
		super.queryTaskUnionNameFilter();
	}	
	
	@Test
	public void queryTaskUnionSet1() {
		super.queryTaskUnionSet1();
	}	
	
	@Test
	public void queryTaskUnionSet2() {
		super.queryTaskUnionSet2();
	}
	
	@Test
	public void queryTaskParallel() {
		super.queryTaskParallel();
	}
	
	@Test
	public void queryTaskUnionOverlap() {
		super.queryTaskUnionOverlap();
	}			
	
	@Test
	public void queryEntityProperties() {
		super.queryEntityProperties();
	}	
	
	@Test
	public void listPatents() {
		super.listPatents();
	}	
	
	@Test
	public void listCatalogOfPatents() {
		super.listCatalogOfPatents();
	}		
}
