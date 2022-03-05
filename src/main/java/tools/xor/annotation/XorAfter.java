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
import tools.xor.ProcessingStage;
import tools.xor.Settings;

/**
 * This annotation allows a method to be executed during the post logic processing stage
 * 
 * @see ProcessingStage#POSTLOGIC
 */  
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XorAfter {
    
	/**
	 * The CRUD/clone actions for which the annotation is valid. An empty action means it is valid for any CRUD/clone action.
	 * @return array of AggregateAction
	 */
	AggregateAction[] action() default {}; 
	
	/**
	 * The tag for which the annotation is valid. An empty tag means it is always invoked.
	 *  Mainly used for custom logic.
	 * @return array of tags
	 */
	String[] tag() default {}; 
    
    /**
     * The version until which this method is valid
     * @return untilVersion
     */
    int untilVersion() default Integer.MAX_VALUE;
    
    /**
     * The version from which this method is valid
     * @return fromVersion
     */
    int fromVersion() default Settings.INITIAL_API_VERSION;
}