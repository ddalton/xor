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

import java.lang.reflect.Method;

import javax.annotation.Resource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.TypeMapper;

/**
 * This class is part of the Data Access Service framework
 * 
 * @author Dilip Dalton
 * 
 */
public class HibernateSpringDataModel extends HibernateDataModel {

	private static final Logger logger = LogManager.getLogger(new Exception()
	.getStackTrace()[0].getClassName());
	
	@Autowired(required = false)
	protected Configuration configuration;	

	@Autowired
	protected SessionFactory sessionFactory;		
	
	@Resource(name = "&sessionFactory")
	protected Object sessionFactoryBean;
	
	public HibernateSpringDataModel(TypeMapper typeMapper, DataModelFactory dasFactory) {
		super(typeMapper, dasFactory);
	}
	
	@Override
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	@Override
	public Configuration getConfiguration()  {
		if(configuration == null) {
			try {
				Method getConfigMethod = sessionFactoryBean.getClass().getMethod("getConfiguration", new Class[0]);
				configuration = (Configuration) getConfigMethod.invoke(sessionFactoryBean, new Object[0]);
			} catch (Exception e) {
				logger.error("Unable to find the getConfiguration method in the session factory object");
			}
		}
		
		return configuration;
	}
    
    @Override
    public PersistenceProvider getDataProvider() {
        if(this.dataProvider == null) {
            this.dataProvider = new PersistenceProvider() {
                @Override
                public PersistenceOrchestrator createPO(Object sessionContext, Object data) {
                    return new HibernateSpringPO(sessionContext, data);
                } 
            };
        }
        
        return super.getDataProvider();
    }	
}