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

import tools.xor.logic.DefaultUpdate3OneToOne;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@Transactional
public class JPAUpdate3OneToOneTest extends DefaultUpdate3OneToOne {

	@Test
	public void testCase1() {
		super.testCase1();
	}
	
	@Test
	public void testCase2() {
		super.testCase2();
	}	
	
	@Test
	public void testCase3() {
		super.testCase3();
	}	
	
	@Test
	public void testCase4() {
		super.testCase4();
	}		
	
	@Test
	public void testCase5() {
		super.testCase5();
	}		
	
	@Test
	public void testCase6() {
		super.testCase6();
	}	
	
	@Test
	public void testCase7() {
		super.testCase7();
	}		
	
	@Test
	public void testCase8() {
		super.testCase8();
	}		

	@Test
	public void testCase9() {
		super.testCase9();
	}	
	
	@Test
	public void testCase10() {
		super.testCase10();
	}
	
	@Test
	public void testCase17() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            super.testCase17();
        });	    
	}		
}
