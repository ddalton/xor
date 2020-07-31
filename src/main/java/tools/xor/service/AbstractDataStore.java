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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.AbstractBO;
import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.operation.MigrateOperation;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.ClassUtil;
import tools.xor.util.IntraQuery;
import tools.xor.util.ObjectCreator;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.Query;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryJoinAction;
import tools.xor.view.QueryTree;
import tools.xor.view.QueryTreeInvocation;
import tools.xor.view.StoredProcedure;
import tools.xor.view.View;

public abstract class AbstractDataStore implements DataStore {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	// Valid if the surrogate id is a global id
	// if not, the type also needs to be saved
	private static final String INSERT_SURROGATE_MAP_SQL = "INSERT INTO XORSURROGATEMAP"
		+ "(SOURCE_ID, MIGRATED_ID) VALUES (?,?)";

	public static final String QUERYJOIN_ID_INT_COL = "ID_INT";
	public static final String QUERYJOIN_ID_STR_COL = "ID_STR";
	public static final String QUERYJOIN_INVOC_COL = "INVOCATION_ID";
	private static final String INSERT_QUERY_JOIN_SQL = "INSERT INTO %s (%s, %s, %s) VALUES (?,?,?)";

	private static final String QUERY_MIGRATED_IDS = "SELECT SOURCE_ID, MIGRATED_ID FROM XORSURROGATEMAP WHERE SOURCE_ID IN (%s)";

	protected abstract void createStatement (StoredProcedure sp);
	
	@Override
	public boolean supportsVersionTracking() {
		return true;
	}
	
	@Override
	public boolean canProcessAggregate() {
		return false;
	}
	
	@Override 
	public void clear() {
		// Overridden by subclasses
	}

	@Override
	public void clear(Set<Object> ids) {
		// Overridden by subclasses
	}
	
	@Override 
	public void refresh(Object object) {
		// Overridden by subclasses
	}	

	private Object getByUserKey(CallInfo callInfo) {
		EntityType entityType = (EntityType)((BusinessObject) callInfo.getInput()).getPropertyType();
		BusinessObject from = (BusinessObject) callInfo.getInput();

		EntityType type = (EntityType)((BusinessObject) callInfo.getInput()).getType();
		do {
			Map<String, Object> param = new HashMap<String, Object>();
			for(String key: type.getExpandedNaturalKey()) {
				if(from.get(key) == null)
					continue;
				
				param.put(key, from.get(key) );
			}

			type = type.getParentType();
			if(param.size() == 0) {
				continue;
			}

			Object result = findByProperty(from.getDomainType(), param);
			if(result != null) {
				return result;
			}
		} // don't go above entityType
		while(type != null && !type.getName().equals(entityType.getName()));

		return null;
	}

	/**
	 * Give a chance to shortcircuit the persistence loading if we know for sure that the
	 * given object is a transient object.
	 * For example, we know that the identifier was generated when the object was created etc.
	 *
	 * @param from the user given object
	 * @return true if the given object is a transient object
	 */
	protected boolean isTransient(BusinessObject from) {
		return false;
	}
	
	@Override
	public Object getPersistentObject(CallInfo callInfo, TypeMapper typeMapper) {
		if(callInfo.isBulkInput()) {
			return null;
		}

		BusinessObject from = (BusinessObject) callInfo.getInput();
		Object persistentObject = null;

		if(!EntityType.class.isAssignableFrom(from.getType().getClass()))
			return null;

		EntityType type = (EntityType) from.getType();

		if(type.isEmbedded()) // We don't separately load embedded values from the database
			return null;

		if(isTransient(from)) {
			return null;
		}

		if(!(callInfo.getSettings().getAction() == AggregateAction.CLONE) )
			persistentObject = getByUserKey(callInfo);

		if(persistentObject == null) {
			ExtendedProperty identifierProperty = (ExtendedProperty) type.getIdentifierProperty();
			if(identifierProperty == null) {
				logger.error("Type without identifier: " + type.getName());
			} else {
				Serializable id = (Serializable)identifierProperty.getValue(from);
				if (id != null && !"".equals(id)) {
					String typeName = typeMapper.toDomain(
						type.isDomainType() ?
							type.getName() :
							typeMapper.getDomainShape().getType(type.getEntityName()).getInstanceClass().getName(), from);
					Type domainType = typeMapper.getDomainShape().getType(typeName);
					if(domainType != null) {
					    persistentObject = findById(domainType, id);
					}
				}
			}
		}

		return persistentObject;
	}
	
