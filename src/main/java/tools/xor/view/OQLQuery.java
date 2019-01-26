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

import java.util.ArrayList;
import java.util.List;

public class OQLQuery
{
    protected List<String> resultList; // Needs to be in dotten notation
    protected String selectClause;
    protected List<Function> function; // should only be of type FREESTYLE


    public List<String> getResultList ()
    {
        return resultList;
    }

    public void setResultList (List<String> attributeList)
    {
        this.resultList = attributeList;
    }

    public String getSelectClause ()
    {
        return selectClause;
    }

    public void setSelectClause (String selectClause)
    {
        this.selectClause = selectClause;
    }

    public List<Function> getFunction ()
    {
        return this.function;
    }

    public void setFunction (List<Function> function)
    {
        this.function = function;
    }

    public OQLQuery copy ()
    {
        OQLQuery result = new OQLQuery();
        result.resultList = new ArrayList<>(resultList);
        result.selectClause = selectClause;
        result.function = new ArrayList<>();
        for(Function function: this.function) {
            result.function.add(function.copy());
        }

        return result;
    }

    public OQLQuery generateQuery (AggregateManager am, QueryTree queryTree, QueryPiece queryPiece)
    {

        am.setPersistenceOrchestrator(am.getDasFactory().getPersistenceOrchestrator(null));

        Settings settings = new Settings();
        am.checkPO(settings);

        QueryBuilder qb = new QueryBuilder(queryTree);
        qb.construct(settings, queryPiece);

        this.selectClause = queryPiece.getQueryString();

        return this;
    }
}
