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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CounterGenerator extends DefaultGenerator implements Iterator<Integer>, GeneratorDriver
{
    private final int count;
    private final int start;
    private StateGraph.ObjectGenerationVisitor visitor;
    private int current;

    public CounterGenerator(int count) {
        this(count, 0);
    }

    public CounterGenerator(int count, int start) {

        super(new String[] {
                Integer.toString(start),
                Integer.toString(start+count-1)
            });

        this.count = count;
        this.start = start;
        this.current = start;
    }

    @Override public boolean hasNext ()
    {
        return current < count+start;
    }

    @Override public Integer next ()
    {
        int value = current++;

        visitor.setContext(value);
        notifyListeners(value, this.visitor);

        return value;
    }

    @Override public void init (Connection connection, StateGraph.ObjectGenerationVisitor visitor)
    {
        this.visitor = visitor;
    }
}
