package tools.xor.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import tools.xor.AggregateAction;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.providers.jdbc.JDBCBatchContext;
import tools.xor.util.Constants;
import tools.xor.util.graph.TypeGraph;

/**
 * This class represents an implementation of the XOR REST api.
 * Provider implementations will extend this class with the appropriate Controller class
 * and provide the necessary transaction scaffolding, path/param mapping etc...
 */
public abstract class XorREST
{

    /**
     * AggregateManager configured with ORM datasource
     * @return AggregateManager instance
     */
    protected abstract AggregateManager getAM ();

    /**
     * AggregateManager configured with JDBC datasource
     * @return AggregateManager instance
     */
    protected abstract AggregateManager getJDBCAM ();

    /**
     * Return the settings object
     * @param jsonString JSON string containing user settings
     * @param userData any additional data passed by the user
     * @return Settings instance
     */
    protected abstract Settings getSettings (String jsonString, Object userData);

    /**
     * Method used to create an entity.
     *
     * @param jsonString JSON string of both the settings and the entity object that needs to be created
     * @return JSON object of the created entity
     */
    protected JSONObject create (String jsonString)
    {
        JSONObject json = new JSONObject(jsonString);
        Object entity = getEntity(json);

        DataModel das = getAM().getDataModel();
        String settings = json.getJSONObject("settings").toString();
        Object persistentObj = getAM().create(entity, das.settings().json(settings).build());
        Settings readSettings = das.settings().base(persistentObj.getClass()).build();
        JSONObject readJson = (JSONObject)getAM().toExternal(persistentObj, readSettings);

        return readJson;
        //readJson.toString(3);
    }

    /**
     * Read an entity from the database in JSON format
     * @param jsonString JSON string containing the user settings and the entity id/key
     * @return JSON representation of the entity
     */
    protected JSONObject read (String jsonString)
    {
        JSONObject json = new JSONObject(jsonString);
        Object entity = getEntity(json);

        DataModel das = getAM().getDataModel();
        String settings = json.getJSONObject("settings").toString();
        JSONObject readJson = (JSONObject)getAM().read(
            entity,
            das.settings().json(settings).build());
        return readJson;
    }

    /**
     * Do a partial or a full update of a persistent entity.
     * @param jsonString JSON string containing the user settings and the entity values that need to be updated.
     */
    protected void update (String jsonString)
    {
        JSONObject json = new JSONObject(jsonString);
        Object entity = getEntity(json);

        DataModel das = getAM().getDataModel();
        String settings = json.getJSONObject("settings").toString();
        getAM().update(entity, das.settings().json(settings).build());
    }

    /**
     * Delete a persistent entity from the database.
     * @param jsonString JSON string containing the user settings and the entity id/key to be deleted.
     */
    protected void delete (String jsonString)
    {
        JSONObject json = new JSONObject(jsonString);
        Object entity = getEntity(json);

        DataModel das = getAM().getDataModel();
        String settings = json.getJSONObject("settings").toString();
        getAM().delete(entity, das.settings().json(settings).build());
    }