	@Override
	public Object getTargetObject(BusinessObject source, String openPropertyName) {
		ExtendedProperty property = (ExtendedProperty) source.getType().getProperty(openPropertyName);
		if(property.getRelationshipType() == RelationshipType.TO_ONE) {
			Map<String, String> keyFields = property.getKeyFields();
			
			if(keyFields.size() == 1) {
				Map.Entry<String, String> entry = keyFields.entrySet().iterator().next();
				if( ((EntityType)property.getType()).getIdentifierProperty().getName().equals(entry.getValue()) ) {
					// The value in the source object referenced by the key property 
					// is the id value of the target object
					return findById(property.getType(), source.get(entry.getKey()));
				}
			} else {
				// For all other cases we query the DB
				Map<String, Object> params = new HashMap<String, Object>();
				for(Map.Entry<String, String> entry: keyFields.entrySet()) {
					params.put(entry.getValue(), source.get(entry.getKey()));
				}
				return findByProperty(property.getType(), params);
			}
		}		
		
		// TODO: Handle TO_MANY
		
		return null;
	}

	@Override
	public Object getCached(Class<?> persistentClass, Object id) {
		return null;
	}	
	

	protected void performAttach(BusinessObject input, Object instance) {
		throw new UnsupportedOperationException("The reattach operation is either not supported or not yet implemented");
	}

	@Override
	public Object attach (BusinessObject input, BusinessObject snapshot, Settings settings)
	{
		Object instance = null;

		View view = settings.getView();
		EntityType type = (EntityType)settings.getEntityType();
		if (view.getTypeGraph(type).supportsDynamicUpdate()) {

			EntityType entityType = (EntityType)settings.getEntityType();
			ObjectCreator oc = input.getObjectCreator();
			try {
				instance = AbstractBO.createInstance(
					input.getObjectCreator(),
					input.getIdentifierValue(),
					null,
					entityType,
					true,
					null);
				if(entityType.getVersionProperty() != null ) {
					((ExtendedProperty)entityType.getVersionProperty()).setValue(oc.getSettings(), instance, input.getVersionValue());
				} else {
					ClassUtil.initSingleLevel(input, snapshot, settings);
				}

				// At this point it is implementation specific, and it overridden by subclass implementations
				performAttach(input, instance);
			}
			catch (Exception e) {
				throw ClassUtil.wrapRun(e);
			}

		}
		else {
			throw new UnsupportedOperationException(
				"The entity type " + settings.getEntityType().getName()
					+ " does not support dynamic update for the view " + view.getName());
		}

		return instance;
	}
	
	@Override
	public boolean supportsStoredProcedure() {
		return false;
	}

	@Override
	public EntityScroll getEntityScroll(AggregateManager source, AggregateManager target, Settings settings)
	{
		throw new UnsupportedOperationException("EntityScroll is not supported");
	}

	@Override
	public MigrateOperation getMigrateOperation(AggregateManager source, AggregateManager target, Integer queueSize)
	{
		return new MigrateOperation(source, target, queueSize);
	}

	@Override
	public String getOQLJoinFragment(QueryTree queryTree, IntraQuery<QueryFragment> joinEdge) {
		// If this is an inheritance edge, then we need to do a downcast to the
		// actual type.
		String joinClause = null;
		if(joinEdge.getProperty() == null) {
			joinClause = getQueryCapability().getDowncastClause(queryTree, joinEdge);
		} else {
			joinClause = joinEdge.getNormalizedName() + " AS ";
		}

		return " LEFT OUTER JOIN " + joinClause + joinEdge.getEnd().getAlias();
	}

	@Override
	public String getPolymorphicClause(Type type) {
		return "";
	}

	protected void persistMapToTable(Connection conn, Map<String, String> surrogateKeyMap) throws
		SQLException
	{
		try(PreparedStatement ps = conn.prepareStatement(INSERT_SURROGATE_MAP_SQL)) {

			for (Map.Entry<String, String> entry : surrogateKeyMap.entrySet()) {
				ps.setString(1, entry.getKey());
				ps.setString(2, entry.getValue());
				ps.addBatch();
			}

			ps.executeBatch();
			conn.commit();
		}
	}

