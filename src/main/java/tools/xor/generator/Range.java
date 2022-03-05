/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
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
import tools.xor.Settings;
import tools.xor.util.graph.StateGraph;

public class Range extends DefaultGenerator
{
    public Range (String[] arguments)
    {
        super(arguments);
    }

    @Override public int getFanout (Property property, Settings settings, String path, StateGraph.ObjectGenerationVisitor visitor)
    {
        if(settings.hasCollectionSparseness(path)) {
            return super.getFanout(property, settings, path, visitor);
        }

        return getIntValue(null);
    }
}
