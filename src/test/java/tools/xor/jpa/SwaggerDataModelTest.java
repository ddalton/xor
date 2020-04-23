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
import java.util.List;

import javax.annotation.Resource;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.CounterGenerator;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.db.base.Person;
import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.Generator;
import tools.xor.generator.StringTemplate;
import tools.xor.logic.DefaultQueryOperation;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCPersistenceOrchestrator;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.Shape;
import tools.xor.service.Transaction;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-swagger-test.xml" })
@TransactionConfiguration(defaultRollback = true)
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
        DataModel model = amSwagger.getModel();
        System.out.println("It was successful!");

        // create person
        Person person = new Person();
        person.setName(NAME);
        person.setDisplayName(DISPLAY_NAME);
        person.setDescription(DESCRIPTION);
        person.setUserName(USER_NAME);
        
        // add a date field, big decimal field, boolean field

        aggregateService.create(person, new Settings());

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
        Shape shape = amJDBC.getModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
        if (shape == null) {
            shape = amJDBC.getModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
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
            DataModel model = amSwagger.getModel();
            Type taskType = model.getShape().getType("Task");
            settings.setEntityType(taskType);
            
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

            deleteTaskEntries();
            tx.rollback();
            // We don't close as the connection belongs to Spring
        }

    }
    
    private void deleteTaskEntries() {
        JDBCPersistenceOrchestrator po = (JDBCPersistenceOrchestrator) amJDBC.getPersistenceOrchestrator();
        JDBCSessionContext sc = po.getSessionContext();

        try (Statement stmt = sc.getConnection().createStatement()) {
            stmt.execute("DELETE from TASK");
            sc.getConnection().commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }        
    }
    
    @Test
    public void testScrolling() {
        
    }
}
