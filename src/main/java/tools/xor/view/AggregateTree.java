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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.InterQuery;
import tools.xor.util.graph.TreeOperations;

/**
 * This is an optimization data structure used by queries.
 * It contains additional properties and a root of type QueryType that is not
 * present in the meta data (Shape object), but specific to this AggregateTree.
 * The additional properties may or may not be part of the meta data. So these properties
 * do not have a domain counterpart.
 *
 * This data structure is used for query processing.
 * Suitable for simple requests that do not involve hierarchical structures a.k.a recursion.
 *
 * The mapping is typically:
 * AggregateView - AggregateTree
 * 
 * A AggregateTree is a graph that typically represents a tree data structure, where the nodes are QueryTree instances.
 * The TreeTraversal algorithm uses this data structure to execute the queries.
 * The AggregateTree represents two types of partitioning of the queries
 * 1. Leaf group
 *    The queries are all in one level, i.e., the first level children
 *    They are split by the leaf attributes, grouping them by what can efficiently be done in a single query
 * 2. State Tree
 *    Queries can be spread through multiple levels and represent the State Tree.
 *    There are 3 ways a State Tree is executed:
 *    a) Using sub-queries
 *       In this approach the ancestors of the current node form a nested sub-query.
 *       The disadvantage of this approach is that if the request is deeply nested, then the query can become complex.
 *       The advantage is that intermediate results do not have to be materialized on the client.
 *    b) Using IN clause
 *       In this approach the parent node has the results materialized and the left query runs this result in an IN clause.
 *       The disadvantage of this approach is that special care needs to be taken if the list is sufficiently long
 *       The advantage is that the queries are much simpler and run faster.
 *    c) Hybrid approach
 *       We can allow the user to specify the mechanism to use on each node to tailor the performance.
 *
 * The AggregateTree execution can be done in either of the following modes:
 * 1. SERIAL
 *    All queries are executed by the same thread and depending on the ORM, that could be the same
 *    JDBC connection
 * 2. PARALLEL
 *    The queries are sent to a "Query Pool", that is responsible for executing each query in parallel
 *    There should be sufficient context to take the results and construct the portion of the object
 *    it is responsible for.
 *    The thread from the Query Pool can be used to do this construction or can hand it off to the
 *    calling thread. The recommended approach is to hand it off so the Query Pool solely focuses
 *    on executing queries
 *    
 * In Dynamic Query Object Reconstitution (DQOR) - where QueryType is not based on an existing type, but specified
 *   [D] Dynamic        - The entity type is specified dynamically in the aggregate view
 *   [Q] Query          - The action is applicable to querying data
 *   [O] Object         - The result is a object
 *   [R] Reconstitution - The act of converting from a query result (set of records) to one or more object  
 *   
 * AggregateTree can represent an acyclic graph where there are multiple references to the same view.
 *
 * @author Dilip Dalton
 *
 */

public class AggregateTree<V extends QueryTree, E extends InterQuery<V>> extends TreeOperations<V, E>
{
	
	//private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger logger = LogManager.getLogger(Constants.Log.VIEW_BRANCH);

	public static final String ENTITY_ALIAS_PREFIX = "_XOR_";
	public static final String PROPERTY_ALIAS_PREFIX = "PROP";

	private ObjectResolver.Type type = ObjectResolver.Type.SHARED;
	private int aliasCounter;
	private View view; // for custom queries

	public AggregateTree (View view) {
		this.view = view;
	}

	public View getView() {
		return this.view;
	}

	public ObjectResolver.Type getType ()
	{
		return type;
	}

	public void setType (ObjectResolver.Type type)
	{
		this.type = type;
	}

	public AggregateTree<V, E> copy() {
		AggregateTree<V, E> result = new AggregateTree(this.view);

		Map<V, V> oldNew = new HashMap<>();
		for(V queryTree: getVertices()) {
			V queryTreeCopy = (V)queryTree.copy();
			oldNew.put(queryTree, queryTreeCopy);

			result.addVertex(queryTreeCopy);
		}

		Map<E, E> edgeMap = new HashMap<>();
		for(E edge: getEdges()) {
			V startCopy = oldNew.get(edge.getStart());
			V endCopy = oldNew.get(edge.getEnd());
			QueryFragment sourceCopy = startCopy.findFragment(edge.getSource().getAncestorPath()).fragment;
			QueryFragment targetCopy = endCopy.findFragment(edge.getTarget().getAncestorPath()).fragment;

			E edgeCopy = (E)new InterQuery(edge.getName(), startCopy, endCopy, sourceCopy, targetCopy);
			result.addEdge(edgeCopy, startCopy, endCopy);

			edgeMap.put(edge, edgeCopy);
		}

		for(V queryTree: getVertices()) {
			queryTree.postCopy(edgeMap, oldNew.get(queryTree));
		}

		return result;
	}

