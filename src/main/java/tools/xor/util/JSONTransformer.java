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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A similar class will be provided for JavaScript code, so it can be used by clients that needs 
 * to communicate with this framework.
 * 
 */
public class JSONTransformer {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	/**
	 * Do a BFS traversal
	 *
	 */
	public static class PackVisitor {
		
		private Map<Object, Boolean> marked = new HashMap<Object, Boolean>();
		private Queue<Object> unprocessed = new LinkedList<Object>();
		private int currentID = 10000;
		
		public PackVisitor(JSONObject obj)
		{
			this.unprocessed.add(obj);
		}
		
		/**
		 * We only need to inject Constants.XOR.ID to JSONObject instances
		 */
		public void process() 
		{
			if(unprocessed.isEmpty()) 
			{
				throw new RuntimeException("Need to invoke IDVisitor with a JSONObject instance");
			}
			
			if(((JSONObject)unprocessed.peek()).has(Constants.XOR.ID)) {
				logger.warn("JSONObject instance has already been packed");
				return;
			}
			
			try {
				while(!this.unprocessed.isEmpty()) 
				{
					Object element = unprocessed.poll();
					if(marked.containsKey(element)) {
						continue;
					}				
					
					if(element instanceof JSONObject) {
						JSONObject jsonObject = (JSONObject) element;
						
						// Is this an entity?
						if(jsonObject.length() > 0) {
							jsonObject.put(Constants.XOR.ID, currentID++);
							
							// TODO: If an ExcelVisitor is supplied then
							// visit it. The logic in the visitor will store
							// it by type and id, so it can build the necessary Excel sheets
						}

						for(String key: JSONObject.getNames(jsonObject)) 
						{
							Object obj = jsonObject.get(key);
							if(obj instanceof JSONObject) {
								JSONObject child = (JSONObject) obj;
								if(!child.has(Constants.XOR.ID)) {
									// this is the first time we are seeing it, 
									// so add it to unprocessed
									unprocessed.add(child);
								} else {
									// We have seen this object before and it is being processed,
									// so replace this reference with the id
									int id = child.getInt(Constants.XOR.ID);
									jsonObject.put(Constants.XOR.IDREF +key, id);
									// Now remove the object link as we have recorded it
									jsonObject.remove(key);
								}
							} else if(obj instanceof JSONArray) {
								unprocessed.add(obj);
							}
						}
						marked.put(element, Boolean.TRUE);
					} else if(element instanceof JSONArray) {
						// TODO: If ExcelVisitor is present then
						// visit it, collections are handled in separate sheets in Excel
						// The sheet will be named <targetType>:<propertyName>
						// Here targetType is not the polymorphic type, but the type where propertyName is
						// defined
						
						JSONArray jsonArray = (JSONArray) element;
						for(int i = 0; i < jsonArray.length(); i++) {
							Object obj = jsonArray.get(i);
							if(obj instanceof JSONObject) {
								JSONObject child = (JSONObject) obj;
								if(!child.has(Constants.XOR.ID)) {
									// this is the first time we are seeing it, 
									// so add it to unprocessed
									unprocessed.add(child);
								}
							} else if(obj instanceof JSONArray) {
								unprocessed.add(obj);
							}
						}
						marked.put(element, Boolean.TRUE);
					}
				}
			} catch (JSONException e) {
				ClassUtil.wrapRun(e);
			}

		}
	}
	
	/**
	 * Do a BFS traversal, and do the reverse of pack,
	 * we replace the keys with prefix Constants.XOR.IDREF with the actual object
	 * 
	 * We do 2 passes, first populate the id, object map.
	 * then replace the object ids with the actual object reference
	 *
	 */
	public static class UnPackVisitor {
		
		private Map<Integer, Object> idMap = new HashMap<Integer, Object>(); 
		private JSONObject json;
		
		public UnPackVisitor(JSONObject obj)
		{
			this.json = obj;
		}
		
