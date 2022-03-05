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

package tools.xor;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.generator.Generator;
import tools.xor.generator.LocalizedString;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

public class StringType extends SimpleType implements Scalar {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public static final int DEFAULT_LENGTH = 255;
    public static final int MIN_LENGTH = 7; // to avoid empty strings in natural keys and reduce the occurrence of unique constraint violations
    public static final int MAX_LENGTH;
    public static final char[] ALPHA_NUMERIC = new char[62]; // Currently restricted to English alphabets
    
    static {
        if (ApplicationConfiguration.config().containsKey(Constants.Config.MAX_STRING_LEN)) {
            MAX_LENGTH = ApplicationConfiguration.config().getInt(Constants.Config.MAX_STRING_LEN);
        } else {
            MAX_LENGTH = -1;
        }
        
        // initialize the numeric characters
        int index = 0;
        for(char c = '0'; c <= '9'; c++) {
            ALPHA_NUMERIC[index++] = c;
        }
        // initialize the lower case alphabetic characters
        for(char c = 'a'; c <= 'z'; c++) {
            ALPHA_NUMERIC[index++] = c;
        }
        // initialize the upper case alphabetic characters
        for(char c = 'A'; c <= 'Z'; c++) {
            ALPHA_NUMERIC[index++] = c;
        }               
    }
    
    public static String randomAlphanumeric(int count) {
        final char[] buffer = new char[count];
        
        int len = ALPHA_NUMERIC.length;
        for(int i = 0; i < count; i++) {
            // int index = (int) (ClassUtil.nextDouble() * len);
            // buffer[i] = ALPHA_NUMERIC[index%len];
            buffer[i] = ALPHA_NUMERIC[ThreadLocalRandom.current().nextInt(0, len)];
        }
        
        return new String(buffer);
    }

    public StringType(Class<?> clazz) {
        super(clazz);
    }

    @Override
    public String getGraphQLName () {
        return Scalar.STRING;
    }

    static public int getLength(Integer value) {
        value = (value == null) ? DEFAULT_LENGTH : value;
        if(MAX_LENGTH != -1 && value > MAX_LENGTH) {
            return MAX_LENGTH;
        }
        
        return value;
    }

    @Override
    public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
                           StateGraph.ObjectGenerationVisitor visitor) {

        Generator gen = ((ExtendedProperty)property).getGenerator(visitor.getRelationshipName());

        Integer stringLen = null;
        if(gen == null || gen instanceof LocalizedString) {
            int length = DEFAULT_LENGTH;
            if (property.getConstraints().containsKey(Constants.XOR.CONS_LENGTH)) {
                length = (int)property.getConstraints().get(Constants.XOR.CONS_LENGTH);
            }
            //stringLen =  (int)(Math.random() * length);
            stringLen = ThreadLocalRandom.current().nextInt(0, length);
            if (stringLen < MIN_LENGTH) {
                stringLen = (MIN_LENGTH > length) ? length : MIN_LENGTH;
            }
        }

        if(gen != null) {
            return gen.getStringValue(property, visitor);
        } else {
            return randomAlphanumeric(getLength(stringLen));
        }
    }   
    
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_STRING_TYPE;
    }    
}
