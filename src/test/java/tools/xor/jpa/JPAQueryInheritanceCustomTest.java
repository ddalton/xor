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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.logic.DefaultQueryInheritanceCustom;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-VO-custom-jpa-test.xml" })
@Transactional
public class JPAQueryInheritanceCustomTest extends DefaultQueryInheritanceCustom {

	@Test
	public void queryPatent() {
		super.queryPatent();
	}

	@Test
	public void listPatents() {
		super.listPatents();
	}	
	
	@Test
	public void listPatentsByName() {
		super.listPatentsByName();
	}
	
	@Test
	public void listPatentsByState() {
		super.listPatentsByState();
	}	
	
	@Test
	public void listPatentsBeforeDate() {
		super.listPatentsBeforeDate();
	}	
	
	@Test
	public void listPatentsBetweenDate() {
		super.listPatentsBetweenDate();
	}
	
	@Test
	public void limitPatents() {
		super.limitPatents();
	}		
}
