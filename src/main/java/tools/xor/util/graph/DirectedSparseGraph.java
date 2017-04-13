package tools.xor.util.graph;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.util.Constants;
import tools.xor.util.Vertex;

/**
 * This class is not thread safe because some methods (getCircuits())
 * change state without synchronization. 
 *
 * @param <V> vertex
 * @param <E> edge
 */
public class DirectedSparseGraph<V, E> implements DirectedGraph<V, E> {
	private static final Logger cfLogger = LogManager.getLogger(Constants.Log.CYCLE_FINDER);
	
	public static final int START = Constants.XOR.TOPO_ORDERING_START;
	
	private Map<Integer, V> vertices;         // A vertex may not have an edge
	private Map<V, Collection<E>> inEdges;  // Get all incoming edges to a vertex
	private Map<V, Collection<E>> outEdges; // Get all outgoing edges from a vertex
	private Map<Pair<V>, Collection<E>> edgesByPair; // There might be multiple edges between a vertex pair
	private Map<E, Pair<V>> pairByEdge;     // Find a vertex pair given the edge
	private Map<V, Integer> id;             // Represents the id of a vertex, needed for some algorithms
	private int nextId = START;                  // The id available to assign to a new vertex
	private Map<E, Pair<V>> unlinked;                // The set of edges that were unlinked to maintain SCC
	private Map<E, Pair<V>> reversed;                // The set of edges that were reversed for toposort
	
	public DirectedSparseGraph() {
		//this.vertices = new IdentityHashMap<Integer, V>();
		this.vertices = new Int2ReferenceOpenHashMap<V>();
		this.inEdges = new IdentityHashMap<V, Collection<E>>();
		this.outEdges = new IdentityHashMap<V, Collection<E>>();
		this.edgesByPair = new HashMap<Pair<V>, Collection<E>>();
		this.pairByEdge = new HashMap<E, Pair<V>>();
		this.id = new IdentityHashMap<V, Integer>();
		this.unlinked = new HashMap<E, Pair<V>>();
		this.reversed = new HashMap<E, Pair<V>>();
	}
	
	@Override
	public void addVertex(V vertex) {
		int vertexId = nextId++;
		
		this.vertices.put(vertexId, vertex);
		this.id.put(vertex, vertexId);
	}
	
	@Override
	public int getId(V vertex) {
		if(id == null) {
			System.out.println("*************** Id is null");
		}
		if(vertex == null) {
			System.out.println("*************** vertex is null");
		}
		return id.get(vertex);
	}
	
	@Override
	public V getVertex(int id) {
		return vertices.get(id);
	}
	
	@Override
	public void unlinkVertex(int id) {
		V vertex = getVertex(id);
		cfLogger.debug("unlinking vertex: " + vertex.toString());
		
		if(inEdges.containsKey(vertex)) {
			Collection<E> edges = new HashSet<E>(inEdges.get(vertex));
			for(E edge: edges) {
				unlinkEdge(edge);
			}
		}
		
		if(outEdges.containsKey(vertex)) {
			Collection<E> edges = new HashSet<E>(outEdges.get(vertex));
			for(E edge: edges) {
				unlinkEdge(edge);
			}
		}
	}
	
	@Override
	public void unlinkEdge(E edge) {
		Pair<V> pair = pairByEdge.get(edge);
		unlinked.put(edge, pair);
		
		removeEdge(edge);
	}
	
	@Override
	public void linkEdge(E edge) {
		Pair<V> pair = unlinked.get(edge);
		addEdge(edge, pair.getStart(), pair.getEnd());
		
		unlinked.remove(edge);
	}
	
	public void restore() {
		cfLogger.debug("Restoring unlinked edges");
		for(Map.Entry<E, Pair<V>> entry: unlinked.entrySet()) {
			addEdge(entry.getKey(), entry.getValue().getStart(), entry.getValue().getEnd());
		}
		
		cfLogger.debug("Restoring reversed edges");
		for(Map.Entry<E, Pair<V>> entry: reversed.entrySet()) {
			performReverse(entry.getKey());
		}

		// clear
		unlinked.clear();
		reversed.clear();
	}

	@Override 
	public Collection<V> getVertices() {
		return vertices.values();
	}
	
	@Override
	public Collection<E> getEdges() {
		Collection<E> result = new HashSet<E>();
		result.addAll(pairByEdge.keySet());

		return result;
	}

