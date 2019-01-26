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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UnmodifiableNativeQuery extends NativeQuery
{

    private NativeQuery query;

    public UnmodifiableNativeQuery (NativeQuery query)
    {
        this.query = query;
    }

    private void raiseException ()
    {
        throw new UnsupportedOperationException(
            "Changes are not allowed on the NativeQuery, make a copy of the view to make necessary changes.");
    }

    public boolean isUsable ()
    {
        return query.isUsable();
    }

    public void setUsable (boolean usable)
    {
        raiseException();
    }

    public List<String> getResultList ()
    {
        return Collections.unmodifiableList(query.getResultList());
    }

    public void setResultList (List<String> attributeList)
    {
        raiseException();
    }

    public String getSelectClause ()
    {
        return query.getSelectClause();
    }

    public void setSelectClause (String queryString)
    {
        raiseException();
    }

    public NativeQuery copy ()
    {
        return new UnmodifiableNativeQuery(query.copy());
    }

    /**
     * The starting position is 1
     *
     * @param value of path
     * @return position
     */
    public int getPosition (String value)
    {
        return query.getPosition(value);
    }

    public List<Function> getFunction ()
    {
        List<Function> result = new LinkedList<>();
        for(Function func: query.getFunction()) {
            result.add(func.copy());
        }
        return result;
    }

    public void setFunction(List<Function> function) {
        raiseException();
    }

    public List<BindParameter> getParameterList ()
    {
        List<BindParameter> result = new LinkedList<>();
        for(BindParameter param: query.getParameterList()) {
            result.add(param.copy());
        }
        return result;
    }

    public void setParameterList (List<BindParameter> parameterList)
    {
        raiseException();
    }
}
