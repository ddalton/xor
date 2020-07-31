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
import tools.xor.Settings;
import tools.xor.util.graph.StateGraph;

public class RangePercent extends DefaultGenerator
{
    private List<PercentNode> nodeList;
    private PercentNode tree;
    protected Integer value;

    /**
     * Arguments are of the form:
     * String template
     * list of range and their percentages
     *
     * For example:
     * ID_[__]
     * 0,3:0.06
     * 4,15:0.18
     * 16,35:0.30
     * 36,85:0.36
     * 86,2576:1.00
     *
     * or
     *
     * 1:0.06          // The same value of 1 is returned
     * 4,15:0.18
     * 16,35:0.30
     * 36,85:0.36
     * 86,2576:1.00
     *
     * :0.06      // to represent a null value
     *
     * Implementation wise, we use a binary tree for efficiency
     *
     * @param arguments data needed to configure this generator
     */
    public RangePercent (String[] arguments)
    {
        super(arguments);

        buildNodes();

        this.tree = buildTree(0, nodeList.size()-1, nodeList);
    }

    private void buildNodes() {
        nodeList = new ArrayList<>(values.length-1);

        RangeNode previous = null;
        for(int i = 1; i < values.length; i++) {
            RangeNode node = new RangeNode();
            nodeList.add(node);
            node.parse(values[i], previous);

            previous = node;
        }
    }

    private static class RangeNode extends PercentNode {
        Integer startVal;
        Integer endVal;

        public void parse(String text, RangeNode previous) {
            super.parse(text, previous);

            // This particular entry is an explicit value and does not constitute a range
            String range = text.substring(0, text.indexOf(PERCENT_DELIM)).trim();
            if(range.indexOf(RANGE_DELIM) == -1) {
                if(!("".equals(range))) {
                    this.startVal = new Integer(range);
                }
                this.endVal = startVal;
            } else {
                this.startVal = new Integer(range.substring(0, range.indexOf(RANGE_DELIM)));
                this.endVal = new Integer(range.substring(
                    text.indexOf(RANGE_DELIM) + RANGE_DELIM.length()));
            }
        }

        @Override
        public Integer getInt () {
            if(startVal == null) {
                return null;
            }

            if(startVal == endVal) {
                return startVal;
            } else {
                long range = endVal - startVal;
                return startVal + ((int)(Math.random() * range));
            }
        }

        @Override
        public String getString() {
            Integer val = getInt();

            return val == null ? null : String.valueOf(val);
        }
    }

    protected Integer getValue() {
        BigDecimal random = BigDecimal.valueOf(Math.random());

        PercentNode node = tree.findNode(random);
        this.value = node.getInt();

        return this.value;
    }

    @Override
    public byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (byte)getValue().intValue();
    }

    @Override
    public short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (short)getValue().intValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getValue().intValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return getValue();
    }

    @Override
    public long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return getValue();
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(getValue());
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValue());
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValue()).floatValue();
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(getValue());
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        return BigInteger.valueOf( getValue() );
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        Integer value = getValue();
        return value == null ? null : values[0].replace(PLACEHOLDER, String.valueOf(value));
    }

    @Override public int getFanout (Property property, Settings settings, String path, StateGraph.ObjectGenerationVisitor visitor)
    {
        return getValue();
    }

    @Override
    public String getCurrentValue(StateGraph.ObjectGenerationVisitor visitor) {
        return value == null ? null : String.valueOf(value);
    }
}
