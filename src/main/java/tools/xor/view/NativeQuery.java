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

import tools.xor.FunctionType;

public class NativeQuery extends QuerySupport
{
    // For positional parameters describes the ordering of the parameters
    // For named parameters helps to identify the type of the parameters
    // should not mix positional and named parameters
    protected List<BindParameter> parameterList;
    protected String selectClause;
    protected boolean usable;
    protected List<Function> function; // should only be of type FREESTYLE
    protected List<String> primaryKey; // Needed for join with parent query

    public boolean isUsable ()
    {
        return usable;
    }

    public void setUsable (boolean usable)
    {
        this.usable = usable;
    }

    public String getSelectClause ()
    {
        return selectClause;
    }

    public void setSelectClause (String queryString)
    {
        this.selectClause = queryString;
    }

    public List<Function> getFunction ()
    {
        return this.function;
    }

    public void setFunction(List<Function> function) {
        this.function = function;
    }

    public List<String> getPrimaryKey() {
        return this.primaryKey;
    }

    public void setPrimaryKey(List<String> primaryKey) {
        this.primaryKey = primaryKey;
    }

    public List<BindParameter> getParameterList ()
    {
        return parameterList;
    }

    public void setParameterList (List<BindParameter> parameterList)
    {
        this.parameterList = parameterList;
    }

    public NativeQuery copy ()
    {
        NativeQuery result = new NativeQuery();
        super.copy(result);
        result.selectClause = selectClause;
        result.usable = usable;

        if(this.parameterList != null) {
            result.parameterList = new ArrayList<>();
            for (BindParameter bind : parameterList) {
                result.parameterList.add(bind.copy());
            }
        }

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

    public boolean contains(String substring) {
        if(selectClause.contains(substring)) {
            return true;
        }

        for(Function f: function) {
            if(f.type == FunctionType.FREESTYLE) {
                for(String arg: f.args) {
                    if(arg.contains(substring)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
