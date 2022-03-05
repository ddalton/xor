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

import tools.xor.AbstractBO;

/**
 * Allows the framework to set the argument to the input object specified by path.
 */ 
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface XorDomain {
	/**
	 * Retrieve the value at the path rooted in the domain object
	 * @return path string
	 */
    String path() default AbstractBO.PATH_CONTAINER;
    
	/**
	 * If true, then the business object wrapper is returned. This provides additional functionality on the object.
	 * 
	 * !!!!!!!! WARNING !!!!!!!
	 * If returning the wrapper, do not leak it outside of the method where it is used as this can cause a memory leak.
	 * In a tightly connected graph, holding on to the BusinessObject instance will hold pretty much the whole ObjectGraph in memory without
	 * releasing it.
	 * @return boolean value
	 */
	boolean wrapper() default false;     
}
