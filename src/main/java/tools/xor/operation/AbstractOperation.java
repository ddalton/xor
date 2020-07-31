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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.ClassUtil;
import tools.xor.view.Query;
import tools.xor.view.QueryFragment;

public abstract class AbstractOperation implements Operation {
	@Override
	public void execute(Settings settings) {
		throw new UnsupportedOperationException("This method is not supported for this operation. Use the execute method that takes a CallInfo object as input.");
	}
	
	@Override
	public BusinessObject createTarget(CallInfo ci, Type domainType) {	
		return createTarget(ci, null, domainType);
	}

	@Override
	public BusinessObject createTarget(CallInfo ci, Object targetInstance, Type domainType) {
		targetInstance = ClassUtil.getInstance(targetInstance);
		if(targetInstance == null && ci.isBulkInput()) {
			targetInstance = new LinkedList<>();
		}
		BusinessObject target = ci.getOutputObjectCreator().createTarget(ci, targetInstance, domainType);

		return target;
	}	

	@Override
	public boolean isNonContainmentRelationship (CallInfo ci) {
		boolean result = ci.isExternal();

		return result;
	}
	
	@Override
	public Object getDomain(CallInfo ci) {
		return ci.getOutput();
	}
	
	@Override
	public Object getExternal(CallInfo ci) {
		return ci.getInput();
	}	
	
	@Override
	public BusinessObject getDomainParent(CallInfo ci) {
		return ci.getParentOutputEntity();
	}
	
	@Override
	public BusinessObject getExternalParent(CallInfo ci) {
		return ci.getParentInputEntity();
	}

	public void preProcess(Settings settings, Query query, boolean isRoot)
	{
		Map<String, Object> params = new HashMap<>();
		if(settings.getParams() != null) {
			params.putAll(settings.getParams());
		}

		for (Map.Entry<String, Object> entry : params.entrySet()) {
			if (query.hasParameter(entry.getKey())) {
				query.setParameter(entry.getKey(), entry.getValue());
			}
		}

		// Set the chunk values
		Map<String, Object> nextToken = settings.getNextToken();
		if (nextToken != null) {
			for (Map.Entry<String, Object> entry : nextToken.entrySet()) {
				if (!query.hasParameter(QueryFragment.NEXTTOKEN_PARAM_PREFIX + entry.getKey())) {
					throw new IllegalStateException(
						"NextToken missing information for orderBy field: " + entry.getKey());
				}
				query.setParameter(
					QueryFragment.NEXTTOKEN_PARAM_PREFIX + entry.getKey(),
					entry.getValue());
			}
		}

		// pagination
		if (settings.getOffset() != null)
			query.setFirstResult(settings.getOffset());
		if (settings.getLimit() != null && isRoot)
			query.setMaxResults(settings.getLimit());
	}
}
