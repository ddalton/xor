/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

package tools.xor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

public class SharedCounterGenerator extends DefaultGenerator implements Iterator
{
    private AtomicLong id;
    private long value;

    public SharedCounterGenerator(AtomicLong id) {
        super(new String[]{});

        this.id = id;
        this.value = id.get();
    }
    
    public SharedCounterGenerator(String[] args) {
        super(args);
    }
    
    public void setId(AtomicLong id) {
        this.id = id;
    }

    @Override public boolean hasNext ()
    {
        return true;
    }

    @Override public Object next ()
    {
        return value = id.getAndIncrement();
    }
    
    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (byte) this.value;
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (short) this.value;
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) this.value;
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (int) this.value;
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return this.value;
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(this.value);
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (double) this.value;
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (float) this.value;
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(new Long(this.value).toString());
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigInteger(new Long(this.value).toString());
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return String.valueOf(this.value);
    }   
}
