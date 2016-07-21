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

/**
 * Specifies the field that has the full aggregate data in JSON format
 * Typically only one field in a entity is marked as such.
 * The main use of this annotation is when large data support is needed or
 * when complex data needs to be stored in a NoSQL database.
 * 
 * On an RDBMS this is supported only on a BLOB field
 * 
 * If the data exceeds the data provider limits, then an error will be
 * thrown. 
 * In the future we might consider supporting splitting of data across multiple
 * rows. But that will entail additional field and annotations (e.g., XorPart, 
 * XorTotal) to support this.
 */ 
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.FIELD})
public @interface XorData {
}
