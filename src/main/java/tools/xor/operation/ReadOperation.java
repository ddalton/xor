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
import tools.xor.ProcessingStage;

public class ReadOperation extends AbstractOperation {
	
	private Object result;

	@Override
	protected void processPostLogic(CallInfo callInfo) throws Exception {
		// Invariants do not need to be run since this operation does not modify data

		if(callInfo.getStage() == ProcessingStage.CREATE) {
			handleOpenProperty(callInfo);
		}
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
	protected boolean supportsStage(ProcessingStage stage, CallInfo callInfo) {
		// Immutable processing is done in POSTLOGIC stage
		if(stage == ProcessingStage.POSTLOGIC && !callInfo.getOutputObjectCreator().getTypeMapper().immutable()) {
			return false;
		}
		
		return true;
	}	
}
