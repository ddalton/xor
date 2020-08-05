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

package tools.xor.providers.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import tools.xor.BusinessObject;
import tools.xor.DataGenerator;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.util.ObjectCreator;

public interface CustomPersister
{
    void process(JSONObject object, EntityType type);

    Object getSnapshot(Object persistentInstance);

    Object getEntity(EntityKey key);

    void persistGraph(ObjectCreator objectCreator, Settings settings);

    void deleteGraph(ObjectCreator objectCreator, Settings settings);

    void create (BusinessObject bo, Settings settings, DataGenerator generator);
    
    void create (BusinessObject bo, List<String> columnsToUpdate);    

    void update (BusinessObject bo, BusinessObject dbBO);
    
    void update (BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys);    

    void delete (BusinessObject bo);

    void beginTransaction();

    /**
     * We create a new transaction if there is none, else we join the existing transaction
     * This is only used for read-only queries
     */
    void readOnlyTransaction();

    void close();

    void commit();

    void rollback();

    void flush ();

    void clear();

    Connection getConnection();

    boolean readFromDB();
}
