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

package tools.xor.action;

import tools.xor.BusinessObject;
import tools.xor.util.ObjectCreator;

public final class RemoveElementAction extends AbstractElementAction {

	public RemoveElementAction(PropertyKey key, BusinessObject collectionElement, BusinessObject collectionOwner, Executable triggeringAction, Object position, ObjectCreator oc) {
		super(key, collectionElement, collectionOwner, triggeringAction, position, oc);
	}

	@Override
	public void execute() {
		// The actual execution is done in the appropriate <collection>UpdateAction class
	}	
}
