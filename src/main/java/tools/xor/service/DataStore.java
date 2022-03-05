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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.operation.MigrateOperation;
import tools.xor.util.IntraQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;
import tools.xor.view.QueryTreeInvocation;

/**
 * Interface XOR uses to interact with persistent storage such as an RDBMS
 * @author i844711
 *
 */
public interface DataStore {

    public enum QueryType {
        OQL,
        SQL,
        SP,
		SP_MULTI
    }
	
	/**
	 * Find the entity by its primary key
     * @param persistentClass Java class of entity we want to find
     * @param id of the entity
     * @return Persistence managed object
     */
	public Object findById(Class<?> persistentClass, Object id);

    /**
     * Find the entity by its primary key
     * @param type of the entity
     * @param id of the entity
     * @return Persistence managed object
     */
    public Object findById(Type type, Object id);

    /**
     * Batch fetch a list of ids from the database.
     * Main use case is to initialize a collection of object references.
     * @see tools.xor.ExtendedProperty#isCollectionOfReferences()
     *
     * @param entityType Type of entities we want to find/load
     * @param ids of the entities
     * @return list of persistence managed objects
     */
    public List<Object> findByIds(EntityType entityType, final Collection ids);
	
	/**
	 * Find the entity by one or more property values
	 * @param type of the entity we want to find
	 * @param propertyValues by which we need to restrict
	 * @return Persistence managed object
	 */
	public Object findByProperty(Type type, Map<String, Object> propertyValues);		

	/**
	 * Return the persisted collection elements in a collection (Set) object
	 * @param type of the entity we want to find
	 * @param collectionOwnerKey The owner id of the collection
	 * @return collection object containing zero or more elements
	 */
	public Object getCollection(Type type, Map<String, Object> collectionOwnerKey);	
	
    /**
     * Save the entity in the persistence store
     * @param entity to save
     */
    public void saveOrUpdate(Object entity);
    
    /**
     * Delete the entity from the persistence store
     * @param entity to delete
     */
    public void delete(Object entity);
    
    /**
     * Flush any buffered content to the persistence store
     */
    public void flush();
    
    /**
     * This disables auto-flush necessary by the persistence layer
     * to run queries with the latest data.
     * Since we manage the latest data and queries are only used
     * to reference objects we can afford to skip this as we manage the
     * references to newly created objects.
     * 
     * @return old flush mode
     * 
     */
	public Object disableAutoFlush();
	
	/**
	 * Set the flush mode
	 * @param flushMode to set
	 */
	public void setFlushMode(Object flushMode);
    
    /**
     * Clears any objects held in the persistence cache
     */
    public void clear();

    /**
     * Clear a specific set of objects containing the following ids.
     * Might not be supported by all PersistenceOrchestrators
     *
     * @param businessObjects that need to be cleared/evicted
     */
    public void clear(Set<Object> businessObjects);
    
    /**
     * Refreshes the object held in the persistence cache
     * @param object Persistence managed object
     */
    public void refresh(Object object);    
    
    /**
     * Indicates if this persistence orchestrator automatically tracks version 
     * If not, the framework takes care of updating the version in order to support optimistic concurrency control
     * @return true if version tracking is supported
     */
    public boolean supportsVersionTracking();
    
    /**
     * Returns the mechanisms supported for querying
     * @return QueryCapability object
     */
    public QueryCapability getQueryCapability();
    
    /**
     * Some ORMs cannot resolve all the issues while persisting an aggregate. 
     * For e.g., cycle dependencies within an aggregate is a problem while persisting it.
     * 
     * This is usually not a problem with NoSQL databases.
     * Setting this flag to false, allows the framework to individually persist
     * objects within the aggregate in based on topological ordering of the required fields.
     * @return true if can process aggregate
     */
    public boolean canProcessAggregate();
    
	/**
	 * Check if there is an existing persistent instance with the identifier 
	 * specified in object
	 * 
     * @param callInfo object
     * @param typeMapper instance to infer the domain type
     * @return A persistent object or null 
	 */
	public Object getPersistentObject(CallInfo callInfo, TypeMapper typeMapper);
	
	/**
	 * If the object is present in the local cache (session) return it 
	 * else return null
	 * 
	 * @param persistentClass of the entity we want to get
	 * @param id of the entity
	 * @return object if present in cache or null 
	 */
	public Object getCached(Class<?> persistentClass, Object id);
	
	/**
	 * Return the query object specific to the persistence mechanism
	 * @param queryString typically the SQL string
	 * @param queryType the type of query
	 * @param queryInput representing the type of Query object
	 * @return Query object
	 */
	public Query getQuery(String queryString, QueryType queryType, Object queryInput);

    /**
     * Create the provider specific query object if the query entails deferred construction
     * @param query for which the provider specific query object needs to be created
     * @param queryType the type of query
     * @param qti query tree invocation details
     */
    public void evaluateDeferred(Query query, QueryType queryType, QueryTreeInvocation qti);
	
