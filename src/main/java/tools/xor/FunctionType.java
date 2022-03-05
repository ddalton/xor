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

public enum FunctionType
{
  ALIAS,      // modify the name of a property in the result structure
  SKIP,       // modify the QueryTree to conditionally skip a QueryFragment
  INCLUDE,    // modify the QueryTree to conditionally include a QueryFragment
  ASC,        // sort in ascending order in a query
  DESC,       // sort in descending order in query
  FREESTYLE,  // is added to the WHERE clause of the query
  COMPARISON, // is used in the WHERE clause to perform simple comparison conditions
  CUSTOM      // future use
}
