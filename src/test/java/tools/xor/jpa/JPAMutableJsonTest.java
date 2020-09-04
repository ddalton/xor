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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.CollectionElementGenerator;
import tools.xor.CollectionOwnerGenerator;
import tools.xor.CounterGenerator;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCType;
import tools.xor.JPAProperty;
import tools.xor.Property;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.SlidingElementGenerator;
import tools.xor.ToOneGenerator;
import tools.xor.Type;
import tools.xor.db.pm.PriorityTask;
import tools.xor.db.pm.Quote;
import tools.xor.db.pm.Task;
import tools.xor.db.sp.P;
import tools.xor.db.sp.S;
import tools.xor.db.sp.SP;
import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.ElementPositionGenerator;
import tools.xor.generator.Generator;
import tools.xor.generator.GeneratorRecipient;
import tools.xor.generator.StringTemplate;
import tools.xor.logic.DefaultMutableJson;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.DomainShape;
import tools.xor.service.Shape;
import tools.xor.service.Transaction;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.InterQuery;
import tools.xor.view.AggregateTree;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryJoinAction;
import tools.xor.view.QueryTree;
import tools.xor.view.View;

@ExtendWith(SpringExtension.class)
@ExtendWith(CSVLoaderTest.TraceUnitExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-mutable-JSON-jpa-test.xml" })
@Transactional
public class JPAMutableJsonTest extends DefaultMutableJson {

	@PersistenceContext
	EntityManager entityManager;

	@Resource(name = "amJDBCjson")
	protected AggregateManager amJDBC;	// Useful for generating data using JDBC
	
	S S1 = null;
		
	@Test
	public void checkStringField() throws JSONException {
		super.checkStringField();
	}	
	
	@Test
	public void checkDateField() throws JSONException {
		super.checkDateField();
	}	
	
	@Test
	public void checkIntField() throws JSONException {
		super.checkIntField();
	}	

	@Test
	public void checkLongField() throws JSONException {
		super.checkLongField();
	}

	@Test
	public void checkEmptyLongField() throws JSONException {
		super.checkEmptyLongField();
	}
		
	@Test
	public void checkBooleanField() throws JSONException {
		super.checkBooleanField();
	}		

	@Test
	public void checkBigDecimalField() throws JSONException {
		super.checkBigDecimalField();
	}	

	@Test
	public void checkBigIntegerField() throws JSONException {
		super.checkBigIntegerField();
	}
		
	@Test
	public void checkEntityField() throws JSONException {
		super.checkEntityField();
	}

	@Test
	public void checkSetField() throws JSONException {
		super.checkSetField();
	}

	@Test
	public void checkOpenField() throws JSONException {
		DataModel das = aggregateService.getDataModel();
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
		ExtendedProperty openProperty = new JPAProperty("ItemList", das.getShape().getType(Object.class), taskType);
		aggregateService.getTypeMapper().addProperty(openProperty);

		super.checkOpenField();
	}
	
	@Test
	public void checkExcelExport() throws JSONException, IOException {
		super.checkExcelExport();
	}

	@Test
	public void checkCSVExport() throws JSONException, IOException {
		super.checkCSVExport();
	}
	
	@Test
	public void checkExcelImport() throws JSONException, IOException {
		super.checkExcelImport();
	}		
	
	@Test
	public void checkExcelImport100() throws JSONException, IOException {
		super.checkExcelImport100();
	}	
	
	private void setupOpenField(DataModel das) {
		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
		if(taskType.getProperty("subTaskObj") == null) {
			ExtendedProperty openProperty = new JPAProperty("subTaskObj", das.getShape().getType(Task.class), taskType, RelationshipType.TO_ONE, null);
			openProperty.addKeyMapping(new String[]{"subTask"}, new String[]{"id"});
            aggregateService.getTypeMapper().addProperty(openProperty);
		}
	}
	
	@Test
	public void checkOpenFieldEntityToOne() {
		setupOpenField(aggregateService.getDataModel());
		super.checkOpenFieldEntityToOne();		
	}
	
	@Test
	public void checkOpenFieldEntityToOneGrandchild() {	
		setupOpenField(aggregateService.getDataModel());
		super.checkOpenFieldEntityToOneGrandchild();			
	}
	
	@Test
	public void checkOpenFieldQuery() {	
		setupOpenField(aggregateService.getDataModel());
		super.checkOpenFieldQuery();			
	}	
			
	@Test
	public void checkExternalData() {	
		super.checkExternalData();			
	}
	
	@Test
	public void checkOpenFieldPaging() {
		setupOpenField(aggregateService.getDataModel());		
		super.checkOpenFieldPaging();
	}

	@Override
	protected void createSPData() {

		// Check if the data has already been created
		if(S1 != null) {
			return;
		}
		
		EntityType stype = (EntityType) aggregateManager.getDataModel().getShape().getType(S.class);
		stype.setNaturalKey(new String[]{"supplierNo"});
		EntityType ptype = (EntityType) aggregateManager.getDataModel().getShape().getType(P.class);
		ptype.setNaturalKey(new String[]{"partNo"});	
		EntityType sptype = (EntityType) aggregateManager.getDataModel().getShape().getType(SP.class);
		sptype.setNaturalKey(new String[]{"supplierNo", "partNo"});		

		S1 = new tools.xor.db.sp.S("S1", "Smith", 20, "London");
		S S2 = new tools.xor.db.sp.S("S2", "Jones", 10, "Paris");
		S S3 = new tools.xor.db.sp.S("S3", "Blake", 30, "Paris");
		S S4 = new tools.xor.db.sp.S("S4", "Clark", 20, "London");
		S S5 = new tools.xor.db.sp.S("S5", "Adams", 30, "Athens");

		P P1 = new P("P1", "Nut", "Red", new BigDecimal(12.0), "London");
		P P2 = new P("P2", "Bolt", "Green", new BigDecimal(17.0), "Paris");
		P P3 = new P("P3", "Screw", "Blue", new BigDecimal(17.0), "Oslo");
		P P4 = new P("P4", "Screw", "Red", new BigDecimal(14.0), "London");
		P P5 = new P("P5", "Cam", "Blue", new BigDecimal(12.0), "Paris");
		P P6 = new P("P6", "Cog", "Red", new BigDecimal(19.0), "London");	

		SP s1p1 = new SP("S1", "P1", 300);
		SP s1p2 = new SP("S1", "P2", 200);
		SP s1p3 = new SP("S1", "P3", 400);
		SP s1p4 = new SP("S1", "P4", 200);
		SP s1p5 = new SP("S1", "P5", 100);
		SP s1p6 = new SP("S1", "P6", 100);
		SP s2p1 = new SP("S2", "P1", 300);
		SP s2p2 = new SP("S2", "P2", 400);
		SP s3p2 = new SP("S3", "P2", 200);
		SP s4p2 = new SP("S4", "P2", 200);
		SP s4p4 = new SP("S4", "P4", 300);
		SP s4p5 = new SP("S4", "P5", 400);		  
			
		entityManager.persist(S1);
		entityManager.persist(S2);
		entityManager.persist(S3);
		entityManager.persist(S4);
		entityManager.persist(S5);
		
		entityManager.persist(P1);
		entityManager.persist(P2);
		entityManager.persist(P3);
		entityManager.persist(P4);
		entityManager.persist(P5);
		entityManager.persist(P6);
		
		entityManager.persist(s1p1);
		entityManager.persist(s1p2);
		entityManager.persist(s1p3);
		entityManager.persist(s1p4);
		entityManager.persist(s1p5);
		entityManager.persist(s1p6);
		entityManager.persist(s2p1);
		entityManager.persist(s2p2);
		entityManager.persist(s3p2);
		entityManager.persist(s4p2);
		entityManager.persist(s4p4);
		entityManager.persist(s4p5);
	}
	
	private void createSPProperty() {
        DataModel das = aggregateService.getDataModel();		
        EntityType partType = (EntityType) das.getShape().getType(P.class);
        if(partType.getProperty("supplierParts") == null) {
            ExtendedProperty openProperty = new JPAProperty("supplierParts", das.getShape().getType(Set.class), partType, RelationshipType.TO_MANY, (EntityType) das.getShape().getType(SP.class));
            openProperty.addKeyMapping(new String[]{"partNo"}, new String[]{"partNo"});
            aggregateService.getTypeMapper().addProperty(openProperty);
        }		
	}
	
	@Test
	public void checkOpenPropertyCollection() {
		
		createSPProperty();
		super.checkOpenPropertyCollection();
	}
	
	@Test
	public void checkOpenPropertyCollectionUpdate() {
		createSPProperty();
		super.checkOpenPropertyCollectionUpdate();
	}

	@Test
	public void checkOpenTypeCrossJoin() {
		super.checkOpenTypeCrossJoin();
	}

	@Test
	public void importCSV() throws Exception
	{
		super.importCSV();
	}

	@Test
	public void generateMediumSizedEntity() throws IOException
	{
		super.generateMediumSizedEntity();
	}

	@Test
	public void generateMediumSizedDomainEntity() throws IOException
	{
		super.generateMediumSizedDomainEntity();
	}

	@Test
	public void sharedOwnedBy() {
		super.sharedOwnedBy();
	}

	@Test
	public void generatePicture() {
		super.generatePicture();
	}

	@Test
	public void checkRegexView() {
		super.checkRegexView();
	}

	@Test
	public void testEmployeeType() {
		super.testEmployeeType();
	}

	@Test
	public void exportEmployee() throws IOException
	{
		super.exportEmployee();
	}

	@Test
	public void generateObjectGraph() throws FileNotFoundException
	{
		super.generateObjectGraph();
	}

	@Test
	public void generatePersonObjectGraph() throws FileNotFoundException
	{
		super.generatePersonObjectGraph();
	}

	@Test
	public void generateCollectionObjectGraph() throws FileNotFoundException
	{
		super.generateCollectionObjectGraph();
	}

	@Test
	public void readDifferentPersonViews()
	{
		super.readDifferentPersonViews();
	}

	@Test
	public void readEmployeeNumber()
	{
		super.readEmployeeNumber();
	}

	@Test
	public void readNarrowedCollectionObjectGraph() throws FileNotFoundException
	{
		super.readNarrowedCollectionObjectGraph();
	}

	@Test
	public void generateBoundedPersonObjectGraph() throws FileNotFoundException
	{
		super.generateBoundedPersonObjectGraph();
	}

	@Test
	public void checkReferenceSemantics () throws JSONException
	{
	    Assertions.assertThrows(javax.persistence.PersistenceException.class, () -> {	    
        		DataModel das = aggregateService.getDataModel();
        		EntityType taskType = (EntityType) das.getShape().getType(Task.class);
        		Property openProperty = taskType.getProperty("ItemList");
        		if(openProperty != null) {
        			das.getShape().removeProperty(openProperty);
        		}
        
        		super.checkReferenceSemantics();
	    });
	}

	@Test
	public void checkBasicView() {
		// Create a task with both OneToOne and OneToMany composition association.
		// The basic view should only retrieve the root entity with the OneToOne reference
		// association values

		// Create the aggregate root entity
		Task task = new Task();
		task.setName("ROOT");
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");

		// Create the OneToOne composition association
		Quote quote = new Quote();
		quote.setPrice(new BigDecimal("99.99"));
		quote.setTask(task);
		task.setQuote(quote);

		// Create the OneToMany composition association
		Task child1 = new Task();
		child1.setName("CHILD1");
		child1.setDisplayName("First child");
		child1.setDescription("This is the first child of the task");
		Task child2 = new Task();
		child2.setName("CHILD2");
		child2.setDisplayName("Second child");
		child2.setDescription("This is the second child of the task");
		Set children = new HashSet();
		children.add(child1);
		children.add(child2);
		task.setTaskChildren(children);

		// save the task
		entityManager.persist(task);

		// Read the task object using the built-in basic view
		DataModel das = aggregateService.getDataModel();
		EntityType taskType = (EntityType)das.getShape().getType(Task.class);
		Settings settings = new Settings();
		settings.setView(das.getShape().getBaseView(taskType));

		JSONObject json = new JSONObject();
		json.put("id", task.getId());
		json.put(Constants.XOR.TYPE, Task.class.getName());
		json = (JSONObject) aggregateManager.read(json, settings);

		assert(json != null);
		assert(!json.has("quote"));

		// Now let us get the aggregate view
		//settings.setView(das.getView(taskType));

		View view = das.getShape().getView(taskType);
		settings = new Settings();
		settings.setView(view);
		System.out.println("********Entity view******");
		for(String path: view.getAttributeList()) {
			System.out.println(path);
		}

		json = (JSONObject) aggregateManager.read(json, settings);
		assert(json != null);
		assert(json.has("quote"));
	}

	@Test
	public void checkReference()
	{

		// First create a natural key for Task based on name
		DataModel das = aggregateManager.getDataModel();
		EntityType externalTask = (EntityType)das.getShape().getType(Task.class);
		EntityType domainTask = (EntityType)das.getShape().getType(Task.class);
		String[] key = { "name" };
		externalTask.setNaturalKey(key);
		domainTask.setNaturalKey(key);

		try {
			// Create the aggregate root entity
			Task task = new Task();
			task.setName("ROOT");
			task.setDisplayName("Setup DSL");
			task.setDescription("Setup high-speed broadband internet using DSL technology");

			// Create the OneToOne composition association
			Task audit = new Task();
			audit.setName("AUDIT");
			audit.setDisplayName("Audit Task");
			audit.setDescription("Audit of the DSL installation");
			task.setAuditTask(audit);

			// save the task
			entityManager.persist(task);

			// check reference
			List<String> paths = new ArrayList<>();
			paths.add("name");
			paths.add("auditTask");
			AggregateView refView = new AggregateView("REFERENCE");
			refView.setAttributeList(paths);
			Settings settings = new Settings();
			settings.setView(refView);

			Task queryTask = new Task();
			queryTask.setId(task.getId());
			JSONObject json = (JSONObject) aggregateManager.read(queryTask, settings);

			assert(json != null);
			assert(json.get("auditTask") != null);
		} finally {

			// reset
			externalTask.setNaturalKey(null);
			domainTask.setNaturalKey(null);
		}
	}

	/**
	 * Change the audit task value
	 */
	@Test
	public void checkReferenceUpdate()
	{

		// First create a natural key for Task based on name
		DataModel das = aggregateManager.getDataModel();
		//EntityType domainTask = (EntityType)das.getShape().getType(Task.class);
        EntityType domainTask = (EntityType)das.getTypeMapper().getDomainShape().getType(Task.class);
        EntityType externalTask = (EntityType)das.getTypeMapper().getDynamicShape().getType(domainTask.getEntityName());		
		String[] key = { "name" };
		externalTask.setNaturalKey(key);
		domainTask.setNaturalKey(key);

		try {
			// Create the aggregate root entity
			Task task = new Task();
			task.setName("ROOT");
			task.setDisplayName("Setup DSL");
			task.setDescription("Setup high-speed broadband internet using DSL technology");

			// Create the OneToOne composition association
			Task audit = new Task();
			audit.setName("AUDIT");
			audit.setDisplayName("Audit Task");
			audit.setDescription("Audit of the DSL installation");
			task.setAuditTask(audit);

			// save the task
			entityManager.persist(task);

			Task auditNew = new Task();
			auditNew.setName("AUDITNEW");
			auditNew.setDisplayName("New Audit Task");
			auditNew.setDescription("New Audit of the DSL installation");
			entityManager.persist(auditNew);
			entityManager.flush();

			// check reference
			List<String> paths = new ArrayList<>();
			paths.add("auditTask");
			AggregateView refView = new AggregateView("REFERENCE_UPDATE");
			refView.setAttributeList(paths);
			Settings settings = new Settings();
			settings.setView(refView);

			JSONObject json = new JSONObject();
			json.put("id", task.getId());
			JSONObject auditJson = new JSONObject();
			auditJson.put("name", "AUDITNEW");
			json.put("auditTask", auditJson);
			settings.setEntityClass(Task.class);

			assert(task.getAuditTask().getId().equals(audit.getId()));
			Task updatedTask = (Task) aggregateManager.update(json, settings);

			assert(updatedTask != null);
			assert(updatedTask.getAuditTask() != null);
			assert(updatedTask.getAuditTask().getId().equals(auditNew.getId()));
		} finally {

			// reset
			externalTask.setNaturalKey(null);
			domainTask.setNaturalKey(null);
		}
	}

	@Test
	public void queryEntity() throws Exception
	{
		Settings settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
		settings.init(aggregateService.getDataModel().getShape());

		aggregateService.importCSV("bulk", settings);

		// query the task object
		settings = new Settings();
		settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
		settings.setView(aggregateService.getView("TASKCHILDREN"));
		List<?> result = aggregateService.query(null, settings);

		// Return the task and its child
		System.out.println("Result.size: " + result.size());
		System.out.println(result.get(0).toString());
		assert(result.size() == 2);
	}

	@Test public void updateSingleField() {

		final String NEW_DESC = "New description";

		// Create the aggregate root entity
		Task task = new Task();
		task.setName("ROOT");
		task.setDisplayName("Setup DSL");
		task.setDescription("Setup high-speed broadband internet using DSL technology");
		entityManager.persist(task);
		entityManager.flush();

		Settings settings = new Settings();
		List<String> attributes = new ArrayList();
		attributes.add("description");
		AggregateView view = new AggregateView("DESC");
		view.setAttributeList(attributes);
		settings.setView(view);
		settings.setEntityClass(Task.class);

		JSONObject json = new JSONObject();
		json.put("id", task.getId());
		json.put("description", NEW_DESC);

		aggregateService.update(json, settings);

		// Now read it
		json = new JSONObject();
		json.put("id", task.getId());
		json.put(Constants.XOR.TYPE, Task.class.getName());
		json = (JSONObject)aggregateService.read(json, settings);

		assert(json != null);
		assert(json.getString("description").equals(NEW_DESC));
	}

	@Test
	public void copyJSONObject() {
		JSONObject o1 = new JSONObject();
		o1.put("name", "Object 1");
		o1.put("desc", "Test object copy");

		JSONObject o2 = new JSONObject();
		o2.put("name", "Object 2");
		o2.put("desc", "Test copy of nested object");

		o1.put("nested", o2);
		o1.put("self", o1);

		JSONObject copy = ClassUtil.copyJson(o1);
		assert(copy.get("self") == copy);

		o2 = copy.getJSONObject("nested");
		assert(o2.get("name").equals("Object 2"));
		assert(o2 != o1.getJSONObject("nested"));
	}

	@Test
	public void testSubquery() {
		// create 2 parallel collections with > 1000 elements each
		// for example:
		// task with 1500 children
		// task with 1500 dependents
		// use the generator to do this

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

		ToOneGenerator toonegen = new ToOneGenerator(new String[] { "1",
																	"1,500:0",
																	"501,2000:2", // 1501 - 2000 represent grand children
																	"2001,4500:0"
		});
		CounterGenerator gensettings = new CounterGenerator(4500);
		gensettings.addListener(toonegen);
		task.addGenerator(gensettings);
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
			(GeneratorRecipient)parentgen));

		// add a couple of children for the dependent tasks from 1501 - 2000
		gensettings = new CounterGenerator(1000, 5001);
		toonegen = new ToOneGenerator(new String[] { "1501",
													 "5001,6000:2"
		});
		// Add the generator visitor
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
				(GeneratorRecipient)parentgen));
		gensettings.addListener(toonegen);
		task.addGenerator(gensettings);


		// Dependants
		JDBCType taskTask = (JDBCType)shape.getType("TASK_TASK");
		// generate 3000 elements from 1500-4499
		CollectionElementGenerator cegen = new SlidingElementGenerator(new String[] { "1500" });
		String[] collectionSizes = new String[] { "3000",
										 "1,1499:2"
		};
		CollectionOwnerGenerator cogen = new CollectionOwnerGenerator(collectionSizes, cegen);
		taskTask.addGenerator(cogen);
		ExtendedProperty uuid = (ExtendedProperty)taskTask.getProperty("TASK_UUID");
		Generator uuidgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
		uuid.setGenerator(uuidgen);
		ExtendedProperty dep = (ExtendedProperty)taskTask.getProperty("DEPENDANTS_UUID");
		Generator depgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
		dep.setGenerator(depgen);
		ExtendedProperty index = (ExtendedProperty)taskTask.getProperty("DEP_SEQ");
		index.setGenerator(new ElementPositionGenerator(cegen));
		cogen.addVisit(new DefaultGenerator.GeneratorVisit(cogen, (GeneratorRecipient)uuidgen));
		cogen.addVisit(new DefaultGenerator.GeneratorVisit(cegen, (GeneratorRecipient)depgen));

		String[] types = new String[] {
			"TASK",
			"TASK_TASK",
		};

		Settings settings = new Settings();
		//settings.setImportMethod(ImportMethod.CSV);
		Transaction tx = amJDBC.createTransaction(settings);
		tx.begin();

		try {
			amJDBC.generateSameTX(shape.getName(), Arrays.asList(types), settings);

			View view = aggregateService.getView("PARALLEL_QUERY");
			view = view.copy();
			DataModel das = aggregateManager.getDataModel();
			Shape ormShape = das.getShape();

			// We use splitToAnchor strategy
			view.setSplitToRoot(false);

			settings = new Settings();
			settings.setView(view);
			settings.setEntityType(ormShape.getType("Task"));
			settings.init(ormShape);

			// Now let us query the task and see if the parallel collections strategy works
			List<?> toList = aggregateService.query(null, settings);

			assert(toList != null);
			assert(toList.size() == 5500);
			JSONObject json = (JSONObject)toList.get(1);

			assert(json.has("id"));
			assert(json.has("name"));
			assert(json.has("taskChildren"));
			assert(json.has("dependants"));

			JSONArray dependants = json.getJSONArray("dependants");
			JSONArray children = json.getJSONArray("taskChildren");

			assert(dependants.length() == 2);
			assert(children.length() == 2);

			JSONObject dependant = dependants.getJSONObject(0);
			assert(dependant.has("taskChildren"));
			assert(dependant.getJSONArray("taskChildren").length() == 2);

			JSONObject child = children.getJSONObject(0);
			assert(child.has("taskChildren"));
			assert(child.getJSONArray("taskChildren").length() == 2);

		} finally {

			JDBCDataStore po = (JDBCDataStore)amJDBC.getDataStore();
			JDBCSessionContext sc = po.getSessionContext();

			try (Statement stmt = sc.getConnection().createStatement()) {
				stmt.execute("DELETE from TASK_TASK");
				stmt.execute("DELETE from TASK");
				sc.getConnection().commit();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			tx.rollback();

			// We don't close because Spring takes care of it
		}
	}

	//@Test
	public void testPriorityTask() {
		// Create a prioritytask and
		// extract the value of the DTYPE column using the RELATIONAL_SHAPE

		// Create the aggregate root entity
		Task task = new PriorityTask();
		task.setName("PTASK");
		task.setDisplayName("PriorityTask");
		task.setDescription("Test of PriorityTask descriminator");
		entityManager.persist(task);
		entityManager.flush();


		Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
		if(shape == null) {
			shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
		}
		JDBCType pTask = (JDBCType)shape.getType("TASK");

		JSONObject queryTask = new JSONObject();
		queryTask.put("UUID", task.getId());

		// Get a few properties
		// check reference
		List<String> paths = new ArrayList<>();
		paths.add("UUID");
		paths.add("DTYPE");
		paths.add("DESCRIPTION");
		AggregateView refView = new AggregateView("DTYPE");
		refView.setAttributeList(paths);
		Settings settings = new Settings();
		settings.setView(refView);

		settings.setEntityType(pTask);
		settings.init(shape);

		Transaction tx = amJDBC.createTransaction(settings);
		tx.begin();
		try {
			List result = amJDBC.query(queryTask, settings);

			assert(result != null);
			assert(result.size() == 1);
			JSONObject json = (JSONObject)result.get(0);
			assert(json.get("DTYPE").equals("PriorityTask"));
		} finally {
			tx.rollback();
		}
	}

	/*

	               taskChildren                    taskChildren
	    Task  - - - - - - - - - - - -  Task - - - - - - - - - - - - - -  Task
	                                                                      /\
	                                                                      |
	                                                                      |
	                                                                      |

	                                                                 PriorityTask

	    View:

	    id
	    name
	    taskChildren.id
	    taskChildren.name
	    taskChildren.description
	    taskChildren.taskChildren.id
	    taskChildren.taskChildren.name
	    taskChildren.taskChildren.description
	    taskChildren.taskChildren.priority

	    Print the state graph to see how it looks

	    We should get 2 queries

	    We first populate the data as following:
	    Root task
	    4 child tasks
	    Will create 10 grandchildren Tasks
	    Will create 4 grandchildren Priority tasks - this will be populated separately

	    We test the query stitching functionality in this test
	    Also, the data from the grandchildren priority task needs to be successfully retrieved.

	 */
	@Test
	public void testSubtypeSubquery() {
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
		ExtendedProperty priority = (ExtendedProperty)task.getProperty("PRIORITY");
		Generator pgen = new DefaultGenerator(new String[] {"0", "0"});
		priority.setGenerator(pgen);

		ToOneGenerator toonegen = new ToOneGenerator(new String[] { "0",
																	"0,0:0", // root task
																	"1,4:4", // 4 children
																	"5,8:2", // 2 children with 2 grand children
																	"9,14:3" // 2 children with 3 grand children
		});

		CounterGenerator gensettings = new CounterGenerator(15);
		gensettings.addListener(toonegen);
		task.addGenerator(gensettings);
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
				(GeneratorRecipient)parentgen));


		// Now add priority task grand children
		toonegen = new ToOneGenerator(new String[] { "1",
													 "15,17:1" // 3 children with 1 PriorityTask grand child
		});
		gensettings = new CounterGenerator(3, 15);
		gensettings.addListener(toonegen);
		task.addGenerator(gensettings);
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
				(GeneratorRecipient)parentgen));
		Generator priotypegen = new DefaultGenerator(new String[] {"PriorityTask"});
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(priotypegen, dtype));
		Generator priogen = new DefaultGenerator(new String[] {"2", "5"});
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(priogen, priority));

		String[] types = new String[] {
			"TASK"
		};

		Settings settings = new Settings();
		//settings.setImportMethod(ImportMethod.CSV);
		Transaction tx = amJDBC.createTransaction(settings);
		tx.begin();
		try {
			amJDBC.generateSameTX(shape.getName(), Arrays.asList(types), settings);

			List<String> paths = new ArrayList<>();
			paths.add("id");
			paths.add("name");
			paths.add("taskChildren.id");
			paths.add("taskChildren.name");
			paths.add("taskChildren.description");
			paths.add("taskChildren.taskChildren.id");
			paths.add("taskChildren.taskChildren.name");
			paths.add("taskChildren.taskChildren.description");
			paths.add("taskChildren.taskChildren.priority");
			AggregateView priorityView = new AggregateView("PRIORITYTASK");
			priorityView.setAttributeList(paths);
			priorityView.setSplitToRoot(false);
			settings = new Settings();
			settings.setView(priorityView);

			shape = aggregateService.getDataModel().getShape();
			Type type = shape.getType(Task.class);
			settings.setEntityType(type);
			settings.init(shape);

			List<?> result = aggregateService.query(null, settings);
			assert(result != null);
			assert(result.size() == 18);
			JSONObject root = (JSONObject)result.get(0);
			assert(root.has("taskChildren"));
			JSONArray children = root.getJSONArray("taskChildren");
			assert(children.length() == 4);

		} finally {

			JDBCDataStore po = (JDBCDataStore)amJDBC.getDataStore();
			JDBCSessionContext sc = po.getSessionContext();

			try (Statement stmt = sc.getConnection().createStatement()) {
				stmt.execute("DELETE from TASK");
				sc.getConnection().commit();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			tx.rollback();
			// We don't close as the connection belongs to Spring
		}
	}

	@Test
	public void testChildrenMix() {
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
			amJDBC.generateSameTX(shape.getName(), Arrays.asList(types), settings);

			// Query using the mix view
			settings = new Settings();
			settings.setView(aggregateService.getView("TASKCHILDRENMIX"));
			shape = aggregateService.getDataModel().getShape();
			Type type = shape.getType(Task.class);
			settings.setEntityType(type);
			settings.init(shape);

			// Print a graph of the aggregateTree
			AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = settings.getView().getAggregateTree(
				type);
			aggregateTree.exportToDOT("taskchildrenmix.dot");

			List<?> result = aggregateService.query(null, settings);
			assert(result.size() == 101);
			JSONObject rootTask = (JSONObject)result.get(0);
			assert(rootTask != null);

			assert(rootTask.has("taskChildren"));
			JSONArray children = rootTask.getJSONArray("taskChildren");
			assert(children.length() == 100);

			JSONObject child = children.getJSONObject(0);
			assert(child != null);
			assert(child.has("name"));
			assert(child.has("id"));

		} finally {

			JDBCDataStore po = (JDBCDataStore)amJDBC.getDataStore();
			JDBCSessionContext sc = po.getSessionContext();

			try (Statement stmt = sc.getConnection().createStatement()) {
				stmt.execute("DELETE from TASK");
				sc.getConnection().commit();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			tx.rollback();
			// We don't close as the connection belongs to Spring
		}
	}
	
    @Test
    public void testGenerateSameThread() {
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
        
        AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = null;
        try {
            // Generate the tasks in the DB
            amJDBC.generateSameTX(shape.getName(), Arrays.asList(types), settings);

            // Query using the mix view
            settings = new Settings();
            settings.setView(aggregateService.getView("TASKCHILDRENMIX"));
            shape = aggregateService.getDataModel().getShape();
            Type type = shape.getType(Task.class);
            settings.setEntityType(type);
            settings.init(shape);

            // Print a graph of the aggregateTree
            aggregateTree = settings.getView().getAggregateTree(type);
            aggregateTree.exportToDOT("taskchildrenmix.dot");

            List<?> result = aggregateService.query(null, settings);
            assert(result.size() == 101);
        } finally {
            tx.rollback();
            
            // Now query the data, it should return 0 records
            List<?> result = aggregateService.query(null, settings);
            assert(result.size() == 0);
            
            // We don't close as the connection belongs to Spring
        }
    }	

	@Test
	public void testChildrenMixTemp() {
		// First create a task with 100 children
		// We use the generators to create this data
		// This data is committed by the generators

		Shape jdbcShape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
		if(jdbcShape == null) {
			jdbcShape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
		}


		JDBCType task = (JDBCType)jdbcShape.getType("TASK");
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
			amJDBC.generateSameTX(jdbcShape.getName(), Arrays.asList(types), settings);

			JDBCDataStore po = (JDBCDataStore)amJDBC.getDataStore();
			po.createQueryJoinTable(null);
			jdbcShape.signalEvent(); // update the types in the shape table

			// Query using the mix view
			settings = new Settings();
			settings.setView(aggregateService.getView("TASKCHILDRENMIXTEMP"));
			Shape shape = aggregateService.getDataModel().getShape();
			((DomainShape)shape).setJDBCShape(jdbcShape);
			Type type = shape.getType(Task.class);
			settings.setEntityType(type);
			settings.init(shape);

			// Print a graph of the aggregateTree
			AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = settings.getView().getAggregateTree(
				type);
			aggregateTree.exportToDOT("taskchildrenmix.dot");

			List<?> result = aggregateService.query(null, settings);
			assert(result.size() == 101);
			JSONObject rootTask = (JSONObject)result.get(0);
			assert(rootTask != null);

			assert(rootTask.has("taskChildren"));
			JSONArray children = rootTask.getJSONArray("taskChildren");
			assert(children.length() == 100);

			JSONObject child = children.getJSONObject(0);
			assert(child != null);
			assert(child.has("name"));
			assert(child.has("id"));

		} finally {

			JDBCDataStore po = (JDBCDataStore)amJDBC.getDataStore();
			JDBCSessionContext sc = po.getSessionContext();

			try (Statement stmt = sc.getConnection().createStatement()) {
				stmt.execute("DELETE from TASK");
				sc.getConnection().commit();

				// drop the temp table
				stmt.executeUpdate(String.format("DROP TABLE %s", QueryJoinAction.JOIN_TABLE_NAME));
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			tx.rollback();
			// We don't close as the connection belongs to Spring
		}
	}
}
