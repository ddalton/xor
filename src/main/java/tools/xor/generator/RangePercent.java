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
import tools.xor.Settings;
import tools.xor.util.graph.StateGraph;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RangePercent extends DefaultGenerator
{
    private static final String PLACEHOLDER = "[__]";
    private static final String RANGE_DELIM = ",";
    private static final String PERCENT_DELIM = ":";

    List<RangeNode> nodeList;
    RangeNode tree;

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
     * Implementation wise, we use a binary tree for efficiency
     *
     * @param arguments
     */
    public RangePercent (String[] arguments)
    {
        super(arguments);

        buildNodes();

        this.tree = buildTree(0, nodeList.size()-1);
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

    private static class RangeNode {
        int startVal;
        int endVal;
        BigDecimal startPercent; // exclusive, exception is 0
        BigDecimal endPercent; // inclusive

        RangeNode left;
        RangeNode right;

        public void parse(String text, RangeNode previous) {
            if(previous == null) {
                startPercent = new BigDecimal(0);
            } else {
                startPercent = previous.endPercent;
            }

            String range = text.substring(0, text.indexOf(PERCENT_DELIM));
            String percent = text.substring(text.indexOf(PERCENT_DELIM)+PERCENT_DELIM.length());

            endPercent = new BigDecimal(percent);

            this.startVal = new Integer(range.substring(0, range.indexOf(RANGE_DELIM)));
            this.endVal = new Integer(range.substring(text.indexOf(RANGE_DELIM)+RANGE_DELIM.length()));
        }

        private RangeNode findNode(BigDecimal random) {
            if( (random.equals(0) && startPercent.equals(0))
                || (random.compareTo(startPercent) == 1 && random.compareTo(endPercent) != 1) ) {
                return this;
            }

            // walk the left tree
            if(random.compareTo(startPercent) != 1) {
                return left.findNode(random);
            } else {
                return right.findNode(random);
            }
        }

        public int getRandom() {
            long range = endVal - startVal;
            return startVal + ((int)(Math.random() * range));
        }
    }

    /**
     * Given a start and end index of the values array, it returns
     * a node, that is the root of the binary tree.
     *
     * @param startIndex of the values array representing the start of the tree
     * @param endIndex of the values array representing the end of the tree
     * @return root node of binary tree
     */
    private RangeNode buildTree(int startIndex, int endIndex)
    {
        if(startIndex > endIndex) {
            return null;
        }

        int mid = (startIndex+endIndex)/2;
        RangeNode root = nodeList.get(mid);

        root.left = buildTree(startIndex, mid-1);
        root.right = buildTree(mid+1, endIndex);

        return root;
    }

    private int getValue() {
        BigDecimal random = BigDecimal.valueOf(Math.random());

        RangeNode node = tree.findNode(random);
        return node.getRandom();
    }

    @Override
    public byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (byte)getValue();
    }

    @Override
    public short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (short)getValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getValue();
    }

    @Override
    public int getIntValue (StateGraph.ObjectGenerationVisitor visitor)
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
        return values[0].replace(PLACEHOLDER, String.valueOf(getValue()));
    }

    @Override public int getFanout (Property property, Settings settings, String path, StateGraph.ObjectGenerationVisitor visitor)
    {
        return getValue();
    }
}
