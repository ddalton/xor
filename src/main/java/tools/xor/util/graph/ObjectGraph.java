package tools.xor.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.BusinessEdge;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;

/**
 * This represents the call graph during the UPDATE stage of the processing for the given view.
 * Useful for the following purposes:
 * 1. Identify loops so they can be broken for serialization
 * 2. Identify the connections represented by open properties, as open properties objects cannot be reached otherwise
 * 
 * @author Dilip Dalton
 *
 * @param <V> vertex
 * @param <E> edge
 */
public class ObjectGraph<V extends BusinessObject, E extends BusinessEdge> extends DirectedSparseGraph<V, E> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	public  static final String AGGREGATE_NAME_PREFIX = "AGGREGATE";
	
	private V root; // aggregate rooted at this type
	private Map<V, Map<String, E>> outTransitions = new IdentityHashMap<V, Map<String, E>>(); 
	
	public ObjectGraph(BusinessObject aggregateRoot) {
		this.root = (V) aggregateRoot; 
	}
	
	public V getRoot() {
		return root;
	}
	
	@Override
	public void addEdge(E edge, V start, V end) {
		if(edge.getStart() != start) {
			throw new IllegalStateException("Transition object start is not the same as given start");
		}
		if(edge.getEnd() != end) {
			throw new IllegalStateException("Transition object end is not the same as given end");
		}
		
		// We do not have property information for edges from 
		// collection elements
		if(edge.getProperty() != null) {
			Map<String, E> outLinks = outTransitions.get(start);
			if(outLinks == null) {
				outLinks = new IdentityHashMap<String, E>();
				outTransitions.put(start, outLinks);
			}
			outLinks.put(edge.getProperty().getName(), edge);
		}
		
		super.addEdge(edge, start, end);
	}
	
	/**
	 * Need to query the collection owner object if needing the
	 * collection elements.
	 * 
	 * @param vertex object
	 * @param name of the edge
	 * @return edge
	 */
	public E getOutEdge(V vertex, String name) {
		if(!outTransitions.containsKey(vertex)) {
			return null;
		}
		
		return outTransitions.get(vertex).get(name);
	}

	public void addEdge(E edge) {
		addEdge(edge, (V) edge.getStart(), (V) edge.getEnd());
	}
	
	public boolean hasPath(String path) {
		return hasPath((V) root, path);
	}
	
	public boolean hasPath(V vertex, String path) {
		// Is this an attribute
		if(vertex.getPropertyByPath(path) != null) {
			return true;
		}

		E t = getOutEdge(vertex, State.getNextAttr(path));
		if(t == null) {
			//logger.debug("Unable to get path in state graph: " + path + " originating state: " + this.type.getName());
			return false;
		}

		path = State.getRemaining(path);
		if(path == null || "".equals(path.trim())) {
			// found it
			return true;
		}

		return hasPath((V) t.getEnd(), path);
	}
	
	public void persistGraph(ObjectCreator objectCreator, Settings settings) {

		EntityType entityType = ((EntityType)settings.getEntityType()).getDomainType();
		StateGraph<State, Edge<State>> sg = settings.getView().getStateGraph(entityType);
		persistRoots(objectCreator, sg);
	}
	
	private void addTopologicalOrderedEdge(Property p, V source, V target ) {
		
		// Do not consider self loops
		if( source == target) {
			return;
		}
		
		// Do not consider nullable non-cascaded relationships
		if(!p.isContainment() && p.isNullable()) {
			return;
		}
		
		Property property = p;
		if(p.isMany()) {
			property = null;
		}
		
		// containment relationships are reversed from
		// a topological ordering perspective		
		if(p.isContainment()) {
			BusinessEdge<BusinessObject> edge = new BusinessEdge<BusinessObject>(target, source, property);
			addEdge((E) edge, target, source);
		} else {
			BusinessEdge<BusinessObject> edge = new BusinessEdge<BusinessObject>(source, target, property);
			addEdge((E) edge, source, target);
		}
	}

	/**
	 * In a typical spanning tree We break all loops from the graph. 
	 * In this method, we preserve the edges by making a copy of the target. So we are able
	 * to convert a DAG to a tree by doing node copy.
	 * It is sufficient to copy only the the fields that are marked for initialization.
	 * 
	 * NOTE: This can be an expensive method since the enumeration of all the loops in a graph
	 * is expensive. Call only when needed.
	 * @param source root
	 * @param settings user specified settings
	 */
	public void spanningTreeWithEdgeSwizzling(BusinessObject source, Settings settings) {

		/*
		 * The algorithm is the following:
		 * 1. Get a stack of all the loops in the graph
		 * 2. Pop out a loop and process it as follows
		 * 3. Randomly select an edge this is non-cascade and not required
		 * 4. Make a copy of the target object and copy the attributes that are marked for initialization.
		 *    This is usually the object id and a business key
		 * 5. Swizzle this edge to point to the new object copy
		 */
		
		// First create the graph
		build(source, settings);
		
		// 1. Get a stack of all the loops
		ArrayDeque<List<E>> stack = new ArrayDeque<List<E>>(this.getLoops());
		while (!stack.isEmpty()) {
			// 2. Get a loop
			List<E> circuit = stack.pop();
			
			// 3. Get an edge
			E edge = null;
			for(E e: circuit) {
				if(e.getProperty() == null 
						|| e.getProperty().isContainment() 
						|| !e.getProperty().isNullable()) {
					continue;
				}
				edge = e;
				break;
			}
			
			if(edge == null) {
				// Should not happen
				throw new RuntimeException("Unable to find an edge in the loop to swizzle");
			}
			
			// 4. Make a reference copy
			Object refCopy = edge.getEnd().createReferenceCopy();
			
			// 5. Swizzle the edge to point to the new object
			swizzle(edge, refCopy);
		}
	}
	
	private void swizzle(E edge, Object refCopy) {
		unlinkEdge(edge);
		
		// point the edge to the new object
		((ExtendedProperty)edge.getProperty()).setValue(edge.getStart(), refCopy);
		
		// Since refCopy is not a node in the object graph and just a POJO we 
		// don't need to explicitly add an edge for it.
	}
	
	/**
	 * This method builds the directed object graph. Used for addressing loops.
	 * Non cascaded references are swizzled to a simpler object copy
	 * 
	 * See {@link ObjectGraph#addEdge(E, V, V)}
	 * @param source
	 * @param settings
	 */
	private void build(BusinessObject source, Settings settings) {
		
		if(source.isVisited())
			return;

		source.setVisited(true);
		
		for(Property property: ((EntityType)source.getType()).getProperties(settings.getApiVersion())) {
			if( ((ExtendedProperty)property).isDataType() && !property.isMany() ) {
				continue;
			}
			BusinessObject target = (BusinessObject) source.getExistingDataObject(property);
			if(target == null) {
				// No edge to a null object
				continue;
			}		
			BusinessEdge<BusinessObject> edge = new BusinessEdge<BusinessObject>(source, target, property);
			addEdge((E) edge, (V) source, (V) target);
			
			if(property.isMany()) {
				for(Object element: source.getList(property)) {
					// No direct property reference for a collection element
					edge = new BusinessEdge<BusinessObject>(target, (BusinessObject) element, null);
					addEdge((E) edge, (V) target, (V) element);

					build((BusinessObject) element, settings);
				}
			} else {
				build(target, settings);
			}
		}
	}
	
	/**
	 * Once the aggregate roots are discovered, we then sort these roots
	 * by the StateComparator for the aggregate root type
	 * @param objectCreator
	 */
	private Set<V> discoverAggregateRoots(ObjectCreator objectCreator) {
		Map<V, V> aggregateRoots = new IdentityHashMap<V, V>();
		Map<V, V> distinctRoots = new IdentityHashMap<V, V>();
		
		// Go through each data object and walk back through it container
		// and set the root 
		for(BusinessObject dataObject: objectCreator.getDataObjects()) {
			V root = getAggregateRoot((V) dataObject);
			aggregateRoots.put((V) dataObject, root);
			distinctRoots.put(root, root);
		}
		
		return distinctRoots.keySet();
	}
	
	private V getAggregateRoot(V dataObject) {
		while(dataObject.getContainer() != null) {
			Property property = dataObject.getContainmentProperty();
			
			// If the property to the data object is not cascade
			// then we have reached the root
			if(property == null) {
				dataObject = (V) dataObject.getContainer();
			} else {
				if(property.isContainment()) {
					// Go up one level
					dataObject = (V) dataObject.getContainer();
				} else {
					// found the root
					return dataObject;
				}
			}
		}
		
		return dataObject;
	}
	
	class StateComparator implements Comparator<V> {
		StateGraph<State, Edge<State>> sg;
		
		public StateComparator(StateGraph<State, Edge<State>> sg) {
			this.sg = sg;
		}
		
		private State getByAncestorType(V vertex) {
			State v = null;
			
			if(EntityType.class.isAssignableFrom(vertex.getType().getClass())) {
				EntityType superType = ((EntityType) vertex.getType()).getSuperType();
				v = sg.getVertex(superType);
				while(v == null) {
					superType = superType.getSuperType();
					if(superType == null)
						break;
					v = sg.getVertex(superType);
				}
			}
			
			if(v == null) {
				throw new RuntimeException("Unable to find type " + vertex.getType().getName() 
						+ " for state graph rooted for type: " + sg.dumpState());
			}
			
			return v;
		}
		
		private State getState(V vertex) {
			 State v = sg.getVertex(vertex.getType());
			 if(v == null) {
				 v = getByAncestorType(vertex);
			 }
			 
			 return v;
		}
		
	    @Override
	    public int compare(V a, V b) {
	    	// sg.getVertex(a.getType());
	    	State aVertex = getState(a); 
	    	State bVertex = getState(b);
	    	
	    	if(aVertex == null) {
	    		
	    		System.out.println("aVertex is null for type: " + a.getType().getName());
	    		for(State s: sg.getVertices()) {
	    			System.out.println("-- Type: " + s.getType().getName());
	    		}
	    	}
	    	if(bVertex == null) {
	    		System.out.println("bVertex is null for type: " + b.getType().getName());
	    	}
	    	
	    	int aOrder = sg.getId(aVertex);
	    	int bOrder = sg.getId(bVertex);
	    	return bOrder - aOrder;
	    }
	}	
	
	private void persistRoots(ObjectCreator objectCreator, StateGraph<State, Edge<State>> sg) {
	
		Date start = new Date();
		List<V> aggregateRoots = new ArrayList<V>(discoverAggregateRoots(objectCreator));
		Collections.sort(aggregateRoots, new StateComparator(sg));
		for(V root: aggregateRoots) {
			objectCreator.getPersistenceOrchestrator().saveOrUpdate(root.getInstance());
		}

		if(logger.isDebugEnabled()) {
			logger.debug("ObjectGraph took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		}
	}
	
	public void replaceInstance(V vertex, Object newInstance) {
		// get the incoming edges and set the value to the new copy
		vertex.setInstance(newInstance);
		for(BusinessEdge edge: getInEdges(vertex)) {
			((ExtendedProperty) edge.getProperty()).setValue(edge.getStart(), edge.getEnd().getInstance());
		}
	}
}
