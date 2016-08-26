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

package tools.xor.operation;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;


public interface Operation {
	/**
	 * The logic in the copying of data from the input to the output objects
	 * @param callInfo object
	 */
	public void execute(CallInfo callInfo);

	/**
	 * Allows operation specific behavior in the creation of DataObjects
	 * @param ci CallInfo object
	 * @param desiredClass of target object
	 * @return BusinessObject
	 */
	public BusinessObject createTarget(CallInfo ci, Class<?> desiredClass);

	/**
	 * Allows a new DataObject to be created based on an existing target instance object
	 * @param ci CallInfo object
	 * @param targetInstance object
	 * @param desiredClass of target object
	 * @return BusinessObject
	 */
	public BusinessObject createTarget(CallInfo ci, Object targetInstance, Class<?> desiredClass);
	
	/**
	 * Returns the result of the execution if any.
	 * @return result object
	 */
	public Object getResult();

	/**
	 * Indicates if the specific association is not part of this aggregate
	 * @param ci CallInfo object
	 * @return true if external association
	 */
	boolean isExternalAssociationLink(CallInfo ci);
}
