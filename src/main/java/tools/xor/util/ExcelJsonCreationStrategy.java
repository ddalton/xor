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

import java.util.UUID;

import org.json.JSONObject;

import tools.xor.Property;

public class ExcelJsonCreationStrategy extends MutableJsonCreationStrategy {
	public ExcelJsonCreationStrategy(ObjectCreator objectCreator) {
		super(objectCreator);
	}

	@Override
	protected void addEntityMeta(JSONObject result, Object from) {
		if(from != null) {
			result.put(Constants.XOR.TYPE, from.getClass().getName());
		}
		
		// Create a new ID for the objects. Useful when reconstructing the object graph during import
		result.put(Constants.XOR.ID, UUID.randomUUID().toString());
	}
	
	public static String getCollectionTypeKey(Property containmentProperty) {
		return Constants.XOR.TYPE + Constants.XOR.SEP + containmentProperty.getName();
	}
	
	@Override
	protected void addCollectionMeta(JSONObject container, Property containmentProperty, Object from) {
		if(from != null && container != null) {
			container.put(getCollectionTypeKey(containmentProperty), from.getClass().getName());
		}
	}		
}
