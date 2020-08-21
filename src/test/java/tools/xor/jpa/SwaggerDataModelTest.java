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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.CounterGenerator;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.ToOneGenerator;
import tools.xor.Type;
import tools.xor.db.base.Person;
import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.Generator;
import tools.xor.generator.GeneratorRecipient;
import tools.xor.generator.StringTemplate;
import tools.xor.logic.DefaultQueryOperation;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.Shape;
import tools.xor.service.Transaction;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-swagger-test.xml" })
@Transactional
public class SwaggerDataModelTest extends DefaultQueryOperation {

    @Resource(name = "amSwagger")
    protected AggregateManager amSwagger;
    
    @Resource(name = "amJDBCjpa")
    protected AggregateManager amJDBC;  // Useful for generating data using JDBC    
    
    final String NAME = "GEORGE_WASHINGTON_4";
    final String DISPLAY_NAME = "George Washington";
    final String DESCRIPTION = "First President of the United States of America";
    final String USER_NAME = "gwashington";    

    @Test
    public void buildModel() {
        DataModel model = amSwagger.getDataModel();
        System.out.println("It was successful!");

        // create person
        Person person = new Person();
        person.setName(NAME);
        person.setDisplayName(DISPLAY_NAME);
        person.setDescription(DESCRIPTION);
        person.setUserName(USER_NAME);
        
        // add a date field, big decimal field, boolean field

        person = (Person) aggregateService.create(person, new Settings());

        // read the person object using a DataObject
        Settings settings = new Settings();
        JSONObject p = new JSONObject();
        p.put("id", person.getId());
        settings.setView(amSwagger.getView("BASICINFO_OQL"));
        Type personType = model.getShape().getType("Person");
        settings.setEntityType(personType);
        settings.setPreFlush(true);
        List<?> toList = amSwagger.query(p, settings);

        assert (toList.size() == 1);
        JSONObject result = null;
        if (JSONObject.class.isAssignableFrom(toList.get(0).getClass()))
            result = (JSONObject) toList.get(0);

        assert (result != null);
        assert (result.get("name").equals(NAME));
        assert (result.get("displayName").equals(DISPLAY_NAME));
        assert (result.get("description").equals(DESCRIPTION));
    }
    
    @Test
    public void testPaging() {
        Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if (shape == null) {
            shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }

        JDBCType task = (JDBCType) shape.getType("TASK");
        task.clearGenerators();

        Generator rootidgen = new StringTemplate(new String[] { "ID_[VISITOR_CONTEXT]" });
        ExtendedProperty rootid = (ExtendedProperty) task.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        Generator namegen = new StringTemplate(new String[] { "NAME_[VISITOR_CONTEXT]" });
        ExtendedProperty namep = (ExtendedProperty) task.getProperty("NAME");
        namep.setGenerator(namegen);
        ExtendedProperty dtype = (ExtendedProperty) task.getProperty("DTYPE");
        Generator dtypegen = new DefaultGenerator(new String[] { "Task" });
        dtype.setGenerator(dtypegen);

        // create 200 tasks with a page size of 25
        // We start at 10000 to avoid any conflict with existing data created by tests
        // that did not cleanup properly
        CounterGenerator gensettings = new CounterGenerator(200, 10000);
        task.addGenerator(gensettings);

        String[] types = new String[] { "TASK" };

        Settings settings = new Settings();
        // settings.setImportMethod(ImportMethod.CSV);
        Transaction tx = amJDBC.createTransaction(settings);
        tx.begin();
        try {
            amJDBC.generate(shape.getName(), Arrays.asList(types), settings);

            // read page 1
            settings = new Settings();
            settings.setView(amSwagger.getView("BASICINFO_OQL_SORT"));
            DataModel model = amSwagger.getDataModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);
            Map<String, Object> userParams = new HashMap<>();
            userParams.put("page", null);
            settings.setParams(userParams);
            
            // Get the first page
            settings.setOffset(0);
            settings.setLimit(25);            
            List<?> toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);
            JSONObject first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_10000"));

