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

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.DataStore;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;

public class DataImporter implements Callable
{
    public static int COMMIT_SIZE;

    static {
        if (ApplicationConfiguration.config().containsKey(Constants.Config.BATCH_COMMIT_SIZE)) {
            COMMIT_SIZE = ApplicationConfiguration.config().getInt(Constants.Config.BATCH_COMMIT_SIZE);
        }
        else {
            COMMIT_SIZE = 1000;
        }
    }

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private ConcurrentLinkedQueue<JSONObject> queue;
    private Settings settings;
    private TypeMapper typeMapper;
    private JDBCDataStore dataStore;
    private DataGenerator dataGenerator;

    public DataImporter(DataGenerator dataGenerator, ConcurrentLinkedQueue<JSONObject> queue, DataStore dataStore, TypeMapper typeMapper, Settings settings) {
        this.queue = queue;
        this.settings = settings;
        this.typeMapper = typeMapper;
        this.dataStore = (JDBCDataStore)dataStore;
        this.dataGenerator = dataGenerator;
    }

    public Object call() throws InterruptedException, SQLException
    {
        Object result = DataGenerator.SUCCESS;

        try {
            // Begin a new transaction
            dataStore.getSessionContext().beginTransaction();
            
            // Initial cleanup
            dataStore.getSessionContext().commit();
            
            int i = 1;
            while (true) {
                // Give a millisecond for the data to start flowing
                while (queue.isEmpty()) {
                    Thread.sleep(1);
                }

                JSONObject json = queue.remove();

                if (json == DataGenerator.END_MARKER) {
                    break;
                }

                importJson(dataStore, json, typeMapper, settings, dataGenerator);

                if (i++ % COMMIT_SIZE == 0) {
                    // commit in batches
                    commit();

                    // Begin a new transaction
                    dataStore.getSessionContext().beginTransaction();
                }
            }

            // last commit
            commit();
        } catch(Exception e) {
            // if we are here and if the queue is not empty then we empty the
            // queue, so the generator is not kept waiting
            e.printStackTrace();
            throw e;
        } finally {
            dataStore.getSessionContext().closeResources();
        }

        return result;
    }
    
    public static void importJson(
            JDBCDataStore po, 
            JSONObject json, 
            TypeMapper typeMapper, 
            Settings settings, 
            DataGenerator dataGenerator) 
                    throws SQLException {
        String entityName = json.getString(Constants.XOR.TYPE);
        Type type = typeMapper.getShape().getType(entityName);
        BusinessObject bo = new ImmutableBO(type, null, null, null);
        bo.setInstance(json);
        if(logger.isDebugEnabled()) {
            logger.debug("DataImporter#call json: " + json.toString());
        }

        po.getSessionContext().create(bo, settings, dataGenerator);
    }

    private void commit() {
        try {
            dataStore.getSessionContext().commit();
        } catch (Exception e) {
            dataStore.getSessionContext().rollback();
            throw e;
        } finally {
            dataStore.getSessionContext().close();
        }
    }
    
    public static void performFlush(JDBCSessionContext sc, int i, boolean isEnd) {
        if(!isEnd) {
            if (i % DataImporter.COMMIT_SIZE == 0) {
                // flush in batches
                // TODO: have option to commit
                sc.flush();
            }          
        } else {
            sc.flush();
        }
    }    
}