	protected void saveQueryJoinTable (Connection conn, String invocationId, Set ids) throws
		SQLException
	{
		String sql = String.format(INSERT_QUERY_JOIN_SQL,
			QueryJoinAction.JOIN_TABLE_NAME,
			QUERYJOIN_ID_INT_COL,
			QUERYJOIN_ID_STR_COL,
			QUERYJOIN_INVOC_COL);

		if(ids.size() == 0) {
			return;
		}

		// Get the type of the id object
		boolean isStringType = false;
		Object firstId = ids.iterator().next();
		if(firstId instanceof String) {
			isStringType = true;
		}

		try(PreparedStatement ps = conn.prepareStatement(sql)) {

			for (Object id: ids) {
				if(!isStringType) {
					Long longValue = new Long(id.toString());
					ps.setLong(1, longValue);
					ps.setString(2, null);
				} else {
					ps.setLong(1, 0);
					ps.setString(2, (String)id);
				}
				ps.setString(3, invocationId);
				ps.addBatch();
			}

			ps.executeBatch();
		}
	}

	protected Map<String, String> queryMigratedIds(Connection conn, Set<String> sourceIds) throws SQLException
	{
		StringBuilder idStr = new StringBuilder();

		for(String id: sourceIds) {
			if(idStr.length() > 0) {
				idStr.append(", ");
			}
			idStr.append("'").append(id).append("'");
		}

		String sql = String.format(QUERY_MIGRATED_IDS, idStr.toString());
		Map<String, String> result = new HashMap<>();

		if(sourceIds.size() > 0) {
			try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
				while(rs.next()) {
					result.put(rs.getString(1), rs.getString(2));
				}
			}
		}

		return result;
	}

	@Override
	public void persistSurrogateMap(Map<String, String> surrogateKeyMap) {
		throw new UnsupportedOperationException("Persisting of the surrogate map is not implemented");
	}

	@Override
	public Map<String, String> findMigratedSurrogateIds(Set<String> sourceSurrogateIds) {
		throw new UnsupportedOperationException("findMigratedSurrogateIds needs to have a provider implementation");
	}

	protected Set<String> getSurrogateKeyPaths(Set<String> migratePropertyPaths, EntityType entityType) {
		throw new UnsupportedOperationException("Persistence provider needs to implement the method getSurrogateKeyPaths");
	}

	@Override
	/*
	 * This implementation is simple because we currently only handle surrogate ids that are global. i.e., unique
	 * for the whole schema.
	 */
	public void fixRelationships(List<JSONObject> batch, Settings settings) {
		/*
	     *  We do this by
		 *  1. Get the list of properties
		 *  2. Retrieve the value of these properties and build a map
		 *  3. Query the database to get the corresponding migrated values
		 *  4. Using the list of properties in step 1, set the new values to form the correct
		 *    relationships
		 */

		// Step 1 - Get the list of surrogateKey properties
		// This needs to be build using the specific provider
		Set<String> surrogateKeyPaths = getSurrogateKeyPaths(
			AggregatePropertyPaths.enumerateMigrate(
				settings.getEntityType()), (EntityType) settings.getEntityType());

		// Step 2 - Build the map
		Set<String> sourceSurrogateIds = new HashSet<>();
		for(JSONObject json: batch) {
			for (String surrogateKeyPath : surrogateKeyPaths) {
				Object sourceSurrogateId = StateGraph.getKeyValue(json, surrogateKeyPath);
				sourceSurrogateIds.add(sourceSurrogateId.toString());
			}
		}

		// Step 3 - Query the database and get the migratedIds given the sourceIds
		Map<String, String> idMap = findMigratedSurrogateIds(sourceSurrogateIds);

		// Step 4 - Fix the relationships using the map
		for(JSONObject json: batch) {
			for (String surrogateKeyPath : surrogateKeyPaths) {
				Object sourceSurrogateId = StateGraph.getKeyValue(json, surrogateKeyPath);
				StateGraph.setKeyValue(json, surrogateKeyPath, idMap.get(sourceSurrogateId));
			}
		}
	}

	@Override
	public void evaluateDeferred(Query query, QueryType queryType, QueryTreeInvocation qti) {
		// Any subclass specific logic is overridden
		
		qti.initInList(query);
	}

	@Override
	public void initForQuery() {}
	
    public boolean isManaged(Class<?> clazz) {
        return false;
    }
}
