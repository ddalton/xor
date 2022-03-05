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

package tools.xor;

import java.util.HashSet;
import java.util.Set;

import tools.xor.service.AggregateManager;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.view.View;

public abstract class AbstractTypeNarrower implements TypeNarrower {

	private AggregateManager aggregateManager;

	public AggregateManager getAggregateManager() {
		return aggregateManager;
	}

	public void setAggregateManager(AggregateManager aggregateManager) {
		this.aggregateManager = aggregateManager;
	}
	
	public static Set<String> getDataTypes(Type type) {
		Set<String> result = new HashSet<>();

		for(Property property: type.getProperties()) {
			if( ((ExtendedProperty)property).isDataType()) {
				result.add(property.getName());
			}
		}
		
		return result;
	}

	/**
	 * This method is used to perform dynamic type narrowing. Subclasses override this method
	 * to provide custom behavior.
	 */
	@Override
	public String downcast(Shape shape, Object entity, View view) {
		Class<?> entityClass = ClassUtil.getUnEnhanced(entity.getClass());

		return entityClass == null ? null : entityClass.getName();
	}
}
