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

import tools.xor.util.CreationStrategy;
import tools.xor.util.ObjectCreator;

/**
 * The framework can be used with both single and dual model hierarchies.
 * In the single model hierarchy, both the JPA and JAXB annotations are defined on the same class
 * But in the dual model hierarchies, the JPA annotations are in the domain model hierarchy
 * and the JAXB annotations are in the external model hierarchy
 * 
 * @author Dilip Dalton
 *
 */
public interface TypeMapper {

	/**
	 * We need to find the domain form of the class
	 * 
	 * @param externalClass the class whose corresponding domain form needs to be returned
	 * @return the domain class corresponding to externalClass
	 */
	public Class<?> toDomain(Class<?> externalClass);
	
	/**
	 * This is a more flexible form of getting the 
	 * external form of the class and allows a particalar
	 * type implementation to store more information on the external type
	 * 
	 * @param  type
	 * @return
	 */
	public Class<?> toDomain(Type type);

	/**
	 * Return the external form of the class
	 * 
	 * @param domainClass the class whose corresponding external class needs to be found
	 * @return the external class of domainClass
	 */
	public Class<?> toExternal(Class<?> domainClass);

	/**
	 * Describes what form corresponds to the source and target types
	 * The source class and target class resolution is dependent upon this.
	 * 
	 * @return
	 * @see TypeMapper#getSourceClass(Class)
	 */
	public MapperDirection getDirection();
	
	/**
	 * Sets the desired forms of the source and target types
	 * 
	 * @param direction
	 */
	public void setDirection(MapperDirection direction);	
	
	/**
	 * Returns the source form of the class given an input class
	 * @param clazz
	 * @param callInfo Need this object to obtain the property or the parent property in case property is null
	 * @return
	 * @see TypeMapper#getDirection()
	 */
	public Class<?> getSourceClass(Class<?> clazz, CallInfo callInfo);
	
	/**
	 * Factory method to create a TypeMapper instance
	 * @param direction
	 * @return
	 */
	public TypeMapper newInstance(MapperDirection direction);

	/**
	 * Check if the given class is in its external form
	 * @param clazz
	 * @return
	 */
	public boolean isExternal(Class<?> clazz);
	
	/**
	 * Check if the given class is in its domain form
	 * @param clazz
	 * @return
	 */
	public boolean isDomain(Class<?> clazz);
	
	/**
	 * Get the associated creation strategy
	 * @param oc
	 * @return
	 */
	public CreationStrategy getCreationStrategy(ObjectCreator oc);
	
	/**
	 * Create an instance of the desired external type
	 * @param domainType
	 * @param derivedClass
	 * @return
	 */
	public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass);

	/**
	 * Allow the typemapper to specify a custom name for the external type
	 * This is necessary for examples like JsonObject where there is not much information
	 * without knowing the domain class that it represents
	 * @param inputClass
	 * @param domainType
	 * @return
	 */
	public String getExternalTypeName(Class<?> inputClass, Type domainType);

	/** 
	 * Flag that denotes if the object needs to be readded after the child properties are processed
	 * This is necessary for immutable objects
	 * @return
	 */
	public boolean immutable();
	
	/**
	 * Identifies if a particular interface is an open type
	 * @param clazz
	 * @return
	 */
	public boolean isOpen(Class<?> clazz);
}