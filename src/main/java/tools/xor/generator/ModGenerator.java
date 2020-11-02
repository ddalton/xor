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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tools.xor.Property;
import tools.xor.util.graph.StateGraph;

/**
 * This generator transforms from one range of values to another
 * specifically for transforming from a wider range to a narrower range.
 * It is also idempotent, meaning invoking with the same value will output the same value
 */
public class ModGenerator extends DefaultGenerator implements GeneratorRecipient
{
    private Generator generator;
    List<ModRange> nodeList;

    public ModGenerator (String[] arguments)
    {
        super(arguments);

        buildNodes();
    }

    public void buildNodes() {
        nodeList = new ArrayList<>(values.length-1);

        for(int i = 1; i < values.length; i++) {
            ModRange node = new ModRange();
            nodeList.add(node);
            node.parse(values[i]);
        }
    }

    @Override public void accept (Generator generator)
    {
        this.generator = generator;
    }

    private ModRange findNode(Long value) {
        for(ModRange range: nodeList) {
            if(range.inRange(value)) {
                return range;
            }
        }

        return null;
    }
    
    private Long getLongValue(Long value) {
        if(value == null) {
            return null;
        }
        ModRange r = findNode(value);
        return r == null ? null : r.getNewValue(value);        
    }

    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Byte value = generator.getByteValue(visitor);
        return value == null ? null : getLongValue(value.longValue()).byteValue();
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Short value = generator.getShortValue(visitor);
        if (value != null) {
            Long longVal = getLongValue(value.longValue());
            return longVal != null ? longVal.shortValue() : null;
        }
        return null;
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        long value = generator.getCharValue(visitor);

        Long longVal = getLongValue(value);
        return longVal != null ? (char)longVal.shortValue() : null;
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Integer value = generator.getIntValue(visitor);
        if (value != null) {
            Long longVal = getLongValue(value.longValue());
            return longVal != null ? longVal.intValue() : null;
        }
        return null;
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Long value = generator.getLongValue(visitor);
        if (value != null) {
            Long longVal = getLongValue(value.longValue());
            return longVal != null ? longVal : null;
        }
        return null;        
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        Long value = generator.getLongValue(visitor);
        
        if (value != null) {
            Long longVal = getLongValue(value.longValue());
            return longVal != null ? new Date(longVal) : null;
        }
        return null;
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {       
        Double value = generator.getDoubleValue(visitor);
        if (value != null) {
            Long longVal = getLongValue(value.longValue());
            return longVal != null ? longVal.doubleValue() : null;
        }
        return null;          
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Float value = generator.getFloatValue(visitor);
        if (value != null) {
            Long longVal = getLongValue(value.longValue());
            return longVal != null ? longVal.floatValue() : null;
        }
        return null;          
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        Long val = getLongValue(visitor);
        return val == null ? null : new BigDecimal(val.toString());
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        Long val = getLongValue(visitor);
        return val == null ? null : new BigInteger(val.toString());        
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        Long value = getLongValue(visitor);

        return value == null ? null : values[0].replace(PLACEHOLDER, String.valueOf(value));
    }
}
