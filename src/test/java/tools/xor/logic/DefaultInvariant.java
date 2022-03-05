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

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Manager;
import tools.xor.service.AggregateManager;

public class DefaultInvariant extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;
	
	protected void savePerson() {
		// create person
		Manager person = new Manager();
		person.setName("DILIP_DALTON");
		person.setDisplayName("Dilip Dalton");
		person.setDescription("Software engineer in the bay area");
		person.setUserName("daltond");
		
		Settings settings = new Settings();
		settings.setSupportsPostLogic(true);
		person = (Manager) aggregateService.create(person, settings);	
		
		assert(person.getId() != null);
	}

}
