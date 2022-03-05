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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/*
 * If no view/qualifier is specified then we return all the properties and children of the aggregate
 * 
 * We are not dealing with an XML document, so there is no inherent context to work with, i.e, one cannot navigate to a parent from the root etc..
 * Also XML document is a tree and not a graph and we have to deal with references, so we need additional constructs to represent that.
 * 
 * The view also allows qualifiers with special meaning.:
 *   <path><empty>    - If the path represents an entity, then include only initialized properties
 *   <path>.<path>*   - recursive path
 *   <path>.(<path>)* - recursive path with multiple elements in a recursive chain
 *   
 *   List the structure and copy the desired subset into the view.
 *   
 *   
 *   
 *   DEPRECATED below
 *   <path>//         - All descendants
 *   <path1>//<path2> - Any property anchored with <path1> that ends with <path2>
 *   !<path>          - Exclude the particular path pattern from the view. The negative rules have greater precedence.
 *   <path>.∞         - All the properties including descendant objects of the aggregate and other associations
 *                      This can potentially load the complete DB!
 *                
 * Precedence rules:
 * 1. ! - Highest precedence
 * 2. <path> 
 * 3. <path>//<path>
 * 4. //<path><qualifier>
 *                 
 * Examples: 
 *   <path>//      - All descendants of the aggregate anchored at <path>
 *   
 * @author Dilip Dalton
 *
 */

import org.json.JSONObject;

import tools.xor.AggregateAction;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.TypeGraph;

public interface View {

	/**
	 * Checks to see if view references in attribute list have been expanded.
	 *
	 * @return true if view references have been expanded.
	 */
	public boolean isExpanded();

	/**
	 * Retrieve the version of this view.
	 * @return version value
	 */
	public int getVersion();

	/**
	 * Used to explicit specify one or more JOIN expressions in OQL.
	 * This is useful to explicitly model query components that cannot be inferred
	 * from the meta data.
	 *
	 * @return object containing the join conditions
	 */
	public List<Join> getJoin();

	/**
	 * Retrieve the filters defined on this view. Currently used only in queries.
	 * @return list of filters
	 */
	public List<Function> getFunction ();

	/**
	 * Allows a user OQL to be used instead of the system generated OQL query.
	 * @see QueryTransformer
	 *
	 * @return user specified OQL
	 */
	public OQLQuery getUserOQLQuery();

	/**
	 * Set a user specified OQL query. If this is present in the view, it is preferred
	 * over a system generated OQL.
	 *
	 * @param userOQLQuery user specified OQL query
	 */
	public void setUserOQLQuery(OQLQuery userOQLQuery);

	/**
	 * Retrieve the native query which is typically SQL, configured on this view.
	 *
	 * @return native query
	 */
	public NativeQuery getNativeQuery();

	/**
	 * Return the StoredProcedure configured on this view. Multiple stored procedures
	 * can be configured, each for a different action.
	 *
	 * @param action value corresponding to this stored procedure
	 * @return instance containing details on the stored procedure
	 */
	public StoredProcedure getStoredProcedure(final AggregateAction action);

	/**
	 * Returns a list of all the stored procedures configured on this view.
	 * @return list
	 */
	public List<StoredProcedure> getStoredProcedure();

	/**
	 * Configure the stored procedures for this view.
	 * @param storedProcedure instance(s)
	 */
	public void setStoredProcedure(List<StoredProcedure> storedProcedure);

	/**
	 * Retrieve the Shape instance associated with this view
	 * @return shape instance
	 */
	public Shape getShape();

	/**
	 * Associate this view with a particular Shape.
	 *
	 * @param shape instance
	 */
	public void setShape(Shape shape);

	/**
	 * Retrieves a collection of all attributes defined in the view
	 * and all its descendant views
	 * @return attributes
	 */
	public List<String> getConsolidatedAttributes ();

	/**
	 * Retrieves the list of attributes defined in the view.
	 * @return attribute list
	 */
	public List<String> getAttributeList();

