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

import java.lang.reflect.Method;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import tools.xor.event.PropertyEvent;
import tools.xor.util.ObjectCreator;

public interface ExtendedProperty extends Property {
	

	public enum Phase {
		/**
		 * Process before the property is processed
		 */
		PRE,    

		/**
		 * Do the actual processing replacing the standard getter/setter processing
		 */
		LOGIC,  

		/**
		 * Process after the property and its descendants are processed
		 */
		POST;    
	}

	/**
	 * Get the access type (if any) specified on the property/field
	 * @return
	 */
	public AccessType getAccessType();

	/**
	 * Use reflection to get the value of this property from the dataObject
	 * @param dataObject
	 * @return
	 */
	public Object getValue(Object dataObject);

	/**
	 * Use reflection to set the value of this property on the dataObject
	 * @param dataObject
	 * @param propertyValue
	 */
	public void setValue(Object dataObject, Object propertyValue);
	
	/**
	 * Adds a collection element to the dataobject that represents a collection
	 * 
	 * @param dataObject
	 * @param element
	 */
	public void addElement(Object dataObject, Object element);
	
	/**
	 * Adds a map entry
	 * @param dataObject
	 * @param key
	 * @param value
	 */
	public void addMapEntry(Object dataObject, Object key, Object value);	

	/**
	 * Make a shallow copy of the value of this property 
	 * @param dataObject
	 * @return
	 * @throws Exception 
	 */
	public Object shallowCopy(Object dataObject, ObjectCreator creator);

	/**
	 * Gets the key type for a map property 
	 * @return
	 */
	public Type getKeyType();

	/**
	 * Gets the element type for a collection property
	 */
	public Type getElementType();	

	/**
	 * Indicates if this property needs to be initialized when the object is created
	 */
	public boolean needsInitialization();
	
	/**
	 * Indicates if this property represents the full data of the aggregate in JSON format
	 * @return
	 */
	public boolean isData();	

	/**
	 * Returns the type of association modelled by this property
	 * @return
	 */
	public PersistentAttributeType getAssociationType();

	/**
	 * For the  derived property it checks if the XmlTransient annotation is present
	 * For a POJO property it checks if the java transient annotation is present
	 * @return
	 */
	public boolean isTransient();

	/**
	 * Does this property refer to a bi-directional relationship
	 */
	public boolean isBiDirectional();
	
	/**
	 * If this property refer to a bi-directional relationship and the types are symmetrical, i.e., the backRef points to the same type and not to a sub-type
	 */
	public boolean isSymmetricalBiDirectionalType();	

	/**
	 * Returns the mapped by Property of a bi-directional relation. The mapped by property is the property that stores the value of the link in the database.
	 * @return 
	 */
	public Property getMappedBy();

	/**
	 * Returns the Property that is the other side of a bi-directional relation.
	 * @return 
	 */
	public Property getMapOf();

	/**
	 * Allows to work with embedded objects
	 * @param mapPath
	 */
	public void setMapPath(String mapPath);

	/**
	 * Returns the path of the mappedBy property
	 * @return
	 */
	public String getMapPath();
	
	/**
	 * Returns true if the collection represents a Map
	 */
	public boolean isMap();
	
	/**
	 * Returns true if the collection represents a List
	 */
	public boolean isList();	
	
	/**
	 * Returns true if the collection represents a Set
	 */
	public boolean isSet();
	
	/**
	 * Allow business logic to be invoked when reading a value
	 * @param dataObject
	 * @param event
	 */
	public Object propertyRead(BusinessObject dataObject, PropertyEvent event);

	/**
	 * Allow business logic to be invoked when setting a value
	 * @param dataObject
	 * @param event
	 * @return true if the processing needs to be short circuited
	 */
	public boolean propertyUpdate(BusinessObject dataObject, PropertyEvent event);

	/**
	 * Convenience method to see if the property refers to a DataObject or a simple object
	 * This method also considers a collection of DataObject as a DataObject whereas the Type.isDataType does 
	 * not make this difference since it does not have access to this information.
	 * @return
	 */
	boolean isDataType();

	/**
	 * Returns the property on the entity responsible for holding the index/key value for this toMany property
	 * @return
	 */
	public Property getPositionProperty();

	/**
	 * @param property refers to the toMany property which has a map key tracked at this property
	 */
	public void setMapKeyOf(ExtendedProperty property);
	
	/**
	 * @param property refers to the toMany property which has an index tracked at this property
	 */	
	public void setIndexOf(ExtendedProperty property);

	/**
	 * Initialize the position property
	 */
	public void initPositionProperty();
	
	/**
	 * If this property is bi-directional then this method is used to synchronize the back pointer
	 * @param dataObject
	 */
	public void linkBackPointer(BusinessObject dataObject);
	
	/**
	 * Unlink the back pointer
	 * @param dataObject
	 */
	public void unlinkBackPointer(BusinessObject dataObject);

	/**
	 * Get the name of the property that has the bi-directional reference
	 * @return
	 */
	public String getMappedByName();

	/**
	 * Method to check meta information on a property to see if it can be updated
	 * @return
	 */
	public boolean isUpdatable();
	
	/**
	 * Get the correct business logic getter for the current API version in settings
	 * 
	 * @param settings
	 * @return
	 */
	public Method getDataReader(Settings settings, ProcessingStage stage);

	/**
	 * Get the correct business logic setter for the current API version in settings
	 * 
	 * @param settings
	 * @return
	 */
	public MethodInfo getDataUpdater(Settings settings, Phase phase, ProcessingStage stage);

	/**
	 * Checks if the property is appliacable for the given api version
	 * 
	 * @param apiVersion
	 * @return
	 */
	public boolean isApplicable(int apiVersion);
	
	/**
	 * Returns true if this property represents the identifier property
	 * @return
	 */
	public boolean isIdentifier();
	
	/**
	 * Returns the property representing the domain type
	 * @return
	 */
	public Property getDomainProperty();
}
