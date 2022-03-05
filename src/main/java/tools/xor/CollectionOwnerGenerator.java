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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

/**
 * Arguments are of the form:
 * We need to support different collection sizes.
 * Some collection owners might have large collections, whereas some might have small ones.
 * list of owner id ranges and their collection sizes
 *
 * For example:
 * 234534             // Total rows in the entity
 * 1,8000:1           // Owners with id from 1-8000 have one element collection
 * 8001,10000:2       // Owners with id from 8001-10000 have 2 element collection
 * 10001,15000:5-10   // Owners with id from 10001-15000 have collections with sizes ranging from 5 to 10
 *
 * NOTE: A negative collection will result in the owner being set as null
 * For example:
 * 1000
 * -1,-1000:1         // The collection size will be multiple with the no. of owners
 * -1,-100:10         // So this is the same as the previous one in the number of elements generated
 *
 */
public class CollectionOwnerGenerator extends DefaultGenerator implements GeneratorDriver, ElementGenerator
{
    private final ElementGenerator elementGenerator;
    private StateGraph.ObjectGenerationVisitor visitor;
    private int currentValue;
    private RangeNode currentNode;
    private int end;                    // Holds the last collection owner value
    private int max;                    // Will not generate more than this value
    private int invocationCount;
    private List<RangeNode> nodeList;
    private int value;                  // Collection owner value
    private int collectionSize;         // Size of the collection when this acts as element generator
    private int counter;

    private static final int COLLECTION_SIZE = 1;

    public CollectionOwnerGenerator(String[] arguments, ElementGenerator elementGenerator) {
        super(arguments);

        this.max = Integer.parseInt(values[0]);

        this.elementGenerator = elementGenerator;
        this.nodeList = new ArrayList<>(values.length-1);
        buildNodes(nodeList, 1, false);

        nextOwner(-1, 0, COLLECTION_SIZE);
    }

    private void setValue() {
        this.value = currentValue++;
    }


    @Override public boolean hasNext ()
    {
        //if(((currentValue <= end || elementGenerator.hasNext() || counter != collectionSize-1) && invocationCount < max) == false) {
        //    System.out.println(String.format("currentValue: %d, end: %d, counter: %d, collectionSize: %d, invocationCount: %d, max: %d", currentValue, end, counter, collectionSize, invocationCount, max));
        //}

        return (currentValue <= end || elementGenerator.hasNext() || counter != collectionSize-1) && invocationCount < max;
    }

    @Override public Integer next ()
    {
        if(!elementGenerator.hasNext()) {

            if(counter++ == collectionSize-1) {
                setValue();

                if (this.value > currentNode.getEnd()) {
                    do {
                        currentNode = currentNode.getNext();
                    } while(currentNode != null && currentNode.getSize() == 0);

                    currentNodeChanged();
                }

                // reset for next value
                counter = 0;
            }
            elementGenerator.nextOwner(this.value, this.counter, currentNode.getSize());

            //if(elementGenerator instanceof CollectionElementGenerator) {
            //    System.out.println(String.format("value: %d, counter: %d, size: %d, currentValue: %d", this.value, this.counter, currentNode.getSize(), currentValue));
            //}
        }
        invocationCount++;

        if(elementGenerator.next() == null) {
            return null;
        }

        notifyListeners(value, this.visitor);

        return this.value;
    }

    @Override public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        if(this.currentNode.isNoOwner()) {
            return null;
        }
        return String.valueOf(this.value);
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        if(this.currentNode.isNoOwner()) {
            return null;
        }
        return this.value;
    }

    // Here the collection owner is being an element generator
    // so it needs to reset its values
    @Override public void nextOwner (int ownerId, int index, int collectionSize)
    {
        this.collectionSize = collectionSize;
        this.counter = 0;

        this.currentNode = nodeList.get(0);
        currentNodeChanged();
//        this.currentValue = currentNode.getStart();
        this.end = nodeList.get(nodeList.size()-1).getEnd();
        this.invocationCount = 0;
//        setValue();
        elementGenerator.nextOwner(this.value, this.counter, currentNode.getSize());
    }

    private void currentNodeChanged() {
        this.currentValue = currentNode.getStart();
        setValue();
    }

    public int getInvocationCount() {
        return this.invocationCount;
    }

    @Override
    public long getCounter() {
        return this.counter;
    }
}
