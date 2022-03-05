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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.xor.TypeMapper;

@Component
public class SwaggerSpringDataModelBuilder implements DataModelBuilder {

    private PersistenceProvider persistenceProvider;
    private List<String> swaggerFiles = new ArrayList<>();
    private List<String> identifiers = new ArrayList<>();
    private String schemaAnchor; // the path where the schema is present in the JSON document
    
    public void setPersistenceProvider(PersistenceProvider pp) {
        this.persistenceProvider = pp;
    }
    
    @Override
    public PersistenceProvider getPersistenceProvider() {
        return this.persistenceProvider;
    }  
    
    public List<String> getSwaggerFiles() {
        return this.swaggerFiles;
    }
    
    public void setSwaggerFiles(List<String> files) {
        this.swaggerFiles = files;
    }
    
    public String getSchemaAnchor() {
        return this.schemaAnchor;
    }
    
    public void setSchemaAnchor(String anchor) {
        this.schemaAnchor = anchor;
    }
    
    public List<String> getIdentifiers() {
        return this.identifiers;
    }
    
    /**
     * This is of the form type_name:key_name
     * @param identifiers list
     */
    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }    
     
    /**
     * Build the DataModel and initialize it with the provided TypeMapper instance
     * @param typeMapper used to derive an external model from the built model 
     * @return DataModel instance
     */
    public DataModel build(String name, TypeMapper typeMapper, AbstractDataModelFactory dataModelFactory) {
        SwaggerDataModel model = new SwaggerSpringDataModel(dataModelFactory, typeMapper);
        model.setSwaggerFiles(getSwaggerFiles());
        model.setSchemaAnchor(getSchemaAnchor());
        model.setIdentifiers(getIdentifiers());
        
        return model;
    }
}