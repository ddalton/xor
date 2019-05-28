package tools.xor.util.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import tools.xor.service.Shape;
import tools.xor.util.Vertex;

public interface DirectedGraph<V, E> extends Graph<V, E> {

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
	 * direction of the edge.
	 *
	 * @param edge object
	 */
	public void reverseEdge(E edge);

	/**
	 * This is dependent upon the implementation since the edge object
	 * needs to be replaced with another edge object.
	 *
	 * @param edge object
	 * @return the reversed edge. This is not the same instance as the given edge.
	 */
	public E getReversedEdge(E edge);
	
	/**
	 * Get a topoologically sorted list of the DAG. Reverse sorted by their importance, i.e.,
	 * nodes with lower number are more dependant on nodes with a higher number.
	 *
	 * A RuntimeException will be thrown if the graph is not a DAG
	 * @param shape of the type
	 * @return list of vertices in topological order
	 */
	public List<V> toposort(Shape shape);

	/**
	 * Remove all loops to self and also to super-types
	 */
	public void unlinkSelfLoops();

	/**
	 * check if a graph is cyclic
	 * @return true if the graph has a cycle
	 */
	public boolean isCyclic();

	/**
	 * Exports the graph to a file in GML format
	 * @param filename containing the graph in .gml format
	 */
	public void exportToGML(String filename);

	/**
	 * Exports the graph in DOT format
	 * @param filename containing the graph in .dot format
	 */
	public void exportToDOT(String filename);

	/**
	 * Gets a graph data structure to be used in rendering a PNG image.
	 * @return graph data structure
	 */
	public edu.uci.ics.jung.graph.Graph getGraph();
}
