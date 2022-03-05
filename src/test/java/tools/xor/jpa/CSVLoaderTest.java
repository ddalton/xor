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

package tools.xor.jpa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.generator.Generator;
import tools.xor.generator.StringTemplate;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.DataModelFactory;
import tools.xor.service.Shape;
import tools.xor.service.exim.CSVLoader;
import tools.xor.service.exim.CSVLoader.CSVState;
import tools.xor.util.ClassUtil;
import tools.xor.util.Edge;
import tools.xor.util.graph.DirectedSparseGraph;
import tools.xor.util.graph.Graph;
import tools.xor.view.AggregateView;

@ExtendWith(SpringExtension.class)
@ExtendWith(CSVLoaderTest.TraceUnitExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-mutable-JSON-jpa-test.xml" })
public class CSVLoaderTest {

	@PersistenceContext
	EntityManager entityManager;

	@Resource(name = "amJDBCjson")
	protected AggregateManager amJDBC;	// Useful for generating data using JDBC
	
	public static class TraceUnitExtension implements BeforeEachCallback {
	    
	    @Override
	    public void beforeEach(ExtensionContext context) throws Exception {
            System.out.println("@@@ Starting test: " + context.getDisplayName());
	    }
	}	
	
	@BeforeAll
	public static void setup() {
        
        ClassUtil.setParallelDispatch(false);
	}
	
    @AfterAll
    public static void teardown() {
        
        ClassUtil.setParallelDispatch(true);
    }	
	
	@Test
	public void test1() throws IOException {
	    String testFolder = "csvloader/test1/";
	    
	    if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
	        throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test1", testFolder));
	    }
	    
	    List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
	    for(String file: files) {
	        System.out.println(file);
	    }
	    
	    DataModel dm = amJDBC.getDataModel();
	    Shape shape = dm.getShape();
	    boolean modelsRelationships = true;
	    if(shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
	        modelsRelationships = false;
	    }
	    
	    CSVLoader csvLoader = new CSVLoader(shape, testFolder);
	    
	   Graph<CSVState, Edge<CSVState>> graph = csvLoader.getGraph();
	   for(CSVState state: graph.getVertices()) {
	       // set the generator for the primary key
	       Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
	       EntityType entityType = (EntityType) state.getType();
	       Property p = entityType.getProperty("UUID");
	       p.setGenerator(rootidgen);
	   }
	    
	    amJDBC.configure(null);
	    JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();	  
        try {
            csvLoader.importData(new Settings(), dataStore);
            
            // Load a task 
            JSONObject queryTask = new JSONObject();
            queryTask.put("UUID", "ID_4");
            
            // Cannot use relationships since
            // some JUnit tests use DataModel.RELATIONAL_SHAPE and this does not have relationship
            List<String> paths = new ArrayList<>();
            paths.add("UUID");
            paths.add("NAME");
            paths.add("CREATEDON");
            paths.add("OWNEDBY_UUID");
            AggregateView ownedByView = new AggregateView("OWNEDBY");
            ownedByView.setAttributeList(paths);
            
            // This whole creating the settings object needs to be updated
            Settings settings = new Settings();
            settings.setSessionContext(sc);
            // Why is the shape getting un-initialized when called after settings.init???
            settings.setView(ownedByView);    
            settings.setEntityType(shape.getType("TASK"));
            settings.init(shape);
                    
            List result = amJDBC.query(queryTask, settings);
            assert(result != null);
            assert(result.size() == 1);
            JSONObject json = (JSONObject)result.get(0);
            System.out.println("CSVLOADER JSON: " + json.toString());
            assert(json.getString("UUID").equals("ID_4"));
            assert(json.getString("CREATEDON").startsWith("2019-12-25T23:59:59"));
            
            JSONObject ownedBy = json;
            if(modelsRelationships) {
                ownedBy = json.getJSONObject("OWNEDBY_UUID");
                assert(ownedBy.getString("UUID").equals("ID_2"));
            } else {
                assert(ownedBy.getString("OWNEDBY_UUID").equals("ID_2"));
            }
            
            
            // Load a quote
            JSONObject quote = new JSONObject();
            quote.put("PRICE", 100.37);
            
            paths = new ArrayList<>();
            paths.add("UUID");
            paths.add("PRICE");
            AggregateView quoteView = new AggregateView("QUOTEPRICE");
            quoteView.setAttributeList(paths);
            
            // This whole creating the settings object needs to be updated
            settings = new Settings();
            settings.setSessionContext(sc);
            // Why is the shape getting un-initialized when called after settings.init???
            settings.setView(quoteView);    
            settings.setEntityType(shape.getType("QUOTE"));
            settings.init(shape);          
            
            result = amJDBC.query(quote, settings);
            assert(result != null);
            assert(result.size() == 1);
            
        } finally {
        	    sc.rollback();
                sc.close();
        }
	}	
	
    private String getAbsoluteResourcePath(String path) {
        path = "src/test/resources/" + path;
        String[] paths = path.split("/");
        String systemFilePath = String.join(File.separator, paths);
        
        File file = new File(systemFilePath);
        return file.getAbsolutePath();
    }	

    @Test
    public void test2() throws IOException {
	    String testFolder = "csvloader/test2";
        String csvFilePath = testFolder + "/Task.csv";
        String csvGenFilePath = csvFilePath + ".gen";

        String absolutePath = getAbsoluteResourcePath(csvGenFilePath);
        System.out.println("Path: " + absolutePath);

        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();
        boolean modelsRelationships = true;
        if (shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
            modelsRelationships = false;
        }

        CSVLoader csvLoader = new CSVLoader(shape);
        CSVState csvState = csvLoader.getCSVState(csvFilePath);

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore) amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            Settings settings = new Settings();
            csvState.writeToCSV(absolutePath, settings, dataStore);
            validateTask(importTasks(dataStore, settings, shape, testFolder, "(?i).*csv"), 5, shape, false);
        } finally {
            sc.rollback();
            sc.close();
        }
    }

    /**
     * Uses a QueryGenerator to populate project from task
     * @throws IOException
     */
    @Test
    public void test3() throws IOException {
        String testFolder = "csvloader/test3/";
        String csvFilePath = testFolder + "Project.csv";
        String csvGenFilePath = csvFilePath + ".gen";   
        String absolutePath = getAbsoluteResourcePath(csvGenFilePath);
        
        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test3", testFolder));
        }
        
        List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
        for(String file: files) {
            System.out.println(file);
        }
        
        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();
        boolean modelsRelationships = true;
        if(shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
            modelsRelationships = false;
        }
        
        CSVLoader csvLoader = new CSVLoader(shape, testFolder);
        
        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();    
        try {
            Graph<CSVState, Edge<CSVState>> graph = csvLoader.getGraph();
            ((DirectedSparseGraph)graph).exportToDOT("test3.dot");

            /* Uncomment the following code to write to file
            for(CSVState state: graph.getVertices()) {
                if(state.getTableName().equals("Project")) {
                    state.createCSVPrinter(absolutePath);
                }
            }
             */

            Settings settings = new Settings();
            csvLoader.importData(settings, dataStore);
            validateTask(queryTasks(settings, shape), 5, shape, false);
            validateProject(queryProjects(settings, shape), 5, shape);

        } finally {
                sc.rollback();
                sc.close();
        }
    }   
    
    @Test
    public void test4() throws IOException {
        String testFolder = "csvloader/test4/";
        
        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test4", testFolder));
        }
        
        List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
        for(String file: files) {
            System.out.println(file);
        }
        
        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();
        boolean modelsRelationships = true;
        if(shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
            modelsRelationships = false;
        }
        
        CSVLoader csvLoader = new CSVLoader(shape, testFolder);
        
       Graph<CSVState, Edge<CSVState>> graph = csvLoader.getGraph();
       for(CSVState state: graph.getVertices()) {
           // set the generator for the primary key
           Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
           EntityType entityType = (EntityType) state.getType();
           Property p = entityType.getProperty("UUID");
           p.setGenerator(rootidgen);
       }
        
        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();    
        try {
            // We are only loading the Person entries
            Settings settings = new Settings();
            csvLoader.importData(settings, dataStore);

            // We shall now generate the Task.schema.gen 
            String csvFilePath = "csvloader/test4/Task.schema";
            String csvGenFilePath = csvFilePath + ".gen";
            String absolutePath = getAbsoluteResourcePath(csvGenFilePath);
            
            CSVState csvState = csvLoader.getCSVState(csvFilePath);
            csvState.writeToCSV(absolutePath, new Settings(), dataStore);

            validateTask(importTasks(dataStore, settings, shape, testFolder), 5, shape);
            
        } finally {
                sc.rollback();
                sc.close();
        }
    } 
    
    @Test
    /*
     * Complex Test.
     * With dependsOn set
     * No CSV data. All data is generated
     * Uses CounterGenerator
     * Has foreign key column generator
     * 
     * Uses 2 csv files
     * 1. Person.csv - has CSV data
     * 2. Task.csv - fully auto generated 
     */
    public void test5() throws IOException {
        String testFolder = "csvloader/test5/";
        
        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test5", testFolder));
        }
        
        List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
        for(String file: files) {
            System.out.println(file);
        }
        
        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();
        boolean modelsRelationships = true;
        if(shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
            modelsRelationships = false;
        }
        
        CSVLoader csvLoader = new CSVLoader(shape, testFolder);
        
        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();    
        try {
            // We are only loading the Person entries
            Settings settings = new Settings();
            csvLoader.importData(settings, dataStore);
            
            // We shall now generate the Task.csv.gen 
            String csvFilePath = "csvloader/test5/Task.schema";
            String csvGenFilePath = csvFilePath + ".gen";
            String absolutePath = getAbsoluteResourcePath(csvGenFilePath);            
            
            CSVState csvState = csvLoader.getCSVState(csvFilePath);
            csvState.writeToCSV(absolutePath, new Settings(), dataStore);

            validateTask(importTasks(dataStore, settings, shape, testFolder), 10, shape);
            
        } finally {
                sc.rollback();
                sc.close();
        }
    }
    
    /*
     * Test used to generate a csv file with 1 million records.
     * To use this rename test6/Task.csv.gen to test6/Task.csv
     *
    @Test
    public void test6() throws IOException {
        String csvFilePath = "csvloader/test6/Task.csv.gen";
        String absolutePath = getAbsoluteResourcePath("csvloader/test6/Task.1M.csv");
        System.out.println("Path: " + absolutePath);

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore) amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            DataModel dm = amJDBC.getDataModel();
            Shape shape = dm.getShape();
            CSVLoader csvLoader = new CSVLoader(shape);
            CSVState csvState = csvLoader.getCSVState(csvFilePath);

            csvState.writeToCSV(absolutePath, new Settings(), dataStore);
        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            sc.rollback();
            sc.close();
        }
    }
     */
    
    @Test
    public void test6_import() throws IOException {

        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();
        String testFolder = "csvloader/test6";
        CSVLoader csvLoader = new CSVLoader(shape, testFolder);

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore) amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            //csvLoader.importData(new Settings(), dataStore);
            Settings settings = new Settings();
            csvLoader.importDataParallel(settings, amJDBC.getDataModelFactory(), 4);
            validateTask(queryTasks(settings, shape), 24997, shape, false);
        } finally {
            try (Statement stmt = sc.getConnection().createStatement()) {
                stmt.execute("DELETE from TASK");
                sc.getConnection().commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }             
            
            sc.rollback();
            sc.close();
        }
    }

    @Test
    /*
     * Direct entity generator Test.
     *
     * Uses 2 csv files
     * 1. Person.csv - has CSV data
     * 2. Task.csv - fully auto generated
     */
    public void test7() throws IOException {
        String testFolder = "csvloader/test7/";

        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test7", testFolder));
        }

        List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
        for(String file: files) {
            System.out.println(file);
        }

        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();

        CSVLoader csvLoader = new CSVLoader(shape, testFolder);

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            // We are only loading the Person entries
            Settings settings = new Settings();
            csvLoader.importData(settings, dataStore);

            // We shall now generate the Task.schema.gen
            String csvFilePath = "csvloader/test7/Task.schema";
            String csvGenFilePath = csvFilePath + ".gen";
            String absolutePath = getAbsoluteResourcePath(csvGenFilePath);

            CSVState csvState = csvLoader.getCSVState(csvFilePath);
            csvState.writeToCSV(absolutePath, new Settings(), dataStore);

            validateTask(importTasks(dataStore, settings, shape, testFolder), 20, shape);

        } finally {
            sc.rollback();
            sc.close();
        }
    }

    /**
     * Tests a columnGenerator on a foreign key that has all tasks with only the same owner
     * @throws IOException
     */
    @Test
    public void test8() throws IOException {
        String testFolder = "csvloader/test8/";

        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test8", testFolder));
        }

        List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
        for(String file: files) {
            System.out.println(file);
        }

        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();

        CSVLoader csvLoader = new CSVLoader(shape, testFolder);

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            // We are only loading the Person entries
            Settings settings = new Settings();
            csvLoader.importData(settings, dataStore);

            // We shall now generate the Task.schema.gen
            String csvFilePath = testFolder + "Task.schema";
            String csvGenFilePath = csvFilePath + ".gen";
            String absolutePath = getAbsoluteResourcePath(csvGenFilePath);

            CSVState csvState = csvLoader.getCSVState(csvFilePath);

            csvState.writeToCSV(absolutePath, settings, dataStore);

            validateSameOwner(importTasks(dataStore, settings, shape, testFolder), 20, shape);

        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.rollback();
            sc.close();
        }
    }

    /**
     * Uses Spring config to populate data. Spring config is required to model dependencies
     * between generators
     *
     * @throws IOException
     */
    @Test
    public void test9() throws IOException {
        String testFolder = "csvloader/test9/";

        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test8", testFolder));
        }

        List<String> files = IOUtils.readLines(CSVLoaderTest.class.getClassLoader().getResourceAsStream(testFolder), StandardCharsets.UTF_8.name());
        for(String file: files) {
            System.out.println(file);
        }

        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if(shape == null) {
            shape = dm.createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }

        CSVLoader csvLoader = new CSVLoader(shape, testFolder);

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();
        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            // We are only loading the Person entries
            Settings settings = new Settings();
            csvLoader.importData(settings, dataStore);

            // We shall now generate the Task.schema.gen
            String csvFilePath = testFolder + "Task.schema";
            String csvGenFilePath = csvFilePath + ".gen";
            String absolutePath = getAbsoluteResourcePath(csvGenFilePath);

            CSVState csvState = csvLoader.getCSVState(csvFilePath);

            csvState.writeToCSV(absolutePath, settings, dataStore);

            validateTaskChildren(importTasks(dataStore, settings, shape, testFolder), 600, shape);

        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.rollback();
            sc.close();
        }
    }

    /**
     * Tests the BlockingQueue functionality of the CSVLoader.
     * @throws IOException
     */
    @Test
    public void test10() throws IOException {
        String testFolder = "csvloader/test10/";

        if (CSVLoaderTest.class.getClassLoader().getResource(testFolder) == null) {
            throw new RuntimeException(String.format("Unable to find the folder '%s' needed to run test10", testFolder));
        }

        DataModel dm = amJDBC.getDataModel();
        Shape shape = dm.getShape();

        amJDBC.configure(null);
        JDBCDataStore dataStore = (JDBCDataStore)amJDBC.getDataStore();

        JDBCSessionContext sc = dataStore.getSessionContext();
        sc.setAutoCommit(false);
        sc.beginTransaction();
        try {
            String schemaStr = "{ \"entityName\":\"tools.xor.db.pm.Task\", \"tableName\":\"Task\", \"dateFormat\":\"yyyy-MM-dd HH:mm:ss\", \"columns\":[\"UUID\", \"DTYPE\", \"name\", \"displayName\", \"description\",\"createdon\"], \"keys\" : [\"name\"], \"entityGenerator\":{\"className\":\"tools.xor.CounterGenerator\",\"arguments\":[10]},\"columnGenerators\":[ { \"column\":\"UUID\", \"className\": \"tools.xor.generator.StringTemplate\", \"arguments\": [\"ID_[VISITOR_CONTEXT]\"]}, { \"column\":\"NAME\", \"className\": \"tools.xor.generator.StringTemplate\", \"arguments\": [\"NAME[VISITOR_CONTEXT]\"]}, { \"column\":\"DISPLAYNAME\", \"className\": \"tools.xor.generator.StringTemplate\", \"arguments\": [\"Name [VISITOR_CONTEXT]\"]}, { \"column\":\"DESCRIPTION\", \"className\": \"tools.xor.generator.StringTemplate\", \"arguments\": [\"My name is [VISITOR_CONTEXT]\"]}, { \"column\":\"DTYPE\", \"className\": \"tools.xor.generator.DefaultGenerator\", \"arguments\": [\"Task\"]} ]}";
            JSONObject schema = new JSONObject(schemaStr);

            CSVLoader csvLoader = new CSVLoader(shape);
            CSVState csvState = csvLoader.getCSVState(schema, null);

            Future<CSVLoader.CreateRecordIteration> future = csvLoader.generateAsynchronous(csvState, 1000);
            CSVLoader.CreateRecordIteration iteration = future.get();

            BlockingQueue<JSONObject> queue = csvState.getBoundedQueue();
            int numRecords = 0;
            JSONObject first = queue.poll();
            while(first != null) {
                System.out.println(first.toString());
                numRecords++;
                first = queue.poll();
            }

            assert numRecords == 10;

            for(String column: iteration.getColumnList()) {
                System.out.println(column);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.rollback();
            sc.close();
        }
    }

    private List queryTasks(Settings settings, Shape shape) {
        return queryEntity(settings, shape, "TASK");
    }

    private List queryProjects(Settings settings, Shape shape) {
        return queryEntity(settings, shape, "PROJECT");
    }

    private List queryEntity(Settings settings, Shape shape, String tableName) {
        List<String> paths = new ArrayList<>();
        paths.add("UUID");
        paths.add("TASKPARENT_UUID");
        paths.add("NAME");
        paths.add("CREATEDON");
        if(tableName.equals("TASK")) {
            paths.add("OWNEDBY_UUID");
        }
        AggregateView ownedByView = new AggregateView(tableName+"_VIEW");
        ownedByView.setAttributeList(paths);

        settings.setView(ownedByView);
        settings.setEntityType(shape.getType(tableName));
        settings.init(shape);

        List result = amJDBC.query(null, settings);

        return result;
    }

    private List importTasks(JDBCDataStore dataStore, Settings settings, Shape shape, String testFolder) {
        return importTasks(dataStore, settings, shape, testFolder, "(?i).*schema");
    }

    private List importTasks(JDBCDataStore dataStore, Settings settings, Shape shape, String testFolder, String fileFilter) {

        // Import the tasks to the DB
        CSVLoader csvLoader = new CSVLoader(shape, testFolder, fileFilter);
        csvLoader.importData(settings, dataStore);

        return queryTasks(settings, shape);
    }

    private void validateSameOwner(List tasks, int numTasks, Shape shape) {
        assert tasks.size() == numTasks;

        boolean modelsRelationships = true;
        if(shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
            modelsRelationships = false;
        }

        for(Object task: tasks) {
            JSONObject t = (JSONObject) task;

            if(modelsRelationships) {
                t = t.getJSONObject("OWNEDBY_UUID");
                assert(t.getString("UUID").equals("ID_1"));
            } else {
                assert(t.getString("OWNEDBY_UUID").equals("ID_1"));
            }
        }
    }

    private void validateTaskChildren(List tasks, int numTasks, Shape shape) {
        assert tasks.size() == numTasks;

        for(int i = 0; i < tasks.size(); i++) {
            JSONObject t = (JSONObject) tasks.get(i);

            // First 100 tasks do not have TASKPARENT_UUID
            String idNum = t.getString("UUID").substring(3);
            int id = Integer.parseInt(idNum);

            // First 100 tasks go from 2  - 101
            if(id < 102) {
                assert !t.has("TASKPARENT_UUID");
            }
        }
    }

    private void validateTask(List tasks, int numTasks, Shape shape) {
        validateTask(tasks, numTasks, shape, true);
    }

    private void validateProject(List tasks, int numTasks, Shape shape) {
        validateTask(tasks, numTasks, shape, false);
    }

    private void validateTask(List tasks, int numTasks, Shape shape, boolean validateOwner) {
        assert tasks.size() == numTasks;

        boolean modelsRelationships = true;
        if(shape.getName().equals(DataModel.RELATIONAL_SHAPE)) {
            modelsRelationships = false;
        }

        for(Object task: tasks) {
            JSONObject t = (JSONObject) task;

            t.has("CREATEDON");
            t.has("UUID");
            t.has("NAME");

            if(validateOwner) {
                if (modelsRelationships) {
                    t = t.getJSONObject("OWNEDBY_UUID");
                    assert (t.has("UUID"));
                }
                else {
                    assert (t.has("OWNEDBY_UUID"));
                }
            }
        }
    }
}
