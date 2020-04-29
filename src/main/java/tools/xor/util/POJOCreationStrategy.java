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

package tools.xor.util;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.Property;


public class POJOCreationStrategy extends AbstractCreationStrategy {
	
	public POJOCreationStrategy(ObjectCreator objectCreator) {
		super(objectCreator);
	}
	
	@Override
	public Object newInstance(Object from, BasicType type, Class<?> toClass) throws Exception {
		return newInstance(from, type, toClass, null, null);
	}

	@Override
	public Object newInstance(Object from, BasicType type, Class<?> toClass, BusinessObject container,
			Property containmentProperty) throws Exception {
		if(type != null) {
			return type.newInstance(from);
		}
		
		return ClassUtil.newInstance(toClass);
	}
}
