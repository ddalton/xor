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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Settings;


public abstract class AbstractCreationStrategy implements CreationStrategy {
	
	private final ObjectCreator objectCreator;
	
	public AbstractCreationStrategy(ObjectCreator objectCreator) {
		this.objectCreator = objectCreator;
	}
	
	protected ObjectCreator getObjectCreator() {
		return objectCreator;
	}
		
	protected boolean immutable(Class<?> c) {
		if (c == null)
			return false;
		
		return c == String.class || c.isPrimitive() || c.isEnum()
				|| Number.class.isAssignableFrom(c)
				|| Boolean.class == c;
	}
	
	/*
	 * for now let us assume that a class with only private constructors is
	 * immutable. Later on we shall enhance this using annotations e.g., the
	 * immutable property in hibernate mapping
	 */
	protected boolean hasOnlyPrivateConstructors(Class<?> sourceClass) {
		boolean result = true;

		for (Constructor<?> con : sourceClass.getDeclaredConstructors()) {
			final int mod = con.getModifiers();
			if (!Modifier.isPrivate(mod)) {
				result = false;
				break;
			}
		}

		return result;
	}
		
	protected boolean useJavaType(Class<?> fromClass) {
		boolean canInstantiate = false;
		for (Constructor<?> con : fromClass.getDeclaredConstructors()) {
			if (con.getParameterTypes().length == 0) {
				canInstantiate = true;
				break;
			}
		}

		return !isJavaOrAppPackage(fromClass) || !canInstantiate;
	}
	
	/**
	 * Returns true if the given class is under a package that starts with
	 * "java.".
	 * @param c class
	 * @return boolean value
	 */
	protected boolean isJavaOrAppPackage(Class<?> c) {
		if (c == null)
			return false;
		return c.getName().startsWith("java.");
	}
	
	
	@Override
	public Object getNormalizedInstance(BusinessObject bo, Settings settings) {
		return bo.getInstance();
	}	
	
	@Override
	public boolean needsObjectGraph() {
		return false;
	}

	public Object patchInstance(EntityType entityType) {
		return ClassUtil.newInstance(entityType.getInstanceClass());
	}
}
