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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class JPAQuery extends AbstractQuery {
	
	private javax.persistence.Query jpaQuery;

	public JPAQuery(javax.persistence.Query jpaQuery) {
		this.jpaQuery = jpaQuery;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getResultList(QueryView queryView) {
		return jpaQuery.getResultList();
	}

	@Override
	public Object getSingleResult(QueryView queryView) {
		return jpaQuery.getSingleResult();
	}

	@Override
	public void setParameter(String name, Object value) {
		jpaQuery.setParameter(name, value);
	}

	@Override
	public boolean hasParameter(String name) {
		Set<String> paramNames = new HashSet<String>();
		
		Iterator<javax.persistence.Parameter<?>> iter = jpaQuery.getParameters().iterator();
		while(iter.hasNext())
			paramNames.add(iter.next().getName());
		
		return paramNames.contains(name);
	}

	@Override
	public void setMaxResults(int limit) {
		jpaQuery.setMaxResults(limit);
	}

	@Override
	public void setFirstResult(int offset) {
		jpaQuery.setFirstResult(offset);
	}	
}
