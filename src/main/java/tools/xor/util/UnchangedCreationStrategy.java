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

package tools.xor.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.ListType;
import tools.xor.Property;


public class UnchangedCreationStrategy extends AbstractCreationStrategy {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    protected POJOCreationStrategy pojoCS;

    public UnchangedCreationStrategy(ObjectCreator objectCreator) {
        super(objectCreator);
        setDomainCreationStrategy(objectCreator);
    }

    protected void setDomainCreationStrategy(ObjectCreator objectCreator) {
        pojoCS = new POJOCreationStrategy(objectCreator);
    }

    @Override
    /**
     * Handle the creation of the following classes
     * JsonObject
     * JsonArray
     * JsonNumber
     * JsonString
     * JsonValue.TRUE
     * JsonValue.FALSE
     * JsonValue.NULL
     */
    public Object newInstance(Object from, BasicType type, Class<?> toClass) throws Exception {
        return this.newInstance(from, type, toClass, null, null);
    }

    @Override
    public Object newInstance(Object from, BasicType type, Class<?> toClass, BusinessObject container,
                              Property containmentProperty) throws Exception {

        Object result;
        if(!type.isDataType()) {
            result = new JSONObject();
        } else if(type instanceof ListType) {
            result = new JSONArray();
        } else {
            result = pojoCS.newInstance(from, type, toClass);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(
                "UnchangedCreationStrategy#newInstance from: " + (from == null ? "null" : from.getClass().getName())
                    + ", toClass: " + (toClass == null ? "null" : toClass.getName())
                    + ", result: " + (result == null ? "null" : result.getClass().getName()));
        }

        return result;
    }

    @Override
    public boolean needsObjectGraph() {
        return true;
    }
}
