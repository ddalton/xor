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

/**
 * Models relationship on a open property. 
 * Use this to enhance (add functionality) to the ORM model.
 * This usually arisis in places where the model is kept very simple, i.e., there is no TO_ONE or
 * TO_MANY relationships in the model for performance reasons. 
 * The user can then add this information so XOR can provide this behavior.
 * 
 * Pretty much any functionality can be modeled using the CUSTOM type and the
 * associated XorLambda functions. But we provide the TO_ONE and TO_MANY support
 * so this implementation does not have to be repeated as these two are the most common use cases.
 * 
 * @author Dilip Dalton
 *
 */
public enum RelationshipType {
	CUSTOM, // user defined relationship. For e.g., can be computed and not based on another entity 
	TO_ONE, // references another entity
	TO_MANY // references many entities. For e.g., a list or a set
}
