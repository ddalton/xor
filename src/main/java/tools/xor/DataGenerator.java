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
import tools.xor.providers.jdbc.ImportMethod;
import tools.xor.providers.jdbc.JDBCPersistenceOrchestrator;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.DASFactory;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Generate JSONObject instances based on the GeneratorSettings for the Entity types in the shape.
 * Multiple instances of this class can be created.
 */
public class DataGenerator
{
    public static final int IMPORTER_POOL_SIZE = 1;

    // Used for throttle control
    public static final int HIGH_WATERMARK = 1500;
    public static final int LOW_WATERMARK = 1000;

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    public static final JSONObject END_MARKER = new JSONObject();
    public static final Object SUCCESS = new Object();

    private Shape shape;
    private Settings settings;
    private List<String> types;
    private DASFactory dasFactory;
    private ImportMethod importMethod;
    private ExecutorService importers = Executors.newFixedThreadPool(DataGenerator.IMPORTER_POOL_SIZE);
    private ConcurrentLinkedQueue[] importerQueues = new ConcurrentLinkedQueue[IMPORTER_POOL_SIZE];
    private boolean reachedHigh[] = new boolean[DataGenerator.IMPORTER_POOL_SIZE];
    private Map<String, List<Property>> generatedFields = new HashMap<>();

    public DataGenerator (List<String> types, Shape shape, Settings settings, DASFactory dasFactory) {
        this.types = types;
        this.shape = shape;
        this.settings = settings;
        this.dasFactory = dasFactory;
        this.importMethod = ImportMethod.PREPARED_STATEMENT;
        //this.importMethod = ImportMethod.CSV;
    }

    /**
     * Get all the types and its supertypes
     * @return set of the types to remove duplicates
     */
    private Set<EntityType> getFlattenedTypes() {
        Set<EntityType> result = new HashSet<>();

        for(String typename: types) {
            Type type = shape.getType(typename);
            if(hasGenerator(type)) {
                EntityType entityType = (EntityType)type;
                while(entityType != null) {
                    result.add(entityType);
                    entityType = entityType.getSuperType();
                }
            }
        }

        return result;
    }

    private boolean hasGenerator(Type type) {
        return type instanceof EntityType && ((EntityType)type).getGenerators().size() > 0;
    }

    /**
     * Generate data for all entity types that have the generator settings set on them
     */
    public Object execute() {
        Object result = SUCCESS;

        if(importMethod == ImportMethod.CSV) {
            for(EntityType entityType: getFlattenedTypes()) {
                writeHeader(entityType);
            }
        }

        for(String typename: types) {
            Type type = shape.getType(typename);
            if(hasGenerator(type)) {
                generateInstances((EntityType)type, settings);
            }
        }

        return result;
    }

    private void writeHeader(EntityType entityType) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(ClassUtil.getCSVFilename(entityType.getName())))) {
            List<String> columnNames = new LinkedList<>();
            for(Property p: getGeneratedFields(entityType)) {
                columnNames.add(p.getName());
            }

            out.write(String.join(",", columnNames));
            out.newLine();
        }
        catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    private List<Future> createImportJobs() {
        // Create the importers
        List<Future> importJobs = new ArrayList<Future>();
        for (int i = 0; i < IMPORTER_POOL_SIZE; i++) {
            importerQueues[i] = new ConcurrentLinkedQueue();
            JDBCPersistenceOrchestrator po = (JDBCPersistenceOrchestrator)dasFactory.createPersistenceOrchestrator(settings.getSessionContext());
            po.getSessionContext().setImportMethod(importMethod);
            if(importMethod == ImportMethod.CSV && IMPORTER_POOL_SIZE > 1)
            {
                throw new RuntimeException("Writing to CSV can have only 1 importer job");
            }

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
            int generationCount = 0;
            while(iter.hasNext()) {
                int jobNo = generationCount%IMPORTER_POOL_SIZE;

                if(iter.next() == null) {
                    continue;
                }

                while(reachedHigh[jobNo]) {
                    // pause until the importer drains down the queue below the low water mark
                    try {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e) {
                        throw ClassUtil.wrapRun(e);
                    }

                    if(importerQueues[jobNo].size() <= LOW_WATERMARK) {
                        // reset the flag once the queue capacity falls
                        // below low watermark
                        reachedHigh[jobNo] = false;
                    }
                }

                generateObject(entityType, visitor, jobNo);

                // Update the generation count
                generationCount++;

                // Check if the queue capacity has reached the high water mark
                if(importerQueues[jobNo].size() >= HIGH_WATERMARK) {
                    reachedHigh[jobNo] = true;
                }
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
            for(Property p: getGeneratedFields(currentType)) {
                json.put(
                    p.getName(), ((BasicType)p.getType()).generate(
                        settings,
                        p,
                        null,
                        null,
                        visitor));
            }

            currentType = currentType.getSuperType();
        }
        json.put(Constants.XOR.TYPE, entityType.getName());

        importerQueues[jobNo].offer(json);
    }

    private List<Property> getGeneratedFields(EntityType entityType) {
        List<Property> properties = generatedFields.get(entityType.getName());
        if(properties == null) {
            properties = new LinkedList<>();
            generatedFields.put(entityType.getName(), properties);

            for (Property p : entityType.getProperties()) {
                if (((ExtendedProperty)p).isDataType() && (
                    ((ExtendedProperty)p).getGenerator() != null || !p.isNullable())) {
                    properties.add(p);
                }
            }
        }

        return properties;
    }
}
