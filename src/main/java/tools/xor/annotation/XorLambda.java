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
import tools.xor.ExtendedProperty.Phase;
import tools.xor.ProcessingStage;
import tools.xor.Settings;

/**
 * This annotation helps a function behave like a lambda function, that can
 * be declaratively chained
 */  

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface XorLambda {
	
	/**
	 * The CRUD/clone actions for which the annotation is valid. An empty action means it is valid for any CRUD/clone action.
	 * @return array of AggregateAction
	 */
	// 
	AggregateAction[] action() default { AggregateAction.CREATE,
										 AggregateAction.UPDATE,
										 AggregateAction.MERGE,
										 AggregateAction.CLONE };
	
	/**
	 * The tag for which the annotation is valid. An empty tag means it is always invoked.
	 *  Mainly used for custom logic.
	 * @return array of tags
	 */
	String[] tag() default {}; 
	
	/**
	 * The name of the property for which this annotation is valid
	 * @return property name
	 */
	String property();   
	
	/**
	 * If true, then the processing does not descend further, applicable only for phase PRE
	 * @return true if the lambda flow stops proceeding further
	 */
	boolean capture() default false; 
	
	/**
	 * The processing phase in which this method is invoked.
	 * @return phase
	 */
    Phase phase() default Phase.PRE;

	/**
	 * The processing pass in which this method is invoked
	 * @return processing stage
	 */
	// TODO: Give the ability to run in multiple stages
	ProcessingStage stage() default ProcessingStage.UPDATE;
    
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
    
    /**
     * The order of execution, if a property has multiple methods
     * @return order number
     */
    int order() default 0;
}
