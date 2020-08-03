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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.annotation.XorEntity;
import tools.xor.db.base.Employee;
import tools.xor.db.base.Manager;
import tools.xor.db.base.MetaEntityState;
import tools.xor.db.base.ParkingSpot;
import tools.xor.db.pm.Project;
import tools.xor.db.pm.Quote;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.util.ClassUtil;


public class DefaultMappedBy {
	@Autowired
	protected AggregateManager aggregateManager;

	/**
	 * Test OneToMany mappedBy
	 */
	public void checkOneToMany() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		ExtendedProperty taskChildren = (ExtendedProperty) taskType.getProperty("taskChildren");
		ExtendedProperty parent = (ExtendedProperty) taskType.getProperty("taskParent");

		assert(taskChildren != null);
		assert(parent != null);

		assert(taskChildren.getMappedBy() != null);
		assert(parent.getMapOf() != null);

		assert(taskChildren.getMapOf() == null);
		assert(parent.getMappedBy() == null);

		assert(taskChildren.getMappedBy() == parent);
		assert(parent.getMapOf() == taskChildren);
	}

	/**
	 * Test OneToOne mappedBy
	 */
	public void checkOneToOne() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		ExtendedProperty quote = (ExtendedProperty) taskType.getProperty("quote");

		Type quoteType = das.getShape().getType(Quote.class);
		ExtendedProperty task = (ExtendedProperty) quoteType.getProperty("task");

		assert(task != null);
		assert(quote != null);

		assert(quote.getMappedBy() != null);
		assert(task.getMapOf() != null);

		assert(quote.getMapOf() == null);
		assert(task.getMappedBy() == null);

		assert(quote.getMappedBy() == task);

		System.out.println("Quote: " + quote.getName());
		System.out.println("Task mapOf: " + task.getMapOf().getName());

		System.out.println("Quoto id: " + System.identityHashCode(quote));
		System.out.println("Task mapOf id: " + System.identityHashCode(task.getMapOf()));
		assert(task.getMapOf() == quote);		
	}

	/**
	 * Test ManyToMany mappedBy
	 */
	public void checkManyToMany() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Project.class);
		ExtendedProperty managers = (ExtendedProperty) ClassUtil.getDelegate(taskType.getProperty("managers"));

		Type quoteType = das.getShape().getType(Manager.class);
		ExtendedProperty projects = (ExtendedProperty) ClassUtil.getDelegate(quoteType.getProperty("projects"));

		assert(managers != null);
		assert(projects != null);

		assert(managers.getMappedBy() != null);
		assert(projects.getMapOf() != null);

		assert(managers.getMapOf() == null);
		assert(projects.getMappedBy() == null);

		assert(ClassUtil.getDelegate(managers.getMappedBy()) == projects);
		assert(ClassUtil.getDelegate(projects.getMapOf()) == managers);			
	}

	/**
	 * Test embedded version of OneToOne
	 */
	public void checkOneToOneEmbedded() {
		DataModel das = aggregateManager.getDataModel(); 

		Type employee = das.getShape().getType(Employee.class);
		ExtendedProperty parkingSpot = (ExtendedProperty) employee.getProperty("location.parkingSpot");

		Type parkingType = das.getShape().getType(ParkingSpot.class);
		ExtendedProperty assignedTo = (ExtendedProperty) parkingType.getProperty("assignedTo");

		assert(parkingSpot != null);
		assert(assignedTo != null);

		assert(assignedTo.getMappedBy() != null);
		assert(parkingSpot.getMapOf() != null);

		assert(assignedTo.getMapOf() == null);
		assert(parkingSpot.getMappedBy() == null);

		assert(assignedTo.getMappedBy() == parkingSpot);
		assert(parkingSpot.getMapOf() == assignedTo);		
	}	

	public void checkImmutable() {
		DataModel das = aggregateManager.getDataModel(); 
		EntityType metaEntityState = (EntityType) das.getShape().getType(MetaEntityState.class);
		Annotation annotation = metaEntityState.getClassAnnotation(XorEntity.class);
		
		boolean value = false;
		if(annotation != null && annotation.annotationType() == XorEntity.class) {
			value = ((XorEntity)annotation).immutable();
		}	

		assert(value == true);
	}
	
	public void checkListIndex() {
		DataModel das = aggregateManager.getDataModel(); 

		Type task = das.getShape().getType(Task.class);
		ExtendedProperty dependants = (ExtendedProperty) task.getProperty("dependants");
		
		assert(dependants.getPositionProperty() != null);
		Property positionProperty = dependants.getPositionProperty();
		assert(positionProperty.getName().equals("depSeq"));
	}
}
