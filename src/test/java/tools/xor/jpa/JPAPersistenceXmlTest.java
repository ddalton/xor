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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import tools.xor.Settings;
import tools.xor.db.common.Contact;
import tools.xor.service.AggregateManager;
import tools.xor.service.DefaultDataModelFactory;
import tools.xor.service.JPAXMLDataModelBuilder;
import tools.xor.util.JPAUtil;
import tools.xor.util.PersistenceType;

public class JPAPersistenceXmlTest {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	@Test
	public void saveContactDirect() {
        EntityManager entityManager = null;
    	EntityTransaction tx = null;
 
    	try{
    		EntityManagerFactory emf = JPAUtil.getEmf("xor");
    		entityManager = emf.createEntityManager();
    		
    		tx = entityManager.getTransaction();
    		tx.begin();

    		Contact contact = new Contact();
    		contact.setName("John smith");
    		contact.setEmail("jsmith@unknown.com");
    		entityManager.persist(contact);
    		
    		assert(contact.getId() != null);
 
    		tx.commit();
 
 
    	}catch(RuntimeException e){
    		e.printStackTrace();
    		try{
    			tx.rollback();
    		}catch(RuntimeException rbe){

    		}
    		throw e;
    	}finally{
    		if(entityManager!=null){
    			entityManager.close();
    		}
    	}	
	}
	
	@Test 
	public void saveContactAM() {
        EntityManager entityManager = null;
    	EntityTransaction tx = null;
 
    	try{
    		EntityManagerFactory emf = JPAUtil.getEmf("xor");
    		entityManager = emf.createEntityManager();
    		
    		tx = entityManager.getTransaction();
    		tx.begin();

    		Contact contact = new Contact();
    		contact.setName("John smith");
    		contact.setEmail("jsmith@unknown.com");
    		
    		DefaultDataModelFactory dmf = new DefaultDataModelFactory("xor");
    		dmf.setDataModelBuilder(new JPAXMLDataModelBuilder());
    		
    		// Create AggregateManager
    		AggregateManager am = new AggregateManager.AggregateManagerBuilder()
    			.dasFactory(dmf)
    			.persistenceType(PersistenceType.JPA)
    			.build();
    		Settings settings = new Settings();
    		settings.setSessionContext(entityManager);
    		
    		contact = (Contact) am.update(contact, settings);
    		
    		assert(contact.getId() != null);
 
    		tx.commit();
 
 
    	}catch(RuntimeException e){
    		e.printStackTrace();
    		try{
    			tx.rollback();
    		}catch(RuntimeException rbe){

    		}
    		throw e;
    	}finally{
    		if(entityManager!=null){
    			entityManager.close();
    		}
    	}			  		
	}
	
}
