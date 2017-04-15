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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JPAProperty;
import tools.xor.Property;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.db.pm.Project;
import tools.xor.db.pm.Quote;
import tools.xor.db.pm.Task;
import tools.xor.db.sp.P;
import tools.xor.db.sp.S;
import tools.xor.db.sp.SP;
import tools.xor.logic.DefaultMutableJson;
import tools.xor.service.DataAccessService;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.ObjectGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.View;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-mutable-JSON-jpa-test.xml" })
@TransactionConfiguration(defaultRollback = true)
@Transactional
public class JPAMutableJsonTest extends DefaultMutableJson {

	@PersistenceContext
	EntityManager entityManager;
	
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
		DataAccessService das = aggregateService.getDAS();
		EntityType taskType = (EntityType) das.getType(Task.class);
		Property openProperty = new JPAProperty("ItemList", das.getType(Object.class), taskType);
		das.addProperty(taskType, openProperty);

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
	
	private void setupOpenField(DataAccessService das) {
		EntityType taskType = (EntityType) das.getType(Task.class);
		if(taskType.getProperty("subTaskObj") == null) {
			ExtendedProperty openProperty = new JPAProperty("subTaskObj", das.getType(Task.class), taskType, RelationshipType.TO_ONE, null);
			openProperty.addKeyMapping(new String[]{"subTask"}, new String[]{"id"});
			das.addProperty(taskType, openProperty);
		}
	}
	
	@Test
	public void checkOpenFieldEntityToOne() {
		setupOpenField(aggregateService.getDAS());
		super.checkOpenFieldEntityToOne();		
	}
	
	@Test
	public void checkOpenFieldEntityToOneGrandchild() {	
		setupOpenField(aggregateService.getDAS());
		super.checkOpenFieldEntityToOneGrandchild();			
	}
	
	@Test
	public void checkOpenFieldQuery() {	
		setupOpenField(aggregateService.getDAS());
		super.checkOpenFieldQuery();			
	}	
			
	@Test
	public void checkExternalData() {	
		super.checkExternalData();			
	}
	
	@Test
	public void checkOpenFieldPaging() {
		setupOpenField(aggregateService.getDAS());		
		super.checkOpenFieldPaging();
	}

	@Override
	protected void createSPData() {

		// Check if the data has already been created
		if(S1 != null) {
			return;
		}
		
		EntityType stype = (EntityType) aggregateManager.getDAS().getType(S.class);
		stype.setNaturalKey(new String[]{"supplierNo"});
		EntityType ptype = (EntityType) aggregateManager.getDAS().getType(P.class);
		ptype.setNaturalKey(new String[]{"partNo"});	
		EntityType sptype = (EntityType) aggregateManager.getDAS().getType(SP.class);
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
        DataAccessService das = aggregateService.getDAS();		
        EntityType partType = (EntityType) das.getType(P.class);
        if(partType.getProperty("supplierParts") == null) {
            ExtendedProperty openProperty = new JPAProperty("supplierParts", das.getType(Set.class), partType, RelationshipType.TO_MANY, (EntityType) das.getType(SP.class));
            openProperty.addKeyMapping(new String[]{"partNo"}, new String[]{"partNo"});
            das.addProperty(partType, openProperty);
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
	public void readNarrowedCollectionObjectGraph() throws FileNotFoundException
	{
		super.readNarrowedCollectionObjectGraph();
	}

	@Test
	public void generateBoundedPersonObjectGraph() throws FileNotFoundException
	{
		super.generateBoundedPersonObjectGraph();
	}

	@Test(expected = javax.persistence.PersistenceException.class)
	public void checkReferenceSemantics () throws JSONException
	{
		DataAccessService das = aggregateService.getDAS();
		EntityType taskType = (EntityType) das.getType(Task.class);
		Property openProperty = taskType.getProperty("ItemList");
		if(openProperty != null) {
			das.removeProperty(taskType, openProperty);
		}

		super.checkReferenceSemantics();
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
		DataAccessService das = aggregateService.getDAS();
		EntityType taskType = (EntityType)das.getType(Task.class);
		Settings settings = new Settings();
		settings.setView(das.getBaseView(taskType));

		JSONObject json = new JSONObject();
		json.put("id", task.getId());
		json.put(Constants.XOR.TYPE, Task.class.getName());
		json = (JSONObject) aggregateManager.read(json, settings);

		assert(json != null);
		assert(!json.has("quote"));

		// Now let us get the aggregate view
		//settings.setView(das.getView(taskType));

		View view = das.getView(taskType);
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
		DataAccessService das = aggregateManager.getDAS();
		EntityType externalTask = (EntityType)das.getExternalType(Task.class);
		EntityType domainTask = (EntityType)das.getType(Task.class);
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
		DataAccessService das = aggregateManager.getDAS();
		EntityType externalTask = (EntityType)das.getExternalType(Task.class);
		EntityType domainTask = (EntityType)das.getType(Task.class);
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
		settings.setEntityType(aggregateService.getDAS().getType(Task.class));

		aggregateService.importCSV("bulk/", settings);

		// query the task object
		settings = new Settings();
		settings.setEntityType(aggregateService.getDAS().getType(Task.class));
		settings.setView(aggregateService.getView("TASKCHILDREN"));
		List<?> result = aggregateService.query(null, settings);

		// Contained objects are not included
		assert(result.size() == 1);
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
		json = (JSONObject)aggregateService.read(json, settings);

		assert(json != null);
		assert(json.getString("description").equals(NEW_DESC));
	}
}
