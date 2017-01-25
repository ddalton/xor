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
	
	private Collection<Object> getCollectionObject(Object list) {
		Collection<Object> collection = null;
		
		// Prefer instanceof to isAssignableFrom due to performance
		if(list instanceof Collection)
			collection = (Collection<Object>) list;
		else if(list instanceof Map)
			collection = ((Map<Object, Object>) list).values();
		else if(list instanceof JSONArray) 
			collection = ClassUtil.jsonArrayToList((JSONArray)list);
		
		return collection;
	}

	public List<BusinessObject> list() {
		return list(null);
	}

	public List<BusinessObject> list(Settings settings) {
		
		// First check to see if dataObject refers to collection owner or the collection object
		Object list = dataObject.getInstance();
		Collection<Object> collection = getCollectionObject(list);
		
		// dataObject is the collection owner
		if(collection == null && property != null) {
			list = ((ExtendedProperty)property).getValue(dataObject);
			collection = getCollectionObject(list);
		}
		
		if(collection == null) {
			return new ArrayList();
		}
		
		Type type = null;
		ObjectCreator objectCreator = dataObject.getObjectCreator();
		if(property == null && dataObject.getContainmentProperty() == null) {
			if(!Map.class.isAssignableFrom(list.getClass())) {
				// Might be a dummy graph root for bulk processing
				if(settings != null && collection.size() > 0) {
					// We use the ObjectCreator to get the External type if relevant
					type = objectCreator.getType(collection.iterator().next().getClass(), settings.getEntityType());
				}
				// Bulk support doesn't have this requirement
				//else {
				//	throw new IllegalStateException("The container and the containment property is required, are you trying to process an association instead? [list class: " + list.getClass() +"]");
				//}
			} else {
				// Dynamic type
				return new ArrayList();
			}
		}
		
		if(dataObject.getContainmentProperty() != null && !dataObject.getContainmentProperty().isMany()) {
			return new ArrayList();
		}

		// A property is provided for a collection owner 
		// and a collection object should have a containment property unless it is an open property in which case we look at the property
		List<BusinessObject> result = new ArrayList<BusinessObject>(collection.size());
		if(collection.size() > 0) {
			if (type == null) {
				type = (property != null) ?
					((ExtendedProperty)property).getElementType() :
					((ExtendedProperty)dataObject.getContainmentProperty()).getElementType();
			}

			//if(objectCreator.getExistingDataObject(collection) != null)
			//	System.out.println("*****!!!!!! has existing collection");

			for (Object element : collection) {
				result.add(objectCreator.createDataObject(element, type, null, null));
			}
		}

		return result;
	}

	public DataObjectList(BusinessObject dataObject, Property property) {
		this.dataObject = dataObject;
		this.property = property;		
	}

	/**
	 * This constructor is for DataObject representing a collection
	 * @param dataObject DataObject
	 */
	public DataObjectList(BusinessObject dataObject) {
		this.dataObject = dataObject;
	}	
}
