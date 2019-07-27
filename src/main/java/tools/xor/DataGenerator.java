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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import tools.xor.service.Shape;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Generate JSONObject instances based on the GeneratorSettings for the Entity types in the shape.
 * Multiple instances of this class can be created.
 */
public class DataGenerator
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    public static final JSONObject END_MARKER = new JSONObject();
    public static final Object SUCCESS = new Object();

    private BlockingDeque<JSONObject> queue;
    private Shape shape;
    private Settings settings;
    private List<String> types;

    public DataGenerator (BlockingDeque<JSONObject> queue, List<String> types, Shape shape, Settings settings) {
        this.queue = queue;
        this.types = types;
        this.shape = shape;
        this.settings = settings;
    }

    /**
     * Generate data for all entity types that have the generator settings set on them
     */
    public Object execute() {
        Object result = SUCCESS;

        for(String typename: types) {
            Type type = shape.getType(typename);
            if(type instanceof EntityType && ((EntityType)type).getGeneratorSettings() != null) {
                generateInstances((EntityType)type, settings);
            }
        }

        return result;
    }

    public void generateInstances(EntityType entityType, Settings settings)
    {
        GeneratorSettings generatorSettings = entityType.getGeneratorSettings();

        int counter = generatorSettings.getAndIncrement();
        StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(null, settings, null);
        while(generatorSettings.isValid(counter)) {
            // Generate the JSONObject
            JSONObject json = new JSONObject();
            visitor.setContext(new Integer(counter));

            EntityType currentType = entityType;
            while(currentType != null) {
                for (Property p : currentType.getProperties()) {
                    if (((ExtendedProperty)p).isDataType() && (
                        ((ExtendedProperty)p).getGenerator() != null || !p.isNullable())) {
                        json.put(
                            p.getName(), ((BasicType)p.getType()).generate(
                                settings,
                                p,
                                null,
                                null,
                                visitor));
                    }
                }

                currentType = currentType.getSuperType();
            }
            json.put(Constants.XOR.TYPE, entityType.getName());

            try {
                queue.put(json);
            }catch(InterruptedException e) {
                logger.info(
                    "Thread interrupted when processing entity: " + entityType.getName()
                        + " at count: " + entityType.getGeneratorSettings().getCounter());
            }
            counter = generatorSettings.getAndIncrement();
        }
    }
}
