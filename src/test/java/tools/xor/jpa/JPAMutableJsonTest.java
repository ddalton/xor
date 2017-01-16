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
import java.math.BigDecimal;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.json.JSONException;
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
import tools.xor.db.pm.Task;
import tools.xor.db.sp.P;
import tools.xor.db.sp.S;
import tools.xor.db.sp.SP;
import tools.xor.logic.DefaultMutableJson;
import tools.xor.service.DataAccessService;

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
}
