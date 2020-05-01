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

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.ExtendedProperty;
import tools.xor.Type;
import tools.xor.db.base.Employee;
import tools.xor.db.pm.Project;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;


public class DefaultAssociationType extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;
	
	/**
	 * Test OneToMany mappedBy
	 */
	public void checkOneToMany() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		ExtendedProperty taskChildren = (ExtendedProperty) taskType.getProperty("taskChildren");
		
		assert(taskChildren != null);
		assert(taskChildren.getAssociationType() == PersistentAttributeType.ONE_TO_MANY);
	}
	
	/**
	 * Test OneToOne mappedBy
	 */
	public void checkOneToOne() {
		DataModel das = aggregateManager.getDataModel(); 
		
		Type taskType = das.getShape().getType(Task.class);
		ExtendedProperty quote = (ExtendedProperty) taskType.getProperty("quote");
		
		assert(quote != null);
		assert(quote.getAssociationType() == PersistentAttributeType.ONE_TO_ONE);		
	}
	
	/**
	 * Test ManyToMany mappedBy
	 */
	public void checkManyToMany() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Project.class);
		ExtendedProperty managers = (ExtendedProperty) taskType.getProperty("managers");
		
		assert(managers != null);
		assert(managers.getAssociationType() == PersistentAttributeType.MANY_TO_MANY);	
	}
	

	public void checkManyToOne() {
		DataModel das = aggregateManager.getDataModel(); 

		Type employee = das.getShape().getType(Employee.class);
		ExtendedProperty createdBy = (ExtendedProperty) employee.getProperty("createdBy");
		
		assert(createdBy != null);
		assert(createdBy.getAssociationType() == PersistentAttributeType.MANY_TO_ONE);	
	}	
}
