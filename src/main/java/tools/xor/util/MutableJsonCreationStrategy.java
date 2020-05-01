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

import java.sql.Blob;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.MutableJsonTypeMapper;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.util.graph.ObjectGraph;


public class MutableJsonCreationStrategy extends AbstractCreationStrategy {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	
	
	protected POJOCreationStrategy pojoCS;

	protected static Set<Class<?>> getUnchanged() {
		return MutableJsonTypeMapper.getUnchanged();
	}
	
	public MutableJsonCreationStrategy(ObjectCreator objectCreator) {
		super(objectCreator);
		setDomainCreationStrategy(objectCreator);
	}

	protected void setDomainCreationStrategy(ObjectCreator objectCreator) {
		pojoCS = new POJOCreationStrategy(objectCreator);
	}
	
	/**
	 * Overridden by subclasses
	 * @param entity to which some meta data is added
	 * @param from object instance
	 */
	protected void addEntityMeta(JSONObject entity, Object from) {
		
	}
	
	/**
	 * Overridden by subclasses
	 * @param container parent object
	 * @param containmentProperty parent property 
	 * @param fromCollectionInstance from collection instance
	 */
	protected void addCollectionMeta(JSONObject container, Property containmentProperty, Object fromCollectionInstance) {
		
	}	
	
	@Override
	/**
	 * Handle the creation of the following classes
	 * JsonObject
	 * JsonArray
	 * JsonNumber
	 * JsonString
	 * JsonValue.TRUE
	 * JsonValue.FALSE
	 * JsonValue.NULL
	 */
	public Object newInstance(Object from, BasicType type, Class<?> toClass) throws Exception {
		return this.newInstance(from, type, toClass, null, null);
	}

	@Override
	public Object newInstance(Object from, BasicType type, Class<?> toClass, BusinessObject container,
			Property containmentProperty) throws Exception {
		
		Object result;
		if(getUnchanged().contains(toClass) || (from != null && from instanceof Blob)) {
			result = from;
		} else if(toClass == JSONObject.class || type.getInstanceClass() == JSONObject.class) {
			result = new JSONObject();
			addEntityMeta((JSONObject)result, from);
		} else if(toClass == JSONArray.class || type.getInstanceClass() == JSONArray.class) {
			result = new JSONArray();
			if(container != null && containmentProperty != null) {
				addCollectionMeta((JSONObject) container.getInstance(), containmentProperty, from);
			}
		} else {
			result = pojoCS.newInstance(from, type, toClass);
		}
		if (logger.isDebugEnabled()) {
			logger.debug(
				"JSONCreationStrategy#newInstance from: " + (from == null ? "null" : from.getClass().getName())
					+ ", toClass: " + (toClass == null ? "null" : toClass.getName())
					+ ", result: " + (result == null ? "null" : result.getClass().getName()));
		}
		
		return result;
	}
	
	@Override
	public Object getNormalizedInstance(BusinessObject bo, Settings settings) {
		ObjectGraph og = bo.getObjectCreator().getObjectGraph();
		if(og == null) {
			bo.getObjectCreator().setObjectGraph(bo);
			og = bo.getObjectCreator().getObjectGraph();
		}
		og.spanningTreeWithEdgeSwizzling(bo);

		return super.getNormalizedInstance(bo, settings);
	}	
	
	@Override
	public boolean needsObjectGraph() {
		return true;
	}	
}
