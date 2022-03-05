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

package tools.xor.service;

public interface PersistenceProvider {
    /**
     * Creates the DataStore appropriate for this DataModel
     * @param sessionContext required if manually creating the session/entityManager
     * @param data any additional data required by the DataStore, e.g., 
     *        persistence unit name
     * @return DataStore object
     */
    DataStore createDS(Object sessionContext, Object data);    
    
    /**
     * Set the PersistenceUtil for this PersistenceProvider.
     * A persistence provider might not support multiple implementations, so
     * this is optional.
     * 
     * @param persistenceUtil instance
     */
    default void setPersistenceUtil(PersistenceUtil persistenceUtil) {  }    
}