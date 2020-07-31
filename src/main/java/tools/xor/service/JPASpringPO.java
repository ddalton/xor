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

package tools.xor.service;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import org.springframework.orm.jpa.JpaTransactionManager;

public class JPASpringPO extends JPADataStore {
	
	@PersistenceContext
	EntityManager entityManager;
	
    @Inject
    EntityManagerFactory entityManagerFactory;

    @Inject
    JpaTransactionManager txManager;
    
    public JPASpringPO() {
    	super();
    }
    
    public JPASpringPO(Object sessionContext, Object data) {
    	super(sessionContext, data);
    }    
	
    @Override
    protected EntityManager getEntityManager() {
		return entityManager;
	}
    
    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

    public JpaTransactionManager getTxManager() {
        return txManager;
    }
}
