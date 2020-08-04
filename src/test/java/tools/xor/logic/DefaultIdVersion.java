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
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.generator.StringTemplate;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;


public class DefaultIdVersion {
	@Autowired
	protected AggregateManager aggregateManager;
	
	public void checkId() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		Property id = (Property) ClassUtil.getDelegate(taskType.getProperty("id"));
		
		assert(((EntityType)taskType).getIdentifierProperty() != null);
		assert(ClassUtil.getDelegate(((EntityType)taskType).getIdentifierProperty()) == id);
		

	}
	
	public void checkVersion() {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		Property version = (Property) ClassUtil.getDelegate(taskType.getProperty("version"));
		
		assert(((EntityType)taskType).getVersionProperty() != null);
		assert(ClassUtil.getDelegate(((EntityType)taskType).getVersionProperty()) == version);	
	}
	
	public void checkTypeInheritanceValue() {
        DataModel das = aggregateManager.getDataModel(); 
        
        Shape shape = das.createShape("ValueShape", null, Shape.Inheritance.VALUE);
        Type taskType = shape.getType(Task.class);
        Type personType = shape.getType(Person.class);
        
        Property taskId = (Property) taskType.getProperty("id");
        Property personId = (Property) personType.getProperty("id");
        
        // At this point it should refer to the same property instance
        assert(ClassUtil.getDelegate(taskId) == ClassUtil.getDelegate(personId));
        
        // let us now set a generator on the person property
        personId.setGenerator(new StringTemplate("UNQ_[VISITOR_CONTEXT]"));

        // again retrieve the person property. It should retrieve the new property
        // due to write on semantics
        Property personIdNew = (Property) ClassUtil.getDelegate(personType.getProperty("id"));
        
        assert(ClassUtil.getDelegate(taskId) != ClassUtil.getDelegate(personId));
        assert(ClassUtil.getDelegate(personId) == ClassUtil.getDelegate(personIdNew));
	}
	
}
