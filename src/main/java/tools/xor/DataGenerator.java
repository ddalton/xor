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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import tools.xor.providers.jdbc.JDBCPersistenceOrchestrator;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.DASFactory;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Generate JSONObject instances based on the GeneratorSettings for the Entity types in the shape.
 * Multiple instances of this class can be created.
 */
public class DataGenerator
{
    public static final int IMPORTER_POOL_SIZE = 4;
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    public static final JSONObject END_MARKER = new JSONObject();
    public static final Object SUCCESS = new Object();

    private Shape shape;
    private Settings settings;
    private List<String> types;
    private DASFactory dasFactory;
    private ExecutorService importers = Executors.newFixedThreadPool(DataGenerator.IMPORTER_POOL_SIZE);
    private ConcurrentLinkedQueue[] importerQueues = new ConcurrentLinkedQueue[IMPORTER_POOL_SIZE];

    public DataGenerator (List<String> types, Shape shape, Settings settings, DASFactory dasFactory) {
        this.types = types;
        this.shape = shape;
        this.settings = settings;
        this.dasFactory = dasFactory;
    }

    /**
     * Generate data for all entity types that have the generator settings set on them
     */
    public Object execute() {
        Object result = SUCCESS;

        for(String typename: types) {
            Type type = shape.getType(typename);
            if(type instanceof EntityType && ((EntityType)type).getGenerators().size() > 0) {
                generateInstances((EntityType)type, settings);
            }
        }

        return result;
    }

    private List<Future> createImportJobs() {
        // Create the importers
        List<Future> importJobs = new ArrayList<Future>();
        for (int i = 0; i < IMPORTER_POOL_SIZE; i++) {
            importerQueues[i] = new ConcurrentLinkedQueue();
            PersistenceOrchestrator po = dasFactory.createPersistenceOrchestrator(settings.getSessionContext());
            importJobs.add(importers.submit(new DataImporter(importerQueues[i], po, shape, settings)));
        }

        return importJobs;
    }

    public void generateInstances(EntityType entityType, Settings settings)
    {
        for(EntityGenerator generator : entityType.getGenerators()) {
            System.out.println(String.format("Generating data for entity: %s", entityType.getName()));

            List<Future> importJobs = createImportJobs();

            StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(
                null,
                settings,
                null);

            JDBCPersistenceOrchestrator po = (JDBCPersistenceOrchestrator)settings.getPersistenceOrchestrator();
            JDBCSessionContext sc = po.getSessionContext();
            sc.beginTransaction();

            generator.init(sc.getConnection(), visitor);
            Iterator iter = (Iterator) generator;
            int jobNo = 0;
            while(iter.hasNext()) {
                if(iter.next() == null) {
                    break;
                }

                generateObject(entityType, visitor, jobNo++%IMPORTER_POOL_SIZE);
            }

            // release the connection
            sc.rollback();

            if(waitForJobs(importJobs)) {
                break;
            }
        }
    }

    private boolean waitForJobs(List<Future> importJobs) {

        boolean encounteredError = false;

        for (int i = 0; i < IMPORTER_POOL_SIZE; i++) {
            importerQueues[i].offer(DataGenerator.END_MARKER);
        }

        // Wait for the import jobs to finish
        for (Future importJob : importJobs) {
            try {
                importJob.get();
            }
            catch (Exception e) {
                logger.error(ExceptionUtils.getStackTrace(e));
                encounteredError = true;
            }
        }

        return encounteredError;
    }

    private void generateObject(EntityType entityType, StateGraph.ObjectGenerationVisitor visitor, int jobNo) {
        // Generate the JSONObject
        JSONObject json = new JSONObject();

        EntityType currentType = entityType;
        while (currentType != null) {
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

        importerQueues[jobNo].offer(json);
    }
}
