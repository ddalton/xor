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

import org.json.JSONArray;
import tools.xor.util.ClassUtil;


/**
 * @author Dilip Dalton
 * 
 */
public class CollectionType extends SimpleType {

	public CollectionType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {
		return ClassUtil.newInstance(instance.getClass());
	}
	
	public Object generate(Settings settings, Property property) {
		return super.generateArray(settings, property);
	}	
}
