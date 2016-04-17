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


import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.service.QueryCapability;


public class ColumnMeta {

	protected String attributePath;      // The full name of the property
	protected QueryViewProperty viewProperty; // A view property can be associated with multiple ColumnMeta
	protected int position;              // position of this column in the result table

	public ColumnMeta(String attributePath, QueryViewProperty viewProperty) {
		this.attributePath = attributePath;
		this.viewProperty = viewProperty;
	}
	
	@Override
	public String toString() {
		return  " ColumnMeta[Path: " + attributePath + 
				", Pos: " + position + 
				((viewProperty != null) ? viewProperty.toString() : "null");  
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + System.identityHashCode(attributePath);
		return result;
	}

	@Override
	public boolean equals(Object other) {
		ColumnMeta otherKey = (ColumnMeta) other;
		return otherKey.attributePath.equals(this.attributePath);
	}	

	public boolean isDependent() {
		// Is this property dependent on its parent. This is true for all special properties, i.e., those that end with the PATH_DELIMITER
		return attributePath.endsWith(Settings.URI_PATH_DELIMITER);
	}

	public String getQueryString(QueryCapability queryCapability) {

		String result = viewProperty.getAlias();

		if(attributePath.endsWith(QueryViewProperty.ENTITYNAME_ATTRIBUTE))
			throw new RuntimeException("Narrowing of the entity is supported only if the whole entity object is retrieved, which we don't support. Use native query instead.");
		else if(attributePath.endsWith(QueryViewProperty.LIST_INDEX_ATTRIBUTE))
			result = queryCapability.getListIndexMechanism(viewProperty.getAlias());
		else if(attributePath.endsWith(QueryViewProperty.MAP_KEY_ATTRIBUTE))
			result = queryCapability.getMapKeyMechanism(viewProperty.getAlias());		
		else if(result == null) { // no alias
			if(viewProperty.getParent() != null)
				result = viewProperty.getParent().getAlias() + viewProperty.getPropertyPath().substring(viewProperty.getParent().getPropertyPath().length());
			else 
				result = viewProperty.getPropertyPath();
		} else {
			if(viewProperty.isEntity()) {
				String idName = ((EntityType)viewProperty.getType()).getIdentifierProperty().getName();
				if(attributePath.equals(QueryViewProperty.qualifyProperty(idName)) )
					result = result + Settings.PATH_DELIMITER + idName;
			}
		}

		return result;
	}	

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}	

	public String getAttributePath() {
		return attributePath;
	}	

	public QueryViewProperty getViewProperty() {
		return viewProperty;
	}

	public void setViewProperty(QueryViewProperty viewProperty) {
		this.viewProperty = viewProperty;
	}	

	public Integer getAttributeLevel() {
		int noOfSteps = 0;
		for(int delimPos = attributePath.indexOf(Settings.PATH_DELIMITER, 0); delimPos != -1; delimPos = attributePath.indexOf(Settings.PATH_DELIMITER, delimPos+1))
			noOfSteps++;

		return noOfSteps;
	}	
}