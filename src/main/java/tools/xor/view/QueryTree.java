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

package tools.xor.view;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ReconstituteRecordVisitor;
import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.ClassUtil;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.util.State;
import tools.xor.util.Vertex;
import tools.xor.util.graph.TreeOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a portion of the user's request that can be satisfied by a single query.
 */
public class QueryTree<V extends QueryFragment, E extends IntraQuery<V>> extends TreeOperations<V, E>
	implements Vertex
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private static final int START_POS = 0;

	private Type      aggregateType;
	private String    name;
	private List<QueryField> fields = new LinkedList<>();
	private Map<String, QueryField> attributeToFieldMap = new HashMap<>();
	private Query     query;          // Query representing this QueryTree
	private String    selectString;
	private View      view; // view associated with this QueryTree, needed for functions

	public QueryTree (EntityType rootType, View view) {
		this.aggregateType = rootType;
		this.view = view;
	}

	public View getView() {
		return this.view;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public List<String> getSelectedColumns() {
		List<String> result = new LinkedList<>();
		for(QueryField field: fields) {
			result.add(field.getFullPath());
		}

		return result;
	}

	public Query getQuery() {
		return this.query;
	}

	public void setSelectString (String selectString) {
		this.selectString = selectString;
	}

	public String getSelectString () {
		return this.selectString;
	}
	
	public Object getQueryValue(Object[] queryResultRow, String path) {
		QueryField field = attributeToFieldMap.get(path);
		if(field != null) {
			return queryResultRow[field.getPosition()];
		} else {
			int position = this.query.getColumnPosition(path);
			if(position == -1) {
				return null;
			}
			return queryResultRow[position];
		}
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
				type = entity.getObjectCreator().getDAS().getShape().getType(entityName);
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

	public Map<String, Object> resolveField(BusinessObject root, Object[] queryResultRow, Map<String, Object> previousResult, QueryTreeInvocation queryInvocation) throws Exception {
		Map<String, Object> propertyResult = new HashMap<>();
		Set<String> propertyPaths = new HashSet<>();

		// system generated query
		if(this.fields.size() > 0) {
			for (QueryField field : this.fields) {
				propertyResult.put(field.getFullPath(), queryResultRow[field.getPosition()]);
			}

			for (String propertyPath : propertyResult.keySet()) {
				QueryField field = attributeToFieldMap.get(propertyPath);
				if (field.isAugmenter()) {
					continue;
				}

				propertyPaths.add(propertyPath);
			}
		} // user specified query
		else if(getQuery().getColumns().size() > 0) {
			List<String> columns = getQuery().getColumns();
			for(int i = 0; i < columns.size(); i++) {
				propertyResult.put(columns.get(i), queryResultRow[i]);
			}

			for(String path: columns) {
				if (!QueryFragment.systemFields.contains(Settings.getBaseName(path))) {
					propertyPaths.add(path);
				}
			}
		}

		// Identify which objects have changed
		Set<String> changed = new HashSet<>();
		if(previousResult != null) {
			for(Map.Entry<String, Object> entry: previousResult.entrySet()) {
				// Meta fields (list index etc) should be skipped
				//if(!propertyPaths.contains(entry.getKey())) {
				//	continue;
				//}
				Object currentValue = propertyResult.get(entry.getKey());
				Object previousValue = entry.getValue();

				if(currentValue == previousValue) continue;

				if(currentValue == null ^ previousValue == null) {
					changed.add(entry.getKey());
					continue;
				}

				if(currentValue != null && currentValue.equals(previousValue)) continue;

				changed.add(entry.getKey());
			}
		} else {
			changed = propertyPaths;
		}

		// We are probably adding duplicate entries in a collection
		// to enable this select an additional column that distinguishes this duplicate value
		// for e.g., a column representing a list index
		if(changed.size() == 0) {
			logger.error("Duplicate record identified, please enhance the view to distinguish this duplicate record, ");
		}

		// We find the longest common prefix of all the changed paths
		// and update all the properties rooted at the least common prefix
		// We need to do this since we need to initialize all those fields even if they
		// are not considered to be changed by checking the previous row.
		String lcp = getLCP(new LinkedList<>(changed));
		changed = new HashSet<>();
		for(String propertyPath: propertyPaths) {
			if(propertyPath.startsWith(lcp)) {
				changed.add(propertyPath);
			}
		}

		ReconstituteRecordVisitor visitor = new ReconstituteRecordVisitor();
		for(String propertyPath: changed) {
			// Set the value and create any intermediate objects if necessary
			root.reconstitute(propertyPath, propertyResult, this, visitor);

			// Notify the queryTreeInvocation visitor
			queryInvocation.visit(propertyPath, propertyResult.get(propertyPath));
		}
		lcp = getDeepestCollection(lcp);
		visitor.process(lcp);

		return propertyResult;
	}

	private String getDeepestCollection(String path) {
		if(StringUtils.isEmpty(path)) {
			return path;
		}

		FragmentAnchor fragmentAnchor = findFragment(path);
		if(fragmentAnchor != null) {
			QueryFragment fragment = fragmentAnchor.fragment;
			Collection<E> inEdges = getInEdges((V)fragment);
			if (inEdges.size() == 1) {
				E edge = inEdges.iterator().next();
				while (edge != null && !edge.getProperty().isMany()) {
					path = Settings.getAnchorName(path);
					inEdges = getInEdges(edge.getStart());
					if (inEdges.size() == 1) {
						edge = inEdges.iterator().next();
					}
					else {
						edge = null;
					}
				}
			}
		}

		return path;
	}

	private String getLCP(List<String> changed) {
		if(changed.size() == 1) {
			return Settings.getAnchorName(changed.get(0));
		} else {
			String result = LCP.findLCP(changed);
			if(result.endsWith(Settings.PATH_DELIMITER)) {
				result = result.substring(0, result.length()-Settings.PATH_DELIMITER.length());
			}
			return result;
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
	 * Copy the root fragment and prepare it to become a root in a new QueryTree
	 * @return new QueryFragment copy
	 */
	public QueryFragment copyRoot(AggregateTree aggregateTree) {
		QueryFragment root = getRoot();
		QueryFragment copy = new QueryFragment(root.getEntityType(), aggregateTree.nextAlias(), null);
		copy.paths = new ArrayList<>(root.paths);
		copy.simpleCollectionPaths = new ArrayList<>(root.simpleCollectionPaths);

		return copy;
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
	 * 1. The root type of the QueryTree should match
	 * 2. The fields of the QueryTree should be a subset of the fields retrieved
	 *    by the custom query
	 * 3. The parameters of the custom query should be a subset of the parameters
	 *    provided by the user. The parameters are found by name matching.
	 * 4. Custom query resolution should be initiated by the user
	 *
	 * If not, the generated OQL query fields is used.
	 *
	 * @param settings to decide if a custom query should be used
	 * @param aggregateTree to get the view containing the custom query
	 */
	public void generateFields(Settings settings, AggregateTree aggregateTree) {
		clearFields();

		// We can have the fields in any position, but we need to know what the position is
		int position = START_POS;
		for(QueryFragment fragment: getVertices()) {
			position = fragment.generateFields(position, settings, this, aggregateTree);
		}

		// We now collect the fields and sort them
		for(QueryFragment fragment: getVertices()) {
			fields.addAll(fragment.getQueryFields());
		}

		if(!getView().hasUserQuery()) {
			Collections.sort(fields);
			generateIdFields(aggregateTree);
		}

		for(QueryField field: this.fields) {
			attributeToFieldMap.put(field.getFullPath(), field);
		}

		// We need to reposition based on the list order
		if(getView().hasUserQuery()) {
			List<QueryField> fieldOrder = new LinkedList<>();
			addQueryField(fieldOrder, getView().getAttributeList());
			QuerySupport qs = getQuerySupport();
			addQueryField(fieldOrder, qs.getAugmenter());
		}
	}

	private void addQueryField(List<QueryField> fieldOrder, List<String> paths) {
		if(paths != null) {
			for (String path : paths) {
				if (attributeToFieldMap.containsKey(path)) {
					fieldOrder.add(attributeToFieldMap.get(path));
				}
				else {
					throw new RuntimeException("Unable to find QueryField with path: " + path);
				}
			}
		}
	}

	private void generateIdFields(AggregateTree aggregateTree) {
		// We generate the id fields only if this entity is selected
		if(fields.size() == 0) {
			return;
		}

		// Generate the id fields if it does not exist for each outgoing edge
		for(Object edge: aggregateTree.getOutEdges(this)) {
			InterQuery outgoing = (InterQuery) edge;

			// check that the source fragment of the edge has the id field
			QueryFragment source = outgoing.getSource();
			// find the position at which to add this field
			int position = (fields.size() > 0) ? fields.get(fields.size()-1).getPosition()+1 : START_POS;
			QueryField newField = source.checkAndAddId(position);

			if(newField != null) {
				fields.add(newField);
			}
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
	 * Make the absolute path relative to this QueryTree.
	 *
	 * @param path absolute path of the AggregateTree
	 * @return path relative to the QueryTree
	 */
	private String makeRelative(String path) {
		if(path == null || getRoot() == null) {
			return null;
		}
		String anchorPath = StringUtils.isEmpty(getRoot().getAncestorPath()) ? "" : (getRoot().getAncestorPath() + Settings.PATH_DELIMITER);

		// we first strip out the ancestor path from the root QueryTree
		return path.startsWith(anchorPath) ? path.substring(anchorPath.length()) : path;
	}

	public boolean isFragment(String path) {
		FragmentAnchor anchor = findFragment(path);
		return anchor.path == null;
	}

	/**
	 * Find the closest fragment at which this path is anchored
	 * @param path for which we want to find the fragment
	 * @return fragment
	 */
	public FragmentAnchor findFragment(String path) {
		return findFragment(getRoot(), makeRelative(path));
	}

	public static class FragmentAnchor {
		public QueryFragment fragment;
		public String path;

		public FragmentAnchor(QueryFragment fragment, String path) {
			this.fragment = fragment;
			this.path = path;
		}

		public boolean isValidPath() {
			if(path == null) {
				return false;
			}
			String property = State.getNextAttr(path);
			Property p = fragment.getEntityType().getProperty(property);

			return p != null && !p.isMany();
		}

		public String getOQLName() {
			return fragment.getAlias() + Settings.PATH_DELIMITER + path;
		}

		public boolean isFragment() {
			return path == null;
		}
	}

	private FragmentAnchor findFragment(V vertex, String path) {

		if(path == null) {
			return new FragmentAnchor(vertex, path);
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
			if(vertex.containsPath(path)) {
				return new FragmentAnchor(vertex, path);
			} else {
				return null;
			}
		} else {
			return findFragment(nextFragment, State.getRemaining(path));
		}
	}

	public QueryField findField(String path) {
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
	 * If the path is applicable to this QueryTree, then we return the OQL name (aliased name)
	 *
	 * @param path to resolve to the OQL name
	 * @param po persistence orchestrator
	 * @return OQL name
	 */
	public String getOQLName(String path, PersistenceOrchestrator po) {
		if(getRoot().getAncestorPath() == null || path.startsWith(getRoot().getAncestorPath())) {
			QueryField field = findField(path);
			if(field == null) {
				FragmentAnchor anchor = findFragment(path);
				if(anchor == null) {
					return null;
				}
				if(anchor.isValidPath()) {
					return anchor.getOQLName();
				}
				if(anchor.isFragment()) {
					return anchor.fragment.getAlias();
				}
			} else {
				return field.getOQL(po.getQueryCapability());
			}
		}

		return null;
	}

	public List<QueryField> getFields() {
		return this.fields;
	}

	protected Query prepare(CallInfo callInfo, ObjectResolver resolver)
	{
		if(query != null) {
			resolver.preProcess(this, callInfo.getSettings());

		}

		return query;
	}

	/**
	 * Check if this QueryTree is part of the path and is not the root anchor
	 * @param path in the QueryTree
	 * @return tree if the QueryTree is part of the path
	 */
	public boolean isPartOf(String path) {
		QueryFragment root = getRoot();

		return root.getAncestorPath() != null && path.startsWith(root.getAncestorPath());
	}

	public Property getProperty(String path)
	{
		QueryTree.FragmentAnchor fragmentAnchor = findFragment(path);
		if (fragmentAnchor != null && fragmentAnchor.fragment != null) {
			// Get the property from the incoming edge
			Iterator<E> iter = getInEdges((V)fragmentAnchor.fragment).iterator();

			if (iter.hasNext()) {
				E incomingEdge = iter.next();
				return incomingEdge.getProperty();
			}
		}

		return null;
	}

	public QuerySupport getQuerySupport() {
		QuerySupport qs = getView().getStoredProcedure(AggregateAction.READ);
		if(qs == null) {
			qs = getView().getNativeQuery();
			if(qs == null) {
				qs = getView().getUserOQLQuery();
			}
		}

		return qs;
	}
}

