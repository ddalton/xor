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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import tools.xor.ExternalType;
import tools.xor.MutableJsonType;
import tools.xor.Type;
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
    private List<String> identifiers = new ArrayList<>(); // for e.g., <type name>:<id name>
    private Map<String, String> idMap = new HashMap<>(); // Map between entityTypeName and id property name
    private String schemaAnchor; // the path where the schema is present in the JSON document
    
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
    
    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }

    public SwaggerDataModel(DataModelFactory factory, TypeMapper typeMapper) {
        super(factory, typeMapper);
    }
    
    private void buildIdMap() {
        if (this.identifiers != null) {
            for (String idStr : identifiers) {
                String[] pair = idStr.split(":");
                if(pair.length != 2) {
                    throw new RuntimeException("entityName to identifier property should be of the form <entity name>:<id name>. Got incorrect input: " + idStr);
                }
                idMap.put(pair[0], pair[1]);
            }
        }
    }
    
    @Override
    public Shape createShape(String name, SchemaExtension extension, Shape.Inheritance typeInheritance) {
        Shape shape = super.createShape(name, extension, typeInheritance);
        
        buildIdMap();
        
        processShape(shape, null, null);
        
        return shape;
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
                JSONObject jsonSchema = new JSONObject(jsonTxt);
                
                if(this.schemaAnchor != null) {
                    String[] paths = this.schemaAnchor.split(MutableJsonType.SCHEMA_REF_SEPARATOR);
                    for(String path: paths) {
                        if(!jsonSchema.has(path)) {
                            throw new RuntimeException("Unable to find key in schemaAnchor path: " + path);
                        }
                        jsonSchema = jsonSchema.getJSONObject(path);
                    }
                }
                
                defineTypes(shape, jsonSchema);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read " + fileName, e);
            }
        }
        
        // Parent types names are extracted during the construction of the MutableJsonType
        for(Type type: shape.getUniqueTypes()) {
            ((ExternalType)type).initParentTypes(null, null);
        }
        
        defineProperties(shape);
        
        for(Type type: shape.getUniqueTypes()) {
            percolateIdProperty(type);
            
            if(type instanceof MutableJsonType) {
                ((MutableJsonType)type).defineRequired();
            }
        }
    }
    
    private void percolateIdProperty(Type type) {
        if(type.isDataType()) {
            return;
        }
        MutableJsonType entityType = (MutableJsonType) type;
        
        if(entityType.getIdentifierProperty() == null) {
            String idPropertyName = findIdProperty(entityType);
            if(idPropertyName != null) {
                entityType.setIdPropertyName(idPropertyName);
            }
        }
    }
    
    private String findIdProperty(ExternalType entityType) {
        if(entityType.getIdentifierProperty() != null) {
            return entityType.getIdentifierProperty().getName();
        }
        
        String idPropertyName = null;
        for(ExternalType parentType: entityType.getParentTypes()) {
            idPropertyName = findIdProperty(parentType);
            if(idPropertyName != null) {
                break;
            }
        }
        
        return idPropertyName;
    }
    
    private void defineTypes(Shape shape, JSONObject jsonSchema) {
        for(String entityTypeName: JSONObject.getNames(jsonSchema)) {   
            JSONObject typeJson = jsonSchema.getJSONObject(entityTypeName);
            MutableJsonType type = new MutableJsonType(entityTypeName, typeJson, idMap.get(entityTypeName));
            shape.addType(entityTypeName, type);
        }
    }

    protected void defineProperties(Shape shape) {
        for(Type type: shape.getUniqueTypes()) {
            if(MutableJsonType.class.isAssignableFrom(type.getClass())) {
                MutableJsonType swaggerType = (MutableJsonType) type;
                swaggerType.defineProperties(shape);
                
                if(idMap.containsKey(swaggerType.getEntityName())) {
                    swaggerType.setIdPropertyName(idMap.get(swaggerType.getEntityName()));
                }
            }
        }     
    }    
}