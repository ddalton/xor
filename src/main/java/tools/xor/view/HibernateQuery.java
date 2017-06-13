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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class HibernateQuery extends AbstractQuery {

	private org.hibernate.Query hibQuery;

	public HibernateQuery(org.hibernate.Query hibQuery) {
		this.hibQuery = hibQuery;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getResultList(View view) {
		return hibQuery.list();
	}

	@Override
	public Object getSingleResult(View view) {
		return hibQuery.uniqueResult();
	}

	@Override
	public void setParameter(String name, Object value) {
			hibQuery.setParameter(name, value);
	}

	@Override
	public boolean hasParameter(String name) {
		return (new HashSet<String>(Arrays.asList(hibQuery.getNamedParameters()))).contains(name);
	}

	@Override public Object execute (AggregateAction action)
	{
		if(action != AggregateAction.READ) {
			return hibQuery.executeUpdate();
		} else {
			return getResultList(null);
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

}