    /**
     * Pass in a json string containing an array of file names containing the generator details.
     * Should only contain the file name and not the full path, since the file is assumed to be part
     * of the classpath.
     *
     * @param jsonString JSON string of the generator excel files
     */
    protected void initializeGenerators(String jsonString) {
        JSONArray array = new JSONArray(jsonString);

        try {
            // Make sure the files are under src/main/resources
            for(int i = 0; i < array.length(); i++) {
                Resource resource = new ClassPathResource(array.getString(i));
                InputStream inputStream = resource.getInputStream();
                initializeGenerators(inputStream);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Use this method if the user is sending the excel file directly.
     *
     * @param inputStream containing the Excel file.
     */
    protected void initializeGenerators(InputStream inputStream) {
        DataModel das = getAM().getDataModel();
        das.initGenerators(inputStream);
    }

    /**
     * Return a Graphviz file in .dot format of the State Graph
     * @param settings object
     * @return .dot format
     */
    protected File getGraphvizDOT(Settings settings) {

        TypeGraph sg = settings.getView().getTypeGraph((EntityType)settings.getEntityType());
        sg.generateVisual(settings);

        File file = new File(settings.getGraphFileName());
        if(file.exists()) {
            return file;
        }

        throw new RuntimeException("File not found: " + settings.getGraphFileName());
    }

    /**
     * Should be wrapped by the provider implementation in a transaction.
     * Alternatively, the user can wrap the populate method instead but this can cause issues
     * if the count is large.
     *
     * @param settings user settings
     * @param userData any user data needed by the provider overridden implementation
     */
    protected void populateBatch(Settings settings, Object userData) {

        TypeGraph sg = settings.getView().getTypeGraph((EntityType)settings.getEntityType());
        List<Object> entityBatch = new LinkedList<>();

        // Generate the batch
        for(int i = 0; i < settings.getBatchSize(); i++) {
            entityBatch.add(sg.generateObjectGraph(settings));
            settings.getAndIncrGlobalSeq();
        }

        // Try and persist this now
        settings.setPostFlush(true);
        getAM().create(entityBatch, settings);
    }

    /**
     * Allows the user to generate and create entities based on random input data. The input data can be configured
     * using generators specified in an Excel file.
     *
     * @param jsonString JSON string containing user settings
     * @param userData any user data that need to be passed to populateSingle
     */
    protected void populate (String jsonString, Object userData)
    {
        JSONObject jsonObj = new JSONObject(jsonString);
        int globalSeq = -1;
        if(jsonObj.has("globalSeq")) {
            globalSeq = jsonObj.getInt("globalSeq");
        }
        int iterations = -1;
        if(jsonObj.has("iterations")) {
            iterations = jsonObj.getInt("iterations");
        }
        if(iterations == -1) {
            iterations = 1;
        }
        int batchSize = 1;
        if(jsonObj.has("batchSize") && jsonObj.getInt("batchSize") > batchSize) {
            batchSize = jsonObj.getInt("batchSize");
        }

        // User should have invoked initializeGenerators, if using custom data patterns
        // during population

        Settings settings = getSettings(jsonString, userData);
        settings.setBatchSize(batchSize);
        if(globalSeq != -1) {
            settings.setGlobalSeq(globalSeq);
        }

        for(int i = 0; i < iterations; i++) {
            // NOTE: This method needs to be overridden and wrapped in transaction
            // semantics to commit the changes of the batch and not run out of memory
            populateBatch(settings, userData);
        }
    }

    /**
     * Retrieve the results of a denormalized query.
     * @param json JSON String containing the query details
     * @return results of the query as JSONArray
     */
    public String query (String json)
    {
        DataModel das = getAM().getDataModel();
        Settings settings = das.settings().json(json).build();

        List list = getAM().query(null, settings);
        JSONArray result = new JSONArray();
        for (Object obj : list) {
            result.put(obj);
        }

        return result.toString();
    }

    private JSONObject toExternal(Object persistentObj) {
        Settings readSettings = getAM().getDataModel().settings().base(persistentObj.getClass()).build();
        JSONObject readJson = (JSONObject)getAM().toExternal(persistentObj, readSettings);

        return readJson;
    }

    public String batchCRUD (InputStream jsonStream)
    {
        DataModel das = getAM().getDataModel();

        // Get the iterator from the json stream
        Settings.SettingsIterator<JSONObject> iter = das.settings().iterator(jsonStream);

        JSONObject current = iter.next();
        while (current != null) {

            Settings settings = iter.extractSettings(current);

            // pipeline the result from the previous call
            JSONObject result = null;
            if(!current.has(Constants.XOR.REST_ENTITY) && result != null) {
                current.put(Constants.XOR.REST_ENTITY, result);
            }

            switch(settings.getAction()) {
            case READ:
                result = (JSONObject)getAM().read(
                    current.getJSONObject(Constants.XOR.REST_ENTITY),
                    settings);
                break;
            case CREATE:
                result = toExternal(getAM().create(current.getJSONObject(Constants.XOR.REST_ENTITY), settings));
                break;
            case UPDATE:
            case MERGE:
                result = toExternal(getAM().update(current.getJSONObject(Constants.XOR.REST_ENTITY), settings));
                break;
            case DELETE:
                getAM().delete(current.getJSONObject(Constants.XOR.REST_ENTITY), settings);
                result = null;
                break;
            case CLONE:
                result = toExternal(getAM().clone(current.getJSONObject(Constants.XOR.REST_ENTITY), settings));
                break;
            }

            current = iter.next();
        }

        return "Success";
    }

    protected abstract BatchContext createSessionContext();

    /**
     * Send a mix of DML statements (INSERT, UPDATE, SELECT and DELETE) to be processed by the server.
     *
     * @param jsonStream containing the JSON payload
     * @return results
     */
    public String batchDML (InputStream jsonStream)
    {
        DataModel das = getAM().getDataModel();

        // Get the iterator from the json stream
        Settings.SettingsIterator<Settings> iter = das.settings().iterator(jsonStream);

        Settings current = iter.next();
        Settings next = iter.next();

        BatchContext bc = createSessionContext();
        current.setSessionContext(bc);
        while (current != null) {

            // Check the next settings to see if it is eligible for batching
            if (next != null) {
                if (current.getView().getName().equals(next.getView().getName())) {
                    if (current.getSessionContext() == null) {
                        current.setSessionContext(bc);
                        bc.setShouldBatch(true);
                        next.setSessionContext(bc);
                    }
                    else {
                        // propagate the batch query
                        bc.setShouldBatch(true);
                        next.setSessionContext(current.getSessionContext());
                    }
                }
                else {
                    if (current.getSessionContext() != null) {
                        ((BatchContext)current.getSessionContext()).setShouldBatch(false);
                    }
                }
            }
            else {
                if (current.getSessionContext() != null) {
                    ((BatchContext)current.getSessionContext()).setShouldBatch(false);
                }
            }

            if (current.getAction() == AggregateAction.READ) {
                List list = (List)getAM().dml(current);
                //TODO: String output = "Retrieved " + (list.size()-1) + " records.\r\n";
            }
            else {
                Integer count = (Integer)getAM().dml(current);
                //TODO: String output = (count >= 0) ? "Updated " + count + " records.\r\n" : "Executed " + (-count) + " SQLs";
            }

            current = next;
            next = iter.next();
        }

        return "Success";
    }

    /**
     * Execute the ANSI SQL DML statements directly, bypassing the ORM. Needed if the
     * ORM does not provide JDBC access.
     *
     * @param jsonStream JSON stream containg the encoded DML statements
     * @return status string
     */
    public String batchJDBC (InputStream jsonStream)
    {
        DataModel das = getJDBCAM().getDataModel();

        // Get the iterator from the json stream
        Settings.SettingsIterator<Settings> iter = das.settings().iterator(jsonStream);

        Settings current = iter.next();
        Settings next = iter.next();
        while (current != null) {

            // Check the next settings to see if it is eligible for batching
            if (next != null) {
                if (current.getView().getName().equals(next.getView().getName())) {
                    if (current.getSessionContext() == null) {
                        JDBCBatchContext bc = new JDBCBatchContext();
                        current.setSessionContext(bc);
                        bc.setShouldBatch(true);
                        next.setSessionContext(bc);
                    }
                    else {
                        // propagate the batch query
                        next.setSessionContext(current.getSessionContext());
                    }
                }
                else {
                    if (current.getSessionContext() != null) {
                        ((JDBCBatchContext)current.getSessionContext()).setShouldBatch(false);
                    }
                }
            }
            else {
                if (current.getSessionContext() != null) {
                    ((JDBCBatchContext)current.getSessionContext()).setShouldBatch(false);
                }
            }

            if (current.getAction() == AggregateAction.READ) {
                List list = (List)getJDBCAM().dml(current);
                //String output = "[JDBC] Retrieved " + (list.size()-1) + " records.\r\n";
                //responseWriter.write(output);
                JSONArray result = new JSONArray();
                for (Object obj : list) {
                    result.put(obj);
                }
            }
            else {
                Integer count = (Integer)getJDBCAM().dml(current);
                // TODO: String output = (count >= 0) ? "[JDBC] Updated " + count + " records.\r\n" : "Executed " + (-count) + " SQLs";
            }

            current = next;
            next = iter.next();
        }

        return "Success";
    }

    public JSONObject getEntityNames (String json)
    {
        JSONObject result = new JSONObject();

        List<String> list = getAM().getMetaModel().getEntityNames();
        result.put("entityNames", list);

        return result;
    }

    public JSONObject getEntityProperties (String json)
    {
        JSONObject input = new JSONObject(json);
        String entityName = input.getString("entityName");

        List<String> list = getAM().getMetaModel().getEntityProperties(entityName);
        JSONObject result = new JSONObject();
        result.put("properties", list);

        return result;
    }

    private Object getEntity(JSONObject jsonObject) {

        if(!jsonObject.has(Constants.XOR.REST_SETTINGS)) {
            throw new RuntimeException("settings property is required");
        }
        if(!jsonObject.has("entity")) {
            throw new RuntimeException("entity information is required");
        }

        return jsonObject.getJSONObject("entity");
    }
}