	/**
	 * If the persistence orchestrator can dynamically track a user object (i.e, track changes),
     * then this operation will allow the user to do just that.
     *
	 * This is in contrast to merge where only a managed object can be managed and
	 * not a user created object
	 * For this to work, the view needs to support dynamic update.
     *
     * This is an update optimization to avoid a select of the whole entity from the database
     * for an update of only a few fields and not the whole entity.
	 *
     * @param input entity
     * @param snapshot object, if non-versioned optimisted locking is being used, then
     *                 the fields in the object will be used for optimistic modification check
	 * @param settings containing the entityType of the object to be created and
     *                 attached to the Persistence layer, so it becomes managed
     *
     * @return the newly created object that was attached to the session
     */
	public Object attach(BusinessObject input, BusinessObject snapshot, Settings settings);
	
	/**
	 * Checks to see if stored procedure support 
	 * This includes OUT parameters support. TODO: relax this requirement in the future.
	 * @return true if stored procedures are supported
	 */
	public boolean supportsStoredProcedure();

	/**
	 * Find the object on the other side of the relationship involving an open property
	 * @param source object on one side of the relationship
	 * @param openPropertyName name of the open property
	 * @return persistence managed object
	 */
	public Object getTargetObject(BusinessObject source, String openPropertyName);

    /**
     * Create a Blob object using the underlying JDBC provider
     * @return Blob instance
     */
    public Blob createBlob();

    /**
     * Uses a cursor to execute the query
     *
     * @param source can be AML or JDBC AggregateManager. Should be able to get a Connection.
     * @param target AML configured AggregateManager
     * @param settings containing migrate view details
     * @return results scroll object
     */
    EntityScroll getEntityScroll(AggregateManager source, AggregateManager target, Settings settings);

    /**
     * Used to migrate database entities from the source to target database.
     * The source can be a JDBC provider, but the target needs to be an ORM provider.
     *
     * @param source database containing the data to be migrated
     * @param target database where the data needs to be populated
     * @param queueSize used to configure the buffering needed for the migration
     * @return MigrateOperation specific to the target ORM provider
     */
    MigrateOperation getMigrateOperation(AggregateManager source, AggregateManager target, Integer queueSize);

    /**
     * Retrieve provider specific OQL join fragment
     * @param queryTree to which this edge belongs
     * @param joinEdge representing the associated property being retrieved
     * @return join fragment
     */
    String getOQLJoinFragment(QueryTree queryTree, IntraQuery<QueryFragment> joinEdge);

    /**
     * Provider specific construct to control polymorphic object retrieval
     * @param type whose retrieval of subclasses we would like to control
     * @return provider specific polymorphic controls for a class
     */
    String getPolymorphicClause(Type type);

    /**
     * Invoked as part of the migrate operation, to persist the surrogate key id mapping between
     * the source and the migrated instance.
     * This mapping is later used to fix the foreign key relationships between 2 entities, especially
     * when the target of the foreign key does not have a natural key.
     *
     * @param surrogateKeyMap map of the surrogate key values between the source and migrated instances
     *                        in a migration batch
     */
    void persistSurrogateMap(Map<String, String> surrogateKeyMap);

    /**
     * Using the migrated Id map information on the target database, the map of the
     * surrogateKey values is obtained, for the ids scanned from the batch.
     *
     * @param batch is scanned for surrogateKey ids and the map resulting from this is used to fix
     *              the relationships before it is processed and saved in the target database.
     * @param settings has details on the meta data
     */
    void fixRelationships(List<JSONObject> batch, Settings settings);

    /**
     * Query the target database in a migration and find the migrated surrogate id values.
     *
     * @param sourceSurrogateIds source surrogate ids for which we want to find the
     *                           corresponding migrated surrogate ids
     * @return map of source and migrated surrogate ids
     */
    Map<String, String> findMigratedSurrogateIds(Set<String> sourceSurrogateIds);

    /**
     * We need an active JDBC connection for queries. But we do not need any
     * transaction overhead
     */
    void initForQuery();

    /**
     * populate the global temporary table - this data is later used by child queries or
     * stored procedures.
     *
     * @param invocationId unique id for the parent query invocation
     * @param ids of all parent objects
     */
    void populateQueryJoinTable (String invocationId, Set ids);

	/**
	 * Create the query join table as a temporary table if not already created
	 * @param stringKeyLen the desired size of a string primary key.
	 *                     If null, then a default size of 36 is used.
	 */
	void createQueryJoinTable(Integer stringKeyLen);
	
	/**
	 * Does this class interact with the persistence store and/or is managed by it
	 * @param clazz java class name
	 * @return true/false
	 */
	boolean isManaged(Class<?> clazz);
}
