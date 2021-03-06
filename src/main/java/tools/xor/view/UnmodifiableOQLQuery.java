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

import tools.xor.service.AggregateManager;

public class UnmodifiableOQLQuery extends OQLQuery
{
    private OQLQuery query;

    public UnmodifiableOQLQuery (OQLQuery query)
    {
        this.query = query;
    }

    private void raiseException ()
    {
        throw new UnsupportedOperationException(
            "Changes are not allowed on the OQLQuery, make a copy of the view to make necessary changes.");
    }

    public OQLQuery copy ()
    {
        return new UnmodifiableOQLQuery(query.copy());
    }

    @Override public String getSelectClause ()
    {
        return query.getSelectClause();
    }

    @Override public void setSelectClause (String queryString)
    {
        raiseException();
    }

    @Override public OQLQuery generateQuery (AggregateManager am, AggregateTree aggregateTree, QueryTree queryTree)
    {
        return query.generateQuery(am, aggregateTree, queryTree);
    }
}
