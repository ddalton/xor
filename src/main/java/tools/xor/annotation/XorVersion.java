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

package tools.xor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tools.xor.Settings;

/**
 * Mainly used for XSD versioning support. 
 * The XSD generator uses this information to decide whether to 
 * include or exclude a certain field depending on the specified API version 
 *
 */  
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface XorVersion {
    int untilVersion() default Integer.MAX_VALUE;
    int fromVersion() default Settings.INITIAL_API_VERSION;
}
