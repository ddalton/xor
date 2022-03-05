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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Parameter;

import tools.xor.AggregateAction;
import tools.xor.Settings;


public class JPAQuery extends AbstractQuery {
	
	private javax.persistence.Query jpaQuery;
	private NativeQuery nativeQuery;
	private Map<String, Object> paramValues = new HashMap<>();
	private Set<String> namedParams;

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
		// We use the parameter list
		QueryStringHelper.initPositionalParamMap(positionByName, nativeQuery.getParameterList());
	}

	@Override
	public void updateParamMap (List<BindParameter> relevantParams) {
		QueryStringHelper.initPositionalParamMap(positionByName, relevantParams);
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
			setParameters(settings, paramValues);
		}

		return jpaQuery.getResultList();
	}

	@Override
	public Object getSingleResult(View view, Settings settings) {
		return jpaQuery.getSingleResult();
	}

	@Override
	public void setParameter(String name, Object value) {
		if(hasNamedParameter(name)) {
			jpaQuery.setParameter(name, value);
		}

		if(isNativeQuery()) {
			if(positionByName.containsKey(name)) {
				paramValues.put(name, value);
			}
		} else if (positionByName.containsKey(name)) { // Needed for deferred queries
			// There can be multiple references to the same parameter in a query
			for(BindParameter p: positionByName.get(name)) {
				jpaQuery.setParameter(p.position, value);
			}
		}
	}

	private boolean hasNamedParameter(String name) {
		if(namedParams == null) {
			namedParams = new HashSet<>();
			for(Parameter p: jpaQuery.getParameters()) {
				namedParams.add(p.getName());
			}
		}
		if(namedParams.contains(name)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean hasParameter(String name) {

		// If the query has named parameters then the provider is able to
		// extract it. So we need to check the provider first.
		// If positional parameters are used, then the user needs to provide the
		// the name to position mapping
		// Ideally it is best to use named parameters everywhere as we can conditionally
		// build the query

		if(hasNamedParameter(name)) {
			return true;
		}

		if(isNativeQuery()) {
			return positionByName.containsKey(name);
		}

		return false;
	}

	@Override public Object execute (Settings settings)
	{
		if(settings.getAction() != AggregateAction.READ) {
			if(isNativeQuery()) {
				setParameters(settings, paramValues);
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
