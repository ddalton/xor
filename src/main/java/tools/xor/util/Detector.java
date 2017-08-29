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

package tools.xor.util;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;

public interface Detector {

	/**
	 * Notify the detector that a new domain instance has been created.
	 * @param source details on the source instance
	 * @param createdBO can be a newly createdBO or an existingBO for an instance with the same key.
	 * @param createdInstance domain instance created by the creation strategy
	 */
	public void notifyCreate(Object source, BusinessObject createdBO, Object createdInstance);

	/**
	 * Invoked as soon as a data value is set from the XOR operation
	 * @param ci details of the call
	 */
	public void performCall(CallInfo ci);

	/**
	 * Perform detection on the given object.
	 * The Detector implementation is usually an inner class of the object, so it has access
	 * to the internal data.
	 *
	 * @param object being investigated
	 */
	public void investigate(Object object);

}
