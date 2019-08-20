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
 * NOTE: A negative collection will result in the owner being set as null
 *
 */
public class CollectionOwnerGenerator extends DefaultGenerator implements EntityGenerator, Iterator<Integer>
{
    private final ElementGenerator elementGenerator;
    private StateGraph.ObjectGenerationVisitor visitor;
    private int currentValue;
    private RangeNode currentNode;
    private int end;
    private int max;
    private int invocationCount;
    private List<RangeNode> nodeList;
    private int value;

    public CollectionOwnerGenerator(String[] arguments, ElementGenerator elementGenerator) {
        super(arguments);

        this.max = Integer.parseInt(values[0]);

        this.elementGenerator = elementGenerator;
        this.nodeList = new ArrayList<>(values.length-1);
        buildNodes(nodeList, 1);

        this.currentNode = nodeList.get(0);
        this.currentValue = currentNode.getStart();
        this.end = nodeList.get(nodeList.size()-1).getEnd();
        this.invocationCount = 0;
        setValue();

        elementGenerator.nextOwner(this.value, currentNode.getSize());
    }

    private void setValue() {
        this.value = currentValue++;
    }


    @Override public boolean hasNext ()
    {
        return (currentValue < end || elementGenerator.hasNext()) && invocationCount < max;
    }

    @Override public Integer next ()
    {
        if(!elementGenerator.hasNext()) {
            setValue();

            if (this.value > currentNode.getEnd()) {
                currentNode = currentNode.getNext();
            }
            elementGenerator.nextOwner(this.value, currentNode.getSize());
        }
        invocationCount++;

        if(elementGenerator.next() == null) {
            return null;
        }

        return this.value;
    }

    @Override public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return String.valueOf(this.value);
    }
}
