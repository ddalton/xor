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

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;


public class DefaultIdVersion {
	@Autowired
	protected AggregateManager aggregateManager;
	
	public void checkId() {
		DataModel das = aggregateManager.getModel(); 

		Type taskType = das.getShape().getType(Task.class);
		Property id = taskType.getProperty("id");
		
		assert(((EntityType)taskType).getIdentifierProperty() != null);
		assert(((EntityType)taskType).getIdentifierProperty() == id);
		

	}
	
	public void checkVersion() {
		DataModel das = aggregateManager.getModel(); 

		Type taskType = das.getShape().getType(Task.class);
		Property version = taskType.getProperty("version");
		
		assert(((EntityType)taskType).getVersionProperty() != null);
		assert(((EntityType)taskType).getVersionProperty() == version);	
	}
	
}
