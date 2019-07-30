package tools.xor.providers.jdbc;

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
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.util.graph.ObjectGraph;
import tools.xor.util.graph.TypeGraph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
    private DBTranslator dbTranslator;
    private JDBCPersistenceOrchestrator po;
    private boolean psBatch = false; // use prepared statement batch if order does not matter
    private Statement insertStatement;
    private Set<PreparedStatement> preparedStatements = new HashSet<>();

    private Map<EntityKey, JSONObject> idToObjects = new HashMap<>();

    private final List<String> insertBatch = new LinkedList<>();

    private final Map<String, PreparedStatement> statementCache = lruCache(1000);

    public static <K,V> Map<K,V> lruCache(final int maxSize) {
        return new LinkedHashMap<K,V>(maxSize*4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
                return size() > maxSize;
            }
        };
    }

    public JDBCSessionContext(JDBCPersistenceOrchestrator po, JDBCSessionContext context) {
        this.po = po;

        init(context);
    }

    public void setPsBatch (boolean psBatch)
    {
        this.psBatch = psBatch;
    }

    public void init(JDBCSessionContext context) {
        if(context != null) {
            this.idToObjects = context.idToObjects;
        }
    }

    /**
     * This method is used to navigate the input object and map the
     * objects with its id.
     *
     * @param object input used as the original from which modifications are identified
     * @param entityType of the object
     */
    @Override
    // TODO: create a copy for optimistic concurrency check
    // Since this object is used as modification
    public void process (JSONObject object, EntityType entityType) {

        EntityKey ek;
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
            if(object.has(property.getName())) {
                if (!property.getType().isDataType()) {
                    JSONObject child = object.getJSONObject(property.getName());
                    process(child, (JDBCType)property.getType());
                }
                else if (property.isMany()
                    && !((ExtendedProperty)property).getElementType().isDataType()) {
                    JSONArray child = object.getJSONArray(property.getName());
                    JDBCType elementType = (JDBCType)((ExtendedProperty)property).getElementType();
                    for (int i = 0; i < child.length(); i++) {
                        process(child.getJSONObject(i), elementType);
                    }
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
    @Override public Connection getConnection() {
        return this.connection;
    }

    private DBTranslator getDbTranslator() {
        boolean enclosingTransaction = true;
        try {
            if(connection == null || connection.isClosed()) {
                beginTransaction();
                enclosingTransaction = false;
            }

            if (this.dbTranslator == null) {
                this.dbTranslator = DBTranslator.instance(connection);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            if(!enclosingTransaction) {
                commit();
            }
        }

        return dbTranslator;
    }

    private void createInsertStatement() throws SQLException
    {
        if(this.insertStatement == null) {
            this.insertStatement = this.connection.createStatement();
        }
    }

    @Override public void persistGraph (ObjectCreator objectCreator, Settings settings)
    {
        try {
            createInsertStatement();
            List<BusinessObject> objects = new ArrayList<>(objectCreator.getDataObjects());
            EntityType entityType = (EntityType)settings.getEntityType();
            TypeGraph<State, Edge<State>> sg = settings.getView().getTypeGraph(entityType);
            Collections.sort(objects, new ObjectGraph.StateComparator(sg));

            for (BusinessObject bo : objects) {
                if (bo.getInstance() instanceof JSONObject) {
                    if (this.po.isTransient(bo)) {
                        // create INSERT statements for this object
                        persist(bo, settings);
                    }
                }
            }

            //  TODO:      Update/Delete
            //            - Update then delete
            //            - If a removed collection element does not have a surrogate/natural primary key then
            //                it will be removed based on all of its fields
        } catch(SQLException exception) {
            throw ClassUtil.wrapRun(exception);
        }
    }

    @Override public void persist (BusinessObject bo, Settings settings) throws SQLException
    {
        try {
            // If preparedStatement batching is not desired due to ordering reasons
            if(!psBatch) {
                createInsertStatement();
                for (String insertSql : getInsertSql(settings, bo)) {
                    insertStatement.addBatch(insertSql);
                    insertBatch.add(insertSql);
                }
            } else {
                for (PreparedStatement ps : getInsertPS(settings, bo)) {
                    ps.addBatch();
                    preparedStatements.add(ps);
                }
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Return 1 or more insert SQLs.
     * Return more than 1 in case of an object participating in an inheritance hierarchy
     *
     * @param settings
     * @param bo
     * @return
     */
    public List<String> getInsertSql(Settings settings, BusinessObject bo)
    {
        List<String> result = new LinkedList<>();
        for(Object o: getInsertObjs(settings, bo, false)) {
            result.add((String)o);
        }

        return result;
    }

    public List<PreparedStatement> getInsertPS(Settings settings, BusinessObject bo) {
        List<PreparedStatement> result = new LinkedList<>();
        for(Object o: getInsertObjs(settings, bo, true)) {
            result.add((PreparedStatement)o);
        }

        return result;
    }

    private List<Object> getInsertObjs(Settings settings, BusinessObject bo, boolean isPreparedStatement) {
        JDBCType entityType = (JDBCType)bo.getType();

        getDbTranslator().setIdentifier(settings, bo, entityType);

        Stack sqlStack = new Stack<>();
        while(entityType != null) {
            if(isPreparedStatement) {
                sqlStack.push(getPreparedStatement(bo, entityType));
            } else {
                sqlStack.push(getDbTranslator().getInsertSql(bo, entityType));
            }

            // Walk up the super-type
            entityType = (JDBCType)entityType.getSuperType();
        }

        List result = new LinkedList<>();
        while(!sqlStack.isEmpty()) {
            result.add(sqlStack.pop());
        }

        return result;
    }

    private PreparedStatement getPreparedStatement(BusinessObject bo, JDBCType entityType)
    {
        String psSQL = getDbTranslator().getInsertSqlFragment(bo, entityType, true);

        PreparedStatement ps = null;
        try {
            if(statementCache.containsKey(psSQL)) {
                ps = statementCache.get(psSQL);
            } else {
                ps = connection.prepareStatement(psSQL);
                statementCache.put(psSQL, ps);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
        getDbTranslator().setValues(ps, bo, entityType);

        return ps;
    }


    @Override public void deleteGraph (ObjectCreator objectCreator, Settings settings)
    {
        // TODO
    }

    @Override public void beginTransaction()
    {
        try {
            if(this.connection != null && !this.connection.isClosed()) {
                throw new RuntimeException("Cannot start a new transaction when there is an existing transaction");
            }
            this.connection = po.getNewConnection();
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override public void close()
    {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override public void commit ()
    {
        try {
            connection.commit();
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            this.close();
        }
    }

    @Override public void rollback ()
    {
        try {
            connection.rollback();
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            this.close();
        }
    }

    @Override
    public void flush() {
        try {
            if (!ApplicationConfiguration.config().containsKey(Constants.Config.BATCH_SKIP)
                || !ApplicationConfiguration.config().getBoolean(Constants.Config.BATCH_SKIP)) {

                if(preparedStatements.size() > 0) {
                    for(PreparedStatement ps: preparedStatements) {
                        ps.executeBatch();
                    }
                } else if(insertStatement != null) {
                    int[] result = insertStatement.executeBatch();

                    if (result.length != insertBatch.size()) {
                        throw new RuntimeException(
                            "The number of insert SQLs do not match the number received by the database");
                    }

                    StringBuilder failedSqls = new StringBuilder();
                    for (int i = 0; i < result.length; i++) {
                        if (result[i] != 1) {
                            failedSqls.append(insertBatch.get(i))
                                .append("\n");
                        }
                    }

                    if (failedSqls.length() > 0) {
                        throw new RuntimeException(
                            "The following SQLs have failed: \n " + failedSqls.toString());
                    }
                }
            } else {
                if(preparedStatements.size() > 0) {
                    throw new RuntimeException("Non batch execution is not supported. The batch.skip setting should be set to false.");
                }
                try(Statement stmt = getConnection().createStatement()) {
                    for (String insertSQL : insertBatch) {
                        //System.out.println("INSERTING: " + insertSQL);
                        stmt.executeUpdate(insertSQL);
                    }
                }
            }
        }
        catch (SQLException e) {
            for(String sql: insertBatch) {
                System.out.println(sql);
            }
            System.out.println("======================");
            throw ClassUtil.wrapRun(e);
        } finally {
            try {
                if(this.insertStatement != null) {
                    this.insertStatement.close();
                }
                if(preparedStatements != null) {
                    for(PreparedStatement ps: preparedStatements) {
                        ps.clearParameters();
                    }
                }
                this.clear();
            }
            catch (SQLException e) {
                throw ClassUtil.wrapRun(e);
            }
        }
    }

    public void clear() {
        this.idToObjects = new HashMap<>();
        this.insertBatch.clear();
        if(this.preparedStatements != null) {
            this.preparedStatements.clear();
        }
        this.statementCache.clear();
        this.insertStatement = null;
    }

    @Override public boolean readFromDB ()
    {
        return false;
    }
}
