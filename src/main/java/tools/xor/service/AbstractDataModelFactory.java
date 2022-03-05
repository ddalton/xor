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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.DefaultTypeMapper;
import tools.xor.MapperSide;
import tools.xor.TypeMapper;

public abstract class AbstractDataModelFactory implements DataModelFactory {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	protected String                              name;	
	protected AggregateManager                    aggregateManager;
	protected static final Map<String, DataModel> models = new ConcurrentHashMap<String, DataModel>(); // If the name is specified then the data model is cached by the name
	protected DataModelBuilder                    dataModelBuilder;

	@Override
	public AggregateManager getAggregateManager() {
		return aggregateManager;
	}

	@Override
	public void setAggregateManager(AggregateManager aggregateManager) {
		this.aggregateManager = aggregateManager;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
    public DataModelBuilder getDataModelBuilder() {
        return dataModelBuilder;
    }

    public void setDataModelBuilder(DataModelBuilder builder) {
        this.dataModelBuilder = builder;
    }	
	
	private TypeMapper createTypeMapper(TypeMapper typeMapper) {
	       return (typeMapper == null) ? new DefaultTypeMapper(null, MapperSide.DOMAIN, null, true) : typeMapper.newInstance(null, typeMapper.getSide(), typeMapper.getShapeName(), typeMapper.isPersistenceManaged());
	}

	/**
	 * Need to synchronize on this method, since the meta model needs to be ready before using XOR
	 */
	public synchronized DataModel create(TypeMapper typeMapper) {
	    typeMapper = createTypeMapper(typeMapper);
	    assert typeMapper.getModel() == null : "Cannot create a new DAS with an already initialized TypeMapper instance";
	    
		if(name == null) {
			throw new RuntimeException("Name needs to be specified for the DASFactory");
		}

		DataModel result = models.get(name);
		if(result != null) {
		    typeMapper.setModel(result);
			return result;
		}
		
		if(getDataModelBuilder() == null) {
		    throw new RuntimeException("DataModelBuilder instance needs to be set on the DataModelFactory");
		}
		
		addModel(getDataModelBuilder().build(name, typeMapper, this), true);

		return models.get(name);
	}

	private DataModel addModel(DataModel instance, boolean addShape) {
		models.put(name, instance);
		injectDependencies(models.get(name), name);

		if(addShape) {
			models.get(name).createShape();
		}

		return models.get(name);
	}

	@Override
	public DataStore createDataStore (Object sessionContext) {
		DataStore result;

		if(models.get(name) == null) {
			this.create(aggregateManager.getTypeMapper());
		}
		
		result = models.get(name).getPersistenceProvider().createDS(sessionContext, name);

		if(result != null)
			injectDependencies(result, null);

		return result;
	}
}
