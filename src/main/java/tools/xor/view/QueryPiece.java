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

package tools.xor.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.AbstractBO;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.QueryCapability;
import tools.xor.util.ClassUtil;
import tools.xor.util.IntraQuery;
import tools.xor.util.State;
import tools.xor.util.Vertex;
import tools.xor.util.graph.Tree;
import tools.xor.view.QueryTree.QueryKey;

/**
 * Represents a portion of the user's request that can be satisfied by a single query.
 */
public class QueryPiece<V extends QueryFragment, E extends IntraQuery<V>> extends Tree<V, E>
	implements Vertex
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private Type      aggregateType;
	private String    name;
	private Query     query; // Query representing this QueryPiece
	private String    queryString;
	private List<QueryField> fields = new LinkedList<>();
	private Map<String, QueryField> attributeToFieldMap = new HashMap<>();

	public QueryPiece(EntityType rootType) {
		this.aggregateType = rootType;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public Query getQuery() {
		return this.query;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getQueryString() {
		return this.queryString;
	}

	
	public Object getQueryValue(Object[] queryResultRow, String path) {
		QueryField field = attributeToFieldMap.get(path);
		if(field != null)
			return queryResultRow[field.getPosition()];
		else
			return null;
	}	

	public BusinessObject getRootObject (Object obj, BusinessObject entity) throws Exception {
		BusinessObject result = null;

		if(ClassUtil.getDimensionCount(obj) == 1) {
			Object[] queryRow = (Object[])obj;

			Object idValue = null;
			if(((EntityType)this.aggregateType).getIdentifierProperty() != null) {
				String idPropertyName = ((EntityType)this.aggregateType).getIdentifierProperty().getName();
				idValue = getQueryValue(queryRow, idPropertyName);
			}

			String entityName = (String) getQueryValue(queryRow, QueryFragment.ENTITY_TYPE_ATTRIBUTE);
			Type type = entity.getType();
			if(entityName != null) {
				// This is padded with space based on the largest type name if a CASE statement is used
				entityName = entityName.trim();
				type = entity.getObjectCreator().getDAS().getType(entityName);
			}

			// find and create data object
			if(idValue != null) {
				result = entity.getBySurrogateKey(idValue, this.aggregateType);
			}
			if(result == null) {
				if(logger.isDebugEnabled()) {
					logger.debug("Creating instance with id: " + idValue + " and type: " + entity.getType().getName() + ", entityName: |" + entityName + "|");
				}
				result = entity.createDataObject(idValue, type);
			}
		}

		return result;
	}

	public void resolveField(BusinessObject root, Object[] queryResultRow) throws Exception {
		Map<String, Object> propertyResult = new HashMap<String, Object>();
		for(QueryField field: this.fields) {
			propertyResult.put(field.getFullPath(), queryResultRow[field.getPosition()]);
		}

		for(String propertyPath: propertyResult.keySet()) {
			QueryField field = attributeToFieldMap.get(propertyPath);
			if(field.isAugmenter()) {
				continue;
			}

			// Set the value and create any intermediate objects if necessary
			root.set(propertyPath, propertyResult, (EntityType) this.aggregateType);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Type getAggregateType() {
		return aggregateType;
	}

	protected String getLabel (V vertex)
	{
		StringBuilder content = new StringBuilder(vertex.toString() + "\\n");
		for (String path : vertex.getPaths()) {
			content.append(Settings.getBaseName(path) + "\\l");
		}
		for (String path : vertex.getSimpleCollectionPaths()) {
			content.append(Settings.getBaseName(path) + "\\l");
		}
		return content.toString();
	}

	public void computeCollectionCount (V node)
	{
		int maxParallelCount = 0;
		int maxSimpleCount = 0;
		for(V fragment: getChildren(node)) {
			computeCollectionCount(fragment);

			// Get the simple count
			maxSimpleCount += fragment.getSimpleCollectionCount();

			// Get the indirect collection count
			// First look at the incoming edge to the child fragment
			IntraQuery incoming = getInEdges(fragment).iterator().next();
			int fragmentParallel = 0;
			if(incoming.getProperty().isMany()) {
				fragmentParallel = 1;
			}
			if(fragment.getParallelCollectionCount() > fragmentParallel) {
				fragmentParallel = fragment.getParallelCollectionCount();
			}
			// update the count for this child fragment
			maxParallelCount += fragmentParallel;
		}

		node.setParallelCollectionCount(maxParallelCount);

		maxSimpleCount += node.getSimpleCollectionPaths().size();
		node.setSimpleCollectionCount(maxSimpleCount);
	}

	/**
	 * Copy the root fragment and prepare it to become a root in a new QueryPiece
	 * @return new QueryFragment copy
	 */
	public QueryFragment copyRoot(QueryTree queryTree) {
		QueryFragment root = getRoot();
		return new QueryFragment(root.getEntityType(), queryTree.nextAlias(), null);
	}

	private void clearFields() {
		this.fields = new LinkedList<>();
		this.attributeToFieldMap = new HashMap<>();
	}

	/**
	 * First generate the QueryFields for a fragment
	 * NOTE: If there are custom OQL, native SQL or stored procedure queries
	 * then the position will need to be updated based on the position of the
	 * corresponding fields in the custom queries
	 *
	 * Custom queries are specified on the view. The following conditions have
	 * to be met to use a custom query.
	 * 1. The root type of the QueryPiece should match
	 * 2. The fields of the QueryPiece should be a subset of the fields retrieved
	 *    by the custom query
	 * 3. The parameters of the custom query should be a subset of the parameters
	 *    provided by the user. The parameters are found by name matching.
	 * 4. Custom query resolution should be initiated by the user
	 *
	 * If not, the generated OQL query fields is used.
	 *
	 * @param settings to decide if a custom query should be used
	 * @param queryTree to get the view containing the custom query
	 */
	public void generateFields(Settings settings, QueryTree queryTree) {
		clearFields();

		// We can have the fields in any position, but we need to know what the position is
		int position = 0;
		for(QueryFragment fragment: getVertices()) {
			position = fragment.generateFields(position, settings, this, queryTree);
		}

		// We now collect the fields and sort them
		for(QueryFragment fragment: getVertices()) {
			fields.addAll(fragment.getQueryFields());
		}
		Collections.sort(fields);

		for(QueryField field: this.fields) {
			attributeToFieldMap.put(field.getFullPath(), field);
		}
	}

	public List<E> getOpenContentJoins() {
		List<E> result = new LinkedList<>();

		for(E joinEdge: getEdges()) {
			if(joinEdge.getProperty().isOpenContent()) {
				result.add(joinEdge);
			}
		}

		return result;
	}

	/**
	 * Make the absolute path relative to this QueryPiece.
	 *
	 * @param path absolute path of the QueryTree
	 * @return path relative to the QueryPiece
	 */
	private String makeRelative(String path) {
		String anchorPath = (getRoot().getAncestorPath() == null) ? "" : (getRoot().getAncestorPath() + Settings.PATH_DELIMITER);

		// we first strip out the ancestor path from the root QueryPiece
		return path.startsWith(anchorPath) ? path.substring(anchorPath.length()) : path;
	}

	public boolean isFragment(String path) {
		QueryFragment fragment = findFragment(path);
		return (fragment == null || fragment.getAncestorPath() == null) ? false : fragment.getAncestorPath().equals(path);
	}

	public QueryFragment findFragment(String path) {
		return findFragment(getRoot(), makeRelative(path));
	}

	private QueryFragment findFragment(V vertex, String path) {

		if(path == null) {
			return vertex;
		}

		String next = State.getNextAttr(path);
		V nextFragment = null;
		for(E e: getOutEdges(vertex)) {
			if(e.getName().equals(next)) {
				nextFragment = e.getEnd();
				break;
			}
		}

		if(nextFragment == null) {
			return null;
		} else {
			return findFragment(nextFragment, State.getRemaining(path));
		}
	}

	private QueryField findField(String path) {
		return findField(getRoot(), makeRelative(path));
	}

	private QueryField findField(V vertex, String path) {

		if(path == null) {
			return null;
		}

		String next = State.getNextAttr(path);
		V nextFragment = null;
		for(E e: getOutEdges(vertex)) {
			if(e.getName().equals(next)) {
				nextFragment = e.getEnd();
				break;
			}
		}

		if(nextFragment == null) {
			return vertex.getField(path);
		} else {
			return findField(nextFragment, State.getRemaining(path));
		}
	}

	/**
	 * If the path is applicable to this QueryPiece, then we return the OQL name (aliased name)
	 *
	 * @param path to resolve to the OQL name
	 * @return OQL name
	 */
	public String getOQLName(String path) {
		if(getRoot().getAncestorPath() == null || path.startsWith(getRoot().getAncestorPath())) {
			QueryField field = findField(path);
			if(field == null) {
				if(isFragment(path)) {
					return findFragment(path).getAlias();
				}
			} else {
				return field.getOQL(null);
			}
		}

		return null;
	}

	public List<QueryField> getFields() {
		return this.fields;
	}

	public List<String> getFieldNames() {
		List<String> result = new ArrayList<>(this.fields.size());

		for(QueryField qf: this.fields) {
			result.add(qf.getPath());
		}

		return result;
	}

	protected Query prepare(CallInfo callInfo, ObjectResolver resolver)
	{
		Map<String, Object> params = new HashMap<String, Object>();
		if(callInfo.getSettings().getParams() != null) {
			params.putAll(callInfo.getSettings().getParams());
		}

		resolver.preProcess(this, callInfo.getSettings(), this.query, params);

		return query;
	}
}

