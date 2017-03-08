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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanCreationException;

import tools.xor.MapperDirection;
import tools.xor.TypeMapper;
import tools.xor.util.PersistenceType;

public abstract class AbstractDASFactory implements DASFactory {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	protected String                                name;	
	protected AggregateManager                      aggregateManager;
	protected static final Map<String, DataAccessService> das = new ConcurrentHashMap<String, DataAccessService>(); // If the name is specified then the DAS is cached by the name

	public AggregateManager getAggregateManager() {
		return aggregateManager;
	}

	public void setAggregateManager(AggregateManager aggregateManager) {
		this.aggregateManager = aggregateManager;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	protected abstract HibernateDAS createHibernateDAS(TypeMapper typeMapper);
	
	protected abstract JPADAS createJPADAS(TypeMapper typeMapper, String name);


	/**
	 * This should be overridden by a custom DASFactory
	 * @param typeMapper object
	 * @param name unique to this mapping
	 * @return DataAccessService object
	 */
	protected AbstractDataAccessService createCustomDAS(TypeMapper typeMapper, String name) {
		throw new UnsupportedOperationException("This method is only supported by a user provided custom DAS factory");
	}

	/**
	 * Need to synchronize on this method, since the meta model needs to be ready before using XOR
	 */
	public synchronized DataAccessService create() {
		if(name == null)
			throw new RuntimeException("Name needs to be specified for the DASFactory");

		if(das.get(name) != null)
			return das.get(name);

		// Needed for creating the external types
		TypeMapper typeMapper = aggregateManager.getTypeMapper().newInstance(MapperDirection.DOMAINTOEXTERNAL);
		PersistenceType persistenceType = aggregateManager.getPersistenceType();

		try { // HibernateDAS
			if(persistenceType == null || persistenceType == PersistenceType.HIBERNATE) {
				das.put(name, createHibernateDAS(typeMapper));
				injectDependencies(das.get(name), name);

				// Try version 4 specifically
				if( ((HibernateDAS)das.get(name)).getConfiguration() == null) {
					das.put(name, new HibernateSpringDAS(typeMapper, this));
					injectDependencies(das.get(name), name);
				}

				if( ((HibernateDAS)das.get(name)).getConfiguration() == null)
					throw new RuntimeException("Could not get Hibernate configuration.");

				das.get(name).addShape(AbstractDataAccessService.DEFAULT_SHAPE);
				return das.get(name);
			}
		} catch(BeanCreationException e) {
			logger.warn("Hibernate configuration not found, hence cannot create a HibernateDAS instance");			
		}

		try { // JPADataAccessService
			if(persistenceType == null || persistenceType == PersistenceType.JPA) {
				das.put(name, createJPADAS(typeMapper, name));
				injectDependencies(das.get(name), name);
				das.get(name).addShape(AbstractDataAccessService.DEFAULT_SHAPE);
				return das.get(name);
			}
		} catch (BeanCreationException e) {
			logger.warn("JPA configuration not found, hence cannot create a JPADataAccessService instance");
		}

		try { // Google App Engine, use JPA for metadata configuration
			if(persistenceType == null || persistenceType == PersistenceType.DATASTORE) {
				das.put(name, createCustomDAS(typeMapper, name));
				injectDependencies(das.get(name), name);
				das.get(name).addShape(AbstractDataAccessService.DEFAULT_SHAPE);
				return das.get(name);
			}
		} catch (BeanCreationException e) {
			logger.warn("App Engine Datastore configuration not found");
		}

		// Ariba persistence uses a custom implementation
		if(persistenceType == null || persistenceType == PersistenceType.AML) {
			das.put(name, createCustomDAS(typeMapper, name));
			injectDependencies(das.get(name), name);
			das.get(name).addShape(AbstractDataAccessService.DEFAULT_SHAPE);
			return das.get(name);
		}

		// Enterprise Objects Framework (EO) persistence uses a custom implementation
		if(persistenceType == null || persistenceType == PersistenceType.EOF) {
			das.put(name, createCustomDAS(typeMapper, name));
			injectDependencies(das.get(name), name);
			das.get(name).addShape(AbstractDataAccessService.DEFAULT_SHAPE);
			return das.get(name);
		}

		return das.get(name);
	}

	@Override
	public PersistenceOrchestrator getPersistenceOrchestrator(Object sessionContext) {
		PersistenceOrchestrator result;

		if(das.get(name) == null)
			this.create();

		
		result = das.get(name).createPO(sessionContext, name);

		if(result != null)
			injectDependencies(result, null);

		return result;
	}
}