		private void collectIds() 
		{
			Queue<Object> unprocessed = new LinkedList<Object>();		
			// marked data structure is not really needed, since an object should having only
			// one incoming edge

			unprocessed.add(json);
			if(!((JSONObject)unprocessed.peek()).has(Constants.XOR.ID)) {
				logger.warn("JSONObject instance has not been packed");
				return;
			}

			try {
				while(!unprocessed.isEmpty()) 
				{
					Object element = unprocessed.poll();
					if(element instanceof JSONObject) {
						JSONObject jsonObject = (JSONObject) element;
						if(!jsonObject.has(Constants.XOR.ID))
						{
							throw new RuntimeException("XOR Id not found, object has not be fully packed");
						}
						if(idMap.containsKey(jsonObject.getInt(Constants.XOR.ID))) {
							throw new RuntimeException("Multiple objects found with same id, or object was modified after being packed");
						}
						idMap.put(jsonObject.getInt(Constants.XOR.ID), jsonObject);
						// Remove the XOR id from the object
						jsonObject.remove(Constants.XOR.ID);

						for(String key: JSONObject.getNames(jsonObject)) 
						{
							Object obj = jsonObject.get(key);
							if(obj instanceof JSONObject ||
									obj instanceof JSONArray) {
								unprocessed.add(obj);
							}
						}
					} else if(element instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray) element;
						for(int i = 0; i < jsonArray.length(); i++) {
							Object obj = jsonArray.get(i);
							if(obj instanceof JSONObject ||
									obj instanceof JSONArray) {
								unprocessed.add(obj);
							}
						}
					}
				}
			} catch (JSONException e) {
				ClassUtil.wrapRun(e);
			}		
		}
		
		/**
		 * We only need to inject Constants.XOR.ID to JSONObject instances
		 */
		public void process() 
		{
			collectIds();

			Map<Object, Boolean> marked = new HashMap<Object, Boolean>();
			Queue<Object> unprocessed = new LinkedList<Object>();
			unprocessed.add(json);

			try {
				while(!unprocessed.isEmpty()) 
				{
					Object element = unprocessed.poll();
					if(marked.containsKey(element)) {
						continue;
					}				

					if(element instanceof JSONObject) {
						JSONObject jsonObject = (JSONObject) element;
						for(String key: JSONObject.getNames(jsonObject)) 
						{
							if(key.startsWith(Constants.XOR.IDREF)) {
								// replace the id ref with the actual object
								Object ref = idMap.get(jsonObject.get(key));
								String newKey = key.substring(Constants.XOR.IDREF.length());
								jsonObject.put(newKey, ref);
								jsonObject.remove(key);
							} else {
								Object obj = jsonObject.get(key);
								if(obj instanceof JSONObject ||
										obj instanceof JSONArray) {
									unprocessed.add(obj);
								}
							}
						}
						marked.put(element, Boolean.TRUE);
					} else if(element instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray) element;
						for(int i = 0; i < jsonArray.length(); i++) {
							Object obj = jsonArray.get(i);
							if(obj instanceof JSONObject ||
									obj instanceof JSONArray) {
								unprocessed.add(obj);
							}
						}
						marked.put(element, Boolean.TRUE);
					}
				}
			} catch (JSONException e) {
				ClassUtil.wrapRun(e);
			}
		}
	}	

	/**
	 * Pack a JSONObject instance so that it can be serialized over the network
	 * Do a DFS and add an id value for each object. Create an object and id map as traversing through the graph, 
	 * when accessing a new node, if it has already been visited then we replace it with an id and 
	 * mark the attribute name (e.g., prefix) This object id can use System.identityHashCode or be based
	 * on a sequence
	 * @param input object graph
	 */
	public static void pack(JSONObject input) 
	{
		PackVisitor visitor = new PackVisitor(input);
		visitor.process();
	}

	/**
	 * Retrieve a JSONObject instance and fix all the broken links thus restoring loops in the object graph
	 * 1. Traverse the object graph and populate the map between an object's id and the object instance
	 * 2. Fix all references to the id (using a special attribute prefix e.g., |XOR|) with the help of this map
	 *    during a second scan
	 * @param input object tree
	 */
	public static void unpack(JSONObject input)
	{
		UnPackVisitor visitor = new UnPackVisitor(input);
		visitor.process();
	}
}
