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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dilip Dalton
 * 
 */
public class ListType extends SimpleType {

	public ListType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {
		return new ArrayList<Object>();
	}
	
	public Object generate(Settings settings, Property property) {
		List result = new ArrayList();
		
		// TODO: move this to settings
		int fanOut = (int) (Math.random() * 1000);
		EntityType elementType = (EntityType) ((ExtendedProperty)property).getElementType();
		for(int i = 0; i < fanOut; i++) {
			result.add(elementType.generate(settings, property));
		}
		
		return result;
	}		
}
