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
import tools.xor.Settings;
import tools.xor.Type;

public interface Operation {
	/**
	 * The logic in the copying of data from the input to the output objects
	 * Use QueryView to power the queries
	 *
	 * @param callInfo object
	 */
	public void execute(CallInfo callInfo);

	/**
	 * Use this form of execute when data does not need to be transformed between
	 * external and domain models. Used when executing DML queries.
	 * @param settings object
	 */
	public void execute(Settings settings);

	/**
	 * Allows operation specific behavior in the creation of DataObjects
	 * @param ci CallInfo object
	 * @param domainType domain type from which either the domain/external type can be derived
	 * @return BusinessObject
	 */
	public BusinessObject createTarget(CallInfo ci, Type domainType);	

	/**
	 * Allows a new DataObject to be created based on an existing target instance object
	 * @param ci CallInfo object
	 * @param targetInstance object
	 * @param domainType provide the type meta data that helps to resolve the correct Entity type (External/domain) for the business
	 *        object being created
	 * @return BusinessObject
	 */
	public BusinessObject createTarget(CallInfo ci, Object targetInstance, Type domainType);
	
	/**
	 * Returns the result of the execution if any.
	 * @return result object
	 */
	public Object getResult();

	/**
	 * Indicates if the specific association is not part of this aggregate and is not an
	 * ownership association within the aggregate
	 * @param ci CallInfo object
	 * @return true if external association
	 */
	boolean isNonContainmentRelationship (CallInfo ci);
	
	/**
	 * Get the domain object in this operation for the current call stack
	 * @param ci call stack object
	 * @return domain object
	 */
	public Object getDomain(CallInfo ci);

	/**
	 * Get the external object in this operation for the current call stack
	 * @param ci call stack object
	 * @return external object
	 */	
	public Object getExternal(CallInfo ci);
	
	/**
	 * Get the parent domain object in this operation for the current call stack
	 * @param ci call stack object
	 * @return parent domain object
	 */
	public BusinessObject getDomainParent(CallInfo ci);
	
	/**
	 * Get the parent external object in this operation for the current call stack
	 * @param ci call stack object
	 * @return parent external object
	 */
	public BusinessObject getExternalParent(CallInfo ci);
}
