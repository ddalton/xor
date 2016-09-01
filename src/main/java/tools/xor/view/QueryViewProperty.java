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

package tools.xor.view;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.Constants;

/**
 * Enhances a property with view specific details.
 * We do not implement ExtendedProperty but rather use delegation because we
 * desire this to be loosely coupled  
 * 
 * @author Dilip Dalton
 *
 */
public class QueryViewProperty {
	public static final String ROOT_PROPERTY_NAME = "root";	

	// Outputs or addendum are in upper case
	public static final String ENTITYNAME_ATTRIBUTE  = "TYPE_";
	public static final String MAP_KEY_ATTRIBUTE     = "KEY_";
	public static final String MAP_VALUE_ATTRIBUTE   = "VALUE_";
	public static final String LIST_INDEX_ATTRIBUTE  = "INDEX_";               // Use this to set the object at the right location or in ORDER BY clause if filter is used	
	public static final String COL_USERKEY_ATTRIBUTE = "COLLECTION_USERKEY_";
	public static final String USERKEY_ATTRIBUTE     = "USERKEY_";
	
	// Inputs or parameters are in lower case
	public static final String ID_PARAMETER_NAME     = "id_";
	public static final String NEXTTOKEN_PARAM_PREFIX = "orderBy_";	

	private QueryViewProperty   parent;        // Needed for embedded or element collection attributes
	private Property       property;      // null for root
	private Type           type;          // the type of the entity
	                                      // TODO: Ideally should be the narrowest type for the subset of properties
	private String         propertyPath;  // the property path that represents the attribute
	private boolean        isDynamic;     // Was not part of the initial set of properties when this view was created
	private String         alias;         // Refers to the entity/collection entity alias
	private String         propertyAlias; // NOTE: A property cannot typically be used in a where clause	
	private boolean        fetch = true;  // Should the data be fetched, by default we always fetch

	
	@Override
	public String toString() {
		return  " ViewProperty[parent: " + ((parent != null) ? parent.getPropertyPath() : "null") + 
				", property: " + ((property != null) ? property.getName() : "null") + 
				", type: " + ((type != null) ? type.getName() : "null") +
				", propertyPath: " + propertyPath +
				", isDynamic: " + isDynamic +
				", alias: " + alias +
				", propertyAlias: " + propertyAlias +
				", fetch: " + fetch;  
	}
	

	public QueryViewProperty(boolean isDynamic, Type type) {
		this.propertyPath = ROOT_PROPERTY_NAME;
		this.isDynamic = isDynamic;
		this.type = type;
		this.parent = null;
	}

	public QueryViewProperty(String attribute, boolean isDynamic, QueryViewProperty parent) {
		this.propertyPath = qualifyProperty(attribute);
		this.isDynamic = isDynamic;
		this.parent = parent;

		setProperty();
	}	
	
	public boolean doFetch() {
		return fetch;
	}

	public void setFetch(boolean fetch) {
		this.fetch = fetch;
	}

	public String getPropertyAlias() {
		return propertyAlias;
	}

	public void setPropertyAlias(String propertyAlias) {
		this.propertyAlias = propertyAlias;
	}

