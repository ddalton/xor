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

import java.sql.Blob;
import java.util.Set;

import tools.xor.view.StoredProcedure;

/**
 * This class is used to get access to Persistence store specific data structures
 * For e.g., the JDBC connection managed by the provider.
 * 
 * @author Dilip Dalton
 *
 */
public interface PersistenceUtil {

    /**
     * Create a Blob instance in the persistence store and return a handle to it
     * @param po PersistenceOrchestrator instance
     * @return handle to Blob instance
     */
    Blob createBlob(DataStore po);
    
    /**
     * Create a JDBC Statement object for the given stored procedure.
     * The created JDBC statement object will be set on the StoredProcedure instance.
     * @param po PersistenceOrchestrator instance
     * 
     * @param sp storedProcedure instance
     */
    void createStatement (DataStore po, final StoredProcedure sp);
    
    /**
     * Insert records into the Query Join Table used by stored procedures for efficient 
     * data sharing.
     * @param po PersistenceOrchestrator instance
     * @param invocationId unique id for the query
     * @param ids to be saved in the table
     */
    void saveQueryJoinTable (DataStore po, String invocationId, Set ids);
    
    /**
     * Create the query join table in the persistence store.
     * @param po PersistenceOrchestrator instance
     * @param stringKeyLen size of the string field used to represent the id
     */
    void createQueryJoinTable(DataStore po, final Integer stringKeyLen);
}