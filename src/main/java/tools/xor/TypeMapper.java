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

import java.util.List;

import tools.xor.service.DataModel;
import tools.xor.service.Shape;
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
     * The DomainShape represents the type system from the perspective of the
     * backend/persistence layer.
     * 
     * @return domain shape instance
     */
    Shape getDomainShape();
    
    /**
     * The DynamicShape represents type system from the perspective of the end user
     * interacting with the persistence/backend layer.
     * 
     * @return dynamic shape instance
     */
    Shape getDynamicShape();
    
    /**
     * Have the domainShape be built externally and set on the TypeMapper instance
     * @param domainShape instance
     */
    void setDomainShape(Shape domainShape);
    
    /**
     * Allow the dynamicShape to be set programmatically.
     * This is needed for cases where the DynamicShape cannot be inferred from the DomainShape.
     * @param dynamicShape instance
     */
    void setDynamicShape(Shape dynamicShape);
    
    /**
     * We need to find the domain form of the type.
     * If the provided name is a domain type name, then it is returned back unchanged.
     * 
     * @param typeName entityName of the type whose corresponding domain form needs to be returned
     * @return the domain type name/entityName corresponding to given name
     */
    String toDomain(String typeName);   
    
    /**
     * If the domain class could not be found from the given class, then try to 
     * infer it from the provided BusinessObject 
     * 
     * @param externalTypeName external type name
     * @param bo BusinessObject
     * @return the domain type name
     */    
    String toDomain(String externalTypeName, BusinessObject bo);  
    
    /**
     * Return the external type name of the domain type name
     * 
     * @param domainTypeName type name whose corresponding external type needs to be found
     * @return the external type name for the domain type name
     */
    String toExternal(String domainTypeName);     

	/**
	 * Return the external form of the class
	 * 
	 * @param type the type whose corresponding external class needs to be found
	 * @return the external class of type
	 */
	public Class<?> toExternal(Type type);

	/**
	 * Describes what form corresponds to the source and target types
	 * The source class and target class resolution is dependent upon this.
	 * 
	 * @return Mapper side value
	 */
	public MapperSide getSide();
	
	/**
	 * Sets the desired forms of the source and target types
	 * 
	 * @param side MapperSide value
	 */
	public void setSide(MapperSide side);	
	
	/**
	 * Returns the correct type name based on the MapperSide value given the entity name 
	 * @param typeName input
	 * @param callInfo Need this object to obtain the property or the parent property in case property is null
	 * @return type name based on the MapperSide value
	 */
	public String getMappedType(String typeName, CallInfo callInfo);	
	
	/**
	 * Factory method to create a TypeMapper instance
	 * @param side either external or domain
	 * @return typemapper instance
	 */
	public TypeMapper newInstance(MapperSide side);
	
    /**
     * Factory method to create a TypeMapper instance
     * @param side either external or domain
     * @param shapeName name of the domain shape that this mapper is based on
     * @return typemapper instance
     */
    public TypeMapper newInstance(MapperSide side, String shapeName);
    
    /**
     * Factory method to create a TypeMapper instance
     * @param das DataAccessService instance for this type mapper
     * @param side either external or domain
     * @param shapeName name of the domain shape that this mapper is based on
     * @param persistenceManaged true if the model is managed by a persistence provider
     * @return typemapper instance
     */
    public TypeMapper newInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged);       
	
    /**
     * Check if the given type is in its external form
     * @param typeName type nname
     * @return true if external form of the class
     */
    boolean isExternal(String typeName);	
	
	/**
     * Check if the given type name is in its domain form
     * @param typeName name
     * @return true if domain type name
     */
    public boolean isDomain(String typeName);
	
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
	@Deprecated public boolean isOpen(Class<?> clazz);
	
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

	public EntityKey getSurrogateKey(Object id, Type type, String anchor);

	public List<EntityKey> getNaturalKey(BusinessObject ob);

	public List<EntityKey> getNaturalKey(BusinessObject ob, String anchor);
	
	/**
	 * Check to see if we are created external Business object(s)
	 * @return true if the target object is of external type
	 */
	public boolean isExternalSide();

	/**
	 * Get the Shape based on the MapperSide
	 * @return shape instance
	 */
    Shape getShape();
    
    /**
     * Get the Shape name
     * @return shape name
     */
    String getShapeName();    

    /**
     * Return the DataAccessService powering this type mapper
     * @return DAS instance
     */
    DataModel getModel();

    /**
     * Set the DataAccessService powering this type mapper
     * @param das instance
     */
    void setModel(DataModel das);

    /**
     * Add the property to both the domain and dynamic shapes
     * managed by the type mapper
     * @param openProperty property to add
     */
    void addProperty(ExtendedProperty openProperty);
    
    /**
     * Add a new type to both the domain and dynamic shapes
     * @param type to add
     */
    void addType(EntityType type);
    
    /**
     * Some models are not persistence managed, though they
     * might be assigned to work with a persistence manager.
     * For e.g., the swagger model.
     * We indicate this setting here.
     *  
     * @return false if model is not persistence backed, true otherwise
     */
    boolean isPersistenceManaged();
}
