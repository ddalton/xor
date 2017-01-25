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
    SMALL(25, 1),  // Has a maximum of 25 objects and loops are 1 level deep
    MEDIUM(250, 3), // Has a maximum of 250 objects and loops are 3 levels deep
    LARGE(2500, 5),  // Has a maximum of 2500 objects and loops are 5 levels deep
    XLARGE(25000, 10); // Has have a maximum of 25000 objects and loops are upto 10 levels deep
	
	private final int size; // maximum number of objects
	private final int depth; // max depth
	
    EntitySize(int size, int depth) {
        this.size = size;
        this.depth = depth;
    }	
    
    private double size() { return this.size; }
    private double depth() { return this.depth; }
}
