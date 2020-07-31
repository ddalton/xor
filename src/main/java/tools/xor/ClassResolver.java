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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.xor.exception.AmbiguousMatchException;
import tools.xor.exception.PropertyNotFoundException;

public class ClassResolver {
	private BasicType type;
	private Map<String, ClassResolver> uniqueProperties;            // Maps to the ClassResolver of the type that contains this property
	private Map<String, List<ClassResolver>> polymorphicProperties; // Overridden polymorphic properties.
	                                                                // More than one class in the hierarchy implements this property

	public ClassResolver(BasicType type) {
		this.type = type;
	}

	public List<PropertyResolver> resolve(String propertyPath) throws PropertyNotFoundException {
		if(uniqueProperties == null || polymorphicProperties == null)
			initProperties();

		List<PropertyResolver> result = new ArrayList<ClassResolver.PropertyResolver>();
		String rootName = Settings.getRootName(propertyPath);
		String remainingPath = Settings.getNext(propertyPath);

		// found a unique result, add the ClassResolver and go to the next property in the property path
		ClassResolver root = uniqueProperties.get(rootName);
		if(root != null) {
			result.add(new PropertyResolver(rootName, root.getResolverType().getInstanceClass()));
			if(remainingPath != null)
				result.addAll(root.resolve(remainingPath));
			return result;
		} 

		// No unique property found, check if it is present in the polymorphic properties
		List<ClassResolver> polymorphicsRoots = polymorphicProperties.get(rootName); 
		if(polymorphicsRoots != null) {
			
			// Call resolve on each of the ClassResolver
			// If there is more than one result, get the ClassResolver that is the parent of all the other resolvers
			// If no such resolver can be found throw an "ambiguous match" exception, A checked exception
			
			List<ClassResolver> resolved = new ArrayList<ClassResolver>();
			for(ClassResolver resolver: polymorphicsRoots) {
				try {
					resolver.resolve(remainingPath);
					resolved.add(resolver);
				} catch (PropertyNotFoundException e) {
					// Nothing to do here, process the next resolver
				}
			}
			
			if(resolved.size() > 0) {
				if(resolved.size() > 1) {
					try {
						root = findRoot(resolved);
					} catch (AmbiguousMatchException e) {
						throw new PropertyNotFoundException(type.getInstanceClass().getName(), rootName);
					}
				}
					
			}
		}
		
		// If a HQL/JPQL query gets this exception, it can use the solution to add the entity object in the 
		// SELECT clause, this forces the persistence provider to create the entity object of the correct type
		// Make this a configurable option		
		// Check the TREAT option in JPA spec - why would one want to use this?

		throw new PropertyNotFoundException(type.getInstanceClass().getName(), rootName);
	}
	
	
	private ClassResolver findRoot(List<ClassResolver> resolved) throws AmbiguousMatchException {
		ClassResolver result = null;
		
		for(ClassResolver resolver: resolved) {
			if(result == null) {
				result = resolver;
				continue;
			}
			
			// result class is the same or the parent class
			if(result.getResolverType().getInstanceClass().isAssignableFrom(resolver.getResolverType().getInstanceClass()))
				continue;
			
			if(resolver.getResolverType().getInstanceClass().isAssignableFrom(result.getResolverType().getInstanceClass()))
				result = resolver;
			else 
				throw new AmbiguousMatchException(result.getResolverType().getName(), resolver.getResolverType().getName());
		}
		
		return result;
	}
	
	public ClassResolver getParentResolver() {
		ClassResolver result = null;
		
		if(type.getParentTypes().size() == 1) {
			BasicType parent = (BasicType) type.getParentTypes().get(0);
			result = parent.getClassResolver();
		}
		
		return result;
	}

	public void initProperties() {
		uniqueProperties = new HashMap<String, ClassResolver>();
		polymorphicProperties = new HashMap<String, List<ClassResolver>>();

		ClassResolver parent = getParentResolver();
		while(parent != null) {
			// First populate the parent entity, if it is not already populated and then populate the child entity
			if(parent.getUniqueProperties() == null)
				parent.initProperties();
			
			// copy all the unique properties from the parent to this class
			uniqueProperties = new HashMap<String, ClassResolver>(parent.getUniqueProperties());
			
			// copy all the overridden polymorphic properties from the parent to this class
			Map<String, List<ClassResolver>> polyParentProperties = parent.getPolymorphicProperties();
			for(String key: polyParentProperties.keySet()) {
				List<ClassResolver> values = new ArrayList<ClassResolver>();
				values.addAll(polyParentProperties.get(key));
				polymorphicProperties.put(key, values);
			}
			
			// iterate through the properties and add it to the correct map
			for(Property property: type.getProperties()) {
				String name = property.getName();
				ClassResolver resolver = ((BasicType)property.getType()).getClassResolver();
				
				// If this property is not present in either of the map, then add it to the
				// uniqueProperties map
				if(!uniqueProperties.containsKey(name) && !polymorphicProperties.containsKey(name))
					uniqueProperties.put(name, resolver);
				
				if(uniqueProperties.containsKey(name) && !polymorphicProperties.containsKey(name)) {
					// Remove it from the uniqueProperties map and add it to the polymorphicProperties map
					ClassResolver existingResolver = uniqueProperties.get(name);
					List<ClassResolver> values = new ArrayList<ClassResolver>();
					values.add(existingResolver);
					values.add(resolver);
					polymorphicProperties.put(name, values);
				}
				
				if(!uniqueProperties.containsKey(name) && polymorphicProperties.containsKey(name))
					polymorphicProperties.get(name).add(resolver);
				
				if(uniqueProperties.containsKey(name) && polymorphicProperties.containsKey(name))
					throw new IllegalStateException("The property " + name + " cannot be in both unique and polymorphics property maps");
			}
		}
	}

	public Map<String, ClassResolver> getUniqueProperties() {
		return uniqueProperties;
	}

	public void setUniqueProperties(Map<String, ClassResolver> uniqueProperties) {
		this.uniqueProperties = uniqueProperties;
	}

	public Map<String, List<ClassResolver>> getPolymorphicProperties() {
		return polymorphicProperties;
	}

	public void setPolymorphicProperties(
			Map<String, List<ClassResolver>> polymorphicProperties) {
		this.polymorphicProperties = polymorphicProperties;
	}

	public BasicType getResolverType() {
		return type;
	}

	public void setResolverType(BasicType resolverType) {
		this.type = resolverType;
	}

	public static class PropertyResolver {
		private String propertyPath;
		private Class<?> propertyClass;

		public PropertyResolver(String propertyPath, Class<?> propertyClass) {
			this.setPropertyPath(propertyPath);
			this.setResolver(propertyClass);
		}

		public Class<?> getResolver() {
			return propertyClass;
		}

		public void setResolver(Class<?> propertyClass) {
			this.propertyClass = propertyClass;
		}

		public String getPropertyPath() {
			return propertyPath;
		}

		public void setPropertyPath(String propertyPath) {
			this.propertyPath = propertyPath;
		}
	}
}
