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

import org.springframework.stereotype.Component;

import tools.xor.TypeMapper;

@Component
public class JPASpringDataModelBuilder implements DataModelBuilder {
    
    private PersistenceProvider persistenceProvider;
    private PersistenceUtil persistenceUtil;

    public PersistenceUtil getPersistenceUtil() {
        return persistenceUtil;
    }

    public void setPersistenceUtil(PersistenceUtil persistenceUtil) {
        this.persistenceUtil = persistenceUtil;
    }

    /**
     * Build the DataModel and initialize it with the provided TypeMapper instance
     * @param typeMapper used to derive an external model from the built model 
     * @return DataModel instance
     */
    public DataModel build(String name, TypeMapper typeMapper, AbstractDataModelFactory dataModelFactory) {
        DataModel dataModel = new JPASpringDataModel(typeMapper, name, dataModelFactory);
        dataModel.setPersistenceUtil(this.persistenceUtil);
        
        return dataModel;
    }
    
    public void setPersistenceProvider(PersistenceProvider pp) {
        this.persistenceProvider = pp;
    }
    
    @Override
    public PersistenceProvider getPersistenceProvider() {
        return this.persistenceProvider;
    }    
}