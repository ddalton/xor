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

import tools.xor.Settings;
import tools.xor.service.AggregateManager;

public class OQLQuery {

	protected String            queryString;

	public String getQueryString() {
		return queryString;
	}
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	public OQLQuery generateQuery(AggregateManager am, QueryTree queryTree, QueryPiece queryPiece) {

		am.setPersistenceOrchestrator(am.getDasFactory().getPersistenceOrchestrator(null));

		Settings settings = new Settings();
		am.checkPO(settings);

		QueryBuilder qb = new QueryBuilder(queryTree);
		qb.construct(settings, queryPiece);

		this.queryString = queryPiece.getQueryString();

		return this;
	} 
}