	public QueryViewProperty getParent() {
		return parent;
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public Property getIdentifierProperty() {
		if(type.isDataType())
			throw new IllegalArgumentException("This method can only be invoked on a data object");

		return ((EntityType)type).getIdentifierProperty();
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public boolean isCollection() {
		return (property != null) ? property.isMany() : false;
	}

	private void setProperty() {
		if(parent == null) { 
			if(propertyPath.equals(ROOT_PROPERTY_NAME))
				return;
			else
				throw new IllegalStateException("anchor is not set for non-root property");
		}

		property = parent.getProperty(propertyPath);
		if(property != null) {
			this.type = this.property.getType();
			if(property.isMany()) {
				this.type = ((ExtendedProperty)property).getElementType();
			}
		}
	}

	public Type getType() {
		return this.type;
	}


	public static String qualifyProperty(String propertyPath) {
		if(!propertyPath.startsWith(ROOT_PROPERTY_NAME+ Settings.PATH_DELIMITER))		
			return ROOT_PROPERTY_NAME + Settings.PATH_DELIMITER + propertyPath;
		else
			return propertyPath;
	}

	public static String unqualifyProperty(String propertyPath) {
		if(propertyPath.startsWith(ROOT_PROPERTY_NAME+ Settings.PATH_DELIMITER))
			return propertyPath.substring(ROOT_PROPERTY_NAME.length() + Settings.PATH_DELIMITER.length());
		else
			return propertyPath;
	}	

	public static boolean isQualified(String propertyPath) {
		if(!propertyPath.startsWith(ROOT_PROPERTY_NAME))		
			return false;
		else
			return true;
	}	

	public static String getBaseName(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(propertyPath.lastIndexOf(Settings.PATH_DELIMITER)+1);
		else
			return propertyPath;
	}	

	public static String getRootName(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(0, propertyPath.indexOf(Settings.PATH_DELIMITER));
		else
			return propertyPath;
	}	

	public static String getNext(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(propertyPath.indexOf(Settings.PATH_DELIMITER)+1);
		else
			return null;
	}	

	public String getAlias() {
		return alias;
	}

	public boolean isEntity() {
		if(type != null && EntityType.class.isAssignableFrom(type.getClass()) && ((EntityType)type).getIdentifierProperty() != null )
			return true;

		return false;
	}

	public void setAlias(String alias) {
		if(!isEntity())
			throw new IllegalArgumentException("Alias can only be set for an entity or a collection of entities");

		this.alias = alias;
	}

	public Property getProperty() {
		return property;
	}	

	/**
	 * Normalize the property name based on any aliases in the ancestor list.
	 * This is needed for HQL to work properly 
	 * @return normalized name
	 */
	protected String getNormalizedName() {

		String baseName = getBaseName(propertyPath);

		if(getParent() == null)
			return baseName;
		else
			return getParent().getAlias() + Settings.PATH_DELIMITER + baseName;
	}	

	/*
	 * Retrieves the property specified in propertyPath
	 */
	public Property getProperty(String childPath) {
		String path = isQualified(childPath) ? null : childPath;
		if(childPath.startsWith(this.propertyPath))
			path = childPath.substring(this.propertyPath.length() + Settings.PATH_DELIMITER.length());

		if(path == null)
			return null;

		return this.type.getProperty(path);
	}		

	public void setProperty(Property property) {
		this.property = property;
	}

	/**
	 * This method adds any additional attributes needed for the view to provide correct functionality
	 * @param narrow true if entity name information needs to be added
	 * @return column information
	 */
	public Set<ColumnMeta> getColumnMeta(boolean narrow) {
		final Logger vb = LogManager.getLogger(Constants.Log.VIEW_BRANCH);
		
		Set<ColumnMeta> result = new HashSet<ColumnMeta>();

		if(!isEntity()) {
			result.add(new ColumnMeta(propertyPath, this));
		} else {
			//System.out.println("Property path: " + propertyPath + ", property: " + ((property!=null)?property.getName():"null"));
			// Add the entity name
			if(narrow)
				result.add(new ColumnMeta(propertyPath + Settings.PATH_DELIMITER + QueryViewProperty.ENTITYNAME_ATTRIBUTE, this));

			// Always add the identifier for an entity. This will be replaced by the viewProperty which is not dynamic in the QueryBuilder
			if( ((EntityType)type).getIdentifierProperty() != null )
				result.add(new ColumnMeta(propertyPath + Settings.PATH_DELIMITER + ((EntityType)type).getIdentifierProperty().getName(), this));		
			
			if(property != null && property.isMany()) { // collection
				if( ((ExtendedProperty)property).isList() ) {
					// add INDEX
					result.add(new ColumnMeta(propertyPath + Settings.PATH_DELIMITER + LIST_INDEX_ATTRIBUTE, this));
				} else if( ((ExtendedProperty)property).isMap() ) {
					// add KEY
					result.add(new ColumnMeta(propertyPath + Settings.PATH_DELIMITER + MAP_KEY_ATTRIBUTE, this));
					// add COLLECTION_USERKEY
					if( ((EntityType)type).getCollectionUserKey() != null ) {
						for(String key: ((EntityType)type).getCollectionUserKey()) {
							result.add(new ColumnMeta(propertyPath + Settings.PATH_DELIMITER + key, this));
						}
					}
				}
			} else { // entity
				// add USERKEY
				if( ((EntityType)type).getUserKey() != null ) {
					for(String key: ((EntityType)type).getUserKey()) {
						result.add(new ColumnMeta(propertyPath + Settings.PATH_DELIMITER + key, this));
					}
				}
			}
			
			if(vb.isDebugEnabled()) {
				vb.debug("===== C O L U M N S (PropertyPath: " + propertyPath + ", Narrow: " + narrow + ") =====" );
				for(ColumnMeta cm: result) {
					vb.debug(cm.toString());
				}
			}
		}

		return result;
	}

}
