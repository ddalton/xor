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

import org.json.JSONObject;
import tools.xor.util.State;

import java.util.List;
import java.util.Map;

/**
 * This type is useful for representing data that does not have properties.
 * @author daltond
 *
 */
public interface BasicType extends Type {
	/**
	 * Assists in finding the actual class for each of the elements in a property path
	 * @return ClassResolver 
	 */
	public ClassResolver getClassResolver();
	
	/**
	 * The type knows how to create a new instance
	 * @param instance prototype whose className is used if necessary to create the new object
	 * @return new object instance
	 */
	public Object newInstance(Object instance);
	
	/**
	 * Generates object(s) based on the property settings and also the general setting.
	 * The generated data is in external form, i.e., JSONObject.
	 *
	 * @param settings controlling the generation
	 * @param property controlling the generation such as Uniqueness etc
	 * @param rootedAt the object where this property is rooted at, i.e., owner
	 * @param entitiesToChooseFrom list of entities already created for this type
	 * @return the generated object
	 */
	public Object generate (Settings settings,
							Property property,
							JSONObject rootedAt,
							List<JSONObject> entitiesToChooseFrom);
}
