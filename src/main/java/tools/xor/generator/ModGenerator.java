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

import tools.xor.Property;
import tools.xor.util.graph.StateGraph;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private void buildNodes() {
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

    private ModRange findNode(int value) {
        for(ModRange range: nodeList) {
            if(range.inRange(value)) {
                return range;
            }
        }

        return null;
    }

    private Integer getIntValue(int value) {
        ModRange r = findNode(value);
        return r == null ? null : r.getNewValue(value);
    }

    @Override
    public byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int value = generator.getByteValue(visitor);
        return getIntValue(value).byteValue();
    }

    @Override
    public short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int value = generator.getShortValue(visitor);
        return getIntValue(value).shortValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int value = generator.getCharValue(visitor);
        return (char)getIntValue(value).intValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Integer value = generator.getIntValue(visitor);
        if(value == null) {
            return null;
        }
        return getIntValue(value);
    }

    @Override
    public long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int value = (int)generator.getLongValue(visitor);
        return getIntValue(value).longValue();
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(getLongValue(visitor));
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int value = generator.getDoubleValue(visitor).intValue();
        return getIntValue(value).doubleValue();
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int value = generator.getFloatValue(visitor).intValue();
        return getIntValue(value).floatValue();
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
        Integer value = getIntValue(visitor);

        return value == null ? null : values[0].replace(PLACEHOLDER, String.valueOf(value));
    }
}
