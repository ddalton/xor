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

public abstract class AbstractElementAction implements ElementAction {

	protected final PropertyKey key;
	protected final BusinessObject collectionElement;	
	protected final BusinessObject collectionOwner;
	protected final Executable triggeringAction;
	protected final Object position;
	protected final ObjectCreator inputObjectCreator;

	public AbstractElementAction(PropertyKey key, BusinessObject collectionElement, BusinessObject collectionOwner, Executable triggeringAction, Object position, ObjectCreator oc) {
		this.key = key;
		this.collectionElement = collectionElement;
		this.collectionOwner = collectionOwner;
		this.triggeringAction = triggeringAction;
		this.position = position;
		this.inputObjectCreator = oc;
	}

	@Override
	public void execute() {

	}	

	@Override
	public PropertyKey getKey() {
		return key;
	}

	@Override
	public BusinessObject getCollectionElement() {
		return collectionElement;
	}
	
	@Override
	public BusinessObject getCollectionOwner() {
		return this.collectionOwner;
	}

	@Override
	public Executable getTriggeringAction() {
		return this.triggeringAction;
	}

	@Override
	public Object getPosition() {
		return this.position;
	}

	@Override
	public ObjectCreator getInputObjectCreator() {
		return this.inputObjectCreator;
	}			
}
