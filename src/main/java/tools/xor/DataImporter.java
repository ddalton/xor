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
import tools.xor.providers.jdbc.JDBCPersistenceOrchestrator;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataImporter implements Callable
{
    private static int COMMIT_SIZE;

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
    private Shape shape;
    private ObjectCreator objectCreator;
    private JDBCPersistenceOrchestrator po;
    private DataGenerator dataGenerator;

    public DataImporter(DataGenerator dataGenerator, ConcurrentLinkedQueue<JSONObject> queue, PersistenceOrchestrator po, Shape shape, Settings settings) {
        this.queue = queue;
        this.settings = settings;
        this.shape = shape;
        this.objectCreator = new ObjectCreator(settings, shape, settings.getPersistenceOrchestrator(), MapperDirection.DOMAINTODOMAIN);
        this.po = (JDBCPersistenceOrchestrator)po;
        this.dataGenerator = dataGenerator;
    }

    public Object call() throws InterruptedException, SQLException
    {
        Object result = DataGenerator.SUCCESS;

        // Begin a new transaction
        po.getSessionContext().beginTransaction();

        int i = 1;
        while (true) {
            // Give a millisecond for the data to start flowing
            while(queue.isEmpty()) {
                Thread.sleep(1);
            }

            JSONObject json = queue.remove();

            if (json == DataGenerator.END_MARKER) {
                break;
            }

            String entityName = json.getString(Constants.XOR.TYPE);
            Type type = shape.getType(entityName);
            BusinessObject bo = new ImmutableBO(type, null, null, objectCreator);
            bo.setInstance(json);
            po.getSessionContext().create(bo, settings, dataGenerator);

            if (i++ % COMMIT_SIZE == 0) {
                // commit in batches
                commit();

                // Begin a new transaction
                po.getSessionContext().beginTransaction();
            }
        }

        // last commit
        commit();

        return result;
    }

    private void commit() {
        try {
            po.getSessionContext().commit();
        } catch (Exception e) {
            po.getSessionContext().rollback();
            throw e;
        } finally {
            po.getSessionContext().close();
        }
    }
}
