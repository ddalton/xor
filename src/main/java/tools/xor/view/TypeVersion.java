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

import javax.xml.bind.annotation.XmlAttribute;


/*
 * Used to define the API version validity of a type. If a type does
 * not have a TypeVersion entry in the AggregateManager, then that type
 * is valid across all versions
 * 
 * If maxVersion < minVersion then that type is currently valid
 * If maxVersion >= minVersion, then the type is no longer supported on 
 *   a API version > maxVersion
 */
public class TypeVersion {
	
	// The API version can never be smaller than this value
	public static final int MIN_VERSION_VALUE = 1; 
	
	@XmlAttribute
	private String viewName;
	
	@XmlAttribute
	private int minVersion;
	
	@XmlAttribute
    private int maxVersion;
    
	public String getViewName() {
		return viewName;
	}
	public int getMinVersion() {
		return minVersion;
	}
	public int getMaxVersion() {
		return maxVersion;
	}
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}
	public void setMinVersion(int minVersion) {
		this.minVersion = minVersion;
	}
	public void setMaxVersion(int maxVersion) {
		this.maxVersion = maxVersion;
	}	
}
