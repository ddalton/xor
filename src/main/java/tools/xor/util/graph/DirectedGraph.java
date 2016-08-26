package tools.xor.util.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import tools.xor.util.Vertex;

public interface DirectedGraph<V, E> {
	
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
	 * Remove the incident edges of a vertex with 
	 * the given id. The vertex itself is not removed
	 * @param id of vertex
	 */
	public void unlinkVertex(int id);
	
	/**
	 * unlinks the edge from the graph
	 * The link can be restored by calling restore
	 * @param edge object
	 */
	public void unlinkEdge(E edge);
	
	/**
	 * Link the unlink edge back to the graph
	 * @param edge object
	 */
	public void linkEdge(E edge);
	
	/**
	 * Restores all the unlinked edges
	 */
	public void restore();
	
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
	 * Returns true if the graph contains the given vertex
	 * @param vertex to check
	 * @return true if the graph contains the vertex
	 */
	public boolean containsVertex(V vertex);
	
	/**
	 * Get the start vertex of the edge
	 * @param edge object
	 * @return start vertex of the edge
	 */
	public V getStart(E edge);
	
	/**
	 * Get the end vertex of the edge
	 * @param edge object
	 * @return end vertex of the edge
	 */
	public V getEnd(E edge);
	
	/**
	 * Get the Incoming edges of a vertex
	 * @param vertex object
	 * @return collection of incoming edges
	 */
	public Collection<E> getInEdges(V vertex);
	
	/**
	 * Get the outgoing edges of a vertex
	 * @param vertex object
	 * @return collection of outgoing edges
	 */
	public Collection<E> getOutEdges(V vertex);
	
	/**
	 * Generates an SCC
	 * @return SCC
	 */
	public Stack<Set<V>> getSCC();

	/**
	 * Gets a list of all the circuits in the graph
	 * @deprecated  Replaced by {@link #getLoops()}
	 * @return list of circuits
	 */
	@Deprecated 
	public List<List<Vertex>> getCircuits();
	
	/**
	 * Gets a list of all the loops in the graph
	 * @return list of loops
	 */
	public List<List<E>> getLoops();	

	/**
	 * Reverse the start and end vertices to change the 
	 * direction of the edge
	 * @param edge object
	 */
	public void reverseEdge(E edge);
	
	/**
	 * Get a topoologically sorted list of the DAG
	 * A RuntimeException will be thrown if the graph is not a DAG
	 * @return list of vertices in topological order
	 */
	public List<V> toposort();

	/**
	 * Remove all self loops
	 */
	public void unlinkSelfLoops();

	/**
	 * check if a graph is cyclic
	 * @return true if the graph has a cycle
	 */
	public boolean isCyclic();

}