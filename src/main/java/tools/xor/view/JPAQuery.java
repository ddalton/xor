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

package tools.xor.view;

import tools.xor.AggregateAction;
import tools.xor.Settings;

import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JPAQuery extends AbstractQuery {
	
	private javax.persistence.Query jpaQuery;
	private NativeQuery nativeQuery;
	private Map<String, Object> paramValues = new HashMap<>();

	public JPAQuery(String queryString, javax.persistence.Query jpaQuery) {
		this(queryString, jpaQuery, null);
	}

	private boolean isNativeQuery() {
		return this.nativeQuery != null;
	}

	public JPAQuery(String queryString, javax.persistence.Query jpaQuery, NativeQuery nativeQuery) {
		super(queryString);
		this.jpaQuery = jpaQuery;
		this.nativeQuery = nativeQuery;

		if(isNativeQuery()) {
			initParamMap();
		}
	}

	public void setProviderQuery(javax.persistence.Query jpaQuery) {
		this.jpaQuery = jpaQuery;
	}

	private void initParamMap() {
		QueryStringHelper.initParamMap(paramMap, nativeQuery.getParameterList());
	}

	@Override
	public void updateParamMap (List<BindParameter> relevantParams) {
		QueryStringHelper.initParamMap(paramMap, relevantParams);
	}

	@Override public boolean isOQL ()
	{
		return !isNativeQuery();
	}

	@Override public boolean isSQL ()
	{
		return isNativeQuery();
	}

	@Override
	protected void setBindParameter(int position, Object value) {
		jpaQuery.setParameter(position, value);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List getResultListInternal(View view, Settings settings) {
		if(isNativeQuery()) {
			setParameters(settings, paramMap, paramValues);
		}

		return jpaQuery.getResultList();
	}

	@Override
	public Object getSingleResult(View view, Settings settings) {
		return jpaQuery.getSingleResult();
	}

	@Override
	public void setParameter(String name, Object value) {
		if(isNativeQuery()) {
			if(paramMap.containsKey(name)) {
				paramValues.put(name, value);
			}
		} else {
			if (hasParameter(name)) {
				jpaQuery.setParameter(name, value);
			} else if (paramMap.containsKey(name)) { // Needed for deferred queries
				jpaQuery.setParameter(paramMap.get(name).position, value);
			}
		}
	}

	@Override
	public boolean hasParameter(String name) {

		if(isNativeQuery()) {
			return paramMap.containsKey(name);
		} else {
			Set<String> paramNames = new HashSet<String>();

			Iterator<javax.persistence.Parameter<?>> iter = jpaQuery.getParameters().iterator();
			while (iter.hasNext()) {
				paramNames.add(iter.next().getName());
			}

			return paramNames.contains(name);
		}
	}

	@Override public Object execute (Settings settings)
	{
		if(settings.getAction() != AggregateAction.READ) {
			if(isNativeQuery()) {
				setParameters(settings, paramMap, paramValues);
			}
			return jpaQuery.executeUpdate();
		} else {
			return getResultList(null, settings);
		}
	}

	@Override
	public void setMaxResults(int limit) {
		jpaQuery.setMaxResults(limit);
	}

	@Override
	public void setFirstResult(int offset) {
		jpaQuery.setFirstResult(offset);
	}

	public boolean isDeferred() {
		return jpaQuery == null;
	}
}
