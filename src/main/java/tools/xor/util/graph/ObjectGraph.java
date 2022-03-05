package tools.xor.util.graph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import javax.swing.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import tools.xor.BusinessEdge;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ListType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.view.AggregateView;
import tools.xor.view.View;

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

	/**
	 * Save the objects collected in the objectCreator instance to the persistence store.
	 * A graph data structure is used so that the order of persistence operations is executed
	 * correctly.
	 *
	 * @param objectCreator containing the objects to be persisted
	 * @param settings controlling this operation
	 */
	public void persistGraph(ObjectCreator objectCreator, Settings settings) {

		EntityType entityType = (EntityType) objectCreator.getTypeMapper().getDomainShape().getType(((EntityType)settings.getEntityType()).getEntityName());

		// For topological sorting we need to use the VIEW_GRAPH
		View view = settings.getView();
		TypeGraph<State, Edge<State>> sg = AggregateView.isAggregateView(view.getName()) ? view.getTypeGraph(
			entityType) : view.getTypeGraph(entityType, StateGraph.Scope.VIEW_GRAPH);
		persistRoots(objectCreator, settings, sg);

		if (ApplicationConfiguration.config().containsKey(Constants.Config.ACTIVATE_DETECTORS)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.ACTIVATE_DETECTORS)) {

			// If this is an ObjectCreator detector then execute it
			if(settings.getDetector() != null && settings.getDetector().getClass().getEnclosingClass() != null &&
				settings.getDetector().getClass().getEnclosingClass() == ObjectCreator.class) {
				settings.getDetector().investigate(objectCreator);
			}
		}
	}

	/**
	 * Delete the objects collected in the objectCreator instance from the persistence store.
	 * A graph data structure is used so that the order of persistence operations is executed
	 * correctly.
	 *
	 * @param objectCreator containing the objects to be deleted
	 * @param settings controlling this operation
	 */
	public void deleteGraph(ObjectCreator objectCreator, Settings settings) {

		EntityType entityType = (EntityType) objectCreator.getTypeMapper().getDomainShape().getType(((EntityType)settings.getEntityType()).getEntityName());
		TypeGraph<State, Edge<State>> sg = settings.getView().getTypeGraph(entityType);
		deleteRoots(objectCreator, sg);
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
	 */
	public void spanningTreeWithEdgeSwizzling(BusinessObject source) {

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
		build(source);
		
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
						|| !e.getProperty().isNullable()
					    || e.getProperty().isMany()) {
					continue;
				}
				edge = e;
				break;
			}
			
			if(edge == null) {
				StringBuilder loopStr = new StringBuilder();
				for(E e: circuit) {
					loopStr.append("--" + e.toString() + "-->(" + e.getEnd().toString() + ")");
				}
				logger.warn("Potential loop: " + loopStr.toString());
				continue;
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
	 */
	private void build(BusinessObject source) {
		
		if(source.isVisited())
			return;

		source.setVisited(true);
		
		for(Property property: source.getType().getProperties()) {
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
				Collection collection = source.getList(property);
				if(property.isOpenContent() && collection.size() == 0) {
					Object obj = ClassUtil.getInstance(source.get(property));
					if(obj != null) {
						if (obj instanceof Collection) {
							collection = (Collection)obj;
						}
						else if (obj instanceof Map) {
							collection = ((Map)obj).entrySet();
						}
					}
				}
				for(Object element: collection) {
					if( !(element instanceof BusinessObject) ) {
						element = source.getObjectCreator().getExistingDataObject(element);
						if(element == null) {
							continue;
						}
					}

					// No direct property reference for a collection element
					edge = new BusinessEdge<BusinessObject>(target, (BusinessObject) element, null);
					addEdge((E) edge, (V) target, (V) element);

					build((BusinessObject) element);
				}
			} else {
				build(target);
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
			if (root == null) {
				continue;
			}
			aggregateRoots.put((V) dataObject, root);
			distinctRoots.put(root, root);
		}
		
		return distinctRoots.keySet();
	}
	
	private V getAggregateRoot(V dataObject) {
		while(dataObject.getContainer() != null) {
			Property property = dataObject.getContainmentProperty();

			if(property == null) {
				// If the containment property is not present then it is
				// a collection element. Check the containment property of the collection object.

				property = dataObject.getContainer().getContainmentProperty();
				if(property.isContainment()) {
					dataObject = (V)dataObject.getContainer().getContainer();
				} else {
					return dataObject;
				}
			} else {
				// If the property to the data object is not cascade
				// then we have reached the root
				if(property.isContainment()) {
					// Go up one level
					dataObject = (V) dataObject.getContainer();
				} else {
					// Potential root

					// We only persist entities
					if( !(dataObject.getType() instanceof EntityType) ) {
						return null;
					}
					if(((EntityType)dataObject.getType()).isEmbedded()) {
						// Embedded types are not directly persisted
						return null;
					} else {
						return dataObject;
					}
				}
			}
		}
		
		return dataObject;
	}
	
	public static class StateComparator<V extends StateComparator.TypedObject> implements Comparator<V> {
		TypeGraph<State, Edge<State>> sg;
		private boolean areTypesOrdered;

		public interface TypedObject {
			Type getType();
		}
		
		public StateComparator(TypeGraph<State, Edge<State>> sg) {
			this.sg = sg;

			areTypesOrdered = (!ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_SKIP)
				|| !ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_SKIP));
		}
		
		private State getByAncestorType(V vertex) {
			State v = null;
			
			if(EntityType.class.isAssignableFrom(vertex.getType().getClass())) {
				EntityType superType = ((EntityType) vertex.getType()).getParentType();
				if(superType == null) {
					throw new RuntimeException("Is the type from the correct shape?");
				}
				v = sg.getVertex(superType);
				while(v == null) {
					superType = superType.getParentType();
					if(superType == null)
						break;
					v = sg.getVertex(superType);
				}
			}
			
			if(v == null) {
				logger.debug("Unable to find type " + vertex.getType().getName()
						+ " for state graph rooted for type: " + sg.dumpState());
			}
			
			return v;
		}
		
		private State getState(V vertex) {
			 State v = sg.getVertex(vertex.getType().getName());
			 if(v == null) {
				 v = getByAncestorType(vertex);
			 }
			 
			 return v;
		}
		
	    @Override
	    public int compare(V a, V b) {
	    	State aVertex = getState(a); 
	    	State bVertex = getState(b);

	    	if(aVertex == null) {
	    		
	    		logger.debug("aVertex is null for type: " + a.getType().getName());
	    		for(State s: sg.getVertices()) {
	    			logger.debug("-- Type: " + s.getType().getName());
	    		}
				return 0;
	    	}
	    	if(bVertex == null) {
	    		logger.debug("bVertex is null for type: " + b.getType().getName());
				return 0;
	    	}
	    	
	    	int aOrder;
	    	int bOrder;

			// If the types have been topologically sorted then we retrieve the order
			// information from the type
			if(areTypesOrdered) {
				aOrder = ((EntityType)aVertex.getType()).getOrder();
				bOrder = ((EntityType)bVertex.getType()).getOrder();
			}
			// else we retrieve the order information from the StateGraph. This means
			// that the StateGraph needs to be topologically sorted.
			else {
				aOrder = sg.getId(aVertex);
				bOrder = sg.getId(bVertex);
			}

	    	return bOrder - aOrder;
	    }
	}	
	
	private void persistRoots(ObjectCreator objectCreator, Settings settings, TypeGraph<State, Edge<State>> sg) {
	
		Date start = new Date();
		List<V> aggregateRoots = new ArrayList<V>(discoverAggregateRoots(objectCreator));
		Collections.sort(aggregateRoots, new StateComparator(sg));
		for(V root: aggregateRoots) {
			if(settings.getDetector() != null) {
				settings.getDetector().investigate(root.getInstance());
			}
			objectCreator.getDataStore().saveOrUpdate(root.getInstance());
		}

		if(logger.isDebugEnabled()) {
			logger.debug("ObjectGraph persistRoots took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		}
	}

	private void deleteRoots(ObjectCreator objectCreator, TypeGraph<State, Edge<State>> sg) {

		Date start = new Date();
		List<V> aggregateRoots = new ArrayList<V>(discoverAggregateRoots(objectCreator));
		Collections.sort(aggregateRoots, new StateComparator(sg));

		// deletion should be in the opposite order of create
		Collections.reverse(aggregateRoots);

		for(V root: aggregateRoots) {
			objectCreator.getDataStore().delete(root.getInstance());
		}

		if(logger.isDebugEnabled()) {
			logger.debug("ObjectGraph deleteRoots took " + ((new Date().getTime()-start.getTime())/1000) + " seconds");
		}
	}
	
	public void replaceInstance(V vertex, Object newInstance) {
		// get the incoming edges and set the value to the new copy
		vertex.setInstance(newInstance);
		for(BusinessEdge edge: getInEdges(vertex)) {
			((ExtendedProperty) edge.getProperty()).setValue(edge.getStart(), edge.getEnd().getInstance());
		}
	}

	public void generateVisual (Settings settings) {
		settings.exportGraph(this);
	}

	@Override
	protected Collection<V> getConnectedVertices() {
		List unconnected = new ArrayList();
		getEdgeMap(unconnected);

		Map<Object, String> verticesToRemove = new IdentityHashMap();
		for(Object vertex: unconnected) {
			verticesToRemove.put(vertex, null);
		}

		List result = new ArrayList();
		for(V vertex: getVertices()) {
			if(!verticesToRemove.containsKey(vertex)) {
				result.add(vertex);
			}
		}

		return result;
	}

	@Override
	protected void writeGMLEdges(BufferedWriter writer) throws IOException
	{
		for(Map.Entry<String, E> entry: getEdgeMap(new ArrayList()).entrySet()) {
			E edge = entry.getValue();

			writer.write("\tedge\n\t[\n");
			writer.write("\t\tsource " + getId((V)edge.getStart()) + "\n");
			writer.write("\t\ttarget " + getId((V)edge.getEnd()) + "\n");
			writer.write("\t\tlabel \"" + entry.getKey() + "\"\n");

			// Give more weights to edges coming from the root, as this helps to identify
			// the root
			if(edge.getStart() == getRoot()) {
				writer.write("\t\tvalue 3\n");
			} else {
				writer.write("\t\tvalue 1\n");
			}
			writer.write("\t]\n");
		}
	}

	@Override
	public void writeGraphvizDot(BufferedWriter writer) throws IOException
	{
		for(Map.Entry<String, E> entry: getEdgeMap(new ArrayList()).entrySet()) {
			E edge = entry.getValue();
			writer.write(getId((V)edge.getStart()) + " -> " + getId((V)edge.getEnd()) + ";\n");
		}
	}

	protected Map<String, E> getEdgeMap(List verticesToRemove) {
		Integer j = 0;
		Iterator edgeIter = getEdges().iterator();

		Map<String, E> result = new HashMap<>();
		while(edgeIter.hasNext()) {
			E edge = (E)edgeIter.next();

			Property p = edge.getProperty();

			// If the object is an empty collection then we don't have to graph it
			if(p != null && p.isMany()) {
				BusinessObject collectionBO = edge.getEnd();
				if(getOutEdges((V)collectionBO).size() == 0) {
					verticesToRemove.add(collectionBO);
					continue;
				}
			}

			String edgeName = (p == null) ? (j++).toString() : (p.getName()+j++);

			if (result.containsKey(edgeName)) {
				System.out.println("Contains edge: " + edgeName);
			} else{
				result.put(edgeName, edge);
			}
		}

		return result;
	}

	@Override
	public void buildGraph() {
		if(this.root.getType() instanceof ListType) {
			for(BusinessObject bo: this.root.getList()) {
				build(bo);
			}
		} else {
			build(this.root);
		}
	}

	@Override
	public Graph getGraph() {

		buildGraph();

		Iterator vertexIter = getVertices().iterator();
		Graph<V, String> g = new SparseMultigraph<V, String>();
		while(vertexIter.hasNext()) {
			V vertex = (V)vertexIter.next();
			g.addVertex(vertex);
		}

		Integer j = 0;
		Iterator edgeIter = getEdges().iterator();
		List verticesToRemove = new ArrayList<>();
		while(edgeIter.hasNext()) {
			E edge = (E)edgeIter.next();

			Property p = edge.getProperty();

			// If the object is an empty collection then we don't have to graph it
			if(p != null && p.isMany()) {
				BusinessObject collectionBO = edge.getEnd();
				if(getOutEdges((V)collectionBO).size() == 0) {
					verticesToRemove.add(collectionBO);
					continue;
				}
			}

			String edgeName = (p == null) ? (j++).toString() : (p.getName()+j++);

			if (g.containsEdge(edgeName)) {
				System.out.println("Contains edge: " + edgeName);
			} else{
				g.addEdge(edgeName, (V) edge.getStart(), (V) edge.getEnd(), EdgeType.DIRECTED);
			}
		}

		for(Object bo: verticesToRemove) {
			g.removeVertex((V)bo);
		}

		return g;
	}

	/*
	public Graph getObjectGraph(Settings settings) {

		if(this.root.getType() instanceof ListType) {
			for(BusinessObject bo: this.root.getList()) {
				build(bo, settings);
			}
		} else {
			build(this.root, settings);
		}

		Iterator vertexIter = getVertices().iterator();
		Graph<V, String> g = new SparseMultigraph<V, String>();
		while(vertexIter.hasNext()) {
			V vertex = (V)vertexIter.next();
			g.addVertex(vertex);
		}

		List verticesToRemove = new ArrayList<>();
		for(Map.Entry<String, E> entry: getEdgeMap(verticesToRemove).entrySet()) {
			E edge = entry.getValue();
			g.addEdge(entry.getKey(), (V) edge.getStart(), (V) edge.getEnd(), EdgeType.DIRECTED);
		}

		for(Object bo: verticesToRemove) {
			g.removeVertex((V)bo);
		}

		return g;
	}*/
}
