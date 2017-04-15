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

import java.util.Collections;
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

    public String getIdentifierClause ()
    {
        return query.getIdentifierClause();
    }

    public void setIdentifierClause (String identifier)
    {
        raiseException();
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

    public String getQueryString ()
    {
        return query.getQueryString();
    }

    public void setQueryString (String queryString)
    {
        raiseException();
    }

    public void expand (AggregateView view)
    {
        query.expand(view);
    }

    public NativeQuery copy ()
    {
        return query.copy();
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
}
