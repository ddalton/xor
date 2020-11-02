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

package tools.xor.generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tools.xor.Property;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

public class ChoicesPercent extends DefaultGenerator
{
    private static final String NULL_VALUE = "NULL";

    List<PercentNode> nodeList;
    PercentNode tree;

    /**
     * Arguments are of the form:
     * list of choices and their percentages of occurrence
     *
     * For example:
     * NULL:0.06   // null value
     * RED:0.18
     * BLUE:0.30
     * GREEN:0.36
     * :0.50        // empty string
     * ORANGE:1.00
     *
     * Implementation wise, we use a binary tree for efficiency
     *
     * @param arguments data needed to configure this generator
     */
    public ChoicesPercent (String[] arguments)
    {
        super(arguments);

        buildNodes();
    }

    public void buildNodes() {
        nodeList = new ArrayList<>(values.length-1);

        ChoicesNode previous = null;
        for(int i = 0; i < values.length; i++) {
            ChoicesNode node = new ChoicesNode();
            nodeList.add(node);
            node.parse(values[i], previous);

            previous = node;
        }
        
        this.tree = (PercentNode)buildTree(0, nodeList.size()-1, nodeList);        
    }

    private static class ChoicesNode extends PercentNode {
        String value;

        public void parse(String text, ChoicesNode previous) {
            super.parse(text, previous);

            value = text.substring(0, text.indexOf(PERCENT_DELIM)).trim();
            if(NULL_VALUE.equals(value)) {
                value = null;
            }
        }

        @Override
        public String getString () {
            return value;
        }

        @Override public Long getLong ()
        {
            String val = getString();
            return val == null ? null : Long.parseLong(val) ;
        }
    }

    protected String getValue() {
        BigDecimal random = BigDecimal.valueOf(ClassUtil.nextDouble());
        PercentNode node = (PercentNode)tree.findNode(random);
        return node.getString();
    }
    
    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();
        return val == null ? null : Byte.valueOf(val);        
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();
        return val == null ? null : Short.valueOf(val);        
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getShortValue(visitor).shortValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();
        return val == null ? null : Integer.valueOf(val);
    }    

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        Long val = getLongValue(visitor);
        return val == null ? null : val.toString();
    }
    
    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        BigDecimal random = BigDecimal.valueOf(ClassUtil.nextDouble());
        PercentNode node = (PercentNode)tree.findNode(random);
        return node.getLong();
    }
    
    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        Long val = getLongValue(visitor);
        return val == null ? null : new Date(val);
    }
    
    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();        
        return val == null ? null : Double.valueOf(val);
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();        
        return val == null ? null : Double.valueOf(val).floatValue();
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();            
        return val == null ? null : new BigDecimal(val);
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        String val = getValue();       
        return val == null ? null : new BigInteger(val);
    }    
}
