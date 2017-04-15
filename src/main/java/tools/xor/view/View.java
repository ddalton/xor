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

import tools.xor.AggregateAction;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.TypeGraph;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public interface View {

	/**
	 * Checks to see if view references in attribute list have been expanded.
	 *
	 * @return true if view references have been expanded.
	 */
	public boolean isExpanded();

	/**
	 * Checks if the child views contained by this view form a UNION relationship as
	 * defined in set theory.
	 *
	 * @return true if the child views need to be combined using the UNION operator
	 */
	public boolean isUnion();

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
	public Join getJoin();

	/**
	 * Used to model user specified values to bind to the query
	 * @return list of parameters defined for this view to be used in the query
	 */
	public List<Parameter> getParameter();

	/**
	 * Retrieve the filters defined on this view. Currently used only in queries.
	 * @return list of filters
	 */
	public List<Filter> getFilter();

	/**
	 * Allows a user OQL to be used instead of the system generated OQL query.
	 * @see tools.xor.view.QueryBuilder
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
	 * Retrieves the list of attributes defined in the view.
	 * @return attribute list
	 */
	public List<String> getAttributeList();

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
	 * Get the QueryView instance that is specific to a particular EntityType.
	 *
	 * @param type entity type
	 * @param narrow true, if the query result returns type information for the entity and
	 *               we want the objects that are created to be based off this type.
	 * @return QueryView instance
	 */
	public QueryView getEntityView(Type type, boolean narrow);

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
	 * Resolve the view references and also help to identify the attributes that are
	 * defined as regular expressions and those that are not.
	 */
	public void expand();

	/**
	 * Returns the attributes in expanded form, i.e., after the view references are
	 * resolved.
	 *
	 * @param input attributes
	 * @return expanded attributes
	 */
	public List<String> getExpandedList(List<String> input);

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
	 */
	public void addTypeGraph (EntityType type, TypeGraph<State, Edge<State>> value);

	/**
	 * Cache the generated state graph by EntityType.
	 * Note: We need to make a distinction between Domain and Reference entity types otherwise
	 * we could end up thrashing between building the graph for these two types.
	 *
	 * @param entityType for which the corresponding StateGraph needs to be built
	 * @return StateGraph instance for the given type for this view
	 */
	public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType);

}
