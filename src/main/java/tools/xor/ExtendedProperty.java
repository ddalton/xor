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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import tools.xor.AbstractProperty.LambdaResult;
import tools.xor.JSONObjectProperty.Converter;
import tools.xor.event.PropertyEvent;
import tools.xor.generator.GeneratorRecipient;

public interface ExtendedProperty extends Property, GeneratorRecipient
{

	/**
	 * Create a new property that is same as the current property
	 * except that it has a new name and an optionally refined property type (subclass)
	 * @param name new name, a.k.a alias
	 * @param type of the property
	 * @param parentType entity type holding this property
	 * @return new property instance
	 */
	Property refine (String name, Type type, EntityType parentType);

	public enum Phase {
		/**
		 * Process before the property is processed
		 */
		PRE,
		
		/**
		 * Process in place of the regular getter and setter
		 */
		INPLACEOF,   		

		/**
		 * Process after the property is set and 
		 * before any further entity members or collection element processing
		 */
		POST;    
	}

	/**
	 * Get the access type (if any) specified on the property/field
	 * @return AccessType
	 */
	public AccessType getAccessType();

	/**
	 * This is an optimization to prevent conversion to an instance of DomainType and
	 * back to a String.
	 * Useful when exporting.
	 *
	 * @param dataObject whose property value is to be returned
	 * @return property value
	 */
	public String getStringValue (BusinessObject dataObject);

	/**
	 * Use reflection to get the value of this property from the dataObject
	 * @param dataObject whose parameter value is to be returned
	 * @return value
	 */
	public Object getValue(BusinessObject dataObject);

	/**
	 * Use reflection to set the value of this property on the dataObject
	 * @param dataObject whose parameter needs to be set.
	 * @param propertyValue the value that needs to be set
	 */
	public void setValue(BusinessObject dataObject, Object propertyValue);

	/**
	 * Use reflection to set the value of this property on the dataObject
	 * @param settings under which this value is being set
	 * @param dataObject whose parameter needs to be set. This cannot be BusinessObject
	 *                   because there are some instances where the BusinessObject instance
	 *                   is not yet created when this method is invoked.
	 * @param propertyValue the value that needs to be set
	 */
	public void setValue(Settings settings, Object dataObject, Object propertyValue);
	
	/**
	 * Adds a collection element to the dataobject that represents a collection
	 * 
	 * @param dataObject the collection
	 * @param element collection element
	 */
	public void addElement(BusinessObject dataObject, Object element);
	
	/**
	 * Adds a map entry
	 * @param dataObject that represents a map
	 * @param key entry key
	 * @param value entry value
	 */
	public void addMapEntry(Object dataObject, Object key, Object value);	

	/**
	 * Gets the key type for a map property 
	 * @return key type
	 */
	public Type getKeyType();

	/**
	 * Gets the element type for a collection property
	 * @return element type
	 */
	public Type getElementType();	

	/**
	 * Indicates if this property needs to be initialized when the object is created
	 * @return boolean value
	 */
	public boolean isAlwaysInitialized();
	
	/**
	 * Use this to always have the field retrieved or set depending on the operation
	 * @param alwaysInitialized true if the value needs to always be initialized
	 */
	public void setAlwaysInitialized(boolean alwaysInitialized);	

	/**
	 * Returns the type of association modelled by this property
	 * @return association type
	 */
	public PersistentAttributeType getAssociationType();

	/**
	 * For the  derived property it checks if the XmlTransient annotation is present
	 * For a POJO property it checks if the java transient annotation is present
	 * @return boolean value
	 */
	public boolean isGenerated ();

	/**
	 * Does this property refer to a bi-directional relationship
	 * @return boolean value
	 */
	public boolean isBiDirectional();
	
	/**
	 * If this property refer to a bi-directional relationship and the types are symmetrical, i.e., the backRef points to the same type and not to a sub-type
	 * @return boolean value
	 */
	public boolean isSymmetricalBiDirectionalType();

	/**
	 * Returns the Property that is the other side of a bi-directional relation.
	 * @return Property object
	 */
	public Property getMapOf();

	/**
	 * Allows to work with embedded objects
	 * @param mapPath map property path
	 */
	public void setMapPath(String mapPath);

	/**
	 * Returns the path of the mappedBy property
	 * @return map property path
	 */
	public String getMapPath();
	
	/**
	 * Returns true if the collection represents a Map
	 * @return boolean value
	 */
	public boolean isMap();
	
	/**
	 * Returns true if the collection represents a List
	 * @return boolean value
	 */
	public boolean isList();	
	
	/**
	 * Returns true if the collection represents a Set
	 * @return boolean value
	 */
	public boolean isSet();

	/**
	 * Allow business logic to be invoked 
	 * @param event event details object to be passed in as argument values if needed
	 * @return LambdaResult object
	 */
	public LambdaResult evaluateLambda(PropertyEvent event);

	/**
	 * Convenience method to see if the property refers to a DataObject or a simple object
	 * This method also considers a collection of DataObject as a DataObject whereas the Type.isDataType does 
	 * not make this difference since it does not have access to this information.
	 * @return boolean value
	 */
	boolean isDataType();

