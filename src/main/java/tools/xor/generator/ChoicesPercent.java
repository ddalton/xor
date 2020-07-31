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
import java.util.ArrayList;
import java.util.List;

import tools.xor.Property;
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

        this.tree = (PercentNode)buildTree(0, nodeList.size()-1, nodeList);
    }

    private void buildNodes() {
        nodeList = new ArrayList<>(values.length-1);

        ChoicesNode previous = null;
        for(int i = 0; i < values.length; i++) {
            ChoicesNode node = new ChoicesNode();
            nodeList.add(node);
            node.parse(values[i], previous);

            previous = node;
        }
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

        @Override public Integer getInt ()
        {
            return Integer.parseInt(getString()) ;
        }
    }

    protected String getValue() {
        BigDecimal random = BigDecimal.valueOf(Math.random());

        PercentNode node = (PercentNode)tree.findNode(random);
        return node.getString();
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return getValue();
    }
}
