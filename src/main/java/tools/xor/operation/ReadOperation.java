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
import tools.xor.ExtendedProperty;

public class ReadOperation extends GraphTraversal {
	
	private Object result;
	
	@Override
	public Object getDomain(CallInfo ci) {
		return ci.getInput();
	}
	
	@Override
	public Object getExternal(CallInfo ci) {
		return ci.getOutput();
	}		
	
	@Override
	public BusinessObject getDomainParent(CallInfo ci) {
		return ci.getParentInputEntity();
	}
	
	@Override
	public BusinessObject getExternalParent(CallInfo ci) {
		return ci.getParentOutputEntity();
	}	
	
	@Override
	protected ExtendedProperty getDomainProperty(CallInfo ci) {
		return ci.getInputProperty();
	}	
	
	@Override
	protected boolean isIdentifier(CallInfo ci) {
		// We do not clear the identifier
		return false;
	}

	@Override
	protected void setResult(Object target) {
		this.result = target;
	}	

	@Override
	public Object getResult() {
		return result;
	}	
	
	@Override
	protected boolean supportsCreate(CallInfo callInfo) {
		// Immutable Json variant requires the creation step
		// to get the JSON builders ready
		if(callInfo.getOutputObjectCreator().getTypeMapper().immutable()) {
			return true;
		}
		return false;
	}		
	
	@Override
	protected boolean supportsPostLogic(CallInfo callInfo) {
		// Immutable processing is done in POSTLOGIC stage
		if(callInfo.getOutputObjectCreator().getTypeMapper().immutable()) {
			return true;
		}
		
		return super.supportsPostLogic(callInfo);
	}	
}
