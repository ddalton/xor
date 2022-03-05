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

package tools.xor.service;

import tools.xor.util.IntraQuery;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;

public interface QueryCapability {
	   
    /**
     * Returns the mechanism for returning the actual type of a subclassable type
	 * @param queryAlias of the type in a query 
	 * @return type mechanism value
	 */
    public String getTypeMechanism(String queryAlias);

    /**
     * Returns the mechanism used to obtain the key of a map attribute
	 * @param queryAlias of the type in a query 
	 * @return key mechanism value
     */
    public String getMapKeyMechanism(String queryAlias);
    
    /**
     * Returns the mechanism used to obtain the value of a map 
	 * @param queryAlias of the type in a query 
	 * @return value mechanism value
     */
    public String getMapValueMechanism(String queryAlias);
    
    /**
     * Returns the mechanism used to obtain the index value of a list
	 * @param queryAlias of the type in a query 
	 * @return index mechanism value
     */
	public String getListIndexMechanism(String queryAlias);

    /**
     * Returns the mechanism used to obtain the surrogate key value
     * @param queryAlias of the type in a query
     * @param idFragment the name of the surrogate key prepended by the path delimiter
     * @return surrogate mechanism value
     */
    public String getSurrogateValueMechanism(String queryAlias, String idFragment);

    /**
     * Gets the downcast clause supported by the OQL
     * @param queryTree to which the joinedge belongs
     * @param joinEdge representing the inheritance edge to the child
     * @return downcast OQL clause
     */
    String getDowncastClause (QueryTree queryTree, IntraQuery<QueryFragment> joinEdge);
}
