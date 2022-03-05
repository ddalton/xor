/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

package tools.xor;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.service.DataModel;
import tools.xor.service.Shape;
import tools.xor.util.CreationStrategy;
import tools.xor.util.ObjectCreator;
import tools.xor.util.UnchangedCreationStrategy;

public class UnchangedTypeMapper extends AbstractTypeMapper
{
    public UnchangedTypeMapper() {
        super();
    }
    
    public UnchangedTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) 
    {
        super(das, side, shapeName, persistenceManaged);
    }
    
    @Override public String toDomain (String typeName)
    {
        return typeName;
    }
    
    @Override
    public String toDomain(String externalTypeName, BusinessObject bo) {
        return externalTypeName;
    }    

    @Override public Class<?> toExternal(Type type)
    {
        return type.getInstanceClass();
    }
    
    public String getMappedType(String typeName, CallInfo callInfo) {
        return typeName;
    }
    
    @Override
    public boolean isExternal(String typeName) {
        return true;
    }    
    
    @Override
    public boolean isDomain(String typeName) {
        return true;
    }       

    @Override
    public CreationStrategy getCreationStrategy(ObjectCreator oc)
    {
        return new UnchangedCreationStrategy(oc);
    }

    @Override
    public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
        return new MutableJsonType(domainType, derivedClass);
    }

    @Override
    public boolean immutable() {
        return false;
    }

    @Override
    public boolean isOpen(Class<?> clazz) {
        if(JSONObject.class.isAssignableFrom(clazz)
            || JSONArray.class.isAssignableFrom(clazz) ) {
            return true;
        }

        return false;
    }

    @Override
    protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        return new UnchangedTypeMapper(das, side, shapeName, persistenceManaged);
    }
    
    @Override 
    public TypeMapper newInstance(MapperSide side) {
        return newInstance(side, null);
    }   
    
    @Override
    public TypeMapper newInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        return createInstance(das, side, shapeName, persistenceManaged);
    }     
    
    @Override
    public Shape getDomainShape() {
        if(this.domainShape == null) {
            this.domainShape = getModel().getShape(getShapeName());
            
            if(this.domainShape == null) {
                // create this shape
                this.domainShape = getModel().createShape(getShapeName());
            }
        }

        return this.domainShape;
    }

    @Override
    public Shape getDynamicShape() {
        if(this.dynamicShape == null) {
            // this call initializes the dynamic shape
            this.getDomainShape();
            
            // By default the dynamic shape is the same as the domain shape
            // implementations can override this behavior
            this.dynamicShape = this.domainShape;            
        }
        return this.dynamicShape;
    }    
}
