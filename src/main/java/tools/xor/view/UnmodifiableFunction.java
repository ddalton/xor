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

public class UnmodifiableFunction extends Function
{
    private Function function;

    public UnmodifiableFunction (Function function)
    {
        this.function = function;
    }

    public Function copy() {
        return new Function(function);
    }

    public boolean isOrderBy() {
        return function.isOrderBy();
    }

    public String getQueryString ()
    {
        return function.getQueryString();
    }

    public String getNormalizedName ()
    {
        return function.getNormalizedName();
    }

    public String getAttribute ()
    {
        return function.getAttribute();
    }

    public int getPositionalParamCount() {
        return function.getPositionalParamCount();
    }

    @Override
    public int compareTo (Function o)
    {
        return function.compareTo(o);
    }

    public boolean isRelevant() {
        return function.isRelevant();
    }
}
