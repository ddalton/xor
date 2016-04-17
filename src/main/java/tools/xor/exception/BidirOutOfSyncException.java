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

package tools.xor.exception;


import tools.xor.BusinessObject;
import tools.xor.Property;
import tools.xor.util.I18NUtils;

public class BidirOutOfSyncException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private BusinessObject object;
	private Property property;
	private BusinessObject otherObject;
	private Property otherProperty;	
	
	public BidirOutOfSyncException(BusinessObject object, Property property, BusinessObject otherObject, Property otherProperty) {
		this.object = object;
		this.property = property;
		this.otherObject = otherObject;
		this.otherProperty = otherProperty;
	}

	public Property getOtherProperty() {
		return otherProperty;
	}

	public void setOtherProperty(Property otherProperty) {
		this.otherProperty = otherProperty;
	}
	
	public BusinessObject getObject() {
		return object;
	}

	public void setObject(BusinessObject object) {
		this.object = object;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public BusinessObject getOtherObject() {
		return otherObject;
	}

	public void setOtherObject(BusinessObject otherObject) {
		this.otherObject = otherObject;
	}	
	
	@Override
	public String getMessage() {
		String[] params = new String[4];
		params[0] = object.getType().getName() + "(id: " + object.getIdentifierValue() + ")";
		params[1] = property.getName();
		params[2] = otherObject.getType().getName() + "(id: " + otherObject.getIdentifierValue() + ")";
		params[3] = otherProperty.getName();		
		return  I18NUtils.getResource(  "exception.bidirOutOfSync",I18NUtils.CORE_RESOURCES, params);			
	}
}
