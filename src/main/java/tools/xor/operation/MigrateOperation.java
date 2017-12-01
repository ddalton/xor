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

package tools.xor.operation;

import org.json.JSONObject;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.service.EntityScroll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Migrates data from the source DB to the target DB
 */
public class MigrateOperation extends AbstractOperation
{
    private final static int QUEUE_SIZE = 10000;
    private final static int CONSUMER_COUNT = 2;
    private final static int CONSUMER_BATCH_SIZE = 100;

    private final static JSONObject POISON_PILL = new JSONObject();

    private final AggregateManager source;
    private final AggregateManager target;
    private final BlockingQueue queue;

    public MigrateOperation(AggregateManager source, AggregateManager target, Integer queueSize) {
        this.source = source;
        this.target = target;

        if(queueSize == null) {
            queueSize = QUEUE_SIZE;
        }
        this.queue = new ArrayBlockingQueue(queueSize);
    }

    /**
     * TODO: Result of the current execution so far. For example:
     * 1. the number of rows processed
     * 2. the number of rows successfully migrated
     * 3. the number of batches with errors (by rows is nice to have)
     *
     * @return result of this execution
     */
    @Override public Object getResult ()
    {
        return null;
    }

    public static class Producer implements Callable {

        BlockingQueue queue;
        AggregateManager source;
        AggregateManager target;
        Settings settings;

        public Producer(BlockingQueue queue, AggregateManager source, AggregateManager target, Settings settings) {
            this.queue = queue;
            this.source = source;
            this.target = target;
            this.settings = settings;
        }

        protected Settings getSettings() {
            return this.settings;
        }

        @Override public Object call () throws Exception
        {
            // Get a cursor of the source database for the desired entity
            // This requires building the appropriate OQL/SQL
            // Build a JSON object out of this result for each row
            // and put it in the queue
            target.checkPO(settings);
            EntityScroll<JSONObject> entityCursor = getEntityScroll();
            while(entityCursor.hasNext()) {
                JSONObject jsonObject = entityCursor.next();
                queue.put(jsonObject);
            }

            // Mark the end
            queue.put(POISON_PILL);

            return null;
        }

        protected EntityScroll<JSONObject> getEntityScroll() {
            return target.getPersistenceOrchestrator().getEntityScroll(source, target, settings);
        }
    }

    public static class Consumer implements Callable {

        private BlockingQueue queue;
        private AggregateManager target;
        private Settings settings;

        public Consumer(BlockingQueue queue, AggregateManager target, Settings settings) {
            this.queue = queue;
            this.target = target;
            this.settings = settings;
        }

        protected Settings getSettings() {
            return this.settings;
        }

        @Override public Object call () throws Exception
        {
            while(true) {
                // Create a batch of objects from the queue
                List<JSONObject> batch = new ArrayList<>(CONSUMER_BATCH_SIZE);
                for (int i = 0; i < CONSUMER_BATCH_SIZE; i++) {
                    Object data = queue.take();

                    // Check if we have reached the end of processing
                    if (data == POISON_PILL) {
                        // put it back for other consumers
                        queue.put(data);
                        return null;
                    }
                    batch.add((JSONObject)data);
                }
                persistToDB(batch);

                System.out.println("Queue size: " + queue.size());
            }
        }

        /**
         * Can be overridden by the provider in a TX context
         * @param batch of entities to be persisted
         */
        protected void persistToDB(List<JSONObject> batch) {
            // create the entities in the target database
            target.create(batch, settings);
        }
    }

    protected Producer createProducer(BlockingQueue queue, AggregateManager source, AggregateManager target, Settings settings) {
        return new Producer(queue, source, target, settings);
    }

    /**
     * Factory method to create a specific type of consumer that can be overridden by subclasses.
     * This is necessary in case the object creation needs to be done in the
     * presence of a specific type of session
     *
     * @param queue that manages throttling
     * @param target database
     * @param settings user provided settings
     * @return an object that extends Consumer
     */
    protected Consumer createConsumer(BlockingQueue queue, AggregateManager target, Settings settings) {
        return new Consumer(queue, target, settings);
    }

    /**
     * Needs to be overridden by the provider to be wrapped in a TX context
     * @param entities whose types we need to return
     * @param settings containing any context needed by the provider
     * @return topologically ordered list of EntityType(s)
     */
    public List<EntityType> getEntitiesInOrder(String[] entities, Settings settings) {

        // include subtypes
        Set<String> allEntities = new HashSet<>();
        for(String entityName: entities) {
            Type type = this.target.getDAS().getType(entityName);
            if (!(type instanceof EntityType)) {
                throw new RuntimeException(
                    "The type " + type.getName() + " needs to represent an entity");
            }

            EntityType entityType = (EntityType)type;
            allEntities.add(entityType.getName());
            for(EntityType subType: entityType.getSubtypes()) {
                allEntities.add(subType.getName());
            }
        }

        // topologically order them. Higher number represents an entity that needs to be
        // processed first. So we need to process them in reverse order.
        Map<Integer, EntityType> sorted = new TreeMap<Integer, EntityType>(Collections.reverseOrder());
        for(String entityName: allEntities) {
            Type type = this.target.getDAS().getType(entityName);

            // If a subtype is not an EntityType we just skip it
            if (!(type instanceof EntityType)) {
                continue;
            }

            EntityType entityType = (EntityType) type;
            sorted.put(entityType.getOrder(), entityType);
        }

        return new ArrayList<>(sorted.values());
    }

    /**
     * Build a settings object with the migrate view in the context of a TX for a particular EntityType
     * @param entityType for which we need to build the settings object
     * @param settings user provided settings
     * @return settings object configured with the migrate view
     */
    public Settings build(EntityType entityType, Settings settings) {
        Settings result = target.getDAS().settings().migrate(entityType.getInstanceClass()).build();
        result.setBatchSize(settings.getBatchSize());

        return result;
    }

    /**
     * Useful to fine tune the size of the queue and the number of consumers needed
     * @return queue size
     */
    public int getQueueSize() {
       return this.queue.size();
    }

    @Override
    /**
     * execute the migrate operation
     * @param Settings user provided settings
     * @param das not used since the source and target are set during the construction
     */
    public void execute(Settings settings, DataAccessService das) {

        // 1 producer since that is cursor powered
        // 1 or more consumers. We need to profile this to find the desirable number.
        ExecutorService threadPool = Executors.newFixedThreadPool(CONSUMER_COUNT+1);

        List<Callable> futureList = new ArrayList<Callable>();

        // Add the single producer task
        threadPool.submit(createProducer(queue, source, target, settings));

        // Add 1 or more consumer tasks
        for(int i = 0; i < CONSUMER_COUNT; i++) {
            threadPool.submit(createConsumer(queue, target, settings));
        }

        // this will wait for the producer to finish its execution.
        threadPool.shutdown();
        try {
            // Give it sufficient amount of time to finish
            threadPool.awaitTermination(15, TimeUnit.DAYS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
