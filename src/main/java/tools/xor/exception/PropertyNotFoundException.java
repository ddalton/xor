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

import tools.xor.util.I18NUtils;

public class PropertyNotFoundException extends ApplicationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String objectTypeName;
	private String objectId;
	
	public PropertyNotFoundException() {
		super();
	}
	
	public PropertyNotFoundException(String objectTypeName, String objectId, Exception e) {
		super(e);
		this.objectTypeName = objectTypeName;
		this.objectId = objectId;		
	}
	
	public PropertyNotFoundException(String objectTypeName, String objectId) {
		this.objectTypeName = objectTypeName;
		this.objectId = objectId;		
	}
	
	public PropertyNotFoundException(Throwable t) {
		super(t);
	}
	
	@Override
	public String getMessage() {
		String[] params = new String[2];
		params[0] = objectTypeName;
		params[1] = objectId;
		return  I18NUtils.getResource(  "exception.objectNotFound",I18NUtils.CORE_RESOURCES, params);		
	}

	public String getObjectTypeName() {
		return objectTypeName;
	}

	public void setObjectTypeName(String objectTypeName) {
		this.objectTypeName = objectTypeName;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
}
