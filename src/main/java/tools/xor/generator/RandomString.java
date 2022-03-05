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

import tools.xor.Property;
import tools.xor.StringType;
import tools.xor.util.graph.StateGraph;

public class RandomString extends DefaultGenerator
{
    private int length = StringType.DEFAULT_LENGTH;

    public RandomString (String[] arguments)
    {
        super(arguments);

        if(values.length == 1) {
            length = Integer.parseInt(values[0]);
        }
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return StringType.randomAlphanumeric(length);
    }
}
