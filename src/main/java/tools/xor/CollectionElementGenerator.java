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

import java.math.BigDecimal;
import java.sql.Connection;

/**
 * Arguments are of the form:
 * list of owner ids and their collection sizes
 *
 * For example:
 * 1,8000:1
 * 8001,10000:2
 * 10001,15000:5-10
 *
 * In the above example a collection owner with id 9000 will have a collection containing 2 elements
 *
 * This implemention does not allow duplicate elements in the collection.
 * If this restriction is not needed, then the calculation can be more efficient.
 *
 */
public class CollectionElementGenerator extends DefaultGenerator implements EntityGenerator, ElementGenerator
{
    // start and end represent the range from which the value is chosen
    private int start;
    private int end;
    private int blockNo;
    private int blockSize;
    private int collectionSize;
    private int counter;
    private int value;
    private StateGraph.ObjectGenerationVisitor visitor;

    public CollectionElementGenerator(String[] arguments) {
        super(arguments);

        this.start = Integer.valueOf(values[0]);

        if(values.length > 1) {
            this.end = Integer.valueOf(values[1]);
        }

        nextOwner(-1, 0, 1);
    }

    @Override public boolean hasNext ()
    {
        return counter < collectionSize;
    }

    @Override public Integer next ()
    {
        if(collectionSize == 0) {
            return null;
        }

        updateValue();

        counter++;
        return value;
    }

    protected void updateValue() {
        int startOfBlock = blockNo++ * blockSize;
        int offset = (int)(blockSize * Math.random());
        this.value = start + (startOfBlock + offset);
    }

    @Override
    public void nextOwner (int ownerId, int counter, int collectionSize) {
        this.collectionSize = collectionSize;
        this.blockNo = 0;
        this.blockSize = collectionSize>0 ? (end-start+1)/collectionSize : 0;
        this.counter = 0;
    }

    @Override public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;
    }

    public int getCounter() {
        return this.counter;
    }

    protected void setStart(int value) {
        if(start > end) {
            throw new RuntimeException(String.format("Start '%d' value cannot be greater than end value: %d", start, end));
        }
        this.start = value;
    }

    protected int getValue() {
        return this.value;
    }

    protected void setValue(int val) {
        this.value = val;
    }

    protected int getStart() {
        return this.start;
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return this.value;
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(this.value);
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return String.valueOf(this.value);
    }
}