	/**
	 * Returns the property on the entity responsible for holding the index/key value for this toMany property
	 * @return Property object
	 */
	public Property getPositionProperty();

	/**
	 * Set the property that contains the index information for a collection element
	 * This is applicable only if this property represents a collection property
	 * @param property index property
	 */
	public void setIndexPositionProperty(Property property);

	/**
	 * Refers to the toMany property which has a map key tracked at this property
	 * @param property toMany property
	 */
	public void setMapKeyOf(ExtendedProperty property);
	
	/**
	 * Refers to the toMany property which has an index tracked at this property
	 * @param property toMany property
	 */	
	public void setIndexOf(ExtendedProperty property);

	/**
	 * Initialize the position property
	 */
	public void initPositionProperty();
	
	/**
	 * If this property is bi-directional then this method is used to synchronize the back pointer
	 * @param dataObject from which the backref is set
	 */
	public void linkBackPointer(BusinessObject dataObject);
	
	/**
	 * Unlink the back pointer
	 * @param dataObject from which the backref is broken
	 */
	public void unlinkBackPointer(BusinessObject dataObject);

	/**
	 * Get the name of the property that has the bi-directional reference
	 * @return backref property name
	 */
	public String getMappedByName();
	
	/**
	 * Checks if the Column has unique attribute set as true or has a unique constraints
	 * defined on this property
	 * 
	 * @return true if the column has unique values
	 */
	public boolean isUnique();	
	
	/**
	 * Currently applicable only for StringType
	 * 
	 * @return length of the string column
	 */
	public int getLength();

	/**
	 * Method to check meta information on a property to see if it can be updated
	 * @return boolean value
	 */
	public boolean isUpdatable();

	/**
	 * Get the lambdas
	 * @param settings user entered settings
	 * @param tags invoke only when a certain label is found
	 * @param phase desired phase
	 * @param stage desired stage
	 * @return list of lambda objects
	 */
	public List<MethodInfo> getLambdas(Settings settings, String[] tags, Phase phase, ProcessingStage stage);
	
	/**
	 * Returns true if this property represents the identifier property
	 * @return boolean value
	 */
	public boolean isIdentifier();
	
	/**
	 * Returns the relationship type for an open property
	 * @return null if not an open property
	 */
	public RelationshipType getRelationshipType();
	
	/**
	 * Return the foreign key field mappings for the open property relationship
	 * @return map of the field mappings. Has more than one entry if the key is composite
	 */
	public Map<String, String> getKeyFields();

	/**
	 * Records the foreign key relationship between two entities
	 * 
	 * @param thisSet The set of fields that comprise the foreign key of the current entity for a TO_ONE relationship
	 *        and refers to the set of fields on the element entity for a TO_MANY relationship
	 * @param thatSet The set of fields that comprise the composite condidate/primary key on the target entity. The source
	 *        entity is the current entity for a TO_ONE relationship and the collection element for a TO_MANY relationship. 
	 */
	public void addKeyMapping(String[] thisSet, String[] thatSet);	
	
	/**
	 * Returns the key that uniquely identifies an element within a collection.
	 * This key does not have to be as precise as a natural key of the element if there is one as
	 * the collection is just a restriction of the element table.
	 * 
	 * The (CollectionKey, Owner key) typically represents the natural key of the element
	 * 
	 * @return collection key
	 */
	public Set<String> getCollectionKey();
	
	/**
	 * Set the collection key for this property. The property should represent a TO_MANY relationship
	 * 
	 * @param collectionKey set of properties forming the collection key
	 */
	public void setCollectionKey(Set<String> collectionKey);

	/**
	 * This is applicable only if the property references a collection and
	 * if the collection only contains references, for example identifiers.
	 * @return true if the property returns a collection of ids
	 */
	public boolean isCollectionOfReferences();

	/**
	 * Helps to identify if this property is part of the natural key of its containing type
	 * @return true if it forms part of the natural key
	 */
	boolean isPartOfNaturalKey ();

	/**
	 * Init business logic methods from the provided class
	 * @param clazz the class to scan for business logic methods
	 */
	public void initLambdas(Class clazz);

	/**
	 * When persisting an object, should the referenced object of this property
	 * have its id based on the owner
	 *
	 * @return true if the referenced object is a dependent object or a sub-object in an
	 *   inheritance hierarchy
	 */
	public boolean doPropagateId();

	/**
	 * Return the JSON property value converter, if this property is on a dynamic type
	 * @return converter object
	 */
    Converter getConverter();
    
    @Override
    EntityType getContainingType();
    
    /** 
     * Returns the domain type name for this property
     * 
     * @return domain type name. null if not present.
     */
    String getDomainTypeName();
    
    /**
     * Mainly used to capture different generators for a property 
     * when the shape containing this property using VALUE typeInheritance
     * 
     * When the copy is done, the correct subtype for which it is needed
     * is passed
     * 
     * @param parentType correct containing type
     * @return copy of the property instance
     */
    ExtendedProperty copy(EntityType parentType);

	/**
	 * Needed for GraphQL. Fields in GraphQL accept arguments.
	 * @return the arguments applicable to this property/GraphQL field
	 */
	default List<InputValue> getArguments() { return new ArrayList<>(); }
}
