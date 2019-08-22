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

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Settings;


public interface CreationStrategy {

	/**
	 * Creates a new instance with the provided information
	 * @param from    A copy of this object is returned if it is mutable, else the from object is returned
	 * @param type    The class information can be extracted from the type that is used to create the instance
	 * @param toClass If the type is not provided then this field is required
	 * @return new object
	 * @throws Exception when trying to create a new java object
	 */
	public Object newInstance(Object from, BasicType type, Class<?> toClass) throws Exception;
	
	/**
	 * This method additionally provides the container and containmentProperty so the collection type can be saved on the container.
	 * This information is useful to support export/import.
	 * 
	 * @param from  source object
	 * @param type  type of the entity
	 * @param toClass target object class
	 * @param container parent object
	 * @param containmentProperty parent property referring to the new instance
	 * @return new instance
	 * @throws Exception when trying to create a new java object 
	 */
	public Object newInstance(Object from, BasicType type, Class<?> toClass, BusinessObject container, Property containmentProperty) throws Exception;

	/**
	 * Creates and attaches an object for patching. The id and version attributes need to be populated.
	 *
	 * @param entityType entity type
	 * @return domain object ready for patching
	 */
	public Object patchInstance(EntityType entityType);
	
	/**
	 * Give a chance for the creation strategy to do any final conversion on the root object.
	 * This can be expensive so invoke only if necessary.
	 *
	 * @param bo BusinessObject
	 * @param settings user specfied settings
	 * @return normalized instance
	 */
	public Object getNormalizedInstance(BusinessObject bo, Settings settings);
	
	/**
	 * Flag that indicates if the object graph should be tracked during the call graph traversal
	 * Useful to track references to open property objects as they are not accessible otherwise
	 * @return boolean value
	 */
	public boolean needsObjectGraph();
}
