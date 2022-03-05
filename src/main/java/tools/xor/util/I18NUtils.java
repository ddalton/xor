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

package tools.xor.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18NUtils {
    /**
     * Constants for the resource bundle names
     */
    public static final String CORE_RESOURCES = "CoreResources";
  
    
    /**
     * Returns the localized string with the given key from the given bundle.
     * Works with messsage with no parameter.
     * @param key of the resource
     * @param bundleName of the resoure
     * @param locale of the user
     * @return the localized resource
     */
    public static String getResource(String key, String bundleName, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            // mimic the way jspc returns when resource is missing
            return "??? " + key + " ???";
        }
    }  
    
    public static String getResource(String key, String bundleName, Locale locale, String[] params ) {
        return MessageFormat.format(getResource(key, bundleName, locale), (Object[])params);
    }
    
      
    public static String getResource(String key, String bundleName) { 
    	return getResource(key, bundleName, Locale.getDefault());
    }   
     
    public static String getResource(String key, String bundleName, String[] params ) {
    	return getResource(key, bundleName, Locale.getDefault(), params);
    }   
  
}