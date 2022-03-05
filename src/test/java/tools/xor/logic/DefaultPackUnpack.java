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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tools.xor.AbstractDBTest;
import tools.xor.util.Constants;
import tools.xor.util.JSONTransformer;

public class DefaultPackUnpack extends AbstractDBTest {

	protected void checkPackSelfReference() throws JSONException {
		// create person
		JSONObject json = new JSONObject();
		json.put("name", "DILIP_DALTON");
		json.put("displayName", "Dilip Dalton");
		json.put("description", "Software engineer in the bay area");
		json.put("userName", "daltond");
		json.put("self", json);

		JSONTransformer.pack(json);
		assert((json.get(Constants.XOR.IDREF +"self")).toString().equals("10000"));
		System.out.println("JSON string: " + json.toString());	
	}

	protected void checkUnpackSelfReference() throws JSONException {
		final String JSON = "{\"displayName\":\"Dilip Dalton\",\"XOR.id\":10000,\"name\":\"DILIP_DALTON\",\"description\":\"Software engineer in the bay area\",\"|XOR|self\":10000,\"userName\":\"daltond\"}";

		JSONObject jsonObj = new JSONObject(JSON);
		JSONTransformer.unpack(jsonObj);

		JSONObject self = jsonObj.getJSONObject("self");
		assert(self == jsonObj);
	}

	protected void checkPackParentChild() throws JSONException {
		final String TASK_NAME = "SETUP_DSL";
		final String CHILD_TASK_NAME = "TASK_1";

		// Create task
		JSONObject json = new JSONObject();
		json.put("name", TASK_NAME);
		json.put("displayName", "Setup DSL");
		json.put("description", "Setup high-speed broadband internet using DSL technology");

		// Create and add 1 child task
		JSONObject child1 = new JSONObject();
		child1.put("name", CHILD_TASK_NAME);
		child1.put("displayName", "Task 1");
		child1.put("description", "This is the first child task");
		child1.put("parent", json);

		JSONArray jsonArray = new JSONArray();
		jsonArray.put(child1);
		json.put("taskChildren", jsonArray);

		JSONTransformer.pack(json);
		assert((child1.get(Constants.XOR.IDREF +"parent")).toString().equals("10000"));
		System.out.println("JSON string: " + json.toString());	
	}

	protected void checkUnpackParentChild() throws JSONException {
		final String JSON ="{\"displayName\":\"Setup DSL\",\"taskChildren\":[{\"|XOR|parent\":10000,\"displayName\":\"Task 1\",\"XOR.id\":10001,\"name\":\"TASK_1\",\"description\":\"This is the first child task\"}],\"XOR.id\":10000,\"name\":\"SETUP_DSL\",\"description\":\"Setup high-speed broadband internet using DSL technology\"}";

		JSONObject jsonObj = new JSONObject(JSON);
		JSONTransformer.unpack(jsonObj);

		JSONArray jsonArray = jsonObj.getJSONArray("taskChildren");
		JSONObject child1 = jsonArray.getJSONObject(0);
		assert(child1.getJSONObject("parent") == jsonObj);	
	}	
}
