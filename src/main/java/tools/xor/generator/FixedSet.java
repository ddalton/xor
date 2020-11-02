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

package tools.xor.generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import tools.xor.Property;
import tools.xor.util.graph.StateGraph;

public class FixedSet extends DefaultGenerator
{
    public FixedSet (String[] arguments)
    {
        super(arguments);
    }

    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[visitor.getSequenceNo()]).byteValue();
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[visitor.getSequenceNo()]).shortValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getIntValue(visitor).intValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Integer.valueOf(getValues()[visitor.getSequenceNo()]).intValue();
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[visitor.getSequenceNo()]).longValue();
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(getLongValue(visitor));
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[visitor.getSequenceNo()]);
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[visitor.getSequenceNo()]).floatValue();
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(new Long(getLongValue(visitor)).toString());
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigInteger(new Long(getLongValue(visitor)).toString());
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        // Find the locale (e.g., en, ja etc)
        return getValues()[visitor.getSequenceNo()];
    }
}
