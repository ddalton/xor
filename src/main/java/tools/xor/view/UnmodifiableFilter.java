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

import java.util.Map;

public class UnmodifiableFilter extends Filter
{
    private Filter filter;

    public UnmodifiableFilter (Filter filter)
    {
        this.filter = filter;
    }

    public Filter narrow() {
        return new Filter(filter.expression, filter.position);
    }

    public boolean isOrderBy() {
        return filter.isOrderBy();
    }

    private void raiseException ()
    {
        throw new UnsupportedOperationException(
            "Changes are not allowed on the filter, make a copy of the view to make necessary changes.");
    }

    public String getQueryString ()
    {
        return filter.getQueryString();
    }

    public String getNormalizedName ()
    {
        return filter.getNormalizedName();
    }

    public String getExpression ()
    {
        return filter.getExpression();
    }

    public void setExpression (String expression)
    {
        raiseException();
    }

    public String getAttribute ()
    {
        return filter.getAttribute();
    }

    public void normalize (Map<String, String> normalizedNames)
    {
        filter.normalize(normalizedNames);
    }

    public Object getNormalizedValue (Object object)
    {
        return filter.getNormalizedValue(object);
    }

    public boolean isFilterIncluded (Map<String, Object> originalFilters,
                                     Map<String, Object> filters,
                                     Map<String, Parameter> parameterMap)
    {
        return filter.isFilterIncluded(originalFilters, filters, parameterMap);
    }

    @Override
    public int compareTo (Filter o)
    {
        return filter.compareTo(o);
    }
}
