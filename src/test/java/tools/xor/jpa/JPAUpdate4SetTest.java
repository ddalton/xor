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

import tools.xor.logic.DefaultUpdate4Set;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@Transactional
public class JPAUpdate4SetTest extends DefaultUpdate4Set {
	@Test
	public void testCase14() {
		super.testCase14();
	}

	@Test
	public void testCase15() {
		super.testCase15();
	}	
	
	@Test
	public void testCase16() {
		super.testCase16();
	}	
	
	@Test
	public void testCase17() {
		super.testCase17();
	}	
	
	@Test
	public void testCase18() {
		super.testCase18();
	}	
	
	@Test
	public void testCase19() {
		super.testCase19();
	}
	
	@Test
	public void testCase21() {
		super.testCase21();
	}
	
	@Test
	public void testCase22() {
		super.testCase22();
	}
	
	@Test
	public void testCase23() {
		super.testCase23();
	}						
	
	@Test
	public void testCase24() {
		super.testCase24();
	}		
}
