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
 * This annotation con only be specified on a public method that
 * returns value and has no arguments
 */ 
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XorInvoke {
	
	/**
	 * The data needed by the business logic method to perform
	 * its operation. This is optional.
	 *
	 * If this view is used, then the version or the optimistic concurrency
	 * control column needs to be automatically added to the view
	 * 
	 * @return
	 */
	String readview();
	
	/**
	 * The data that is persisted to the database
	 * This information is not necessarily the same as the information
	 * in the readview. By separating these two pieces of data it makes it possible
	 * to optimize each separately thereby bringing much improvement in performance, than
	 * if trying to optimize them together.
	 * 
	 * If the readview is provided then the version value needs to be copied
	 * 
	 * @return
	 */
	String updateview();
	
    /**
     * The version until which this method is valid
     * @return
     */
    int untilVersion() default Integer.MAX_VALUE;
    
    /**
     * The version from which this method is valid
     * @return
     */
    int fromVersion() default Settings.INITIAL_API_VERSION;
}
