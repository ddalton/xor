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

import java.lang.reflect.Array;
import java.util.List;

import org.json.JSONObject;

import tools.xor.util.graph.StateGraph;

/**
 * @author Dilip Dalton
 * 
 */
public class ArrayType extends SimpleType {

	public ArrayType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {
		Class<?> fromComponentType = getInstanceClass().getComponentType();

		int len = instance == null ? 0 : Array.getLength(instance);
		Object toArray = Array.newInstance(fromComponentType, len);

		return toArray;
	}

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		return super.generateArray(settings, property, rootedAt, entitiesToChooseFrom, visitor);
	}	
	
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_ARRAY_TYPE;
    }	
}
