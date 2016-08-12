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
import tools.xor.util.ClassUtil;

public class CloneOperation extends AbstractOperation {
	
	private Object result;
	
	@Override
	protected void processCollection(CallInfo callInfo) throws Exception {

		if(callInfo.isDataType())
			return;

		CallInfo next = new CallInfo();
		for (Object nextSource : ((BusinessObject)callInfo.getInput()).getList()) {
			next.init(nextSource, null, callInfo, null);
			if(next.isCascadable()) {
				next.setOutput(getExistingTarget(next));
				next.setOutput(createTarget(next, null));
				processAttribute(next);				
			} else
				next.setOutput(createTarget(next, ClassUtil.getInstance(nextSource), null));
		}
		cloneToMany(callInfo);
	}

	@Override
	protected void setResult(Object target) {
		this.result = target;
	}	
	
	@Override
	public Object getResult() {
		return result;
	}
	
}
