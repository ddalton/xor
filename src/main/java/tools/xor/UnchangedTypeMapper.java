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
import tools.xor.util.CreationStrategy;
import tools.xor.util.MutableJsonCreationStrategy;
import tools.xor.util.ObjectCreator;

public class UnchangedTypeMapper extends AbstractTypeMapper
{
    @Override public Class<?> toDomain (Class<?> externalClass)
    {
        return externalClass;
    }

    @Override public Class<?> toDomain (Class<?> externalClass, BusinessObject bo)
    {
        return externalClass;
    }

    @Override public Class<?> toDomain(Type type)
    {
        return type.getInstanceClass();
    }

    @Override public Class<?> toExternal(Class<?> domainClass)
    {
        return domainClass;
    }

    @Override
    public Class<?> getSourceClass(Class<?> clazz, CallInfo callInfo)
    {
        return clazz;
    }

    public Class<?> getTargetClass(Class<?> clazz, CallInfo callInfo)
    {
        return clazz;
    }

    @Override
    public boolean isExternal(Class<?> clazz)
    {
        return true;
    }

    @Override
    public boolean isDomain(Class<?> clazz)
    {
        return true;
    }

    @Override
    public CreationStrategy getCreationStrategy(ObjectCreator oc)
    {
        return new MutableJsonCreationStrategy(oc);
    }

    @Override
    public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
        return new MutableJsonType(domainType, derivedClass);
    }

    @Override
    public String getExternalTypeName(Class<?> inputClass, Type domainType) {
        return domainType.getName();
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

    protected TypeMapper createInstance() {
        return new UnchangedTypeMapper();
    }

    @Override
    public TypeMapper newInstance(MapperDirection direction) {
        TypeMapper mapper = createInstance();
        mapper.setDirection(direction);

        return mapper;
    }
}
