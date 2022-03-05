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

import java.util.Collection;

/**
 * This interface needs to be implemented by a user created temporary application cache.
 * @author Dilip Dalton
 *
 */
public interface PrefetchCache {

	/**
	 * Get the entity cached by its primary key.  
	 * @param type of entity we need to return  
	 * @param key A primitive object if the key is a single field and a map otherwise
	 * @return a persistence managed entity object
	 */
	public Object getEntity(Type type, Object key);
	
	/**
	 * Fetch the collection from a given owner entity. Since an owner entity might have multiple 
	 * collection properties, we need to also specify the property. 
	 * @param collectionProperty the property of the collection object we are interested in fetching
	 * @param ownerPrimaryKey A primitive object if the key is a single field and a map otherwise
	 * @return a collection of persistence managed objects
	 */
	public Collection getCollection(ExtendedProperty collectionProperty, Object ownerPrimaryKey);
	
	/**
	 * Use this method to specify the type of collection object that needs to be created. The returned
	 * object will be a copy of a prototype collection object.
	 * @param collectionProperty the collection property
	 * @return collection object
	 */
	public Collection getDefaultCollection(ExtendedProperty collectionProperty);
	
}