	@Override
	public void addEdge(E edge, V start, V end) {
		if(!vertices.containsValue(start)) {
			addVertex(start);
		}
		if(!vertices.containsValue(end)) {
			addVertex(end);
		}
		cfLogger.debug("Adding edge: " + edge.toString());

		Pair<V> pair = new Pair<V>(start, end);
		if(edgesByPair.containsKey(pair)) {
			Collection<E> edges = edgesByPair.get(pair);
			edges.add(edge);
		} else {
			Collection<E> edges = new HashSet<E>();
			edges.add(edge);
			edgesByPair.put(pair, edges);
		}

		if(pairByEdge.containsKey(edge) && !pairByEdge.get(edge).equals(pair)) {
			throw new IllegalStateException("The edge refers to a different pair");
		}
		pairByEdge.put(edge, pair);
		
		if(inEdges.containsKey(end)) {
			Collection<E> edges = inEdges.get(end);
			edges.add(edge);
		} else {
			Collection<E> edges = new HashSet<E>();
			edges.add(edge);
			inEdges.put(end, edges);
		}
		
		if(outEdges.containsKey(start)) {
			Collection<E> edges = outEdges.get(start);
			edges.add(edge);
		} else {
			Collection<E> edges = new HashSet<E>();
			edges.add(edge);
			outEdges.put(start, edges);
		}
	}
	
	@Override
	public void unlinkSelfLoops() {
		
		for(V v: getVertices()) {
			// Need to make a copy of the edges
			// since it is mutating
			Set<E> edges = new HashSet<E>(getOutEdges(v));
			for(E e: edges) {
				
				// self loop
				if(getStart(e) == getEnd(e)) {
					unlinkEdge(e);
				}
			}
		}
	}
	
	@Override
	public void removeEdge(E edge) {
		Pair<V> pair = pairByEdge.get(edge);
		pairByEdge.remove(edge);
		
		cfLogger.debug("Removing edge: " + edge.toString());
		
		if(edgesByPair.get(pair).size() > 1) {
			edgesByPair.get(pair).remove(edge);
		} else {
			edgesByPair.remove(pair);
		}
			
		if(outEdges.get(pair.getStart()).size() > 1) {
			outEdges.get(pair.getStart()).remove(edge);
		} else {
			outEdges.remove(pair.getStart());
		}
		
		if(inEdges.get(pair.getEnd()).size() > 1) {
			inEdges.get(pair.getEnd()).remove(edge);
		} else {
			inEdges.remove(pair.getEnd());
		}
	}

	private E performReverse(E edge) {
		Pair<V> pair = pairByEdge.get(edge);

		// remove existing edge
		removeEdge(edge);

		// add edge with reversed vertices
		edge = getReversedEdge(edge);
		addEdge(edge, pair.getEnd(), pair.getStart());

		return edge;
	}
	
	@Override
	public void reverseEdge(E edge) {
		edge = performReverse(edge);

		reversed.put(edge, pairByEdge.get(edge));
	}

	@Override
	public E getReversedEdge(E edge) {
		return edge;
	}
	
	@Override
	public V getStart(E edge) {
		return pairByEdge.get(edge).getStart();
	}
	
	@Override
	public V getEnd(E edge) {
		return pairByEdge.get(edge).getEnd();
	}

	@Override
	public Collection<E> getInEdges(V vertex) {
		Collection<E> result = inEdges.get(vertex);
		return result == null ? new HashSet<E>() : result;
	}

	@Override
	public Collection<E> getOutEdges(V vertex) {
		Collection<E> result = outEdges.get(vertex);
		return result == null ? new HashSet<E>() : result;
	}

	@Override
	public boolean containsVertex(V vertex) {
		return this.vertices.containsValue(vertex);
	}
	
