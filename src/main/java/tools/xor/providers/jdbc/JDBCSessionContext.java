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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.AbstractTypeMapper;
import tools.xor.BusinessObject;
import tools.xor.DataGenerator;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ImmutableBO;
import tools.xor.JDBCType;
import tools.xor.NaturalEntityKey;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SurrogateEntityKey;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.util.graph.ObjectGraph;
import tools.xor.util.graph.TypeGraph;

public class JDBCSessionContext implements CustomPersister
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private DBTranslator dbTranslator;
    private JDBCDataStore po;
    private ImportMethod importMethod = ImportMethod.PREPARED_STATEMENT; // use prepared statement batch if order does not matter
    private Statement statement;
    //private Set<PreparedStatement> preparedInsert = new HashSet<>();
    private Map<PSKey, PreparedStatement> preparedInsert = new HashMap<>();
    private Map<PSKey, PreparedStatement> preparedUpdate = new HashMap<>();
    private Map<PSKey, PreparedStatement> preparedDelete = new HashMap<>();
    private Map<EntityKey, JSONObject> idToObjects = new HashMap<>();
    private Map<JSONObject, JSONObject> snapshots = new HashMap<>();
    private final Map<String, List<String>> sqlByType = new HashMap<>();
    private List<String> literalSQLs = new LinkedList<>();
    private final Map<String, PreparedStatement> statementCache = lruCache(1000);
    private Stack<ConnectionHolder> connections = new Stack<>();
    private Map<String, BufferedWriter> csvWriters = new HashMap<>();
    private Boolean autoCommit; // 3 value logic, only set if initialized
    private boolean orderSQL = true;

    public boolean isOrderSQL() {
        return orderSQL;
    }

    public void setOrderSQL(boolean orderSQL) {
        this.orderSQL = orderSQL;
    }

    public Boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    private static class ConnectionHolder {
        private final boolean owner;
        private final Connection connection;
        private final boolean readOnly; // If this is true, then there are no modifications and
                                        // it does not have to be committed
        private boolean originalAutoCommit;

        public boolean isOriginalAutoCommit() {
            return originalAutoCommit;
        }

        public void setOriginalAutoCommit(boolean originalAutoCommit) {
            this.originalAutoCommit = originalAutoCommit;
        }

        public Connection getConnection() {
            return this.connection;
        }

        public ConnectionHolder(Connection c, boolean owner, boolean readOnly) {
            this.connection = c;
            this.owner = owner;
            this.readOnly = readOnly;
        }

        public boolean isOwner() {
            return this.owner;
        }
    }

    private static class PSKey implements ObjectGraph.StateComparator.TypedObject {
        private final Type type;

        @Override public Type getType ()
        {
            return this.type;
        }

        PSKey(JDBCType type) {
            this.type = type;
        }

        @Override
        public int hashCode() {
            return type.getName().hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if( !(other instanceof PSKey)) {
                return false;
            }

            return type.getName().equals(((PSKey)other).getType().getName());
        }
    }

    public static <K,V> Map<K,V> lruCache(final int maxSize) {
        return new LinkedHashMap<K,V>(maxSize*4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
                return size() > maxSize;
            }
        };
    }

    public JDBCSessionContext(JDBCDataStore po, JDBCSessionContext context) {
        this.po = po;

        init(context);
    }

    public ImportMethod getImportMethod() {
        return this.importMethod;
    }

    public void setImportMethod (ImportMethod importMethod)
    {
        this.importMethod = importMethod;
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
    // The user should create a copy for optimistic concurrency check
    // Since this object is used as modification
    public void process (JSONObject object, EntityType entityType) {

        JSONObject snapshot = ClassUtil.copyJson(object);

        EntityKey ek;
        JDBCType type = (JDBCType) entityType;
        if(type.getIdentifierProperty() != null) {
            ek = new SurrogateEntityKey(object.get(type.getIdentifierProperty().getName()), AbstractTypeMapper.getSurrogateKeyTypeName(type));
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
        snapshots.put(object, snapshot);

        Iterator<String> iterator = object.keys();
        while(iterator.hasNext()) {
            Property property = type.getProperty(iterator.next());
            if(property != null) {
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
    public Object getSnapshot(Object persistentInstance) {
        return snapshots.get(persistentInstance);
    }

    @Override
    public Object getEntity (EntityKey key) {
        return idToObjects.get(key);
    }

    /**
     * Needed for the current session activities with XOR
     * @return JDBC connection
     */
    @Override public Connection getConnection() {

        if(connections.size() > 0 ) {
            return connections.peek().getConnection();
        }
        return null;
    }

    private DBTranslator getDbTranslator ()
    {
        if (this.dbTranslator == null) {
            beginTransaction();
            try {
                this.dbTranslator = DBTranslator.instance(getConnection());
            } finally {
                close();
            }
        }

        return dbTranslator;
    }

    private void createStatement () throws SQLException
    {
        if(this.statement == null) {
            this.statement = getConnection().createStatement();
        }
    }

    @Override public void persistGraph (ObjectCreator objectCreator, Settings settings)
    {
        List<BusinessObject> objects = new ArrayList<>(objectCreator.getDataObjects());
        EntityType entityType = (EntityType)settings.getEntityType();
        TypeGraph<State, Edge<State>> sg = settings.getView().getTypeGraph(entityType);
        Collections.sort(objects, new ObjectGraph.StateComparator(sg));

        for (BusinessObject bo : objects) {
            if (bo.getInstance() instanceof JSONObject) {
                Object persistentInstance = this.po.getEntity(bo);
                if (persistentInstance == null) {
                    // create INSERT statements for this object
                    create(bo, settings, null);
                } else {
                    // update
                    BusinessObject dbBO = new ImmutableBO(entityType, null, null, objectCreator);
                    Object snapshotInstance = getSnapshot(persistentInstance);
                    if(snapshotInstance != null) {
                        dbBO.setInstance(snapshotInstance);
                        update(bo, dbBO);
                    }
                }
            }
        }
    }
    
    @Override public void create (BusinessObject bo, Settings settings, DataGenerator generator) {
        List<EntitySQL> entitySQLs = getInsertObjs(settings, bo, importMethod, generator);
        createEntity(bo, entitySQLs);
    }
    
    @Override public void create (BusinessObject bo, List<String> columnsToUpdate) {
        List<EntitySQL> entitySQLs = getInsertObjs(bo, importMethod, columnsToUpdate);
        createEntity(bo, entitySQLs);
    }    

    private void createEntity (BusinessObject bo, List<EntitySQL> entitySQLs)
    {
        try {
            // If preparedStatement batching is not desired due to ordering reasons
            switch(importMethod) {
            case LITERAL_SQL:
                createStatement();
                for (EntitySQL entitySQL : entitySQLs) {
                    statement.addBatch(entitySQL.sql);
                    literalSQLs.add(entitySQL.sql);
                }
                break;
            case PREPARED_STATEMENT:
                for (EntitySQL entitySQL : entitySQLs) {
                    entitySQL.ps.addBatch();
                    preparedInsert.put(new PSKey(entitySQL.entityType), entitySQL.ps);

                    if(logger.isDebugEnabled()) {
                        logger.debug(entitySQL.sql);
                    }
                }
                break;
            case CSV:
                for (EntitySQL entitySQL : entitySQLs) {
                    addSQL(entitySQL);
                }
                break;
            }
        } catch(SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override
    public void update (BusinessObject bo, BusinessObject dbBO) {
        try {
            // If preparedStatement batching is not desired due to ordering reasons
            switch(importMethod) {
            case LITERAL_SQL:
                createStatement();
                for (EntitySQL entitySQL : getUpdateObjs(bo, dbBO, importMethod)) {
                    statement.addBatch(entitySQL.sql);
                    literalSQLs.add(entitySQL.sql);
                }
                break;
            case PREPARED_STATEMENT:
                for (EntitySQL entitySQL : getUpdateObjs(bo, dbBO, importMethod)) {
                    entitySQL.ps.addBatch();
                    preparedUpdate.put(new PSKey(entitySQL.entityType), entitySQL.ps);
                }
                break;
            case CSV:
                throw new RuntimeException("CSV is not supported for update");
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }
    
    @Override
    public void update (BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys) {
        try {
            // If preparedStatement batching is not desired due to ordering reasons
            switch(importMethod) {
            case LITERAL_SQL:
                createStatement();
                for (EntitySQL entitySQL : getUpdateObjs(bo, columnsToSet, lookupKeys, importMethod)) {
                    statement.addBatch(entitySQL.sql);
                    literalSQLs.add(entitySQL.sql);
                }
                break;
            case PREPARED_STATEMENT:
                for (EntitySQL entitySQL : getUpdateObjs(bo, columnsToSet, lookupKeys, importMethod)) {
                    entitySQL.ps.addBatch();
                    preparedUpdate.put(new PSKey(entitySQL.entityType), entitySQL.ps);
                }
                break;
            case CSV:
                throw new RuntimeException("CSV is not supported for update");
            }
        }catch(SQLException e) {
            throw ClassUtil.wrapRun(e);
        }        
    }

    public Object getSingleResult(BusinessObject bo, String primaryKeyColumn, Map<String, String> lookupKeys) {
        try {
            // If preparedStatement batching is not desired due to ordering reasons
            switch(importMethod) {
            case PREPARED_STATEMENT:
                PreparedStatement ps = getSelectStmt(bo, primaryKeyColumn, lookupKeys);
                Object result = null;
                try(ResultSet rs = ps.executeQuery()) {
                    if(rs.next()) {
                        result = rs.getObject(1);
                    }
                }                
                return result;
            case LITERAL_SQL:
                throw new RuntimeException("Literal sql is not supported for select");                
            case CSV:
                throw new RuntimeException("CSV is not supported for update");
            default:
                return null;                
            }
        }catch(SQLException e) {
            throw ClassUtil.wrapRun(e);
        }  
    }    
    
    private PreparedStatement getSelectStmt (BusinessObject bo, String primaryKeyColumn, Map<String, String> lookupKeys)
    {
        JDBCType entityType = (JDBCType)bo.getType();
        String psSQL = getDbTranslator().getSelectStmt(entityType, primaryKeyColumn, lookupKeys);

        PreparedStatement ps = getOrCreate(psSQL);
        getDbTranslator().setSelectValues(entityType, ps, bo, primaryKeyColumn, lookupKeys);

        return ps;
    }    

    private void addSQL(EntitySQL entitySQL) {
        List<String> sqls = sqlByType.get(entitySQL.entityType.getName());
        if(sqls == null) {
            sqls = new LinkedList<>();
            sqlByType.put(entitySQL.entityType.getName(), sqls);
        }

        sqls.add(entitySQL.sql);
    }

    private static class EntitySQL {
        JDBCType entityType;
        PreparedStatement ps;
        String sql;

        EntitySQL(JDBCType entityType, PreparedStatement ps, String sql) {
            this.entityType = entityType;
            this.ps = ps;
            this.sql = sql;
        }
    }

    /*
     * Return 1 or more insert SQLs.
     * Return more than 1 in case of an object participating in an inheritance hierarchy
     */
    private List<EntitySQL> getInsertObjs(Settings settings, BusinessObject bo, ImportMethod importMethod, DataGenerator dataGenerator) {
        JDBCType entityType = (JDBCType)bo.getType();

        getDbTranslator().setIdentifier(settings, bo, entityType);

        Stack sqlStack = new Stack<>();
        while(entityType != null) {
            switch(importMethod) {
            case PREPARED_STATEMENT:
                String literalSQL = null;
                if(logger.isDebugEnabled()) {
                    literalSQL = getDbTranslator().getInsertSql(entityType, bo, dataGenerator);
                }
                sqlStack.push(new EntitySQL(entityType, getPreparedInsert(
                        entityType,
                        bo,
                        dataGenerator), literalSQL));
                break;
            case LITERAL_SQL:
                sqlStack.push(new EntitySQL(entityType, null, getDbTranslator().getInsertSql(entityType, bo, dataGenerator)));
                break;
            case CSV:
                sqlStack.push(new EntitySQL(entityType, null, getDbTranslator().getCSV(entityType, bo, dataGenerator)));
                break;
            }

            // Walk up the super-type
            entityType = (JDBCType)entityType.getParentType();
        }

        List result = new LinkedList<>();
        while(!sqlStack.isEmpty()) {
            result.add(sqlStack.pop());
        }

        return result;
    }
    
    /*
     * Use this method is loading data from a CSV file that has the identifier already populated
     * Inheritance does not need to be handled as it is explicitly handled by the client
     */
    private List<EntitySQL> getInsertObjs(BusinessObject bo, ImportMethod importMethod, List<String> columns) {
        JDBCType entityType = (JDBCType)bo.getType();
        
        List<Property> properties = new ArrayList<>();
        for(String column: columns) {
            Property p = entityType.getProperty(column.toUpperCase());
            if(p == null) {
                throw new RuntimeException(String.format("Unable to find property for column %s on table %s", column, entityType.getName()));
            }
            properties.add(p);
        }

        List result = new LinkedList<>();        
        switch(importMethod) {
        case PREPARED_STATEMENT:
            String literalSQL = null;
            if(logger.isDebugEnabled()) {
                literalSQL = getDbTranslator().getInsertSql(entityType, bo, columns, properties);
            }
            result.add(new EntitySQL(entityType, getPreparedInsert(
                    entityType,
                    bo,
                    columns,
                    properties), literalSQL));
            break;
        case LITERAL_SQL:
            result.add(new EntitySQL(entityType, null, getDbTranslator().getInsertSql(entityType, bo, columns, properties)));
            break;
        case CSV:
            result.add(new EntitySQL(entityType, null, getDbTranslator().getCSV(entityType, bo, properties)));
            break;
        }

        return result;
    }    

    private List<EntitySQL> getUpdateObjs(BusinessObject bo, BusinessObject dbBO, ImportMethod importMethod) {
        JDBCType entityType = (JDBCType)bo.getType();

        Stack sqlStack = new Stack<>();
        while(entityType != null) {
            switch (importMethod) {
            case PREPARED_STATEMENT:
                sqlStack.push(
                    new EntitySQL(
                        entityType,
                        getPreparedUpdate(entityType, bo, dbBO),
                        null));
                break;
            case LITERAL_SQL:
                sqlStack.push(
                    new EntitySQL(
                        entityType,
                        null,
                        getDbTranslator().getUpdateSql(entityType, bo, dbBO)));
                break;
            case CSV:
                throw new RuntimeException("CSV option not supported for update");
            }

            // Walk up the super-type
            entityType = (JDBCType)entityType.getParentType();
        }

        List result = new LinkedList<>();
        while(!sqlStack.isEmpty()) {
            result.add(sqlStack.pop());
        }

        return result;
    }
    
    private List<EntitySQL> getUpdateObjs(BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys, ImportMethod importMethod) {
        JDBCType entityType = (JDBCType)bo.getType();

        List result = new LinkedList<>();
        switch (importMethod) {
        case PREPARED_STATEMENT:
            result.add(
                new EntitySQL(
                    entityType,
                    getPreparedUpdate(entityType, bo, columnsToSet, lookupKeys),
                    null));
            break;
        case LITERAL_SQL:
            result.add(
                new EntitySQL(
                    entityType,
                    null,
                    getDbTranslator().getUpdateSql(entityType, bo, columnsToSet, lookupKeys)));
            break;
        case CSV:
            throw new RuntimeException("CSV option not supported for update");
        }

        return result;
    }    

    private List<EntitySQL> getDeleteObjs(BusinessObject bo) {
        JDBCType entityType = (JDBCType)bo.getType();
        List<EntitySQL> result = new LinkedList<>();

        while(entityType != null) {
            result.add(new EntitySQL(
                entityType,
                getPreparedDelete(entityType, bo),
                null));

            // Walk up the super-type
            entityType = (JDBCType)entityType.getParentType();
        }

        return result;
    }

    private PreparedStatement getPreparedDelete (
        JDBCType entityType,
        BusinessObject bo)
    {
        String psSQL = getDbTranslator().getDeleteSqlFragment(entityType, bo);

        PreparedStatement ps = getOrCreate(psSQL);
        getDbTranslator().setDeletePredicate(entityType, ps, bo);

        return ps;
    }

    private PreparedStatement getPreparedInsert (
        JDBCType entityType,
        BusinessObject bo,
        DataGenerator dataGenerator)
    {
        String psSQL = getDbTranslator().getInsertSqlFragment(entityType, bo, true, dataGenerator);

        PreparedStatement ps = getOrCreate(psSQL);
        getDbTranslator().setInsertValues(entityType, ps, bo, false, dataGenerator);

        return ps;
    }
    
    private PreparedStatement getPreparedInsert (
            JDBCType entityType,
            BusinessObject bo,
            List<String> columns,
            List<Property> properties)
        {
            String psSQL = getDbTranslator().getInsertSqlFragment(entityType, true, columns);

            PreparedStatement ps = getOrCreate(psSQL);
            getDbTranslator().setInsertValues(entityType, ps, bo, false, properties, false);

            return ps;
        }
    
    private PreparedStatement getOrCreate(String psSQL) {
        PreparedStatement ps = null;
        try {
            if(statementCache.containsKey(psSQL)) {
                ps = statementCache.get(psSQL);
            } else {
                ps = getConnection().prepareStatement(psSQL);
                statementCache.put(psSQL, ps);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
        
        return ps;
    }

    private PreparedStatement getPreparedUpdate (JDBCType entityType, BusinessObject bo, BusinessObject dbBO)
    {
        String psSQL = getDbTranslator().getUpdateSqlFragment(entityType, bo, true);

        PreparedStatement ps = getOrCreate(psSQL);
        getDbTranslator().setUpdateValues(entityType, ps, bo, dbBO);

        return ps;
    }
    
    private PreparedStatement getPreparedUpdate (JDBCType entityType, BusinessObject bo, List<String> columnsToSet, Map<String, Object> lookupKeys)
    {
        String psSQL = getDbTranslator().getUpdateSqlFragment(entityType, bo, columnsToSet, lookupKeys);

        PreparedStatement ps = getOrCreate(psSQL);
        getDbTranslator().setUpdateValues(entityType, ps, bo, columnsToSet, lookupKeys);

        return ps;
    }    

    @Override public void deleteGraph (ObjectCreator objectCreator, Settings settings)
    {
        List<BusinessObject> objects = new ArrayList<>(objectCreator.getDataObjects());
        EntityType entityType = (EntityType)settings.getEntityType();
        TypeGraph<State, Edge<State>> sg = settings.getView().getTypeGraph(entityType);
        Collections.sort(objects, new ObjectGraph.StateComparator(sg));
        Collections.reverse(objects);

        for (BusinessObject bo : objects) {
            delete(bo);
        }
    }

    @Override
    public void delete (BusinessObject bo) {
        try {
            for (EntitySQL entitySQL : getDeleteObjs(bo)) {
                entitySQL.ps.addBatch();
                preparedDelete.put(new PSKey(entitySQL.entityType), entitySQL.ps);
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override public void beginTransaction() {
        beginTransaction(false);
    }

    public void beginTransaction(boolean readOnly)
    {
        // check if the connections are valid
        while(!connections.isEmpty()) {
            ConnectionHolder holder = connections.peek();

            try {
                if (holder.getConnection().isClosed()) {
                    // this connection is no longer valid, so we get rid of it
                    connections.pop();
                } else {
                    // We have a valid connection
                    break;
                }
            } catch (SQLException exception) {
                break;
            }
        }

        if(connections.size() == 0) {
            Connection c = po.getNewConnection();
            ConnectionHolder ch = new ConnectionHolder(c, true, readOnly);
            try {
                if(isAutoCommit() != null) {
                    ch.setOriginalAutoCommit(c.getAutoCommit());
                    c.setAutoCommit(isAutoCommit());
                }
            } catch(SQLException e) {
                throw new RuntimeException(e);
            }
            connections.push(ch);
        } else {
            connections.push(new ConnectionHolder(getConnection(), false, readOnly));
        }
    }
    
    /**
     * Used to participate in an existing JDBC connection
     * @param connection existing JDBC connection
     */
    public void attachToExisting(Connection connection) {
        assert connection != null : "Provided JDBC connection should be valid and not null!";
        
        // Mark we are not the owner and that we should not commit on this connection
        connections.push(new ConnectionHolder(connection, false, true));        
    }

    @Override public void readOnlyTransaction() {
        if(connections.size() == 0) {
            beginTransaction(true);
        }
    }

    @Override public void close()
    {
        if(connections.size() == 0) {
            throw new RuntimeException("Calling close() on a non-existing transaction");
        }

        ConnectionHolder holder = connections.pop();
        try {
            if(holder.isOwner()) {
                // restore the autocommit value
                if(isAutoCommit() != null) {
                    holder.getConnection().setAutoCommit(holder.isOriginalAutoCommit());
                }
                holder.getConnection().close();
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override public void commit ()
    {
        try {
            flush();

            if(connections.size() == 0) {
                throw new RuntimeException("Calling commit() on a non-existing transaction");
            }

            if(connections.peek().isOwner()) {
                getConnection().commit();
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    @Override public void rollback ()
    {
        try {
            if(connections.size() == 0) {
                throw new RuntimeException("Calling rollback() on a non-existing transaction");
            }

            if(connections.peek().isOwner()) {
                getConnection().rollback();
            }

        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    private Map<PSKey, PreparedStatement> getSortedMap(Map<PSKey, PreparedStatement> input, boolean reverse) {
        if(input.size() == 0) {
            return input;
        }

        // Get the shape that is responsible for ordering the entities
        // TODO: this needs to be configurable
        Shape shape = ((EntityType)input.keySet().iterator().next().getType()).getShape();
        TypeGraph<State, Edge<State>> defaultOrdering = shape.getOrderedGraph();

        TreeMap<PSKey, PreparedStatement> result = new TreeMap<>(new ObjectGraph.StateComparator(defaultOrdering));
        for(Map.Entry<PSKey, PreparedStatement> entry: input.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        if(reverse) {
            return result.descendingMap();
        }

        return result;
    }

    @Override
    public void flush() {
        try {
            if (!ApplicationConfiguration.config().containsKey(Constants.Config.BATCH_SKIP)
                || !ApplicationConfiguration.config().getBoolean(Constants.Config.BATCH_SKIP)) {

                switch(importMethod) {
                case PREPARED_STATEMENT:
                    if (orderSQL) {
                        this.preparedInsert = getSortedMap(this.preparedInsert, false);
                        this.preparedUpdate = getSortedMap(this.preparedUpdate, false);
                        this.preparedDelete = getSortedMap(this.preparedDelete, true);
                    }

                    for(PreparedStatement ps: preparedInsert.values()) {
                        int[] result = ps.executeBatch();
                        logger.info("Inserting batch with size: " + result.length);
                    }

                    for(PreparedStatement ps: preparedUpdate.values()) {
                        ps.executeBatch();
                    }

                    for(PreparedStatement ps: preparedDelete.values()) {
                        ps.executeBatch();
                    }
                    break;
                case LITERAL_SQL:
                    if(statement != null) {
                        int[] result = statement.executeBatch();

                        if (result.length != literalSQLs.size()) {
                            throw new RuntimeException(
                                "The number of insert SQLs do not match the number received by the database");
                        }

                        StringBuilder failedSqls = new StringBuilder();
                        for (int i = 0; i < result.length; i++) {
                            if (result[i] != 1) {
                                failedSqls.append(literalSQLs.get(i))
                                    .append("\n");
                            }
                        }

                        if (failedSqls.length() > 0) {
                            throw new RuntimeException(
                                "The following SQLs have failed: \n " + failedSqls.toString());
                        }
                    }
                    break;
                case CSV:
                    writeToFile();
                    break;
                }
            } else {
                switch(importMethod) {
                case PREPARED_STATEMENT:
                    throw new RuntimeException("Non batch execution is not supported. The batch.skip setting should be set to false.");
                case LITERAL_SQL:
                    try(Statement stmt = getConnection().createStatement()) {
                        for (String insertSQL : literalSQLs) {
                            logger.debug(insertSQL);
                            stmt.executeUpdate(insertSQL);
                        }
                    }
                    break;
                case CSV:
                    writeToFile();
                    break;
                }
            }
        }
        catch (SQLException e) {
            throw ClassUtil.wrapRun(e);
        } finally {
            try {
                if(this.statement != null) {
                    this.statement.close();
                }
                if(preparedInsert != null) {
                    preparedInsert.clear();
                }
                if(preparedUpdate != null) {
                    preparedUpdate.clear();
                }
                if(preparedDelete != null) {
                    preparedDelete.clear();
                }
                this.clear();
            }
            catch (SQLException e) {
                throw ClassUtil.wrapRun(e);
            }
        }
    }

    private void writeToFile() {
        for(Map.Entry<String, List<String>> entry: sqlByType.entrySet()) {
            BufferedWriter out = null;

            try {
                if (csvWriters.containsKey(entry.getKey())) {
                    out = csvWriters.get(entry.getKey());
                }
                else {
                    out = new BufferedWriter(new FileWriter(
                        ClassUtil.getCSVFilename(entry.getKey()),
                        true));
                    csvWriters.put(entry.getKey(), out);
                }
                for (String sql : entry.getValue()) {
                    out.write(sql);
                    out.newLine();
                }
            } catch (IOException e) {
                throw ClassUtil.wrapRun(e);
            }
        }
    }

    public void closeResources() {
        try {
            for(BufferedWriter writer: csvWriters.values()) {
                writer.close();
            }
            csvWriters.clear();
        }
        catch (IOException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    public void clear() {
        this.idToObjects = new HashMap<>();
        this.snapshots = new HashMap<>();
        this.sqlByType.clear();
        this.literalSQLs.clear();
        if(this.preparedInsert != null) {
            this.preparedInsert = new HashMap<>();
        }
        if(this.preparedUpdate != null) {
            this.preparedUpdate = new HashMap<>();
        }
        if(this.preparedDelete != null) {
            this.preparedDelete = new HashMap<>();
        }
        this.statementCache.clear();
        this.statement = null;
    }

    @Override public boolean readFromDB ()
    {
        return false;
    }
}
