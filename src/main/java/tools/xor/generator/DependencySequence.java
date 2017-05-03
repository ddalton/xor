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

import org.json.JSONObject;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.QueryView;

/**
 * This class takes a field of the form
 * [PARENT].attribute.[PARENT].attribute ...
 * attribute.[PARENT].attribute
 * ...
 *
 * Here [_PARENT_] represents the object to which the current object is connected to.
 * If an object is shared, then the connected to object is non-deterministic as the order is
 * currently not enforced.
 * The way we solve the non-deterministic case is to provide multiple paths to resolve the
 * non determinism.
 *
 */
public class DependencySequence extends DefaultGenerator
{
    public static final String PARENT_LINK = "[PARENT]";

    public DependencySequence (String[] arguments)
    {
        super(arguments);
    }

    /**
     * Get the dependency value in String form. Do the conversion if not in string form.
     * @return value in String form
     */
    private String getDependencyValue(JSONObject rootedAt) {

        if(rootedAt == null) {
            return null;
        }

        // Loop through each path
        for(String path: getValues()) {
            // For each path, loop through each component and see if the value is available
            while(path != null) {
                // Extract the next attribute in the path
                String component = QueryView.getTopAttribute(path);

                // Get the path ready for the next round if applicable
                path = QueryView.getNext(path);
                if(PARENT_LINK.equals(component)) {
                    rootedAt = rootedAt.getJSONObject(Constants.XOR.GEN_PARENT);

                    // If the object has not yet been created, then try the next path
                    if(rootedAt == null) {
                        continue;
                    }
                } else if(rootedAt.has(component)) {
                    // last component, should refer to a string value
                    if(path == null) {

                        //perform conversion if necessary
                        Object value = rootedAt.get(component);
                        if(value != null && value instanceof Number) {
                            return value.toString();
                        }

                        return rootedAt.getString(component);
                    }
                    rootedAt = rootedAt.getJSONObject(component);
                } else {
                    continue;
                }
            }
        }

        return null;
    }

    @Override
    public String getStringValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (getDependencyValue(visitor.getRootedAt()) == null ?
            "ID" :
            getDependencyValue(visitor.getRootedAt())) + "_" + visitor.getSequenceNo();
    }
}
