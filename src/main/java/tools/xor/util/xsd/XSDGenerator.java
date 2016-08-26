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

package tools.xor.util.xsd;

import tools.xor.Type;


public class XSDGenerator {

	/**
	 * The XSDVisitor will keep track of all additional types that need to be part of the
	 * XSD document 
	 * 
	 * @param coordinator is a visitor object that gathers information 
	 * @param type of entity to generate
	 * @return result
	 */
	public String generate(XSDVisitor coordinator, Type type) {

		/* Iterate through the properties
		 * If the property is a primitive type, then call the primitive decorator
		 * If the property is a collection type, then call the collection decorator along with the appropriate type
		 * If the property is an entity
		 *   - If part-of parent entity then call the entity decorator
		 *   - If not, call the IDREF decorator
		 */

		if(type == null) {
			return null;
		} else if(type.isDataType()) {
			throw new RuntimeException("XSD cannot be generated for a non-entity: " + type.getName());
		} else {
			(new XSDEntityGenerator(type)).generate(coordinator);
		}

		return coordinator.build();

	}


}
