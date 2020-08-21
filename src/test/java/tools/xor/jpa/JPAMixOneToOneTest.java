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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.logic.DefaultUpdateMixOneToOne;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@Transactional
public class JPAMixOneToOneTest extends DefaultUpdateMixOneToOne {
	
	@Test
	public void testCase19() {
		super.testCase19();
	}
	
	@Test
	public void testCase20() {
		super.testCase20();
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
	
	@Test
	public void testCase25() {
		super.testCase25();
	}		
	
	@Test
	public void testCase26() {
		super.testCase26();
	}
	
	@Test
	public void testCase27() {
		super.testCase27();
	}	
	
	@Test
	public void testCase28() {
		super.testCase28();
	}	
	
	@Test
	public void testCase29() {
		super.testCase29();
	}		
	
	@Test
	public void testCase30() {
		super.testCase30();
	}
	
	@Test
	public void testCase31() {
		super.testCase31();
	}	
	
	@Test
	public void testCase32() {
	    Assertions.assertThrows(IllegalArgumentException.class, () -> {
	        super.testCase32();
	    });
	}		
}
