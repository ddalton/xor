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
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.db.base.Person;
import tools.xor.db.pm.AddressEntity;
import tools.xor.db.pm.FinancialSummary;
import tools.xor.db.pm.Project;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;

public class DefaultModifyOneToOne extends AbstractDBTest {
	
	@Autowired
	protected AggregateManager aggregateManager;
	
	protected void checkBidirMapping() {
		DataModel das = aggregateManager.getDataModel(); 

		EntityType projectType = (EntityType) das.getShape().getType(Project.class);
		ExtendedProperty fs = (ExtendedProperty) projectType.getProperty("financialSummary");
		
		assert(fs != null);
		assert(fs.getMappedBy() != null);
		assert(fs.getAssociationType() == PersistentAttributeType.ONE_TO_ONE);
		
		EntityType fsType = (EntityType) das.getShape().getType(FinancialSummary.class);
		ExtendedProperty project = (ExtendedProperty) fsType.getProperty("project");
		
		assert(project != null);
		assert(project.getMapOf() != null);
		assert(project.getAssociationType() == PersistentAttributeType.ONE_TO_ONE);
	}
	
	protected void checkSingleMapping() {
		DataModel das = aggregateManager.getDataModel(); 

		EntityType personType = (EntityType) das.getShape().getType(Person.class);
		ExtendedProperty address = (ExtendedProperty) personType.getProperty("address");
		
		assert(address == null);
		
		EntityType addressType = (EntityType) das.getShape().getType(AddressEntity.class);
		ExtendedProperty person = (ExtendedProperty) addressType.getProperty("person");
		
		assert(person != null);
		assert(person.getMapOf() == null);
		assert(person.getMappedBy() == null);
		assert(person.getAssociationType() == PersistentAttributeType.ONE_TO_ONE);
		assert(person.getType().getInstanceClass() == Person.class);
	}	
	
	protected void checkBidirModifyInAggregate() {
		
	}

	protected void checkBidirModifyAcrossAggregate() {
		
	}
	
	protected void checkBrokenBidirModifyInAggregate() {
		
	}

	protected void checkBrokenBidirModifyAcrossAggregate() {
		
	}	
	
	protected void checkSingleModifyInAggregate() {
		
	}

	protected void checkSingleModifyAcrossAggregate() {
		
	}	
}
