package tools.xor.util.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import tools.xor.AssociationSetting;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.Edge;
import tools.xor.util.State;

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
	 * Get the outgoing edges of a vertex
	 * @param vertex object
	 * @return collection of outgoing edges
	 */
	public Collection<E> getOutEdges(V vertex);

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
	 * Get a vertex by name.
	 * This is useful if a graph from another shape is needed for topological sorting.
	 *
	 * @param name  of the vertex
	 * @return vertex
	 */
	public V getVertex(String name);

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

	/**
	 * This method is to prune the state graph of the given associations.
	 *
	 * @param associations to be deleted
	 */
	void prune (List<AssociationSetting> associations);

	/**
	 * Mark the types that need to be handled as references
	 * @param references list of types
	 */
	void markReferences (List<String> references);

	/**
	 * This method is to enhance the state graph since the states are reused across other state graph entities.
	 * We cannot just rebuild a part of the state graph with new state graph if we don't account for the sharing.
	 *
	 * @param associations new properties e.g., open properties being added to the state graph
	 */
	void enhance(List<AssociationSetting> associations);

	/**
	 * Extend the scope of the graph by adding the property mentioned by the path attribute.
	 * The path anchor is allowed to be controlled using the anchor state attribute.
	 * Additionally any attributes that need initialization for an entity can be specified using
	 * the initialize attribute.
	 *
	 * @param path that is added to the existing graph
	 * @param anchor on which the path is based. Usually it is the root state.
	 * @param initialize Applicable if the path represents an entity
	 */
	void extend(String path, V anchor, boolean initialize);

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

	/**
	 * Checks if the path is present in the graph. Useful for validating a view.
	 * @param path of edges to check in the graph 
	 * @return true if the path is found in the graph, false otherwise
	 */
	public boolean hasPath(String path);

	/**
	 * Export the graph in DOT format
	 * @param filename of the DOT file
	 */
	public void exportToDOT(String filename);
}
