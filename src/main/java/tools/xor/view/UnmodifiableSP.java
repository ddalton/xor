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

import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import tools.xor.AggregateAction;

public class UnmodifiableSP extends StoredProcedure
{
    private final StoredProcedure sp;

    public UnmodifiableSP (StoredProcedure sp)
    {
        this.sp = sp;
    }

    private void raiseException ()
    {
        throw new UnsupportedOperationException(
            "Changes are not allowed on the StoredProcedure, make a copy of the view to make necessary changes.");
    }

    @Override public String getName ()
    {
        return sp.getName();
    }

    @Override public void setName (String name)
    {
        raiseException();
    }

    @Override public String getCallString ()
    {
        return sp.getCallString();
    }

    @Override public void setCallString (String sql)
    {
        raiseException();
    }

    @Override public AggregateAction getAction ()
    {
        return sp.getAction();
    }

    @Override public void setAction (AggregateAction action)
    {
        raiseException();
    }

    @Override public List<BindParameter> getParameterList ()
    {
        return sp.getParameterList();
    }

    @Override public void setParameterList (List<BindParameter> parameterList)
    {
        raiseException();
    }

    @Override public List<OutputLocation> getOutputLocation ()
    {
        return sp.getOutputLocation();
    }

    @Override public void setOutputLocation (List<OutputLocation> outputLocation)
    {
        raiseException();
    }

    @Override public List<String> getAugmenter ()
    {
        return sp.getAugmenter() == null ? null : Collections.unmodifiableList(sp.getAugmenter());
    }

    @Override public void setAugmenter (List<String> resultList)
    {
        raiseException();
    }

    @Override public String getMaxResults ()
    {
        return sp.getMaxResults();
    }

    @Override public Statement getStatement ()
    {
        return sp.getStatement();
    }

    @Override public void setStatement (Statement statement)
    {
        raiseException();
    }

    @Override public StoredProcedure copy ()
    {
        return sp.copy();
    }

    /**
     * Creates a JDBC stored procedure string.
     * e.g.,
     * "{call GET_SP(?, ?)}"
     *
     * @return SP string
     */
    @Override public String jdbcCallString ()
    {
        return sp.jdbcCallString();
    }

    @Override public boolean isImplicit ()
    {
        return sp.isImplicit();
    }

    @Override public void setImplicit (boolean implicit)
    {
        raiseException();
    }

    @Override public boolean isMultiple ()
    {
        return sp.isMultiple();
    }

    @Override public void setMultiple (boolean multiple)
    {
        raiseException();

    }
}