	/**
	 * Get the view contents using JSON representation
	 * Every time this method is invoked a copy is created. So this needs to be called
	 * sparingly.
	 *
	 * @return json representation
	 */
	public JSONObject getJson();

	/**
	 * Set the attributes that form the scope of this view.
	 * Additionally a view scope might be extended using the StateGraph in case of
	 * cyclic relationships, @see StateGraph.
	 *
	 * @param attributeList attribute list
	 */
	public void setAttributeList(List<String> attributeList);

	/**
	 * Return the Type name of the entity
	 * @return entity type name
	 */
	public String getTypeName();
	
	/**
	 * Set the type for which the view is based off
	 * @param typeName name of the type
	 */
	public void setTypeName(String typeName);	

	/**
	 * Retrieve the name of the view.
	 * @return view name
	 */
	public String getName();

	/**
	 * Set the name for the view. A view cannot exist without a name.
	 * @param name required
	 */
	public void setName(String name);

	/**
	 * Retrieve the anchor path of the view.
	 * The anchor path is the point in the parent view where this view is anchored
	 * @return anchor path
	 */
	public String getAnchorPath();

	/**
	 * Set the anchor path of the view.
	 * @param path of the anchor
	 */
	public void setAnchorPath(String path);

	/**
	 * Get the QueryTree instance that is specific to a particular EntityType.
	 *
	 * @param type entity type
	 * @return QueryView instance
	 */
	public AggregateTree getAggregateTree (Type type);

	/**
	 * Helps to infer the Entity Type from the view name.
	 * This is possible to do for built-in views.
	 *
	 * @return domain class for the view's EntityType
	 */
	public Class inferDomainClass();

	/**
	 * Return a copy of view.
	 *
	 * @return view copy
	 */
	public View copy();

	/**
	 * Returns the names of all the views that this view references from its attribute list.
	 * Includes attributes from child views.
	 *
	 * @return set of view names
	 */
	public Set<String> getViewReferences();

	/**
	 * Checks to see if the attributes have any view references.
	 * This can return false positives, since attributes can also be specified as
	 * Regular expressions. So check the existence of views with the view reference names,
	 * @see View#getViewReferences
	 *
	 * @return true if there are view references.
	 */
	public boolean hasViewReference();

	/**
	 * Checks if the view contains other direct custom views, i.e., the
	 * attribute starts with a view reference.
	 *
	 * [TODO] There are 1 other aspect we need to consider
	 * If there are other attributes that need to retrieved apart from the view reference
	 * One solution is that we club all these other attributes and add it as a child view
	 *
	 * @return true if composition view
	 */
	boolean isCompositionView ();

	/**
	 * Resolve the view references and also help to identify the attributes that are
	 * defined as regular expressions and those that are not.
	 */
	public void expand();

	/**
	 * Same as expand above but with an argument for cyclic detection.
	 * @param expanding list of views seen so far
	 */
	public void expand(List<String> expanding);

	/**
	 * Returns the attributes in expanded form, i.e., after the view references are
	 * resolved.
	 *
	 * @param input attributes
	 * @param expanding views seen so far to help with cyclic detection
	 * @return expanded attributes
	 */
	public List<String> getExpandedList(List<String> input, List<String> expanding);

	/**
	 * Return the attributes referenced from within functions
	 * @return function attribute paths
	 */
	public Set<String> getFunctionAttributes();

	/**
	 * The set of attributes that are not regular expressions
	 * @return set of plain attributes
	 */
	public Set<String> getExactAttributes ();

	/**
	 * Map of attributes that are specified as Regular expressions, along with
	 * the compiled form specified as the map value.
	 * @return map of regex attributes
	 */
	public Map<String, Pattern> getRegexAttributes();

	/**
	 * Checks to see if path matches any of the regex attributes.
	 * @param path to be validated
	 * @return true if the path matches any of the regular expression attributes.
	 */
	public boolean matches(String path);

