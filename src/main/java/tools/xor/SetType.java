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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;

/**
 * @author Dilip Dalton
 * 
 */
public class SetType extends SimpleType {

	public SetType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {
		if(instance != null ) {
			if(instance instanceof Set) {
				int expected = ((Set) instance).size();
				return new ObjectOpenHashSet<Object>(expected, 1);
			} else if(instance instanceof JsonArray) {
				int expected = ((JsonArray) instance).size();
				return new ObjectOpenHashSet<Object>(expected, 1);
			} else 
				return new ObjectOpenHashSet<Object>();
		} else
			return new ObjectOpenHashSet<Object>();
	}
	
	public Object generate(Settings settings, Property property) {
		Set result = new HashSet();
		
		// TODO: move this to settings
		int fanOut = (int) (Math.random() * 1000);
		EntityType elementType = (EntityType) ((ExtendedProperty)property).getElementType();
		for(int i = 0; i < fanOut; i++) {
			result.add(elementType.generate(settings, property));
		}
		
		return result;
	}	
}
