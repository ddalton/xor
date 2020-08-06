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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.generator.Generator;
import tools.xor.generator.StringTemplate;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.Shape;
import tools.xor.service.exim.CSVLoader;
import tools.xor.service.exim.CSVLoader.CSVState;
import tools.xor.util.Edge;
import tools.xor.util.graph.Graph;
import tools.xor.view.AggregateView;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-mutable-JSON-jpa-test.xml" })
public class CSVLoaderTest {

	@PersistenceContext
	EntityManager entityManager;

	@Resource(name = "amJDBCjson")
	protected AggregateManager amJDBC;	// Useful for generating data using JDBC
	
	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("@@@ Starting test: " + description.getMethodName());
		}
	};
	
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
            paths.add("OWNEDBY_UUID");
            AggregateView ownedByView = new AggregateView("OWNEDBY");
            ownedByView.setAttributeList(paths);
            
            // This whole creating the settings object needs to be updated
            Settings settings = new Settings();
            // Why is the shape getting un-initialized when called after settings.init???
            settings.setView(ownedByView);    
            settings.setSessionContext(sc);
            settings.setEntityType(shape.getType("TASK"));
            settings.init(shape);
                    
            List result = amJDBC.query(queryTask, settings);
            assert(result != null);
            assert(result.size() == 1);
            JSONObject json = (JSONObject)result.get(0);
            System.out.println("CSVLOADER JSON: " + json.toString());
            assert(json.getString("UUID").equals("ID_4"));
            
            JSONObject ownedBy = json;
            if(modelsRelationships) {
                ownedBy = json.getJSONObject("OWNEDBY_UUID");
                assert(ownedBy.getString("UUID").equals("ID_2"));
            } else {
                assert(ownedBy.getString("OWNEDBY_UUID").equals("ID_2"));
            }
            
        } finally {
        	    sc.rollback();
        }
	}	
	
}
