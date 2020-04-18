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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import tools.xor.TypeMapper;

/**
 * Reads a swagger file containing a json schema component.
 *
 * Inheritance - allOf (Need to support multiple inheritance)
 * Association - $ref
 * toMany      - "type" : "array"
 * 
 */
public class SwaggerDataModel extends AbstractDataModel {
    
    private List<String> swaggerFiles = new ArrayList<>();
    private List<String> identifiers = new ArrayList<>();
    private String schemaAnchor; // the path where the schema is present in the JSON document
    
    public void setSwaggerFiles(List<String> files) {
        this.swaggerFiles = files;
    }
    
    public void setSchemaAnchor(String anchor) {
        this.schemaAnchor = anchor;
    }
    
    /**
     * This is of the form <type name>:<key name>
     * @param identifiers
     */
    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public SwaggerDataModel(DataModelFactory factory, TypeMapper typeMapper) {
        super(factory, typeMapper);
    }

    @Override
    public void processShape(Shape shape, SchemaExtension extension, Set<String> entityNames) {
        for (String fileName : swaggerFiles) {
            try {
                InputStream stream = SwaggerDataModel.class.getClassLoader().getResourceAsStream(fileName);
                if (stream == null) {
                    throw new RuntimeException("Unable to find the swagger configuration file: " + fileName);
                }
                String jsonTxt = IOUtils.toString(stream);
                JSONObject json = new JSONObject(jsonTxt);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read " + fileName, e);
            }
        }
    }

}