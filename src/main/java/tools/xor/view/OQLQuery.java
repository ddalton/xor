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

import tools.xor.Settings;
import tools.xor.service.AggregateManager;

public class OQLQuery extends QuerySupport
{
    protected String selectClause;
    protected List<Function> function; // should only be of type FREESTYLE
    protected List<String> primaryKey; // Needed for join with parent query

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

    public List<String> getPrimaryKey() {
        return this.primaryKey;
    }

    public void setPrimaryKey(List<String> primaryKey) {
        this.primaryKey = primaryKey;
    }

    public OQLQuery copy ()
    {
        OQLQuery result = new OQLQuery();
        super.copy(result);
        result.selectClause = selectClause;

        if(this.function != null) {
            result.function = new ArrayList<>();
            for (Function function : this.function) {
                result.function.add(function.copy());
            }
        }

        if(this.primaryKey != null) {
            result.primaryKey = new ArrayList<>();
            for (String keyPart : this.primaryKey) {
                result.primaryKey.add(keyPart);
            }
        }

        return result;
    }

    public OQLQuery generateQuery (AggregateManager am, AggregateTree aggregateTree, QueryTree queryTree)
    {

        am.setDataStore(am.getDataModelFactory().createDataStore(null));

        Settings settings = new Settings();
        am.configure(settings);

        QueryBuilder qb = new QueryBuilder(aggregateTree);
        qb.construct(settings, queryTree);

        this.selectClause = queryTree.getSelectString();

        return this;
    }
}
