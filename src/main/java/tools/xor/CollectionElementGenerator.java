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
import java.util.Iterator;

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
public class CollectionElementGenerator extends DefaultGenerator implements Iterator<Integer>, EntityGenerator
{
    private StateGraph.ObjectGenerationVisitor visitor;
    private int start;
    private int end;
    private int blockNo;
    private int blockSize;
    private int collectionSize;
    private int counter;
    private int value;

    public CollectionElementGenerator(String[] arguments) {
        super(arguments);

        this.start = Integer.valueOf(values[0]);
        this.end = Integer.valueOf(values[1]);

        init(1);
    }

    @Override public boolean hasNext ()
    {
        return counter < collectionSize;
    }

    @Override public Integer next ()
    {
        int startOfBlock = blockNo * blockSize;
        int offset = (int)(blockSize * Math.random());
        this.value = start + (startOfBlock + offset);

        counter++;
        return value;
    }

    public void init(int collectionSize) {
        this.collectionSize = collectionSize;
        this.blockNo = 0;
        this.blockSize = (end-start+1)/collectionSize;
        this.counter = 0;
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
