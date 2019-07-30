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

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Arguments are of the form:
 * list of owner ids and their collection sizes
 *
 * For example:
 * 234534    - Total rows in the entity
 * 1,8000:1
 * 8001,10000:2
 * 10001,15000:5-10
 *
 * In the above example a collection owner with id 9000 will have a collection containing 2 elements
 *
 */
public class CollectionOwnerGenerator extends DefaultGenerator implements Iterator<Integer>, EntityGenerator
{
    private static final String RANGE_DELIM = ",";
    private static final String SIZE_DELIM = ":";

    private final CollectionElementGenerator elementGenerator;
    private StateGraph.ObjectGenerationVisitor visitor;
    private int currentValue;
    private RangeNode currentNode;
    private int end;
    private int max;
    private int invocationCount;
    private List<RangeNode> nodeList;
    private int value;

    public CollectionOwnerGenerator(String[] arguments, CollectionElementGenerator elementGenerator) {
        super(arguments);

        this.max = Integer.parseInt(values[0]);

        this.elementGenerator = elementGenerator;
        buildNodes();

        this.currentNode = nodeList.get(0);
        this.currentValue = currentNode.start;
        this.end = nodeList.get(nodeList.size()-1).end;
        this.invocationCount = 0;
        this.value = currentValue;

        elementGenerator.init(currentNode.getSize());
    }

    @Override public boolean hasNext ()
    {
        return (currentValue < end || elementGenerator.hasNext()) && invocationCount < max;
    }

    @Override public Integer next ()
    {
        if(!elementGenerator.hasNext()) {
            this.value = currentValue++;

            if (this.value > currentNode.end) {
                currentNode = currentNode.next;
            }
            elementGenerator.init(currentNode.getSize());
        }
        elementGenerator.next();
        invocationCount++;

        return this.value;
    }

    @Override public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;
    }

    private void buildNodes() {
        nodeList = new ArrayList<>(values.length-1);

        RangeNode previous = null;
        for(int i = 1; i < values.length; i++) {
            RangeNode node = new RangeNode();
            if(previous != null) {
                previous.next = node;
            }

            nodeList.add(node);
            node.parse(values[i]);

            previous = node;
        }
    }

    private static class RangeNode {
        int start; // inclusive
        int end;   // inclusive
        int sizemin;  // or sizemin
        int sizemax = -1; // if it represents a range of sizes
        RangeNode next;

        public void parse(String text) {
            if(text.indexOf(SIZE_DELIM) == -1) {
                throw new RuntimeException(String.format("Unable to find size delimiter '%s' in input: %s", SIZE_DELIM, text));
            }
            String rangeStr = text.substring(0, text.indexOf(SIZE_DELIM));
            String sizeStr = text.substring(text.indexOf(SIZE_DELIM)+SIZE_DELIM.length());

            if(sizeStr.indexOf(RANGE_DELIM) == -1) {
                this.sizemin = Integer.parseInt(sizeStr);
            } else {
                this.sizemin = new Integer(sizeStr.substring(0, sizeStr.indexOf(RANGE_DELIM)));
                this.sizemax = new Integer(sizeStr.substring(sizeStr.indexOf(RANGE_DELIM)+RANGE_DELIM.length()));
            }

            if(rangeStr.indexOf(RANGE_DELIM) == -1) {
                throw new RuntimeException(String.format("Unable to find range delimiter '%s' in input: %s", RANGE_DELIM, rangeStr));
            }
            this.start = new Integer(rangeStr.substring(0, rangeStr.indexOf(RANGE_DELIM)));
            this.end = new Integer(rangeStr.substring(rangeStr.indexOf(RANGE_DELIM)+RANGE_DELIM.length()));
        }

        public int getSize() {
            if(sizemax == -1) {
                return sizemin;
            }

            return sizemin + ((int)(Math.random() * (sizemax-sizemin)));
        }
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return String.valueOf(this.value);
    }
}
