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

import java.util.List;

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
	 * If the domain class could not be found from the given class, then try to 
	 * infer it from the provided BusinessObject 
	 * 
	 * @param externalClass external java class
	 * @param bo BusinessObject
	 * @return the domain java class
	 */
	public Class<?> toDomain(Class<?> externalClass, BusinessObject bo);	
	
	/**
	 * This is a more flexible form of getting the 
	 * external form of the class and allows a particalar
	 * type implementation to store more information on the external type
	 * 
	 * @param  type external
	 * @return the domain java class
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
	 * @return direction
	 */
	public MapperDirection getDirection();
	
	/**
	 * Sets the desired forms of the source and target types
	 * 
	 * @param direction value
	 */
	public void setDirection(MapperDirection direction);	
	
	/**
	 * Returns the source form of the class given an input class
	 * @param clazz input
	 * @param callInfo Need this object to obtain the property or the parent property in case property is null
	 * @return source class
	 * @see TypeMapper#getDirection()
	 */
	public Class<?> getSourceClass(Class<?> clazz, CallInfo callInfo);
	
	/**
	 * Returns the target form of the class given an input class
	 * @param clazz input
	 * @param callInfo Need this object to obtain the property or the parent property in case property is null
	 * @return target class
	 * @see TypeMapper#getDirection()
	 */
	public Class<?> getTargetClass(Class<?> clazz, CallInfo callInfo);	
	
	/**
	 * Factory method to create a TypeMapper instance
	 * @param direction value
	 * @return typemapper instance
	 */
	public TypeMapper newInstance(MapperDirection direction);

	/**
	 * Check if the given class is in its external form
	 * @param clazz value
	 * @return true if external form of the class
	 */
	public boolean isExternal(Class<?> clazz);
	
	/**
	 * Check if the given class is in its domain form
	 * @param clazz value
	 * @return true if domain form of the class
	 */
	public boolean isDomain(Class<?> clazz);
	
	/**
	 * Get the associated creation strategy
	 * @param oc objectcreator input
	 * @return creationstrategy
	 */
	public CreationStrategy getCreationStrategy(ObjectCreator oc);
	
	/**
	 * Create an instance of the desired external type
	 * @param domainType domain type
	 * @param derivedClass external java class
	 * @return external type
	 */
	public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass);

	/**
	 * Allow the typemapper to specify a custom name for the external type
	 * This is necessary for examples like JsonObject where there is not much information
	 * without knowing the domain class that it represents
	 * 
	 * @param inputClass external class
	 * @param domainType domain type
	 * @return external name
	 */
	public String getExternalTypeName(Class<?> inputClass, Type domainType);

	/** 
	 * Flag that denotes if the object needs to be read after the child properties are processed
	 * This is necessary for immutable objects
	 * @return true if immutable
	 */
	public boolean immutable();
	
	/**
	 * Identifies if a particular interface is an open type
	 * @param clazz input
	 * @return true if the clazz supports creating open properties e.g., JSONObject
	 */
	public boolean isOpen(Class<?> clazz);
	
	/**
	 * Get the EntityKey object that is used as the key in the ObjectCreator
	 * cache.
	 * 
	 * @param id identifier
	 * @param type Type
	 * @return EntityKey
	 */
	@Deprecated public EntityKey getEntityKey(Object id, Type type);
	
	/**
	 * Get the EntityKey object that is used as the key in the ObjectCreator
	 * cache. This method allows type information to be inferred from a BusinessObject
	 * and can lead to a more robust behavior in Polymorphic situations where the Type object 
	 * is not present in dynamic type situations (e.g., JSON)
	 *  
	 * @param id identifier
	 * @param bo BusinessObject from which the Type is inferred
	 * @return EntityKey
	 */
	@Deprecated public EntityKey getEntityKey(Object id, BusinessObject bo);


	public EntityKey getSurrogateKey(Object id, Type type);

	public List<EntityKey> getNaturalKey(BusinessObject ob);
	
	/**
	 * Check to see if we are created external Business object(s)
	 * @return true if the target object is of external type
	 */
	public boolean isToExternal();
}
