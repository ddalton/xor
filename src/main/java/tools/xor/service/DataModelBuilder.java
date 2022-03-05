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

public interface DataModelBuilder {

    /**
     * Build the DataModel and initialize it with the provided TypeMapper instance
     * @param name given to this data model instance
     * @param typeMapper used to derive an external model from the built model
     * @param dataModelFactory factory responsible for creating this data model instance 
     * @return DataModel instance
     */
    DataModel build(String name, TypeMapper typeMapper, AbstractDataModelFactory dataModelFactory);    
    
    /**
     * Returns a PersistenceProvider instance that is to be used with the DataModel created by the builder.
     * @return persistenceProvider instance
     */
    default PersistenceProvider getPersistenceProvider() {
        return null;
    }
}