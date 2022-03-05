/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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
 * The restriction with this generator is that the element id will never be repeated.
 *
 * This generator does not make use of end limit, but only start
 *
 */
public class SlidingElementGenerator extends CollectionElementGenerator
{

    public SlidingElementGenerator(String[] arguments) {
        super(arguments);

        // initialized value
        setValue(getStart());
    }

    @Override protected void updateValue() {
        // increment value
        setValue(getValue()+1);
    }
}
