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
import tools.xor.Settings;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.service.EntityScroll;

import java.util.ArrayList;
import java.util.List;
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
        Settings settings;

        public Producer(BlockingQueue queue, AggregateManager source, Settings settings) {
            this.queue = queue;
            this.source = source;
            this.settings = settings;
        }

        @Override public Object call () throws Exception
        {
            // Get a cursor of the source database for the desired entity
            // This requires building the appropriate OQL/SQL
            // Build a JSON object out of this result for each row
            // and put it in the queue
            EntityScroll<JSONObject> entityCursor = source.getPersistenceOrchestrator().getEntityScroll(settings);
            while(entityCursor.hasNext()) {
                JSONObject jsonObject = entityCursor.next();
                queue.put(jsonObject);
            }

            // Mark the end
            queue.put(POISON_PILL);

            return null;
        }
    }

    public static class Consumer implements Callable {

        BlockingQueue queue;
        AggregateManager target;
        Settings settings;

        public Consumer(BlockingQueue queue, AggregateManager target, Settings settings) {
            this.queue = queue;
            this.target = target;
            this.settings = settings;
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
                // create the entities in the target database
                target.create(batch, settings);

                System.out.println("Queue size: " + queue.size());
            }
        }
    }

    protected Producer createProducer(BlockingQueue queue, AggregateManager source, Settings settings) {
        return new Producer(queue, source, settings);
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
        threadPool.submit(createProducer(queue, source, settings));

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