	@Override
	public boolean isCyclic() {
		Stack<Set<V>> sccs = getSCC();
		if(sccs.size() > 0) {
			return true;
		}
		
		// check for self loops
		for(V start: getVertices()) {
			for(E outEdge: getOutEdges(start)) {
				V end = getEnd(outEdge);
				if(start == end) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public Stack<Set<V>> getSCC() {
		SCCFinder<V, E> scc = new SCCFinder<V, E>(this);
		return scc.generate(getVertices());
	}

	private static class SCCFinder<V, E> {
		// Used by Tarjan's SCC algorithm
		private Stack<Set<V>> scc = new Stack<Set<V>>();
		private Map<V, Integer> root = new HashMap<V, Integer>();
		private Map<V, Integer> lowLink = new HashMap<V, Integer>();
		private int index;
		private DirectedGraph<V, E> dg;
		
		public SCCFinder(DirectedGraph<V, E> dg) {
			this.dg = dg;
		}
		
		/**
		 * The component id is populated based on Tarjan's SCC algorithm
		 */
		public Stack<Set<V>> generate(Collection<V> vertices) {
		
			Stack<V> stack =  new Stack<V>();
			
			this.index = 0;
			this.scc = new Stack<Set<V>>();
			this.root = new HashMap<V, Integer>();
			this.lowLink = new HashMap<V, Integer>();

			for (V vertex: vertices) {
				if (!root.containsKey(vertex)) {
					strongConnect(stack, vertex);
				}
			}
			
			return this.scc;
		}
		
		public void addSCC(Set<V> vertices) {
			scc.push(vertices);

			for(V vertex: vertices) {
				for(E inEdge: dg.getInEdges(vertex)) {
					V start = dg.getStart(inEdge);
					if(!vertices.contains(start)) {
						dg.unlinkEdge(inEdge);
					}
				}

				for(E outEdge: dg.getOutEdges(vertex)) {
					V end = dg.getEnd(outEdge);
					if(!vertices.contains(end)) {
						dg.unlinkEdge(outEdge);
					}
				}
			}
		}

		public void strongConnect(Stack<V> stack, V v) {
			
			lowLink.put(v, index);
			root.put(v, index++);
			stack.push(v);

			// Consider successors of state
			for(E e: dg.getOutEdges(v)) {
				V w = dg.getEnd(e);
				if(!root.containsKey(w)) {
					// Successor w has not yet been visited; recurse on it
					strongConnect(stack, w);
					lowLink.put(v, Math.min(lowLink.get(v), lowLink.get(w)));
				} else if(stack.contains(w)) {
					lowLink.put(v, Math.min(lowLink.get(v), root.get(w)));
				}
			}

			// If v is a root node, pop the stack and generate an SCC
			if (root.get(v).intValue() == lowLink.get(v).intValue()) {
				Set<V> currentSCC = new HashSet<V>();			
				// start a new strongly connected component
				V w;
				do {
					w = stack.pop();
					currentSCC.add(w);
				} while (w != v);
				
				// Don't add self loops
				if(currentSCC.size() > 1) {
					addSCC(currentSCC);
				} 
			}
		}
	}
	
	/**
	 * Algorithm by Donald B. Johnson
	 * @author Dilip Dalton
	 *
	 */
	private static class CircuitFinder<V, E> {
		final Logger cfLogger = LogManager.getLogger(Constants.Log.CYCLE_FINDER);
		
		private List<Boolean> blocked = new ArrayList<Boolean>();
		private List<Set<V>> B = new ArrayList<Set<V>>();
		private List<List<V>> circuits = new ArrayList<List<V>>();
		private List<List<E>> loops = new ArrayList<List<E>>();
		private DirectedGraph<V, E> dg; 
		private Stack<V> cycle = new Stack<V>();
		private Stack<E> loop = new Stack<E>();
		private int n; // number of vertices
		private SCCFinder<V, E> sccFinder;
		
		 /**
		  * This algorithm modifies the given DirectedGraph object
		  * @param dg
		  */
		public CircuitFinder(DirectedGraph<V, E> dg) {
			this.dg = dg;
			
			this.n = dg.getVertices().size() + START; // We index from 1
			this.blocked = new ArrayList<Boolean>(n);
			this.B = new ArrayList<Set<V>>(n);
			this.sccFinder = new SCCFinder<V, E>(dg);
			
			for(int i = 0; i < this.n; i++) {
				this.blocked.add(Boolean.FALSE);
				this.B.add(new HashSet<V>());
			}
		}
		
		private void unblock(int u) {
			blocked.set(u, Boolean.FALSE);
			
			Set<V> copyB = new HashSet<V>(B.get(u));
			for(V w: copyB) {
				B.get(u).remove(w);
				if(blocked.get(dg.getId(w))) {
					unblock(dg.getId(w));
				}
			}
		}
		
		private boolean circuit(int v, int s) {
			// stack v
			cycle.push(dg.getVertex(v));
			
			blocked.set(v, Boolean.TRUE);

			boolean f = false;
			for(E edge: dg.getOutEdges(dg.getVertex(v))) {
				cfLogger.debug("Processing edge: " + edge);
				int w = dg.getId(dg.getEnd(edge));

				loop.push(edge);
				
				// Got a cycle
				if(w == s) {
					List<E> l = new ArrayList<E>(this.loop);
					List<V> c = new ArrayList<V>(this.cycle);
					c.add(dg.getVertex(w));
					loops.add(l);
					circuits.add(c);
					f = true;
					
					if(cfLogger.isDebugEnabled()) {
						StringBuilder sb = new StringBuilder();
						for(V vertex: c) {
							sb.append(sb.length() > 0 ? "->" : "");
							sb.append(vertex.toString());
						}
						cfLogger.debug("A loop is detected and is: " + sb.toString());
					}
					
				} else if(!blocked.get(w)) {
					if(circuit(w, s)) {
						f = true;
					}
				}
				
				loop.pop();
			}

			if(f) {
				unblock(v);
			} else {
				for(E edge: dg.getOutEdges(dg.getVertex(v))) {
					int w = dg.getId(dg.getEnd(edge));
					if(!B.get(w).contains(dg.getVertex(v))) {
						B.get(w).add(dg.getVertex(v));
					}
				}
			}
			
			// unstack v
			cycle.pop();

			return f;
		}
		
		public List<List<V>> execute() {
			cfLogger.debug("Entering CycleFinder#execute");

			Stack<Set<V>> sccs = dg.getSCC();
			Map<Integer, Set<V>> sccById = new HashMap<Integer, Set<V>>();
			
			for(Set<V> strongComp: sccs) {
				for(V s: strongComp) {
					sccById.put(dg.getId((V) s), strongComp);
				}
			}
			
			/* 
			 * Go by vertex id order
			 */
			for(int s = START; s < (dg.getVertices().size() + START); s++) {
				V vertex = dg.getVertex(s);
				if(!sccById.containsKey(s)) {
					continue;
				}

	            Set<V> scc = sccById.get(s);
	            for(V v: scc) {
	            	blocked.set(dg.getId(v), Boolean.FALSE);
	            	B.set(dg.getId(v), new HashSet<V>());	
	            }
            	
				cfLogger.debug("Processing vertex: " + s);
            	circuit(s, s);
            	
	            // remove the edges incident on vertex with id 's"
	            scc.remove(vertex);
	            for(V v: scc) {
	            	cfLogger.debug("Removing SCC mapping for vertex: " + v);
	            	sccById.remove(dg.getId(v));
	            }
	            
	            dg.unlinkVertex(s);
	            
	            Stack<Set<V>> newSCC = sccFinder.generate(sccById.get(s));
	            for(Set<V> vertices: newSCC) {
	            	for(V v: vertices) {
	            		cfLogger.debug("Adding SCC mapping for vertex: " + v);
	            		sccById.put(dg.getId(v), vertices);
	            	}
	            }

			}
			
			 
			return circuits;
		}
	}

	@Override
	public List<List<Vertex>> getCircuits() {
		CircuitFinder<V, E> ec = new CircuitFinder<V, E>(this);
		List<List<V>> cycles = ec.execute();
		
		// restore the links that was broken 
		this.restore();
		
		List<List<Vertex>> result = new ArrayList<List<Vertex>>();
		for(List<V> l : cycles) {
			result.add((List<Vertex>) l);
		}
		
		return result;
	}
	

	@Override
	public List<List<E>> getLoops() {
		CircuitFinder<V, E> ec = new CircuitFinder<V, E>(this);
		ec.execute();
		List<List<E>> loops = ec.loops;
		
		// restore the links that was broken 
		this.restore();
		
		List<List<E>> result = new ArrayList<List<E>>();
		for(List<E> l : loops) {
			result.add((List<E>) l);
		}
		
		return result;
	}	
	
	/**
	 * The vertex that has no incoming edges (dependant) gets the lower number 
	 *
	 * @param <V>
	 * @param <E>
	 */
	static class TopoSort<V, E> {

		Set<V> temporary = new HashSet<V>();
		Set<V> unmarked = new HashSet<V>();
		LinkedList<V> sorted = new LinkedList<V>();
		private DirectedGraph<V, E> dg;
		
		public TopoSort(DirectedGraph<V, E> dg) {
			this(dg, dg.getVertices());
		}
		
		public TopoSort(DirectedGraph<V, E> dg, Collection<V> vertices) {
			this.dg = dg;
			this.unmarked.addAll(vertices);
		}
		
		public List<V> execute() {
			while(!unmarked.isEmpty()) {
				visit(unmarked.iterator().next());
			}
			
			return sorted;
		}
		
		private void visit(V vertex) {
			if(temporary.contains(vertex)) {
				StringBuilder cycle = new StringBuilder();
				for(V v: temporary) {
					cycle.append( cycle.length() > 0 ? ","+v.toString() : v.toString());
				}
				throw new RuntimeException("The graph is not a DAG, found vertex " + vertex.toString() + " in the set of " + cycle.toString());
			}
			
			if(unmarked.contains(vertex)) {
				temporary.add(vertex);
				
				for(E edge: dg.getOutEdges(vertex)) {
					visit(dg.getEnd(edge));
				}
				
				unmarked.remove(vertex);
				temporary.remove(vertex);
				sorted.addFirst(vertex);
			}
		}
	}
	
	/**
	 * Renumber the id based on sorted list
	 * @param sorted list of vertices
	 */
	protected void renumber(List<V> sorted) {
		this.nextId = START;

		// Clear the vertices
		this.vertices.clear();
		this.id.clear();
		
		for(V vertex: sorted) {
			addVertex(vertex);
		}
	}

	@Override
	public List<V> toposort() {
		TopoSort<V, E> ts = new TopoSort<V, E>(this);
		return ts.execute();
	}
}
