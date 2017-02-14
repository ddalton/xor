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
 * This is used during entity data generation. Controls the size of the generated object graph.
 * If collections are present in the loop, the collection size represents the total elements across
 * all depths.
 * 
 * @author Dilip Dalton
 *
 */
public enum EntitySize {
    SMALL(35),
    MEDIUM(350),
    LARGE(1000),
    XLARGE(3500);
	
	private final int size; // maximum number of objects in the object graph
	
    EntitySize(int size) {
        this.size = size;
    }	
    
    public int size() { return this.size; }
}
