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

import javax.sql.DataSource;

import tools.xor.TypeMapper;
import tools.xor.providers.jdbc.JDBCConfigDataModel;

public class JDBCConfigDataModelBuilder implements DataModelBuilder {

    DataSource dataSource;
    
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Build the DataModel and initialize it with the provided TypeMapper instance
     * @param typeMapper used to derive an external model from the built model 
     * @return DataModel instance
     */
    public DataModel build(String name, TypeMapper typeMapper, AbstractDataModelFactory dataModelFactory) {
        JDBCConfigDataModel model = new JDBCConfigDataModel(dataModelFactory, typeMapper);
        model.setDataSource(getDataSource());
        
        return model;
    }
}