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
 * Use this if the parent needs to be generated.
 * The collection owner drives the child elements.
 *
 * Negative values represent a null parent.
 *
 * For example:
 * 1,500:0
 * 501,2000:2
 *
 * The owners with id from 1-500 have a null value generated.
 * The onwer with id 501 and 502 will have a value with the same id
 *
 *
 */
public class ToOneGenerator extends DefaultGenerator implements IteratorListener
{
    private static final int COUNTER_INIT = -1;

    private int start;
    private RangeNode currentNode;
    private List<RangeNode> nodeList;
    private int counter = COUNTER_INIT;
    private int value;

    public ToOneGenerator (String[] arguments) {
        super(arguments);

        this.start = Integer.valueOf(values[0]);
        this.nodeList = new ArrayList<>(values.length-1);
        buildNodes(nodeList, 1);

        this.currentNode = nodeList.get(0);
        this.value = this.start;
    }

    @Override public void handleEvent (int sourceId, StateGraph.ObjectGenerationVisitor visitor)
    {
        if (sourceId > currentNode.getEnd()) {
            currentNode = currentNode.getNext();
            counter = COUNTER_INIT;
        }

        if(currentNode.getSize() > 0) {
            if (counter++ == currentNode.getSize()-1) {
                counter = 0;
                value++;
            }
        }
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        if(currentNode.getSize() == 0) {
            return null;
        }
        return String.valueOf(this.value);
    }
}
