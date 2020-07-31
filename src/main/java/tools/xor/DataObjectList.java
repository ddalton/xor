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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.service.DataStore;
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

	/**
	 * If the element is a JSONObject then get the narrowed type based on XOR.type information
	 * @param element of the collection
	 * @param fallback type of the element, usually a parent type as defined in the domain model.
	 * @return
	 */
	private Type getElementTypeDowncast (Object element, Type fallback)
	{
		// This method is only applicable for EntityType
		if( !(fallback instanceof EntityType) ) {
			return fallback;
		}
		EntityType fallbackET = (EntityType) fallback;

		Type type = null;
		if (element instanceof JSONObject) {
			try {
				Class elementClass = MutableJsonTypeMapper.getEntityClass((JSONObject)element);
				type = dataObject.getObjectCreator().getShape().getType(elementClass);
			}
			catch (Exception e) {
				type = fallback;
			}
		}

		return (type != null && type instanceof EntityType) ? type : fallback;
	}

	public List<BusinessObject> list(Settings settings) {

		if(settings == null) {
			settings = dataObject.getSettings();
		}
		
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

		// Check if the list is being invoked on the wrong properly and fail gracefully
		if(property == null && dataObject.getContainmentProperty() != null && !dataObject.getContainmentProperty().isMany()) {
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

			List toBeLoaded = new ArrayList();
			Property collectionProperty = (property != null) ? property : dataObject.getContainmentProperty();
			for (Object element : collection) {
				// check if it can be found by the entity key, as some collections return only
				// the ids for performance reasons

				if(type instanceof EntityType && collectionProperty != null && ((ExtendedProperty)collectionProperty).isCollectionOfReferences()) {
					String entityTypeName = AbstractTypeMapper.getSurrogateKeyTypeName(type);
					EntityKey entityKey = new SurrogateEntityKey(element, entityTypeName);
					BusinessObject bo = objectCreator.getByEntityKey(entityKey, type);
					if(bo != null) {
						result.add(bo);
						continue;
					}
				}
				toBeLoaded.add(element);
			}

			// We want the collectionDataObject
			BusinessObject collectionDataObject = null;
			if(!type.isDataType() && ((EntityType)type).isEmbedded()) {
				collectionDataObject = property == null ? dataObject : null;
			}

			// Bulk load the collection of references
			if(collectionProperty != null && ((ExtendedProperty)collectionProperty).isCollectionOfReferences()) {
				DataStore po = dataObject.getObjectCreator().getDataStore();
				List persistedInstances = po.findByIds((EntityType)type, toBeLoaded);
				if(persistedInstances != null) {

					Set persistedObjectIds = new HashSet();
					for (Object persisted : persistedInstances) {
						// cache the persisted instance in the ObjectCreator
						BusinessObject persistedBO = objectCreator.createDataObject(
							persisted,
							type,
							collectionDataObject,
							null);
						result.add(persistedBO);
						persistedObjectIds.add(persistedBO.getIdentifierValue());
					}
					Set nonPersistedObjectIds = new HashSet(toBeLoaded);
					nonPersistedObjectIds.removeAll(persistedObjectIds);
					toBeLoaded = new ArrayList(nonPersistedObjectIds);
				}
			}

			// For collection of references, need to check if this is a persistent owner
			boolean isPersistentOwner = (property == null && dataObject.getContainer() != null
				&& ((BusinessObject)dataObject.getContainer()).isPersistent()) ? true : false;

			// handle those objects that include those that are not persisted here
			for (Object element : toBeLoaded) {
				if(collectionProperty != null && ((ExtendedProperty)collectionProperty).isCollectionOfReferences()) {
					if (settings.getAction() == AggregateAction.LOAD || settings.getAction() == AggregateAction.READ || isPersistentOwner) {
						EntityKey surrogateKey = objectCreator.getTypeMapper().getSurrogateKey(
							element,
							type);
						BusinessObject collectionElement = objectCreator.getByEntityKey(surrogateKey, type);
						if(collectionElement != null) {
							result.add(collectionElement);
						} else {
							throw new RuntimeException("Unable to find object in ObjectCreator cache: " + surrogateKey.toString() );
						}
					}  else {
						try {
							BusinessObject collectionElement = dataObject.createDataObject(
								element,
								getElementTypeDowncast(element, type));
							collectionElement.setContainer(collectionDataObject);
							collectionElement.setContainmentProperty(null);
							result.add(collectionElement);
						}catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				} else {
					BusinessObject collectionElement =
						objectCreator.createDataObject(
							element,
							getElementTypeDowncast(element, type),
							collectionDataObject,
							null);
					result.add(collectionElement);
				}
			}
		}

		return result;
	}

	/**
	 * This constructor is for DataObject representing the collection owner
	 * @param dataObject collection owner
	 * @param property collection property
	 */
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