            // Get the second page
            settings.setOffset(25);
            toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);
            first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_10025"));            
            
        } finally {

            deleteEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }

    }
    
    private void deleteEntries() {
        JDBCDataStore po = (JDBCDataStore) amJDBC.getDataStore();
        JDBCSessionContext sc = po.getSessionContext();

        try (Statement stmt = sc.getConnection().createStatement()) {
            stmt.execute("DELETE from TASK");
            stmt.execute("DELETE from PERSON");
            sc.getConnection().commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }        
    }
    
    @Test
    public void testScrolling() {
        Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if (shape == null) {
            shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }

        JDBCType task = (JDBCType) shape.getType("TASK");
        task.clearGenerators();

        Generator rootidgen = new StringTemplate(new String[] { "ID_[VISITOR_CONTEXT]" });
        ExtendedProperty rootid = (ExtendedProperty) task.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        Generator namegen = new StringTemplate(new String[] { "NAME_[VISITOR_CONTEXT]" });
        ExtendedProperty namep = (ExtendedProperty) task.getProperty("NAME");
        namep.setGenerator(namegen);
        ExtendedProperty dtype = (ExtendedProperty) task.getProperty("DTYPE");
        Generator dtypegen = new DefaultGenerator(new String[] { "Task" });
        dtype.setGenerator(dtypegen);

        // create 200 tasks with a page size of 25
        // We start at 10000 to avoid any conflict with existing data created by tests
        // that did not cleanup properly
        CounterGenerator gensettings = new CounterGenerator(200, 10000);
        task.addGenerator(gensettings);

        String[] types = new String[] { "TASK" };

        Settings settings = new Settings();
        // settings.setImportMethod(ImportMethod.CSV);
        Transaction tx = amJDBC.createTransaction(settings);
        tx.begin();
        try {
            amJDBC.generate(shape.getName(), Arrays.asList(types), settings);

            // read page 1
            settings = new Settings();
            settings.setView(amSwagger.getView("BASICINFO_OQL_SORT"));
            DataModel model = amSwagger.getDataModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);
            Map<String, Object> userParams = new HashMap<>();
            userParams.put("scroll", null);
            settings.setParams(userParams);
            
            // Get the first page
            settings.setLimit(25);            
            List<?> toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);
            JSONObject first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_10000"));

            // Get the second page
            Map<String, Object> nextToken = new HashMap<>();
            nextToken.put("startName", "NAME_10035");
            nextToken.put("startId", "ID_10035");    
            settings.setNextToken(nextToken);
            toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);
            first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_10036"));            
            
        } finally {

            deleteEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }        
    }
    
    @Test
    public void testToOne() {
        // Hibernate: insert into Person (createdBy_UUID, createdOn, updatedBy_UUID, updatedOn, version, description, detailedDescription, displayName, iconUrl, isCriticalSystemObject, name, objectId, commonName, email, password, photo, userName, UUID)
        
        Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if (shape == null) {
            shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }

        // Task
        
        JDBCType task = (JDBCType) shape.getType("TASK");
        task.clearGenerators();

        Generator rootidgen = new StringTemplate(new String[] { "ID_[VISITOR_CONTEXT]" });
        ExtendedProperty rootid = (ExtendedProperty) task.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        Generator namegen = new StringTemplate(new String[] { "NAME_[VISITOR_CONTEXT]" });
        ExtendedProperty namep = (ExtendedProperty) task.getProperty("NAME");
        namep.setGenerator(namegen);
        ExtendedProperty dtype = (ExtendedProperty) task.getProperty("DTYPE");
        Generator dtypegen = new DefaultGenerator(new String[] { "Task" });
        dtype.setGenerator(dtypegen);
        ExtendedProperty assignedTo = (ExtendedProperty) task.getProperty("ASSIGNEDTO_UUID");
        assignedTo.setGenerator(rootidgen);        
        
        // create 200 tasks with a page size of 25
        // We start at 10000 to avoid any conflict with existing data created by tests
        // that did not cleanup properly
        CounterGenerator gensettings = new CounterGenerator(200, 10000);
        task.addGenerator(gensettings);
        
        
        // Person
        JDBCType person = (JDBCType) shape.getType("PERSON");
        person.clearGenerators();   
        
        rootidgen = new StringTemplate(new String[] { "ID_[VISITOR_CONTEXT]" });
        rootid = (ExtendedProperty) person.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        namegen = new StringTemplate(new String[] { "USERNAME_[VISITOR_CONTEXT]" });
        namep = (ExtendedProperty) person.getProperty("NAME");
        namep.setGenerator(namegen);  
        gensettings = new CounterGenerator(200, 10000);
        person.addGenerator(gensettings);        

        String[] types = new String[] { "PERSON", "TASK" };
        Settings settings = new Settings();
        // settings.setImportMethod(ImportMethod.CSV);
        Transaction tx = amJDBC.createTransaction(settings);
        tx.begin();
        try {
            amJDBC.generate(shape.getName(), Arrays.asList(types), settings);

            
            // Update swagger
            // Create view with assignedTo relationship
            
            // read page 1
            settings = new Settings();
            settings.setView(amSwagger.getView("BASICINFO_TO_ONE_OQL"));
            DataModel model = amSwagger.getDataModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);
            
            // Get the first page
            settings.setLimit(25);            
            List<?> toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);
            JSONObject first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_10000"));
            JSONObject assignedToJson = first.getJSONObject("assignedTo");
            assert(assignedToJson.get("name").equals("USERNAME_10000"));

        } finally {

            deleteEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }            
    }
    
    @Test
    public void testToManySimple() {
        // First create a task with 100 children
        // We use the generators to create this data
        // This data is committed by the generators

        Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if(shape == null) {
            shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }


        JDBCType task = (JDBCType)shape.getType("TASK");
        task.clearGenerators();

        ExtendedProperty taskparent = (ExtendedProperty)task.getProperty("TASKPARENT_UUID");
        Generator parentgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
        taskparent.setGenerator(parentgen);
        Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
        ExtendedProperty rootid = (ExtendedProperty)task.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        Generator namegen = new StringTemplate(new String[] {"NAME_[VISITOR_CONTEXT]"});
        ExtendedProperty namep = (ExtendedProperty)task.getProperty("NAME");
        namep.setGenerator(namegen);
        ExtendedProperty dtype = (ExtendedProperty)task.getProperty("DTYPE");
        Generator dtypegen = new DefaultGenerator(new String[] {"Task"});
        dtype.setGenerator(dtypegen);

        ToOneGenerator toonegen = new ToOneGenerator(new String[] { "0",
                                                                    "0,0:0", // root task
                                                                    "1,100:100" // root task with 100 children
        });

        // 101 is the total number of tasks. 1 root task and 100 child tasks
        CounterGenerator gensettings = new CounterGenerator(101);
        gensettings.addListener(toonegen);
        task.addGenerator(gensettings);
        gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
                (GeneratorRecipient)parentgen));

        String[] types = new String[] {
            "TASK"
        };

        Settings settings = new Settings();
        //settings.setImportMethod(ImportMethod.CSV);
        Transaction tx = amJDBC.createTransaction(settings);
        tx.begin();
        try {
            // Generate the tasks in the DB
            amJDBC.generate(shape.getName(), Arrays.asList(types), settings);  
            
            // Query using OQL
            settings = new Settings();
            settings.setView(aggregateService.getView("BASICINFO_TO_MANY_SIMPLE_OQL"));
            DataModel model = amSwagger.getDataModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);  
            
            List<?> result = amSwagger.query(null, settings);
            assert(result.size() == 101);
            // The first element has 100 subtasks
            JSONObject first = (JSONObject) result.get(0);
            JSONArray subTasks = first.getJSONArray("subTasks");
            assert(subTasks.length() == 100);
            
        } finally {

            deleteEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }              
    }
    
    @Test
    public void testToManyEntity() {
        // First create a task with 100 children
        // We use the generators to create this data
        // This data is committed by the generators

        Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if(shape == null) {
            shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }


        JDBCType task = (JDBCType)shape.getType("TASK");
        task.clearGenerators();

        ExtendedProperty taskparent = (ExtendedProperty)task.getProperty("TASKPARENT_UUID");
        Generator parentgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
        taskparent.setGenerator(parentgen);
        Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
        ExtendedProperty rootid = (ExtendedProperty)task.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        Generator namegen = new StringTemplate(new String[] {"NAME_[VISITOR_CONTEXT]"});
        ExtendedProperty namep = (ExtendedProperty)task.getProperty("NAME");
        namep.setGenerator(namegen);
        ExtendedProperty dtype = (ExtendedProperty)task.getProperty("DTYPE");
        Generator dtypegen = new DefaultGenerator(new String[] {"Task"});
        dtype.setGenerator(dtypegen);

        ToOneGenerator toonegen = new ToOneGenerator(new String[] { "0",
                                                                    "0,0:0", // root task
                                                                    "1,100:100" // root task with 100 children
        });

        // 101 is the total number of tasks. 1 root task and 100 child tasks
        CounterGenerator gensettings = new CounterGenerator(101);
        gensettings.addListener(toonegen);
        task.addGenerator(gensettings);
        gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
                (GeneratorRecipient)parentgen));

        String[] types = new String[] {
            "TASK"
        };

        Settings settings = new Settings();
        //settings.setImportMethod(ImportMethod.CSV);
        Transaction tx = amJDBC.createTransaction(settings);
        tx.begin();
        try {
            // Generate the tasks in the DB
            amJDBC.generate(shape.getName(), Arrays.asList(types), settings);  
            
            // Query using OQL
            settings = new Settings();
            settings.setView(aggregateService.getView("BASICINFO_TO_MANY_ENTITY_OQL"));
            DataModel model = amSwagger.getDataModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);  
            
            List<?> result = amSwagger.query(null, settings);
            assert(result.size() == 101);
            // The first element has 100 subtasks
            JSONObject first = (JSONObject) result.get(0);
            JSONArray subTasks = first.getJSONArray("childTasks");
            assert(subTasks.length() == 100);
            JSONObject firstSubTask = subTasks.getJSONObject(0);
            assert(firstSubTask.get("name").equals("NAME_1"));
            
        } finally {

            deleteEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }            
    }    
    
    @Test
    public void testToManySimpleMulti() {
        // First create a task with 100 children
        // We use the generators to create this data
        // This data is committed by the generators

        Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if(shape == null) {
            shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
        }


        JDBCType task = (JDBCType)shape.getType("TASK");
        task.clearGenerators();

        ExtendedProperty taskparent = (ExtendedProperty)task.getProperty("TASKPARENT_UUID");
        Generator parentgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
        taskparent.setGenerator(parentgen);
        Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
        ExtendedProperty rootid = (ExtendedProperty)task.getProperty("UUID");
        rootid.setGenerator(rootidgen);
        Generator namegen = new StringTemplate(new String[] {"NAME_[VISITOR_CONTEXT]"});
        ExtendedProperty namep = (ExtendedProperty)task.getProperty("NAME");
        namep.setGenerator(namegen);
        ExtendedProperty dtype = (ExtendedProperty)task.getProperty("DTYPE");
        Generator dtypegen = new DefaultGenerator(new String[] {"Task"});
        dtype.setGenerator(dtypegen);

        ToOneGenerator toonegen = new ToOneGenerator(new String[] { "0",
                                                                    "0,0:0", // root task
                                                                    "1,100:100" // root task with 100 children
        });

        // 101 is the total number of tasks. 1 root task and 100 child tasks
        CounterGenerator gensettings = new CounterGenerator(101);
        gensettings.addListener(toonegen);
        task.addGenerator(gensettings);
        gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
                (GeneratorRecipient)parentgen));

        String[] types = new String[] {
            "TASK"
        };

        Settings settings = new Settings();
        //settings.setImportMethod(ImportMethod.CSV);
        Transaction tx = amJDBC.createTransaction(settings);
        tx.begin();
        try {
            // Generate the tasks in the DB
            amJDBC.generate(shape.getName(), Arrays.asList(types), settings);  
            
            // Query using OQL
            settings = new Settings();
            settings.setView(aggregateService.getView("GROUP_TASK_SUBTASKS"));
            DataModel model = amSwagger.getDataModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);  
            Map<String, Object> userParams = new HashMap<>();
            userParams.put("scroll", null);
            settings.setParams(userParams);
            
            // Get first page
            settings.setLimit(25);
            Map<String, Object> nextToken = new HashMap<>();
            //nextToken.put("startName", "NAME_0");
            //nextToken.put("startId", "ID_0");   
            //settings.setNextToken(nextToken);
            
            // TODO: Limit should apply only to the root query
            
            List<?> toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);
            JSONObject first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_0"));
            JSONArray subTasks = first.getJSONArray("subTasks");
            assert(subTasks.length() == 100);            
            
            // Get second page
            nextToken = new HashMap<>();
            nextToken.put("startName", "NAME_45");
            nextToken.put("startId", "ID_45");    
            settings.setNextToken(nextToken);
            toList = amSwagger.query(null, settings);
            assert (toList.size() == 25);     
            
            first = (JSONObject) toList.get(0);
            assert (first.get("name").equals("NAME_46"));             
            
        } finally {

            deleteEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }              
    }    
    
    @Test
    public void negativeTest() {
        
        // Query using OQL
        Settings settings = new Settings();
        settings.setView(aggregateService.getView("BASICINFO_TO_MANY_SIMPLE_MULTI_OQL"));
        DataModel model = amSwagger.getDataModel();
        Type taskType = model.getShape().getType("Task");
        settings.setEntityType(taskType);  
        
        List<?> result = amSwagger.query(null, settings);
        assert(result.size() == 0);        
    }
    
    @Test
    public void jsonSchemaTest() {
        JSONObject json = aggregateService.getDataModel().getShape().getJsonSchema();
        System.out.println("JPA Model: " + json.toString());
    }
}