	/**
	 * We do a BFS and if a particular vertex's view is a custom query, then we skip it and all its
	 * children.
	 *
	 * @return all non-custom query vertices
	 */
	public List<V> getNonCustomVertices() {
		List<V> result = new ArrayList<>();

		Queue<V> vertices = new LinkedList<>();
		vertices.addAll(getRoots());
		while(!vertices.isEmpty()) {
			V queryTree = vertices.remove();
			if(!queryTree.getView().isCustom()) {
				result.add(queryTree);
				vertices.addAll(getChildren(queryTree));
			}
		}

		return result;
	}

	public List<AggregateView> extractViews(AggregateManager am) {
		List<AggregateView> result = new LinkedList<AggregateView>();

		V root = getRoot();
		for (E edge : getOutEdges((V) root)) {
			QueryTree qv = edge.getEnd();
			AggregateView av = new AggregateView(qv);
			av.setSystemOQLQuery((new OQLQuery()).generateQuery(am, this, qv));
			result.add(new AggregateView(qv));
		}

		return result;
	}

	public final static class QueryKey {
		final Type type;
		final String viewName;

		public QueryKey(Type type, String viewName) {
			this.type = type;
			this.viewName = viewName;
		}

		@Override
		public boolean equals(Object object) {
			if(!QueryKey.class.isAssignableFrom(object.getClass()))
				return false;

			QueryKey otherKey = (QueryKey) object;

			if(viewName.equals(otherKey.viewName) && 
					this.type == otherKey.type) {
				return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			int result = 17;
			result = 37 * result + viewName.hashCode();
			result = 37 * result + this.type.hashCode();
			return result;
		}
	}

	public static String getNext(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(propertyPath.indexOf(Settings.PATH_DELIMITER)+1);
		else
			return null;
	}	

	public static String getTopAttribute(String propertyPath) {
		if(propertyPath == null || "".equals(propertyPath))
			return null;

		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1) {
			return propertyPath.substring(0, propertyPath.indexOf(Settings.PATH_DELIMITER));
		} else
			return propertyPath;
	}

	public String nextAlias() {
		return generateAlias(aliasCounter++);
	}

	public static String generateAlias(int counter) {
		return AggregateTree.ENTITY_ALIAS_PREFIX + counter;
	}

	@Override
	protected void writeGraphvizDotHeader(BufferedWriter writer) throws IOException
	{
		super.writeGraphvizDotHeader(writer);
		writer.write("  style=filled;\n");
		writer.write("  node[style=filled,color=white];\n");
	}

	@Override
	public void writeGraphvizDot(BufferedWriter writer) throws IOException
	{
		// Write the content of each QueryTree as a cluster
		for(V qp: toposort(null)) {
			writer.write("  subgraph cluster" + getId(qp) + " {\n");
			qp.writeGraphvizDot(writer);
			writer.write("  }\n\n");
		}

		// Write the edges
		// Do not constrain all the types that have been explicitly expanded
		for(E edge: getEdges()) {

			StringBuilder result = new StringBuilder("  " + edge.getSource() + " -> " + edge.getTarget());

			String label = edge.getDisplayName();
			if(DFAtoNFA.UNLABELLED.equals(label)) {
				result.append("[dir=back, arrowtail=empty,penwidth=3,color=\"#8B4513\"]\n");
			} else {
				result.append("[label=").append(edge.getDisplayName()).append(",penwidth=3,color=\"#8B4513\"]\n");
			}

			writer.write(result.toString());
		}
	}

	public Property getProperty(String path) {
		return getProperty(getRoot(), path);
	}

	private Property getProperty(V queryTree, String path) {

		Collection<E> edges = getOutEdges(queryTree);
		for(E edge: edges) {
			if(edge.getEnd().isPartOf(path)) {
				return getProperty(edge.getEnd(), path);
			}
		}

		return queryTree.getProperty(path);
	}
}
