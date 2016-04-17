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

import tools.xor.service.DataAccessService;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dilip Dalton
 * 
 */
public class SimpleTypeFactory {

   public static SimpleType getType(Class<?> clazz, DataAccessService das) {
		if (clazz.isArray()) {
			return new ArrayType(clazz);
		} else if(Date.class.isAssignableFrom(clazz)) {
			return new DateType(clazz);
		} else if(Set.class.isAssignableFrom(clazz)) {
			return new SetType(clazz);
		} else if(List.class.isAssignableFrom(clazz)) {
			return new ListType(clazz);
		} else if(Map.class.isAssignableFrom(clazz)) {
			return new MapType(clazz);
		} else if(Collection.class.isAssignableFrom(clazz)) {
			return new CollectionType(clazz);
		} else {
			return new SimpleType(clazz, das);
		}
   }

}
