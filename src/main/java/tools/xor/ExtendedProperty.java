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

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import tools.xor.event.PropertyEvent;

public interface ExtendedProperty extends Property {
	

	public enum Phase {
		/**
		 * Process before the property is processed
		 */
		PRE,    

		/**
		 * Process after the property and its descendants are processed
		 */
		POST;    
	}

	/**
	 * Get the access type (if any) specified on the property/field
	 * @return AccessType
	 */
	public AccessType getAccessType();

	/**
	 * Use reflection to get the value of this property from the dataObject
	 * @param dataObject whose parameter value is to be returned
	 * @return value
	 */
	public Object getValue(Object dataObject);

	/**
	 * Use reflection to set the value of this property on the dataObject
	 * @param dataObject whose parameter needs to be set
	 * @param propertyValue the value that needs to be set
	 */
	public void setValue(Object dataObject, Object propertyValue);
	
	/**
	 * Adds a collection element to the dataobject that represents a collection
	 * 
	 * @param dataObject the collection
	 * @param element collection element
	 */
	public void addElement(Object dataObject, Object element);
	
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
	public boolean needsInitialization();

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
	public boolean isTransient();

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
	 * Returns the mapped by Property of a bi-directional relation. The mapped by property is the property that stores the value of the link in the database.
	 * @return Property object
	 */
	public Property getMappedBy();

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
	 * @param dataObject the promises on this dataObject for this property
	 * @param event event details object to be passed in as argument values if needed
	 * @return true if the processing needs to be short circuited
	 */
	public boolean evaluatePromise(BusinessObject dataObject, PropertyEvent event);

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
	 * Method to check meta information on a property to see if it can be updated
	 * @return boolean value
	 */
	public boolean isUpdatable();

	/**
	 * Get the promises
	 * @param settings user entered settings
	 * @param phase desired phase
	 * @param stage desired stage
	 * @return list of promise objects
	 */
	public List<MethodInfo> getPromises(Settings settings, Phase phase, ProcessingStage stage);

	/**
	 * Checks if the property is appliacable for the given api version
	 * 
	 * @param apiVersion api version
	 * @return boolean value
	 */
	public boolean isApplicable(int apiVersion);
	
	/**
	 * Returns true if this property represents the identifier property
	 * @return boolean value
	 */
	public boolean isIdentifier();
	
	/**
	 * Returns the property representing the domain type
	 * @return Property object
	 */
	public Property getDomainProperty();
}
