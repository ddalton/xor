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
import javax.persistence.EntityManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.TypeMapper;

/**
 * This class is part of the Data Access Service framework 
 * @author Dilip Dalton
 *
 */
public class JPASpringDataModel extends JPADataModel {

	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
    @Inject
    EntityManagerFactory entityManagerFactory;	

	public EntityManagerFactory getEmf() {
		return entityManagerFactory;
	}
	
	public JPASpringDataModel(TypeMapper typeMapper, String name, DataModelFactory dasFactory) {
		super(typeMapper, name, dasFactory);
	}
	
    @Override
    public PersistenceProvider getPersistenceProvider() {
        if(super.getPersistenceProvider() == null) {
            this.persistenceProvider = new JPASpringProvider();
            this.persistenceProvider.setPersistenceUtil(getPersistenceUtil());
        }
        
        return this.persistenceProvider;
    }   	
}