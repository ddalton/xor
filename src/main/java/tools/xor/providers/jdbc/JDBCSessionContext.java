package tools.xor.providers.jdbc;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.BusinessObject;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCType;
import tools.xor.NaturalEntityKey;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SurrogateEntityKey;
import tools.xor.action.Executable;
import tools.xor.action.PropertyKey;
import tools.xor.util.ClassUtil;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.util.graph.ObjectGraph;
import tools.xor.util.graph.TypeGraph;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
public class JDBCSessionContext implements CustomPersister
{
    private Connection connection;
    private Statement insertStatement;

    private Map<PropertyKey, List<Executable>> propertyActions = new Object2ObjectOpenHashMap<PropertyKey, List<Executable>>();
    private Map<EntityKey, JSONObject> idToObjects = new HashMap<>();

    private final List insertBatch = new LinkedList<>();

    /**
     * This method is used to navigate the input object and map the
     * objects with its id.
     *
     * @param object input used as the original from which modifications are identified
     * @param entityType of the object
     */
    @Override
    public void process (JSONObject object, EntityType entityType) {

        EntityKey ek = null;
        JDBCType type = (JDBCType) entityType;
        if(type.getIdentifierProperty() != null) {
            ek = new SurrogateEntityKey(object.get(type.getIdentifierProperty().getName()), type.getName());
        } else if(type.getNaturalKey() != null) {
            Map<String, Object> naturalKey = new HashMap<>();
            for(String key: type.getNaturalKey()) {
                naturalKey.put(key, object.get(key));
            }
            ek = new NaturalEntityKey(naturalKey, type.getName());
        } else {
            throw new RuntimeException("Type " + type.getName() + " does not have a primary key");
        }
        idToObjects.put(ek, object);

        for(Property property: type.getProperties()) {
            if(!property.getType().isDataType()) {
                JSONObject child = object.getJSONObject(property.getName());
                process(child, (JDBCType)property.getType());
            } else if(property.isMany() && !((ExtendedProperty)property).getElementType().isDataType()) {
                JSONArray child = object.getJSONArray(property.getName());
                JDBCType elementType = (JDBCType)((ExtendedProperty)property).getElementType();
                for(int i = 0; i < child.length(); i++) {
                    process(child.getJSONObject(i), elementType);
                }
            }
        }
    }

    @Override
    public Object getEntity (EntityKey key) {
        return idToObjects.get(key);
    }

    /**
     * Needed for the current session activities with XOR
     * @return
     */
    public Connection getConnection() {
        return this.connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override public void persistGraph (ObjectCreator objectCreator, Settings settings)
    {
        JDBCPersistenceOrchestrator po = (JDBCPersistenceOrchestrator)settings.getPersistenceOrchestrator();
        Connection connection = po.getConnection();

        try {
            this.insertStatement = connection.createStatement();
            List<BusinessObject> objects = new ArrayList<>(objectCreator.getDataObjects());
            EntityType entityType = (EntityType)settings.getEntityType();
            TypeGraph<State, Edge<State>> sg = settings.getView().getTypeGraph(entityType);
            Collections.sort(objects, new ObjectGraph.StateComparator(sg));

            for (BusinessObject bo : objects) {
                if (bo.getInstance() instanceof JSONObject) {
                    if (po.isTransient(bo)) {
                        // create INSERT statement for this object
                        String insertSql = DBTranslator.instance(connection).getInsertSql(settings, bo);
                        insertStatement.addBatch(insertSql);
                        insertBatch.add(insertSql);
                    }
                }
            }

            //        Create
            //            - Iterate through the BusinessObjects in the object creator
            //            - If the object is a transient instance then it needs to be created.
            //            NOTE: A transient instance might have a user provided id. If not, the id generator associated with its type needs to be used
            //            - sort all the transient instances
            //            - Create INSERT statements
            //
            //        Update/Delete
            //            - Update then delete
            //            - If a removed collection element does not have a surrogate/natural primary key then
            //                it will be removed based on all of its fields
        } catch(SQLException exception) {
            throw ClassUtil.wrapRun(exception);
        }
    }

    @Override public void deleteGraph (ObjectCreator objectCreator, Settings settings)
    {
        // TODO
    }

    @Override
    public void addActions(Map<PropertyKey, List<Executable>> actions) {
        this.propertyActions.putAll(actions);
    }

    @Override public void commit ()
    {
        try {
            connection.commit();
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            try {
                connection.close();
            }
            catch (SQLException e) {
                throw ClassUtil.wrapRun(e);
            }
        }
    }

    @Override
    public void flush() {
        try {
            int[] result = insertStatement.executeBatch();

            if(result.length != insertBatch.size()) {
                throw new RuntimeException("The number of insert SQLs do not match the number received by the database");
            }

            StringBuilder failedSqls = new StringBuilder();
            for(int i = 0; i < result.length; i++) {
                if(result[i] != 1) {
                    failedSqls.append(insertBatch.get(i))
                        .append("\n");
                }
            }

            if(failedSqls.length() > 0) {
                throw new RuntimeException("The following SQLs have failed: \n " + failedSqls.toString());
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            try {
                this.insertStatement.close();
            }
            catch (SQLException e) {
                throw ClassUtil.wrapRun(e);
            }
        }
    }

    public void clear() {
        this.propertyActions = new Object2ObjectOpenHashMap<>();
        this.idToObjects = new HashMap<>();
    }
}
