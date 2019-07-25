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

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Generate JSONObject instances based on the GeneratorSettings for the Entity types in the shape.
 * Multiple instances of this class can be created.
 */
public class DataGenerator implements Callable
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    public static final JSONObject END_MARKER = new JSONObject();
    public static final Object SUCCESS = new Object();

    private BlockingDeque<JSONObject> queue;
    private Shape shape;
    private Settings settings;
    private CountDownLatch latch;

    public DataGenerator (BlockingDeque<JSONObject> queue, CountDownLatch latch, Shape shape, Settings settings) {
        this.queue = queue;
        this.latch = latch;
        this.shape = shape;
        this.settings = settings;
    }

    /**
     * Generate data for all entity types that have the generator settings set on them
     */
    public Object call() {
        Object result = SUCCESS;
        latch.countDown();

        for(Type type: shape.getUniqueTypes()) {
            if(type instanceof EntityType && ((EntityType)type).getGeneratorSettings() != null) {
                generateInstances((EntityType)type, settings);
            }
        }
        latch.countDown();

        return result;
    }

    public void generateInstances(EntityType entityType, Settings settings)
    {
        GeneratorSettings generatorSettings = entityType.getGeneratorSettings();

        int counter = generatorSettings.getAndIncrement();
        StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(null, settings, null);
        while(generatorSettings.isValid(counter)) {
            System.out.println("Data generator: " + counter);
            // Generate the JSONObject
            JSONObject json = new JSONObject();
            visitor.setContext(new Integer(counter));
            for(Property p: entityType.getProperties()) {
                if( ((ExtendedProperty) p).isDataType() && (((ExtendedProperty)p).getGenerator() != null || !p.isNullable())) {
                    json.put(p.getName(), ((BasicType)p.getType()).generate(
                            settings,
                            p,
                            null,
                            null,
                            visitor));
                }
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
