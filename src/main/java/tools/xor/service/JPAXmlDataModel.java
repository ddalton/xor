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

import javax.persistence.EntityManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.TypeMapper;
import tools.xor.util.JPAUtil;

/**
 * This class is part of the Data Access Service framework 
 * @author Dilip Dalton
 *
 */
public class JPAXmlDataModel extends JPADataModel {

	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private EntityManagerFactory emf;
    private PersistenceUtil      persistenceUtil;	
	
	public JPAXmlDataModel(TypeMapper typeMapper, String name, DataModelFactory dasFactory) {
		super(typeMapper, name, dasFactory);
		this.emf = JPAUtil.getEmf(name);
	}
	
    @Override
    public PersistenceProvider getPersistenceProvider() {
        if(super.getPersistenceProvider() == null) {
            this.persistenceProvider = new PersistenceProvider() {
                @Override
                public DataStore createDS(Object sessionContext, Object data) {
                    
                    DataStore po = new JPAPersistenceXMLPO(sessionContext, data);
                    ((JPAPersistenceXMLPO)po).setPersistenceUtil(persistenceUtil);
                    
                    return po;                    
                } 
            };
        }
        
        return this.persistenceProvider;
    }	

	@Override
	public EntityManagerFactory getEmf() {
		return this.emf;
	}
    
    @Override
    public
    void setPersistenceUtil(PersistenceUtil persistenceUtil) {
        this.persistenceUtil = persistenceUtil;
    }	
}