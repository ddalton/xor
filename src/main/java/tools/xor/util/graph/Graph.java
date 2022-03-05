package tools.xor.util.graph;

import java.util.Collection;

public interface Graph<V, E> {
	
	/**
	 * Get a list of the nodes of the graph
	 * @return collection of vertices
	 */
	public Collection<V> getVertices();
	
	/**
	 * Get the id of a vertex
	 * @param vertex object
	 * @return id value
	 */
	public int getId(V vertex);
	
	/**
	 * Gets the vertex by id
	 * @param id value
	 * @return vertex
	 */
	public V getVertex(int id);
	
	/**
	 * Get all the edges in the graph
	 * @return collection of edges
	 */
	public Collection<E> getEdges();
	
	/**
	 * Add an edge to the graph and maintain the order
	 * of addition
	 * 
	 * @param edge  object representing a directed edge
	 * @param start the start object of the edge
	 * @param end   the end object of the edge
	 */
	public void addEdge(E edge, V start, V end);
	
	/**
	 * Remove an edge from the graph
	 * @param edge object
	 */
	public void removeEdge(E edge);
	
	/**
	 * Add a vertex to the graph
	 * @param vertex to add
	 */
	public void addVertex(V vertex);

	/**
	 * Remove a vertex from the graph
	 * @param vertex to remove
	 */
	void removeVertex(V vertex);

	/**
	 * Return the roots in the graph, i.e., all the trees of a forest.
	 * @return roots of the forest
	 */
	Collection<V> getRoots();
}
