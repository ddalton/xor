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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.providers.jdbc.ImportMethod;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.DataModelFactory;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

/**
 * Generate JSONObject instances based on the GeneratorSettings for the Entity types in the shape.
 * Multiple instances of this class can be created.
 */
public class DataGenerator
{
    // value > 1 only makes sense if sameThread is false
    public static final int IMPORTER_POOL_SIZE;
    
    static {
        int poolSize = 1;
        if (ApplicationConfiguration.config().containsKey(Constants.Config.IMPORTER_POOL_SIZE)) {
            poolSize = ApplicationConfiguration.config().getInt(Constants.Config.IMPORTER_POOL_SIZE);
            if(poolSize < 1) {
                poolSize = 1;
            }
        }

        IMPORTER_POOL_SIZE = poolSize;
    }    

    // Used for throttle control
    public static final int HIGH_WATERMARK = 1500;
    public static final int LOW_WATERMARK = 1000;

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    public static final JSONObject END_MARKER = new JSONObject();
    public static final Object SUCCESS = new Object();

    private TypeMapper typeMapper;
    private Settings settings;
    private List<String> types;
    private DataModelFactory dataModelFactory;
    private ImportMethod importMethod;
    private ExecutorService importers = Executors.newFixedThreadPool(DataGenerator.IMPORTER_POOL_SIZE);
    private Map<String, List<Property>> generatedFields = new HashMap<>();
    private boolean sameThread;

    // DataGenerator and DataImporter communication data structures
    private ConcurrentLinkedQueue[] importerQueues = new ConcurrentLinkedQueue[IMPORTER_POOL_SIZE];
    private boolean reachedHigh[] = new boolean[DataGenerator.IMPORTER_POOL_SIZE];

    public DataGenerator (List<String> types, TypeMapper typeMapper, Settings settings, DataModelFactory dasFactory, boolean sameThread) {
        this.types = types;
        this.typeMapper = typeMapper;
        this.settings = settings;
        this.dataModelFactory = dasFactory;
        this.importMethod = settings.getImportMethod();
        this.sameThread = sameThread;
    }
    
    public DataGenerator (List<String> types, TypeMapper typeMapper, Settings settings, DataModelFactory dasFactory) {
        this(types, typeMapper, settings, dasFactory, false);
    }    

    /**
     * Get all the types and its supertypes
     * @return set of the types to remove duplicates
     */
    private Set<EntityType> getFlattenedTypes() {
        Set<EntityType> result = new HashSet<>();

        for(String typename: types) {
            Type type = typeMapper.getShape().getType(typename);
            if(hasGenerator(type)) {
                EntityType entityType = (EntityType)type;
                while(entityType != null) {
                    result.add(entityType);
                    entityType = entityType.getParentType();
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
    public void execute() {
        if(importMethod == ImportMethod.CSV) {
            for(EntityType entityType: getFlattenedTypes()) {
                writeHeader(entityType);
            }
        }

        Set<String> processed = new HashSet<>();
        for(String typename: types) {
            Type type = typeMapper.getShape().getType(typename);
            if(hasGenerator(type)) {
                if(sameThread) {
                    generateInstancesSameThread((EntityType)type, settings);
                } else {
                    generateInstancesUsingJobs((EntityType)type, settings);
                }
                processed.add(typename);
            }
        }
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

    private void initQueues() {
        for(int i = 0; i < IMPORTER_POOL_SIZE; i++) {
            importerQueues[i] = new ConcurrentLinkedQueue();;
            reachedHigh[i] = false;
        }
    }

    private List<Future> createImportJobs() {
        initQueues();

        // Create the importers
        List<Future> importJobs = new ArrayList<Future>();
        for (int i = 0; i < IMPORTER_POOL_SIZE; i++) {
            JDBCDataStore dataStore = (JDBCDataStore)dataModelFactory.createDataStore(settings.getSessionContext());
            dataStore.getSessionContext().setImportMethod(importMethod);
            if(importMethod == ImportMethod.CSV && IMPORTER_POOL_SIZE > 1)
            {
                throw new RuntimeException("Writing to CSV can have only 1 importer job");
            }

            importJobs.add(importers.submit(new DataImporter(this, importerQueues[i], dataStore, typeMapper, settings)));
        }

        return importJobs;
    }

    public void generateInstancesUsingJobs(EntityType entityType, Settings settings)
    {
        for(GeneratorDriver generator : entityType.getGenerators()) {
            System.out.println(String.format("Generating data for entity: %s", entityType.getName()));

            List<Future> importJobs = createImportJobs();

            StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(
                null,
                settings,
                null);

            JDBCDataStore dataStore = (JDBCDataStore)settings.getDataStore();
            JDBCSessionContext sc = dataStore.getSessionContext();
            sc.beginTransaction();

            generator.init(sc.getConnection(), visitor);
            generator.processVisitors();

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
                        // First check if the importer ran into an exception
                        if(importJobs.get(jobNo).isDone()) {
                            try {
                                importJobs.get(jobNo).get();
                            }
                            catch (ExecutionException e) {
                                // If an exception occurred we stop right away
                                throw ClassUtil.wrapRun(e);
                            }
                        }
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
            sc.close();

            if(waitForJobs(importJobs)) {
                break;
            }
        }
    }
    
    /*
     * This function uses the same thread as the generator to import the data
     * so rolling back the JDBC transaction by the caller rolls back the generated data
     * 
     * Whereas if the import jobs are used then the caller has to explicitly delete the data
     */
    public void generateInstancesSameThread(EntityType entityType, Settings settings)
    {
        for(GeneratorDriver generator : entityType.getGenerators()) {
            System.out.println(String.format("Generating data for entity: %s", entityType.getName()));

            StateGraph.ObjectGenerationVisitor visitor = new StateGraph.ObjectGenerationVisitor(
                null,
                settings,
                null);

            JDBCDataStore dataStore = (JDBCDataStore)settings.getDataStore();
            JDBCSessionContext sc = dataStore.getSessionContext();
            assert sc.getConnection() != null : "Can import only in the context of an existing JDBC connection";
            
            generator.init(sc.getConnection(), visitor);
            generator.processVisitors();
            Iterator iter = (Iterator) generator;
            
            int i = 1;
            while(iter.hasNext()) {
                if(iter.next() == null) {
                    continue;
                }

                JSONObject json = generateObject(entityType, visitor, -1);
                try {
                    DataImporter.importJson(dataStore, json, typeMapper, settings, this);
                } catch(SQLException sqe) {
                    throw new RuntimeException(sqe);
                }
                
                DataImporter.performFlush(sc, i++, false);
            }
            
            // last flush
            DataImporter.performFlush(sc, i, true);  
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

    private JSONObject generateObject(EntityType entityType, StateGraph.ObjectGenerationVisitor visitor, int jobNo) {
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

            currentType = currentType.getParentType();
        }
        json.put(Constants.XOR.TYPE, entityType.getName());

        if(jobNo >= 0) {
            importerQueues[jobNo].offer(json);
        }
        
        // Needed when running within same thread
        return json;
    }

    public List<Property> getGeneratedFields(EntityType entityType) {
        List<Property> properties = generatedFields.get(entityType.getName());
        if(properties == null) {
            properties = new LinkedList<>();
            generatedFields.put(entityType.getName(), properties);

            for (Property p : entityType.getDeclaredProperties()) {
                if (((ExtendedProperty)p).isDataType() && (
                    ((ExtendedProperty)p).getGenerator() != null || !p.isNullable())) {
                    properties.add(p);
                }
            }
        }

        return properties;
    }
}
