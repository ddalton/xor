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
import java.util.Iterator;

import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

/**
 * The CounterGenerator is a generator driver that helps to control other generators
 * such as the property generators.
 * 
 */
public class CounterGenerator extends DefaultGenerator implements Iterator<Integer>, GeneratorDriver
{
    // A negative value means that counter does not end
    private final int count;
    private final int start;
    private int current;
    private StateGraph.ObjectGenerationVisitor visitor;

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
    
    /**
     * If no arguments is passed then a default count of 100 is used
     * @param arguments to initialized the counter generator
     *        arg0 = count
     *        arg1 = start value
     */
    public CounterGenerator (String[] arguments)
    {
        super(arguments);
        
        if(arguments.length >= 1) {
            this.count = Integer.parseInt(arguments[0]);
        } else {
            this.count = 100;
        }
        
        if(arguments.length >= 2) {
            this.start = Integer.parseInt(arguments[1]);
        } else {
            this.start = 0;
        }
        
        this.current = this.start;
    }  

    @Override public boolean hasNext ()
    {
        return current < count+start || count < 0;
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
        
        // reset the values
        this.current = this.start;
    }

    @Override
    public String getCurrentValue(StateGraph.ObjectGenerationVisitor visitor) {
        return String.valueOf(current);
    }
}
