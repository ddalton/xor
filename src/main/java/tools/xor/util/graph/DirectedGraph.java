package tools.xor.util.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import tools.xor.util.Vertex;

public interface DirectedGraph<V, E> {
	
	/**
	 * Get a list of the nodes of the graph
	 * @return
	 */
	public Collection<V> getVertices();
	
	/**
	 * Get the id of a vertex
	 */
	public int getId(V vertex);
	
	/**
	 * Gets the vertex by id
	 */
	public V getVertex(int id);
	
	/**
	 * Remove the incident edges of a vertex with 
	 * the given id. The vertex itself is not removed
	 */
	public void unlinkVertex(int id);
	
	/**
	 * unlinks the edge from the graph
	 * The link can be restored by calling restore
	 * @param edge
	 */
	public void unlinkEdge(E edge);
	
	/**
	 * Link the unlink edge back to the graph
	 * @param edge
	 */
	public void linkEdge(E edge);
	
	/**
	 * Restores all the unlinked edges
	 */
	public void restore();
	
	/**
	 * Get all the edges in the graph
	 * @return
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
	 * @param edge
	 */
	public void removeEdge(E edge);
	
	/**
	 * Add a vertex to the graph
	 * @param vertex
	 */
	public void addVertex(V vertex);
	
	/**
	 * Returns true if the graph contains the given vertex
	 * @param vertex
	 * @return
	 */
	public boolean containsVertex(V vertex);
	
	/**
	 * Get the start vertex of the edge
	 * @param edge
	 * @return
	 */
	public V getStart(E edge);
	
	/**
	 * Get the end vertex of the edge
	 * @param edge
	 * @return
	 */
	public V getEnd(E edge);
	
	/**
	 * Get the Incoming edges of a vertex
	 * @param vertex
	 * @return
	 */
	public Collection<E> getInEdges(V vertex);
	
	/**
	 * Get the outgoing edges of a vertex
	 * @param vertex
	 * @return
	 */
	public Collection<E> getOutEdges(V vertex);
	
	/**
	 * Generates an SCC
	 * @return
	 */
	public Stack<Set<V>> getSCC();

	/**
	 * Gets a list of all the circuits in the graph
	 * @deprecated  Replaced by {@link #getLoops()}
	 * @return
	 */
	@Deprecated 
	public List<List<Vertex>> getCircuits();
	
	/**
	 * Gets a list of all the loops in the graph
	 * @return
	 */
	public List<List<E>> getLoops();	

	/**
	 * Reverse the start and end vertices to change the 
	 * direction of the edge
	 * @param edge
	 */
	public void reverseEdge(E edge);
	
	/**
	 * Get a topoologically sorted list of the DAG
	 * A RuntimeException will be thrown if the graph is not a DAG
	 * @return
	 */
	public List<V> toposort();

	/**
	 * Remove all self loops
	 */
	public void unlinkSelfLoops();

	/**
	 * check if a graph is cyclic
	 * @return
	 */
	public boolean isCyclic();

}