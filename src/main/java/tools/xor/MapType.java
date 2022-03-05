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

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.util.graph.StateGraph;


/**
 * @author Dilip Dalton
 * 
 */
public class MapType extends SimpleType {
	
	private static final String KEY_PREFIX = "KEY";

	public MapType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {
		return new HashMap<Object, Object>();
	}

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		JSONObject result = new JSONObject();

		JSONArray jsonArray = super.generateArray(settings, property, rootedAt, entitiesToChooseFrom, visitor);
		for(int i = 0; i < jsonArray.length(); i++) {
			result.put(KEY_PREFIX+i, jsonArray.get(i));
		}
		
		return result;
	}		
	
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_OBJECT_TYPE;
    }	
}
