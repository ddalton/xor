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

import tools.xor.AggregateAction;
import tools.xor.Settings;

/**
 * This annotation con only be specified on a public method that
 * returns value and has no arguments
 */ 
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XorRead {
    /**
     * The CRUD/clone actions for which the annotation is valid. An empty action means it is valid for any CRUD/clone action.
     * @return
     */
    //
    AggregateAction[] action() default { AggregateAction.READ };

	/**
	 * The tag for which the annotation is valid. An empty tag means it is always invoked.
	 *  Mainly used for custom logic.
	 * @return
	 */
	String[] tag() default {}; 
	
	/**
	 * The name of the property for which this annotation is valid
	 * @return
	 */
	String property();   
    
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
