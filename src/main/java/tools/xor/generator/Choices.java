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

import tools.xor.util.graph.StateGraph;

/**
 * We treat the input as a Double to be aligned with POI library numeric value of a cell
 * being returned as a double type.
 */
public class Choices extends DefaultGenerator
{
    public Choices (String[] arguments)
    {
        super(arguments);
    }

    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[getPosition()]).byteValue();
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[getPosition()]).shortValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getIntValue(visitor).intValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Integer.valueOf(getValues()[getPosition()]).intValue();
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[getPosition()]).longValue();
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(getLongValue(visitor));
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[getPosition()]);
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValues()[getPosition()]).floatValue();
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
}
