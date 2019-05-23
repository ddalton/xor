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

import org.json.JSONObject;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.action.Executable;
import tools.xor.action.PropertyKey;
import tools.xor.util.ObjectCreator;

import java.util.List;
import java.util.Map;

public interface CustomPersister
{
    void process(JSONObject object, EntityType type);

    Object getEntity(EntityKey key);

    void persistGraph(ObjectCreator objectCreator, Settings settings);

    void deleteGraph(ObjectCreator objectCreator, Settings settings);

    void addActions(Map<PropertyKey, List<Executable>> actions);

    void commit();

    void flush ();

    void clear();

    boolean readFromDB();
}
