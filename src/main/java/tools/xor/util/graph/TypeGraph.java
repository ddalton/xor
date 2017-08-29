package tools.xor.util.graph;

import org.json.JSONObject;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.view.QueryView;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TypeGraph<V extends State, E extends Edge<V>> extends Graph<V, E> {

	/**
	 * Retrieves the vertex corresponding to the EntityType around which this StateGraph is based.
	 * @return vertex of the EntityType corresponding to this StateGraph
	 */
	public V getRootState();

	/**
	 * Add a new edge to the graph
	 * @param edge object
	 * @param start vertex
	 * @param end vertex
	 */
	public void addEdge(E edge, V start, V end);

	/**
	 * Remove an edge from the graph
	 * @param edge to remove
	 */
	public void removeEdge(E edge);

	/**
	 * Get the directed edge whose start state is vertex and with the given name
	 * @param vertex start state
	 * @param name of the edge
	 * @return the edge if present, or null
	 */
	public E getOutEdge(V vertex, String name);

	/**
	 * Add a new vertex to the graph
	 * @param vertex to add
	 */
	public void addVertex(V vertex);

	/**
	 * Add a new edge
	 * @param edge to add
	 */
	public void addEdge(E edge);

	/**
	 * Retrieve the vertex by a given Type
	 * @param type type
	 * @return vertex
	 */
	public V getVertex(Type type);

	/**
	 * Initialize all the vertices with the simple attributes
	 * @return the current graph
	 */
	public TypeGraph<V, E> getFullStateGraph();

	/**
	 * Make a copy of the graph
	 * @return copy
	 */
	public StateGraph<V, E> copy();

	/**
	 *
	 * @param mergeStates map of states to be merged. Needs to be the original states map and not a copy, since it is needed to properly remove duplicates
	 * @return merged graph
	 */
	public StateGraph<V, E> copy(Map<Type, V> mergeStates);

	/**
	 * This determines the next valid set of properties to process given the current state
	 * at the location specified by propertyPath.
	 * Also takes into account the exact set of properties defined on the view.
	 *
	 * @param state of the current step in the Type graph
	 * @param propertyPath traversed so far
	 * @param exactSet of properties defined by the view
	 * @return the set of properties that need to be processed next
	 */
	public List<Property> next(State state, String propertyPath, Set<String> exactSet);

	/**
	 * Get the set of child attributes anchored at a given type. Not all TypeGraph support
	 * this operation.
	 *
	 * @param type EntityType
	 * @param propertyPath current location of the program execution
	 * @param exactSet of properties in the view
	 * @return list of child attributes
	 */
	public List<Property> next(Type type, String propertyPath, Set<String> exactSet);

	/**
	 * Print both the Vertex and Edge information of the StateGraph
	 * @return a string representation of the contents
	 */
	public String dumpState();


	
	/*
		To support Excel export/import. We need to create non-duplicated data by creating appropriate queries.
		    The approach is to add a method to the state graph that will pull all the toMany and cascaded loop
		    relationships.
		    The goal is to find the disjoint regions. A disjoint region is rooted at a toMany relationship
		    There is one toMany relationship in a disjoint region.
		    The next step is to find the owner of the toMany collection. With this information a query containing the data of the owner id and the disjoint region is created.
		    A single such query will map to a single disjoint region.
		
		    The API of the state graph will be:
		    QueryView getQueryableRegions()
		
		    QueryView 
		      String name  // becomes the fieldName for getting the query from parent DisjointRegion
		      DisjointRegion parent
		      List<String> fields
		
		      getSubQuery(String fieldName)
		
		    Example fields
		    ==============
		    owner.id
		    owner.child.id
		    owner.child.name
		
		    Query 
		    =====
		    SELECT owner.id, owner.child.id, owner.child.name ... From ownerType owner
		     WHERE owner.id IN (SELECT id ... <From parent disjoint region if any> | <id> )
		
		    The subquery is extracted from the parent DisjointRegion provided the field
		
		    Example:
		    [Export]
		      Select owner.id, child.field1, child.field2 from Owner owner inner join owner.child child where
		        owner.id in (Select ...)
		    [Import]
		      Here based on FIFO order we build the root object first and add the remaining objects
		
		    The QueryView will contain a list of HQL/SQLs related to the number of queries.
		
		
		Algorithm
		=========
		1. Start from the root node and go through each field. Collect all simple fields and toOne cascaded relationships
		
		2. If the field is a toMany cascaded relationship then create a new QueryView and save it (BFS)
		
		3. Once all the fields are processed in step 1. Then create the query string in this form 
		   SELECT [LIST] FROM typeName. The [LIST] will be substituted based on usage. i.e., fields if
		   selecting the data, name if being used as a subquery.
		
		4. Go through each saved QueryView in step 2 and initialize it with the subquery obtained from
		   the parent queryView. Do the same with this queryView starting from Step 1.
		   
		UPDATE
		======
		A simpler algorithm is now proposed:
		Each sheet in the Excel will represent a query region, roughly mapping to a database table.
		A sheet will contain data that is shareable.
		1. Each attribute (even one-to-one associations) is in a different sheet, as long as it is shareable
		2. Cascaded loops should be saved 
		3. Reference (Foreign key) to other entities should not be by Id, but by Business key 
			(Json object, if the business key is composite). This helps with migration from one system, to 
			another system where the ids of the same object can be different.
		4. For attributes participating in cascade loops. The query might not be simple and cannot be deduced
			automatically. For these queries, manual support is needed and the method should generate a skeleton view (XML),
			which the user needs to fill with the appropriate query for the entity and attribute appropriately.
		5. Each query representing a sheet should contain the parent information.
		6. Subquery chaining is used to get the necessary rows if the model does not provide ancestor information. i.e.,
		   if we consider A->B->C and the entity is rooted in A, then inorder to find all C rows, we create a query like
		   SELECT c.* FROM C c WHERE c.parentId IN (SELECT b.id FROM B b WHERE b.parentId IN (SELECT a.id FROM A a))
		   Alternatively, if ancestor information was provided we could do something like:
		   SELECT c.* FROM C c WHERE c.ancestorPath LIKE '?%', and the parameter is bound to A.id
		7. In any case with whatever approach is taken (subquery chaining or ancestor path), we will create an 
			XML document that contains all the QueryViews for each attribute comprising an entity.
			The default query approach will be subquery chaining, starting with the root entity id.
		8. QueryViews involving cascading loop attributes will not be populated and the user is expected to provide
		 	the actual SQL query for performance reasons. Only the fields that need to be returned will be provided in the view
		 	and the SQL needs to map to these fields.
		9. Query chaining could be very expensive for graphs with long cascade attribute chains. In these cases, it is better to 
			augment the model with the ancestor information and a warning will be generated to this effect if the chain gets
			longer than a set configurable value (e.g., 10).
		10. Entity QueryViews are different from the hand crafted query views, since they are generated by the software. So the
			the queries might be inefficient and there will be lots of opportunities to tune them.
		11. A placeholder will be put in the generated query if it is auto-generated. For example,
			SELECT c.* FROM C c WHERE c.parentId IN (SELECT p.id FROM [%<parentAttributePath>.QUERY%])
		12. The XML generation algorithm is based on BFS.
		    a) Staring with the root node we traverse all cascade attributes
		    b) Mark the node as visited
		    c) For each child cascade attribute create a QueryView with the name as the attribute path from the root
		    d) Create the query based on the parent node
		    e) If a node is part of a cascade loop, then mark this in the view, so the user can fill in the appropriate query
		    f) Once the XML file is generated and the queries populated, it is flagged as READY
		    g) The export can then be processed from least dependent to most dependent. As the objects are being written out,
		       a map between the id and business key (by type) is maintained, this helps in writing out the business key values.
		       Similarly during import, a map between business key and id (on the target system) will be maintained. This
		       will help to hydrate the object before it is used for update/create.
		
		NOTE: Ideally the best performance is got by storing the ancestor information, so it is easy to query the needed
		rows efficiently. 
   
	 * @return the root QueryView
	 */
	public List<QueryView> getQueryableRegions();

	/**
	 * Checks to types to see if all them support dynamic update. Even if a single concrete type
	 * in the graph does not support dynamic update, it returns false.
	 *
	 * @return true if all types support dynamic update
	 */
	public boolean supportsDynamicUpdate();
	
	/**
	 * Generates a random object graph using JSON objects.
	 *
	 * @param settings used to control the size of the generated object graph
	 * @return the generated object graph
	 */
	public JSONObject generateObjectGraph (Settings settings);

	/**
	 * Generate a PNG image file of the graph
	 * @param settings containing the file name
	 */
	public void generateVisual (Settings settings);
}
