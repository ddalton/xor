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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import tools.xor.util.ClassUtil;
import tools.xor.util.ObjectCreator;

public class DataObjectList {

	private BusinessObject dataObject;
	private Property property;

	public List<BusinessObject> list() {
		
		Object list = (property != null) ? ((ExtendedProperty)property).getValue(dataObject) : dataObject.getInstance();
		if(list == null)
			return new ArrayList();

		Collection<Object> collection = null;
		/*
		if(Collection.class.isAssignableFrom(list.getClass()))
			collection = (Collection<Object>) list;
		else if(Map.class.isAssignableFrom(list.getClass()))
			collection = ((Map<Object, Object>) list).values();
			*/
		// Prefer instanceof to isAssignableFrom due to performance
		if(list instanceof Collection)
			collection = (Collection<Object>) list;
		else if(list instanceof Map)
			collection = ((Map<Object, Object>) list).values();
		else if(list instanceof JSONArray) 
			collection = ClassUtil.jsonArrayToCollection((JSONArray) list);
		else
			//throw new IllegalArgumentException("Cannot create a DataObjectList for a non-list property: " + property.getName() + ", with type: " + property.getType().getName());
			return new ArrayList();
		
		if(property == null && dataObject.getContainmentProperty() == null) {
			if(!Map.class.isAssignableFrom(list.getClass())) {
				throw new IllegalStateException("The container and the containment property is required, are you trying to process an association instead? [list class: " + list.getClass() +"]");
			} else {
				// Dynamic type
				return new ArrayList();
			}
		}
		
		if(dataObject.getContainmentProperty() != null && !dataObject.getContainmentProperty().isMany()) {
			return new ArrayList();
		}

		Type type = (property != null) ? property.getType() : ((ExtendedProperty)dataObject.getContainmentProperty()).getElementType();		
		List<BusinessObject> result = new ArrayList<BusinessObject>(collection.size());
		ObjectCreator objectCreator = dataObject.getObjectCreator();
		
		//if(objectCreator.getExistingDataObject(collection) != null)
		//	System.out.println("*****!!!!!! has existing collection");
		
		for(Object element: collection)
			result.add(objectCreator.createDataObject(element, type, (BusinessObject) dataObject, property));

		return result;
	}

	public DataObjectList(BusinessObject dataObject, Property property) {
		this.dataObject = dataObject;
		this.property = property;		
	}

	/**
	 * This constructor is for DataObject representing a collection
	 * @param dataObject
	 */
	public DataObjectList(BusinessObject dataObject) {
		this.dataObject = dataObject;
	}	
}
