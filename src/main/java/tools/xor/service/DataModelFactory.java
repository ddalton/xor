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

import tools.xor.TypeMapper;

public interface DataModelFactory {
    /**
     * Create the DataModel instance. This is the domain model.
     * 
     * @param typeMapper encapsulates mapping logic between an external and domain model
     * @return DataModel instance.
     */
	DataModel create(TypeMapper typeMapper);

	/**
	 * Set the AggregateManager service corresponding to this DataModelFactory.
	 * @param aggregateManager instance
	 */
	void setAggregateManager(AggregateManager aggregateManager);

	/**
	 * Return the AggregateManager instance corresponding to this DataMdoelFactory.
	 * @return aggregateManager instance
	 */
	AggregateManager getAggregateManager();
	
	/**
	 * Create the DataStore instance for this DataModel. The DataStore
	 * encapsulates the interactions with a particular persistence manager (JDBC, JPA etc)
	 * for this model.
	 * 
	 * @param sessionContext session related data
	 * @return DataStore instance
	 */
	DataStore createDataStore (Object sessionContext);
	
	/**
	 * Used to inject dependencies for an object managed by a Dependency injection framework
	 * such as Spring.
	 * @param bean java object
	 * @param name of the model
	 */
	void injectDependencies(Object bean, String name);
	
	/**
	 * Return the builder for this model.
	 * 
	 * @return DataModelBuilder instance
	 */
	DataModelBuilder getDataModelBuilder();
}
