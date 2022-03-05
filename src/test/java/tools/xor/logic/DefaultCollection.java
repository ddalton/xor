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

import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.db.pm.Project;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;


public class DefaultCollection {
	@Autowired
	protected AggregateManager aggregateManager;
	
	public void checkSet() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		Property children = taskType.getProperty("taskChildren");
		
		assert(((ExtendedProperty)children).isSet() == true);
		assert(((ExtendedProperty)children).isList() == false);
		assert(((ExtendedProperty)children).isMap() == false);
	}

	public void checkList() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		Property dependencies = taskType.getProperty("dependants");
		
		assert(((ExtendedProperty)dependencies).isSet() == false);
		assert(((ExtendedProperty)dependencies).isList() == true);
		assert(((ExtendedProperty)dependencies).isMap() == false);
	}	
	
	public void checkMap() {
		DataModel das = aggregateManager.getDataModel(); 

		Type prjType = das.getShape().getType(Project.class);
		Property subProjects = prjType.getProperty("subProjects");
		
		assert(((ExtendedProperty)subProjects).isSet() == false);
		assert(((ExtendedProperty)subProjects).isList() == false);
		assert(((ExtendedProperty)subProjects).isMap() == true);
	}		
}
