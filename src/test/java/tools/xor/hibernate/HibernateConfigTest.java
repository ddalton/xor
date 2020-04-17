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

package tools.xor.hibernate;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

import tools.xor.Settings;
import tools.xor.db.common.Contact;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModelBuilder;
import tools.xor.service.DefaultDASFactory;
import tools.xor.service.HibernateConfigDataModelBuilder;
import tools.xor.util.PersistenceType;
import tools.xor.util.HibernateUtil;

public class HibernateConfigTest {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	@Test
	public void saveContactDirect() {
        Session session = null;
    	Transaction tx = null;
 
    	try{
    		session = HibernateUtil.getSessionFactory().openSession();
    		tx = session.beginTransaction();

    		Contact contact = new Contact();
    		contact.setName("John smith");
    		contact.setEmail("jsmith@unknown.com");
    		session.save(contact);
    		
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
    		if(session!=null){
    			session.close();
    		}
    	}	
	}
	
	@Test 
	public void saveContactAM() {
        Session session = null;
    	Transaction tx = null;
 
    	try{
    		session = HibernateUtil.getSessionFactory().openSession();
    		tx = session.beginTransaction();
    	
    		Contact contact = new Contact();
    		contact.setName("John smith");
    		contact.setEmail("jsmith@unknown.com");
    		
    		DefaultDASFactory factory = new DefaultDASFactory("hconfig");
    		factory.setDataModelBuilder(new HibernateConfigDataModelBuilder());

    		// Create AggregateManager
    		AggregateManager am = new AggregateManager.AggregateManagerBuilder()
    			.dasFactory(factory)
    			.persistenceType(PersistenceType.HIBERNATE)
    			.build();
    		Settings settings = new Settings();
    		settings.setSessionContext(session);
    		
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
    		if(session!=null){
    			session.close();
    		}
    	}    		
	}
	
}
