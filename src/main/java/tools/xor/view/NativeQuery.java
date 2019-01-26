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

public class NativeQuery
{

    protected List<String> resultList;  // Names
    protected String selectClause;
    protected List<BindParameter> parameterList;
    protected boolean usable;
    protected List<Function> function; // should only be of type FREESTYLE

    public boolean isUsable ()
    {
        return usable;
    }

    public void setUsable (boolean usable)
    {
        this.usable = usable;
    }

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
        result.resultList = new ArrayList<>(resultList);
        result.selectClause = selectClause;
        result.usable = usable;
        result.parameterList = new ArrayList<>();
        for (BindParameter bind : parameterList) {
            result.parameterList.add(bind.copy());
        }
        result.function = new ArrayList<>();
        for(Function function: this.function) {
            result.function.add(function.copy());
        }

        return result;
    }

    /**
     * The starting position is 1
     *
     * @param value of path
     * @return position
     */
    public int getPosition (String value)
    {
        int result = -1;

        for (int i = 0; i < resultList.size(); i++) {
            if (resultList.get(i).equals(value)) {
                result = i;
                break;
            }
        }

        return result;
    }

}
