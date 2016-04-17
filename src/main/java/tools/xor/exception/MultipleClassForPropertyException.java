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

package tools.xor.exception;

import java.util.HashSet;
import java.util.Set;

import tools.xor.Type;
import tools.xor.util.I18NUtils;

public class MultipleClassForPropertyException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private Type entityType;
	private String propertyName;
	private Set<Class<?>> matchedClasses;

	public Set<Class<?>> getMatchedClasses() {
		return matchedClasses;
	}

	public void setMatchedClasses(Set<Class<?>> matchedClasses) {
		this.matchedClasses = matchedClasses;
	}

	public MultipleClassForPropertyException(Type entityType, String propertyName, Object matchedClasses) {
		// Multiple properties in the subclass for the entityType matched the property
		// This happened because of the static (default) type narrowing strategy.
		// A custom type narrower will have to be provided to solve this.
		
		this.entityType = entityType;
		this.propertyName = propertyName;
		
		if(Set.class.isAssignableFrom(matchedClasses.getClass())) {
			this.matchedClasses = new HashSet<Class<?>>();
			
			Set<Class<?>> classes = (Set<Class<?>>) matchedClasses;
			for(Class<?> clazz: classes)
				this.matchedClasses.add(clazz);
		}
	}

	@Override
	public String getMessage(){
		String[] params = new String[3];
		params[0] = entityType.getInstanceClass().getName();
		params[1] = propertyName;
		
		StringBuilder multipleClasses = new StringBuilder();
		for(Class<?> clazz: matchedClasses) {
			if(multipleClasses.length() > 0)
				multipleClasses.append(", ");
			multipleClasses.append(clazz.getName());
		}
			
		params[2] = multipleClasses.toString();
		
		return  I18NUtils.getResource(  "exception.multipleClassForProperty",I18NUtils.CORE_RESOURCES, params);			
		
	}
	
}
