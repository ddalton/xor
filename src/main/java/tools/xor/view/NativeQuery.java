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

import java.util.ArrayList;
import java.util.List;

public class NativeQuery {

	protected List<String>      resultList;
	protected String            queryString;
	protected String            identifierClause;
	protected boolean           usable;
	
	public String getIdentifierClause() {
		return identifierClause;
	}

	public void setIdentifierClause(String identifier) {
		this.identifierClause = identifier;
	}	
	
	public boolean isUsable() {
		return usable;
	}
	
	public void setUsable(boolean usable) {
		this.usable = usable;
	}
	
	public List<String> getResultList() {
		return resultList;
	}
	public void setResultList(List<String> attributeList) {
		this.resultList = attributeList;
	}
	public String getQueryString() {
		return queryString;
	}
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	} 
	
	public void expand(AggregateView view) {
		if(getResultList() == null) {
			throw new IllegalStateException("ResultList need to be provided for the native query. TODO: make the view attributeList the same as resultList as default");
		}
		this.resultList = view.getExpandedList(getResultList());
	}
	
	public NativeQuery copy() {
		NativeQuery result = new NativeQuery();
		result.resultList = new ArrayList<String>(resultList);
		result.queryString = queryString;
		result.identifierClause = identifierClause;
		result.usable = usable;
		
		return result;
	}

	/**
	 * The starting position is 1
	 * @param value of path
	 * @return position
	 */
	public int getPosition(String value) {
		int result = -1;
		
		for(int i = 0; i < resultList.size(); i++) {
			if(resultList.get(i).equals(value)) {
				result = i;
				break;
			}
		}
		
		return result;
	}
}