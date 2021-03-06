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

import tools.xor.logic.DefaultAggregatePaths;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@Transactional
public class JPAAggregatePathsTest extends DefaultAggregatePaths {

	@Test
	public void checkDirPath() {
		super.checkDirPath();
	}

	@Test
	public void checkLocale() {
		super.checkLocale();
	}

	@Test
	public void checkPaths() {
		super.checkPaths();		
	}

	@Test
	public void checkCyclicPaths() {
		super.checkCyclicPaths();		
	}		
	
	@Test
	public void checkOptionModel() {
		super.checkOptionModel();		
	}

	@Test
	public void metaViewList() {
		super.metaViewList();
	}

	@Test
	public void generateStateGraph() {
		super.generateStateGraph();
	}

	@Test
	public void generateStateInheritanceGraph() {
		super.generateStateInheritanceGraph();
	}

	@Test
	public void generateStatePersonGraph() {
		super.generateStatePersonGraph();
	}

	@Test
	public void checkPersonSubTypes() {
		super.checkPersonSubTypes();
	}

	@Test
	public void checkJSON() {
		super.checkJSON();
	}
	
	@Test
	public void checkStateGraph() {
	    super.checkStateGraph();
	}
}
