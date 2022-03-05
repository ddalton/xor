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

public class AmbiguousMatchException extends ApplicationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String propertyPath1;
	private String propertyPath2;	
	
	public AmbiguousMatchException(String propertyPath1, String propertyPath2, Exception e) {
		super(e);
		this.setPropertyPath1(propertyPath1);
		this.setPropertyPath2(propertyPath2);		
	}
	
	public AmbiguousMatchException(String propertyPath1, String propertyPath2) {
		this.setPropertyPath1(propertyPath1);
		this.setPropertyPath2(propertyPath2);		
	}
	
	public AmbiguousMatchException(Throwable t) {
		super(t);
	}
	
	@Override
	public String getMessage() {
		String[] params = new String[2];
		params[0] = propertyPath1;
		params[1] = propertyPath2;
		return  I18NUtils.getResource(  "exception.ambiguousMatch",I18NUtils.CORE_RESOURCES, params);		
	}

	public String getPropertyPath2() {
		return propertyPath2;
	}

	public void setPropertyPath2(String propertyPath2) {
		this.propertyPath2 = propertyPath2;
	}

	public String getPropertyPath1() {
		return propertyPath1;
	}

	public void setPropertyPath1(String propertyPath1) {
		this.propertyPath1 = propertyPath1;
	}

}
