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

public class UnmodifiableParam extends Parameter
{

    private Parameter parameter;

    public UnmodifiableParam (Parameter parameter)
    {
        this.parameter = parameter;
    }

    private void raiseException ()
    {
        throw new UnsupportedOperationException(
            "Changes are not allowed on the parameter, make a copy of the view to make necessary changes.");
    }

    @Override
    public String getName ()
    {
        return parameter.getName();
    }

    @Override public void setName (String name)
    {
        raiseException();
    }

    @Override public String getFilterName ()
    {
        return parameter.getFilterName();
    }

    @Override public void setFilterName (String filterName)
    {
        raiseException();
    }

    @Override public Object getDefaultValue ()
    {
        return parameter.getDefaultValue();
    }

    @Override public void setDefaultValue (Object defaultValue)
    {
        raiseException();
    }

    @Override public String getExpression ()
    {
        return parameter.getExpression();
    }

    @Override public void setExpression (String expression)
    {
        raiseException();
    }

}
