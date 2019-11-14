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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernateQuery extends AbstractQuery {

	private org.hibernate.Query hibQuery;
	private NativeQuery nativeQuery;
	private Map<String, Object> paramValues = new HashMap<>();
	private Set<String> namedParams;

	public HibernateQuery(String queryString, org.hibernate.Query hibQuery) {
		this(queryString, hibQuery, null);
	}

	public HibernateQuery(String queryString, org.hibernate.Query hibQuery, NativeQuery nativeQuery) {
		super(queryString);
		this.hibQuery = hibQuery;
		this.nativeQuery = nativeQuery;

		if(isNativeQuery()) {
			initParamMap();
		}
	}

	@Override public boolean isOQL ()
	{
		return !isNativeQuery();
	}

	@Override public boolean isSQL ()
	{
		return isNativeQuery();
	}

	public void setProviderQuery(org.hibernate.Query hibQuery) {
		this.hibQuery = hibQuery;
	}

	private boolean isNativeQuery() {
		return this.nativeQuery != null;
	}

	private void initParamMap() {
		QueryStringHelper.initPositionalParamMap(positionByName, nativeQuery.getParameterList());
	}

	@Override
	public void updateParamMap (List<BindParameter> relevantParams) {
		QueryStringHelper.initPositionalParamMap(positionByName, relevantParams);
	}

	@Override
	protected List getResultListInternal(View view, Settings settings) {
		if (isNativeQuery()) {
			setParameters(settings, paramValues);
		}

		return hibQuery.list();
	}

	@Override
	public Object getSingleResult(View view, Settings settings) {
		return hibQuery.uniqueResult();
	}

	@Override
	public void setParameter(String name, Object value) {
		if(hasNamedParameter(name)) {
			hibQuery.setParameter(name, value);
		}

		if(isNativeQuery()) {
			if(positionByName.containsKey(name)) {
				paramValues.put(name, value);
			}
		} else if (positionByName.containsKey(name)) { // Needed for deferred queries
			// There can be multiple references to the same parameter in a query
			for(BindParameter p: positionByName.get(name)) {
				// Hibernate indexes this from 0
				hibQuery.setParameter(p.position-1, value);
			}
		}
	}

	private boolean hasNamedParameter(String name) {
		if(namedParams == null) {
			namedParams = new HashSet<>(Arrays.asList(hibQuery.getNamedParameters()));
		}
		if(namedParams.contains(name)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean hasParameter(String name) {

		if(hasNamedParameter(name)) {
			return true;
		}

		if(isNativeQuery()) {
			return positionByName.containsKey(name);
		}

		return false;
	}

	@Override
	protected void setBindParameter(int position, Object value) {
		// Hibernate indexes this from 0
		hibQuery.setParameter(position-1, value);
	}

	@Override public Object execute (Settings settings)
	{
		if(settings.getAction() != AggregateAction.READ) {
			if(isNativeQuery()) {
				setParameters(settings, paramValues);
			}
			return hibQuery.executeUpdate();
		} else {
			return getResultList(null, settings);
		}
	}

	@Override
	public void setMaxResults(int limit) {
		hibQuery.setMaxResults(limit);
	}

	@Override
	public void setFirstResult(int offset) {
		hibQuery.setFirstResult(offset);
	}

	public boolean isDeferred() {
		return hibQuery == null;
	}
}
