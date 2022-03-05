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

package tools.xor.event;

import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty.Phase;
import tools.xor.ProcessingStage;
import tools.xor.Settings;

public interface PropertyEvent {

	public Settings getSettings();
	
	/**
	 * Gets the domain object
	 * @return an object and not a BusinessObject since simple values are not wrapped in a BusinessObject
	 */
	public Object getDomain ();
	
	public Object getExternal ();	
	
	public BusinessObject getDomainParent ();
	
	public BusinessObject getExternalParent ();	

	// Get the processing stage, is this the first pass (object creation) or the second pass (
	//object update)
	public ProcessingStage getStage();
	
	public String[] getTags();	
	
	public Phase getPhase();	
	
	public Object getValue();
}