	/**
	 * Adds a StateGraph instance for a particular type against this view
	 * @param type for which the StateGraph is configured based off this view
	 * @param value StateGraph instance
	 * @param scope of the state graph for that view
	 */
	public void addTypeGraph (EntityType type, TypeGraph<State, Edge<State>> value, StateGraph.Scope scope);

	/**
	 * Used to check if the view represents a tree or a graph
	 * @param settings object
	 * @return true if tree false otherwise
	 */
	public boolean isTree(Settings settings);

	/**
	 * Cache the generated state graph by EntityType.
	 * Note: We need to make a distinction between Domain and Reference entity types otherwise
	 * we could end up thrashing between building the graph for these two types.
	 *
	 * @param entityType for which the corresponding StateGraph needs to be built
	 * @param scope of the state graph for that view
	 * @return StateGraph instance for the given type for this view
	 */
	public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType, StateGraph.Scope scope);

	/**
	 * By default we return the Type Graph includes all types expanded by subtypes and supertypes.
	 *
	 * @param entityType for which the corresponding StateGraph needs to be built
	 * @return StateGraph instance for the given type for this view
	 */
	public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType);

	/**
	 * Return the list of child aggregate view instances
	 * @return list of children views
	 */
	public  List<? extends View> getChildren();
	
	/**
	 * Checks if all the paths of the view are valid. This requires a type to be specified in the view.
	 * This operation can be expensive depending on the size of the view.
	 * If the type is not present then an IllegalStateException is thrown.
	 * @return true if the view has all valid paths
	 */
	public boolean isValid();

	/**
	 * When processing views with parallel collections, should the strategy by either:
	 * 1. Split to root
	 * 2. Split to anchor
	 *
	 * The default is split to root and this can be changed by setSplitToRoot() method.
	 * @return true if we split to root, false otherwise
	 */
	boolean isSplitToRoot();

	/**
	 * Choose either between the splitToRoot or splitToAnchor strategies for breaking down
	 * the views.
	 * @param value true to use the splitToRoot strategy
	 */
	public void setSplitToRoot(boolean value);

	/**
	 * Denotes if the view is powered by a custom query, such as a Native SQL, custom OQL or
	 * a stored procedure
	 * @return true if it has a custom query
	 */
	public boolean isCustom();

	/**
	 * Identifies the primary key referred by this view
	 * This is useful, if only the primary key information is needed for joining.
	 * @return primary key attribute names
	 */
	List<String> getPrimaryKeyAttribute ();

	/**
	 * Flag to indicate if the query powering this view populates the temp table.
	 * This is usually enabled for a stored procedure that does this for efficiency reasons,
	 * i.e., The XOR framework can avoid populating this table if this flag is true.
	 *
	 * NOTE: If this flag is true then the parallel dispatcher will not work as it cannot
	 *       see the temp table data from a different session
	 *
	 * @return true if the temp table is populated by the stored procedure
	 */
	boolean isTempTablePopulated ();

	/**
	 * Used by a parent AggregateView that uses a stored procedure
	 * If this is true, then the parent query will populate the temp table, before the
	 * child query is executed.
	 *
	 * @param tempTablePopulated flag to indicate if the query/sp for this view populates the temp
	 *                           table
	 */
	void setTempTablePopulated (boolean tempTablePopulated);

	/**
	 * Returns the position of the result for this view. Usually the view is a child view.
	 * Used in conjunction with a stored procedure that returns multiple results.
	 * The position starts from 0
	 *
	 * @return position of result set corresponding to this view
	 */
	Integer getResultPosition();

	/**
	 * Set the fields that form the scope of this view. Used to implement GraphQL functionality.
	 *
	 * @return the fields of the view
	 */
	default List<Field> getFields() { return null; }

	/**
	 * Set the fields of the view
	 * @param value list of fields
	 */
	default void setFields(List<Field> value) {}
}
