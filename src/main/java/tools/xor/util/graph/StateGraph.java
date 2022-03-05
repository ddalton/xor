package tools.xor.util.graph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import tools.xor.AbstractProperty;
import tools.xor.AssociationSetting;
import tools.xor.BasicType;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.MatchType;
import tools.xor.NaturalEntityKey;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.Generator;
import tools.xor.service.Shape;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.DFAtoRE.Expression;
import tools.xor.util.DFAtoRE.LiteralExpression;
import tools.xor.util.DFAtoRE.TypedExpression;
import tools.xor.util.DFAtoRE.UnionExpression;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.State;

public class StateGraph<V extends State, E extends Edge<V>> extends DirectedSparseGraph<V, E> implements TypeGraph<V, E> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger sgLogger = LogManager.getLogger(Constants.Log.STATE_GRAPH);

	public  static final String EMPTY_EDGE = DFAtoNFA.UNLABELLED;
	private static final String ALL = "_all_";

	private Type root; // aggregate rooted at this type
	private Map<Type, V> states = new HashMap<>();
	private Map<String, V> statesByName = new HashMap<>(); // Needed for topological sorting by a different shape
	private Map<V, Map<String, E>> outTransitions = new HashMap<V, Map<String, E>>(); 
	//private Map<Type, List<Property>> attrByType = new HashMap<Type, List<Property>>();
	private Map<Type, Map<String, List<Property>>> attrByType = new HashMap<Type, Map<String, List<Property>>>();
	private Shape shape; // The shape of type system on which this state graph is based

	public enum Scope {
		FULL_GRAPH, // Represents a full graph state of the type, sub-types are included
		TYPE_GRAPH, // Represents a graph state of the type (sub-types are not included)
		VIEW_GRAPH, // Represents a graph state of the view. Useful for topological ordering of a view.
		EDGE        // Represents a tree state of the view
	};
	
	public StateGraph(Type aggregateRoot, Shape shape) {
		super();
		this.shape = shape;
		this.root = aggregateRoot;

		assert(this.shape != null) : "Shape cannot be null";
	}
	
	public V getRootState() {
		return states.get(root);
	}

	protected Type getAggregateRoot() {
		return this.root;
	}

	protected Shape getShape () {
		return this.shape;
	}
	
	@Override
	public void addEdge(E edge, V start, V end) {
		// There could be multiple edges with name UNLABELLED, so we have to
		// use the superclass implementation to support that
		if(!edge.isUnlabelled()) {
			if (edge.getStart() != start) {
				if (edge.getEnd() == start) {
					logger.debug("Adding a reversed edge");
				}
				else {
					throw new IllegalStateException(
						"Transition object start is not the same as given start");
				}
			}
			if (edge.getEnd() != end) {
				if (edge.getStart() == end) {
					logger.debug("Adding a reversed edge");
				}
				else {
					throw new IllegalStateException(
						"Transition object end is not the same as given end");
				}
			}

			Map<String, E> outLinks = outTransitions.get(start);
			if (outLinks == null) {
				outLinks = new HashMap<>();
				outTransitions.put(start, outLinks);
			}
			outLinks.put(edge.getName(), edge);
		}
		
		super.addEdge(edge, start, end);
	}

	@Override
	public void removeEdge(E edge) {
		if(!edge.isUnlabelled()) {
			Map<String, E> outLinks = outTransitions.get(edge.getStart());
			if (outLinks == null) {
				throw new IllegalStateException("Unable to find outTransition entry for edge");
			}
			outLinks.remove(edge.getName());
		}

		super.removeEdge(edge);
	}

	@Override
	public E getOutEdge(V vertex, String name) {
		if(DFAtoNFA.UNLABELLED.equals(name)) {
			throw new RuntimeException("Cannot get an inheritance edge as it does not have a name");
		}

		if(!outTransitions.containsKey(vertex)) {
			// vertex is not in scope
			return null;
		}

		if(outTransitions.get(vertex).containsKey(name)) {
			return outTransitions.get(vertex).get(name);
		} else {
			// The graph only has unlabelled edges from supertype to child
			// and not vice-versa so we have to explicitly get the supertype
			// vertex and check it

			// check supertype if there is an inheritance relationship
			V parentVertex = getParentVertex(vertex);
			return parentVertex == null ? null : getOutEdge(parentVertex, name);
		}
	}

	private V getParentVertex(V vertex) {

		if(vertex.getType() != null && vertex.getType() instanceof EntityType) {
			EntityType entityType = (EntityType)vertex.getType();
			if(entityType.getParentType() != null) {
				return getVertex(entityType.getParentType());
			}
		}

		return null;
	}

	@Override
	public void addVertex(V vertex) {
		if(containsVertex(vertex)) {
			return;
		}

		super.addVertex(vertex);
		this.states.put(vertex.getType(), vertex);
		this.statesByName.put(vertex.getType().getName(), vertex);
	}

	@Override
	public void removeVertex(V vertex) {
		if(!containsVertex(vertex)) {
			return;
		}

		super.removeVertex(vertex);
		this.states.remove(vertex.getType());
		this.statesByName.remove(vertex.getType().getName(), vertex);
	}

	@Override
	public void addEdge(E edge) {
		addEdge(edge, edge.getStart(), edge.getEnd());
	}

	@Override
	public V getVertex(Type t) {
		return states.get(t);
	}

	@Override
	public V getVertex(String name) {
		return statesByName.get(name);
	}

	@Override
	public StateGraph<V, E> getFullStateGraph() {

		for(State state: states.values()) {
			state.initDataTypes();
		}
		
		return this;
	}

	@Override
	public StateGraph<V, E> copy() {
		return copy(null);
	}
	
	private Map<Type, V> getStates() {
		return this.states;
	}
	
	/**
	 * 
	 * @param mergeStates map of states to be merged. Needs to be the original states map and not a copy, since it is needed to properly remove duplicates
	 * @return merged graph
	 */
	@Override
	public StateGraph<V, E> copy(Map<Type, V> mergeStates) {
		
		StateGraph<V, E> copy = new StateGraph<V, E>(this.root, this.shape);
		
		copyData(copy, mergeStates);

		return copy;
	}

	protected Map<V, V> copyData(StateGraph<V, E> copy, Map<Type, V> mergeStates) {
		Map<V, V> oldNew = new HashMap<V, V>();
		for(V v: getVertices()) {
			V newState = (V) v.copy();
			if(mergeStates != null) {
				if(mergeStates.containsKey(v.getType())) {
					newState = mergeStates.get(v.getType());
				}
			}
			oldNew.put(v, newState);

			// A graph may not have any edges, so let us add the
			// vertices here
			copy.addVertex(newState);
		}

		for(E edge: getEdges()) {
			copy.addEdge(
				(E) new Edge<State>(
					edge.getName(),
					oldNew.get(edge.getStart()),
					oldNew.get(edge.getEnd()),
					edge.isQualified()));
		}

		return oldNew;
	}

	@Override
	public List<Property> next(Type type, String propertyPath, Set<String> exactSet) {
		State state = getVertex(type);

		return next(state, propertyPath, exactSet);
	}

	@Override
	public List<Property> next(State state, String propertyPath, Set<String> exactSet) {
		Type type = state.getType();

		// Ensure the type is coerced to the correct shape
		if(shape != null) {
			type = shape.getType(type.getName());
		}

		String key = propertyPath;
		if(key == null || "".equals(key)) {
			key = ALL;
		}
		if(attrByType.containsKey(type)) {
			if(attrByType.get(type).containsKey(key)) {
				return attrByType.get(type).get(key);
			}
		}

		Map<String, List<Property>> propertyMap = attrByType.get(type);
		if(propertyMap == null) {
			propertyMap = new HashMap();
			attrByType.put(type, propertyMap);
		}

		List<Property> result = getPropertiesInScope(state, type, propertyPath, exactSet);
		propertyMap.put(key, result);
		
		return result;
	}

	protected List<Property> getPropertiesInScope(State vertex, Type type, String propertyPath, Set<String> exactSet) {
		List<Property> result = new ArrayList<Property>();

		if(exactSet != null && exactSet.size() > 0) {
			// filter the exactSet by propertyPath
			for(String path: exactSet) {
				if(path.startsWith(propertyPath)) {
					if(path.equals(propertyPath)) {
						// If it equals it, it is a reference association and we return all simple required properties
						for(String propertyName: AggregatePropertyPaths.enumerateRequiredSimple(type)) {
							Property p = type.getProperty(propertyName);
							if(p != null) {
								result.add(p);
							}
						}
					} else {
						int delimLen = propertyPath.length() > 0 ? Settings.PATH_DELIMITER.length() : 0;
						String remaining = path.substring(propertyPath.length()+delimLen);
						Property p = type.getProperty(Settings.getRootName(remaining));
						if(p != null) {
							result.add(p);
						}
					}
				}
			}
		}

		// Previous call was using MatchType.TYPE so add the attributes from the vertex
		if(result.size() == 0 || exactSet == null || exactSet.size() == 0) {
			// Addresses scope extension using AssociationSetting with MatchType.ABSOLUTE_PATH
			collectProperties(result, (V)vertex);
		}

		// Addresses scope extension using AssociationSetting with MatchType.TYPE
		collectEdges(result, (V)vertex);

		return result;
	}

	protected void collectProperties(List<Property> properties, V vertex) {
	    if(vertex != null) {
	        
	    /*
		while(vertex != null) {
			V parent = null;
			for (Edge e : getInEdges(vertex)) {

				// If this is an inheritance edge, we should also collect
				// the properties from the super-type
				if (EMPTY_EDGE.equals(e.getName())) {
					parent = (V)e.getStart();
					break;
				}
			}
*/
			// For a state graph the vertex contains only direct properties
			for (String simpleAttribute : vertex.getAttributes()) {
				properties.add(vertex.getType().getProperty(simpleAttribute));
			}

			//vertex = parent;
		}
	}

	protected void collectEdges(List<Property> properties, V vertex) {
		while(vertex != null) {
			V parent = null;
			for (Edge e : getOutEdges(vertex)) {

				// skip inheritance edges
				if (EMPTY_EDGE.equals(e.getName())) {
					continue;
				}
				properties.add(vertex.getType().getProperty(e.getName()));
			}

			for (Edge e : getInEdges(vertex)) {
				if (EMPTY_EDGE.equals(e.getName())) {
					parent = (V)e.getStart();
					break;
				}
			}

			vertex = parent;
		}
	}
	
	@Override
	public boolean hasPath(String path) {
		return hasPath(getRootState(), path);
	}

	public boolean hasPath(V vertex, String path) {
		// Is this an attribute
		if(vertex.getAttributes() != null && vertex.getAttributes().contains(path)) {
			return true;
		}

		Edge t = getOutEdge(vertex, State.getNextAttr(path));
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
	

	public String dumpState() {
		StringBuilder builder = new StringBuilder("StateGraph dumpState for type: " + root.getName() + "\r\n");

		for(State state: states.values()) {
			builder.append("Processing state: " + state.getType().getName() + "\r\n");
			builder.append(Constants.Format.INDENT_STRING + "Outgoing Transitions\r\n");
			for(E entry: getOutEdges((V) state)) {
				builder.append( Constants.Format.getIndentString(2) + entry.getName() + " -> " + entry.getEnd().getType().getName() + "\r\n");
				for(String attribute: entry.getEnd().getAttributes()) {
					builder.append( Constants.Format.getIndentString(3) + attribute +  "\r\n");
				}
			}
		}

		return builder.toString();
	}

	@Override
	public void prune (List<AssociationSetting> associations) {
		for(AssociationSetting assoc: associations) {
			if (assoc.getMatchType() == MatchType.TYPE) {
				V state = states.get(shape.getType(assoc.getEntityName()));
				removeVertex(state);
			} else if(assoc.getMatchType() == MatchType.RELATIVE_PATH) {
				String propertyToPrune = assoc.getPathSuffix();
				for(V state: getVertices()) {
					E e = getOutEdge(state, propertyToPrune);
					if(e != null) {
						removeEdge(e);
					}
				}
			} else if(assoc.getMatchType() == MatchType.ABSOLUTE_PATH) {
				throw new UnsupportedOperationException("Prune by absolute path is not yet implemented");
			}
		}
	}

	@Override
	public void markReferences (List<String> references)
	{
		for (String referenceType : references) {
			V state = states.get(shape.getType(referenceType));
			if (state == null) {
				throw new RuntimeException(
					"The type " + referenceType
						+ " needs to be part of the states in order to be marked as a reference state");
			}

			state.setReference(true);

			// Since these states are now references, there is no use in having
			// out edges from these states as they will not be processed, unless
			// they are part of the natural key
			EntityType entityType = (EntityType)state.getType();
			Set<String> naturalKeyMap = new HashSet<>();
			if(entityType.getNaturalKey() != null) {
				naturalKeyMap = new HashSet<>(entityType.getNaturalKey());
			}
			for(E edge: new LinkedList<>(getOutEdges(state))) {
				if(!naturalKeyMap.contains(edge.getName())) {
					removeEdge(edge);
				}
			}
		}
	}

	@Override
	public void enhance (List<AssociationSetting> associations) {

		if(sgLogger.isDebugEnabled()) {
			sgLogger.debug("Enhancing the state graph with associations/Types");
			if(associations != null && associations.size() > 0) {
				for(AssociationSetting assoc: associations) {
					sgLogger.debug(Constants.Format.INDENT_STRING + assoc.toString());
				}
			}
			sgLogger.debug("List of current states:");
			for(Type type: states.keySet()) {
				sgLogger.debug(type.getName());
			}
		}

		List<EntityType> exactTypes = new ArrayList<EntityType>();
		List<EntityType> fullTypes = new ArrayList<EntityType>();
		for (AssociationSetting assoc : associations) {
			if (assoc.getMatchType() == MatchType.TYPE) {
				Type type = shape.getType(assoc.getEntityName());
				if (type == null) {
					throw new RuntimeException(
						"Unable to get the type for class: " + assoc.getClass().getName());
				}
				if (!EntityType.class.isAssignableFrom(type.getClass())) {
					throw new RuntimeException("Can only extend an entity type");
				}
				if(assoc.isExact()) {
					exactTypes.add((EntityType)type);
				} else {
					fullTypes.add((EntityType)type);
				}
			}
		}

		for(EntityType type: fullTypes) {
			extend(type, false);
		}
		for(EntityType type: exactTypes) {
			extend(type, true);
		}

		for (AssociationSetting assoc : associations) {
			if (assoc.getMatchType() == MatchType.ABSOLUTE_PATH) {
				extend(assoc.getPathSuffix(), getRootState(), true);
			}
			else if (assoc.getMatchType() == MatchType.RELATIVE_PATH) {
				String propertyToAdd = assoc.getPathSuffix();
				for (V state : getVertices()) {
					E e = getOutEdge(state, propertyToAdd);
					if (e == null) {
						Type type = state.getType();
						if (type instanceof EntityType) {
							EntityType entityType = (EntityType)type;
							if (entityType.getProperty(propertyToAdd) != null) {
								Property property = entityType.getProperty(propertyToAdd);
								Type endType = GraphUtil.getPropertyEntityType(property, shape);

								if (!states.containsKey(endType)) {
									addVertex((V)new State(endType, false));
								}
								State end = states.get(endType);
								Edge newEdge = new Edge(propertyToAdd, state, end);
								addEdge((E)newEdge);
							}
						}
					}
				}
			}
		}

		// If the type system is not topologically ordered then we need to
		// sort the StateGraph so we can get the correct ordering for create and delete
		// operations to work correctly across aggregates
		if(ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_SKIP)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_SKIP)) {
			toposort(shape);
		}
	}
	
	/** Extend the state graph with the new type if needed and create all associations referencing this type in the state graph
	 *  Probably should be called before create(String path...)
	 * 
	 * @param additionalType to extend
	 * @param isExact true if extending only by an exact type
	 */
	public void extend(EntityType additionalType, boolean isExact) {
		if(this.states.containsKey(additionalType)) {
			logger.warn("Already includes the type being extended: " + additionalType.getName());
		}
		
		if(additionalType.isDataType()) {
			throw new RuntimeException("Type " + additionalType.getName() + " has to be an entity type");
		}

		StateGraph.Scope scope = isExact ? Scope.TYPE_GRAPH : Scope.FULL_GRAPH;

		StateGraph<State, Edge<State>> addendum = shape.getView(additionalType)
			.getTypeGraph(additionalType, scope)
			.copy((Map<Type, State>)this.states);

		merge(addendum);
	}

	/**
	 * Enlarges the current StateGraph with another StateGraph.
	 *
	 * @param other stategraph that will be merged with the current StateGraph
	 */
	public void merge(StateGraph<State, Edge<State>> other) {
		// We pass in the current StateGraph's states so the common states between the two entities
		// are not duplicated when the copy is made
		StateGraph<State, Edge<State>> addendum = other.copy((Map<Type, State>) this.states);

		// Link any missing edges from the states of the current StateGraph to the states
		// in the other StateGraph
		link(addendum);

		// Copy the edges from the other StateGraph. This will also copy any additional states.
		// This in essence combines the two graphs
		// We need to copy only the out-edges from the states that are linked from addendum
		// This step handles the inheritance edges
		for(State addendumState: addendum.getStates().values()) {
			addVertex((V)addendumState);
			for (Edge<State> e : addendum.getOutEdges(addendumState)) {
				addEdge((E)e);
			}
		}
	}

	private void link(StateGraph<State, Edge<State>> addendum) {
		// create 2 maps keyed by type name and value state for each StateGraph to do the linking
		Map<String, State> propertyStateMap = new HashMap<>();
		Map<String, State> addendumPropertyStateMap = new HashMap<>();
		for(State state: this.states.values()) {
			propertyStateMap.put(state.getType().getName(), state);
		}
		for(State state: addendum.getStates().values()) {
			if(!propertyStateMap.containsKey(state.getType().getName())) {
				addendumPropertyStateMap.put(state.getType().getName(), state);
			}
		}

		// If all the states are already captured in the current StateGraph then there is
		// nothing to extend
		if(addendumPropertyStateMap.size() == 0) {
			return;
		}

		// Go through all the states in the StateGraph and add any edges for properties
		// referring to the states in the addendum
		for(State fromState: propertyStateMap.values()) {
			for(Property property: fromState.getType().getProperties()) {
				Type propertyType = GraphUtil.getPropertyEntityType(property, shape);
				if(addendumPropertyStateMap.containsKey(propertyType.getName())) {
					State toState = addendumPropertyStateMap.get(propertyType.getName());
					addEdge((E) new Edge(property.getName(), fromState, toState, true));
				}
			}
		}

		// Do the reverse, go through all the states in the addendum StateGraph and
		// add any edges for properties referring to the states in the current StateGraph
		for(State fromState: addendum.states.values()) {
			for(Property property: fromState.getType().getProperties()) {
				Type propertyType = GraphUtil.getPropertyEntityType(property, shape);
				if(propertyStateMap.containsKey(propertyType.getName())) {
					State toState = propertyStateMap.get(propertyType.getName());
					addEdge((E) new Edge(property.getName(), fromState, toState, true));
				}
			}
		}
	}

	@Override
	public void extend (String path, V current, boolean initialize) {

		if(path == null || "".equals(path.trim())) {
			return; // terminating condition
		}

		// Is this an attribute
		if(current.getAttributes().contains(path)) {
			return;
		}

		String attribute = State.getNextAttr(path);
		E t = getOutEdge(current, attribute);
		
		// Not in the current state graph, let us find and add it
		if(t == null) {
			Property childProperty = current.getType().getProperty(attribute);
			if(childProperty == null) {
				logger.error("Unable to add unknown attribute to state graph: " + attribute + " to state: " + current.getType().getName() + ". Does this attribute belong to a subtype?");
				return;
			}
			Type propertyType = GraphUtil.getPropertyEntityType(childProperty, shape);
			if(propertyType.isDataType()) {
				// This is an attribute of this state
				current.addAttribute(childProperty.getName());
				return;
			} else {
				// check if a new state has to be added to the state graph
				V end = states.get(propertyType);
				if(end == null) {			
					end = (V) new State(propertyType, false); 
					if(initialize) {
						end.setAttributes( ((EntityType)propertyType).getInitializedProperties());
					}
					addVertex(end);
				}

				// add the transition
				t = (E) new Edge<State>(childProperty.getName(), current, end, true);
				addEdge(t);
			}
		}

		extend(State.getRemaining(path), t.getEnd(), initialize);
	}

	public void scopeStart(State state) {

		if(state.isInScope())
			return;

		state.setInScope(true);
		for(E transition: getInEdges((V) state)) {
			scopeStart(transition.getStart());
		}
	}
	
	public Expression getExpression(State state) {

		Expression result = null;
		Expression previous = null;
		for(E transition: this.getOutEdges((V) state)) {
			if(!transition.getEnd().isInScope())
				continue;

			if(previous != null) { // We have more than one
				if(result == null)
					result = new UnionExpression(previous);
				else
					((UnionExpression)result).addAlternate(previous);
			}

			Expression exp = LiteralExpression.instance(transition);
			previous = new TypedExpression(exp, transition.getEnd().getType());
		}

		if(result != null || state.isFinishState()) {
			if(result == null) {
				if(previous != null)
					result = new UnionExpression(previous);
			} else
				((UnionExpression)result).addAlternate(previous);

			if(result != null) {
				if(state.isFinishState())
					((UnionExpression)result).addAlternate(LiteralExpression.EMPTY_STRING);

				if(((UnionExpression)result).getChildren().size() == 1)
					result = ((UnionExpression)result).getChildren().iterator().next();
				else
					result = ((UnionExpression)result).consolidateTypes();
			} else
				result = LiteralExpression.EMPTY_STRING;
		} else {
			if(state.isFinishState()) {
				if(previous != null) {
					result = new UnionExpression(previous);
					((UnionExpression)result).addAlternate(LiteralExpression.EMPTY_STRING);
					result = ((UnionExpression)result).consolidateTypes();
				} else
					return LiteralExpression.EMPTY_STRING;
			} else 
				result = previous;
		}

		return result;
	}

	/**
	 * Renumber the vertices based on topological order
	 * Cascaded relationships are reversed because it represents IS_DEPENDED_BY and
	 * required relationships models IS_DEPENDED_ON
	 */
	@Override
	public List<V> toposort(Shape shape) {

	    // Reverse each edge only once
	    Set<E> edgesToReverse = new HashSet<E>();

		// Go through all the types and reverse the cascaded relationships
		for(V state: getVertices()) {

			if(outTransitions.get(state) != null) {
				Type type = state.getType();
				Set<E> edges = new HashSet<>(outTransitions.get(state).values());
				for(E edge: edges) {
					Property p = type.getProperty(edge.getName());

					// Foreign keys cannot typically be enforced on a containment
					// relationship, especially on abstract entity types.
					// So we will not consider their ordering unless they are required
					// Only TO_ONE required relationships are considered as
					// TO_MANY required relationship cannot be enforced on the DB side
					if(p != null) {
					    if(p.isNullable() || p.isMany()) {
					        unlinkEdge(outTransitions.get(state).get(p.getName()));
					    } else if(p.isContainment()) {
					        edgesToReverse.add(edge);
					    }
					}
				}
			}

			// Handle inheritance edges, these need to be reversed
			if(getOutEdges(state) != null) {
				for(E edge: getOutEdges(state)) {
					// Is this an inheritance edge
					if(edge.isUnlabelled()) {
						edgesToReverse.add(edge);
					}
				}
			}
		}

		// perform the reverse
		for(E edge: edgesToReverse) {
			reverseEdge(edge);
		}

		// Remove all self loops
		unlinkSelfLoops();
		
		List<V> sorted = super.toposort(shape);
		if(sgLogger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder();
			for(int i = START; i < getVertices().size()+START; i++) {
				sb.append(i + ": " + getVertex(i).getName() + "\r\n");
			}
			sgLogger.debug(sb.toString());
		}
		
		// renumber the vertices
		renumber(sorted);

		if (ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_VISUAL)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_VISUAL)) {
			if(getEdges().size() > 0) {
				printEntityOrder();
			}
			Settings settings = new Settings();
			settings.setGraphFileName(
				"ApplicationStateGraph_Topological" + (shape != null ? shape.getName() : "")
					+ ".dot");
			generateVisual(settings);
		}

		restore();

		return sorted;
	}

	@Override
	public E getReversedEdge(E edge) {
		return (E)edge.reverse();
	}

	public void orderTypes() {
		for(V state: getVertices()) {
			int order = getId(state);
			Type type = state.getType();
			
			if(!EntityType.class.isAssignableFrom(type.getClass())) {
				throw new RuntimeException("Trying to set order on an non-entity type");
			}
			((EntityType)type).setOrder(order);
		}
	}

	public void printEntityOrder () {
		Map<Integer, String> map = new TreeMap<Integer, String>();

		for(V state: getVertices()) {
			int order = getId(state);
			Type type = state.getType();

			if(type instanceof EntityType && !((EntityType)type).isEmbedded()) {
				map.put(order, type.getName());
			}
		}

		// Print in sorted order
		System.out.println(String.format("******* TOPOLOGICAL ORDER [%s] *******", shape.getName()));
		Set<Map.Entry<Integer, String>> entrySet = map.entrySet();
		Iterator<Map.Entry<Integer, String>> iter = entrySet.iterator();
		while(iter.hasNext()) {
			Map.Entry<Integer, String> entry = iter.next();
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
		System.out.println("******* END [TOPOLOGICAL ORDER] *******");
	}
	
	public void populateEdges(Shape shape) {
		// Go through every state and add the edges
		try {
			for (V state : getVertices()) {
				if (!state.getType().isDataType()) {
					for (Property p : (state.getType()).getProperties()) {
						Type propertyType = GraphUtil.getPropertyEntityType(p, shape);
						//sgLogger.debug("checking edge " + p.getName() + " of type " + propertyType.getName() );
						if (states.containsKey(propertyType)) {
							Edge<State> edge = new Edge<State>(
								p.getName(),
								state,
								states.get(propertyType));

							sgLogger.debug(
								"Adding edge " + p.getName() + " to type "
									+ state.getType().getName());
							addEdge((E)edge);
						}
					}
				}
			}
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
	}
	
	public boolean supportsDynamicUpdate() {
		for(V state: getVertices()) {
			EntityType entityType = (EntityType)state.getType();

			// We are only interested in concrete types
			if(!entityType.supportsDynamicUpdate()) {
				if(entityType.isAbstract() || entityType.getSubtypes().size() > 0) {
					continue;
				}
				return false;
			}
		}
		
		return true;
	}
	
	protected boolean hasSubStates(V vertex) {
		for(E edge: getOutEdges(vertex)) {
			if(edge.isUnlabelled() ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets a list of all the subtype states
	 *
	 * @param vertex the root of the state inheritance hierarchy
	 * @return a list of all states that are the descendants of the type of the input vertex
	 */
	protected List<State> getSubStates(V vertex) {
		List<State> result = new ArrayList<State>();

		for(EntityType entityType: ((EntityType)vertex.getType()).getSubtypes()) {
			State state = getVertex(entityType);
			if(state != null) {
				result.add(state);
			}
		}

		return result;
	}

	public static interface ContextAware {
		Object getContext();

		void setContext(Object ctx);
	}

	public static class ObjectGenerationVisitor implements ContextAware
	{
		private Map<JSONObject, State> objectStateMap = new HashMap<JSONObject, State>();
		private Settings settings;
		private Property property;
		private EntityType sourceEntityType;
		private JSONObject parent;
		private int sequenceNo;
		private JSONObject root;
		private StateGraph stateGraph;
		private Object context; // used for passing data, for example string length
		private Map<Integer, Object> additionalContext; // any additional context is set here
		private String relationshipName; // Allows multiple generators to be set for a property

		public ObjectGenerationVisitor (Map<JSONObject, State> objectStateMap, Settings settings, StateGraph stateGraph) {
			this.objectStateMap = objectStateMap;
			this.settings = settings;
			this.stateGraph = stateGraph;
			this.additionalContext = new HashMap<>();
		}

		public boolean hasReachedLimit() {
			boolean result = false;

			// Check limits
			// NOTE: if using a generator to share objects, then
			// the resulting object graph will be smaller in the number of vertices since
			// the objects get shared.
			if (objectStateMap.size() > settings.getEntitySize().size()) {
				result = true;
			}

			return result;
		}

		public JSONObject getRoot ()
		{
			return root;
		}

		public void setRoot (JSONObject root)
		{
			this.root = root;
		}

		public int getSize() {
			return objectStateMap.size();
		}

		public void setProperty (Property property)
		{
			this.property = property;
		}

		public Property getProperty() {
			return this.property;
		}

		public void setSourceEntityType (EntityType entityType)
		{
			this.sourceEntityType = entityType;
		}

		public EntityType getSourceEntityType () {
			return this.sourceEntityType;
		}

		public String getRelationshipName ()
		{
			if(this.relationshipName != null && !"".equals(this.relationshipName)) {
				return this.relationshipName;
			}

			if (this.sourceEntityType == null || this.getProperty() == null) {
				return AbstractProperty.TYPE_GENERATOR;
			}

			return
				Constants.XOR.getRelationshipName(
					getSourceEntityType(),
					getProperty());
		}

		public void setRelationshipName (String name) {
			this.relationshipName = name;
		}

		public int getSequenceNo ()
		{
			return sequenceNo;
		}

		public void setSequenceNo (int sequenceNo)
		{
			this.sequenceNo = sequenceNo;
		}

		public JSONObject getParent ()
		{
			return parent;
		}

		public void setParent (JSONObject parent)
		{
			this.parent = parent;
		}

		public Settings getSettings() {
			return this.settings;
		}

		public StateGraph getStateGraph() {
			return this.stateGraph;
		}

		public Object getContext ()
		{
			return context;
		}

		public void setContext (Object context)
		{
			this.context = context;
			if(logger.isDebugEnabled())  {
			    logger.debug(String.format("Setting context[%s] on the visitor", context==null?"null":context.getClass().getName()));
			}
		}
		
		public void setContext(int i, Object value) {
		    this.additionalContext.put(i, value);
		}
		
		public Object getContext(int i) {
		    return this.additionalContext.get(i);
		}
	}

	public static Object getKeyValue(JSONObject object, String keyPath) {
		Object result = null;

		do {
			String root = Settings.getRootName(keyPath);
			keyPath = Settings.getNext(keyPath);

			if(object.has(root) && object.get(root) != null) {
				if(keyPath != null) {
					if(object.get(root) instanceof JSONObject) {
						object = (JSONObject)object.get(root);
					} else {
						object = null;
					}
				} else {
					result = object.get(root);
				}
			} else {
				object = null;
			}
		} while (keyPath != null && object != null);

		return result;
	}


	public static void setKeyValue(JSONObject object, String keyPath, Object value) {
		do {
			String root = Settings.getRootName(keyPath);
			keyPath = Settings.getNext(keyPath);

			if(object.has(root) && object.get(root) != null) {
				if(keyPath != null) {
					if(object.get(root) instanceof JSONObject) {
						object = (JSONObject)object.get(root);
					} else {
						// We do not support other types e.g., arrays
						throw new RuntimeException("Expecting a JSONObject instance");
					}
				} else {
					object.put(root, value);
				}
			} else {
				if(keyPath != null) {
					JSONObject embeddedObject = new JSONObject();
					object.put(root, embeddedObject);
					object = embeddedObject;
				} else {
					object.put(root, value);
				}
			}
		} while (keyPath != null && object != null);
	}

	/**
	 * Creates a random object graph instance based on the given state graph.
	 * Useful for populating data.
	 */
	public static class RandomInstance {
		private StateGraph stateGraph;
		private Settings settings;
		private Set<JSONObject> visited;
		private Map<Type, List<JSONObject>> typeObjectMap;
		private Map<JSONObject, State> objectStateMap;
		private Map<JSONObject, State> embeddedObjectStateMap;
		private Map<EntityKey, JSONObject> entityKeyMap;
		private ObjectGenerationVisitor visitor;
		private Queue<JSONObject> q;

		public RandomInstance(Settings settings, StateGraph stateGraph) {
			this.settings = settings;
			this.stateGraph = stateGraph;
		}

		private JSONObject addObject(State state, JSONObject object, String objectPath) {

			// We use the Natural key to link to existing objects and hence form a graph
			// We cannot depend on the surrogate key as that is not user entered
			Map<String, Object> naturalKey = new HashMap<String, Object>();
			EntityType type = (EntityType)state.getType();
			if(type.getNaturalKey() != null) {
				for(String key: type.getExpandedNaturalKey()) {
					Object keyValue = getKeyValue(object, key);
					if(keyValue == null) {
						// We do not support partial natural keys
						naturalKey = new HashMap();
						break;
					}
					naturalKey.put(key, keyValue);
				}
			}

			boolean exists = false;
			if(naturalKey.size() > 0) {
				EntityKey entityKey = new NaturalEntityKey(naturalKey, type.getName());
				if(!entityKeyMap.containsKey(entityKey)) {
					entityKeyMap.put(entityKey, object);
				} else {
					exists = true;
					// Get the existing object
					object = entityKeyMap.get(entityKey);
				}
			}

			// For keys that are based off other entities, we may not populate it due to BFS.
			// But this is ok, as the XOR engine can properly figure them out.
			if(!exists) {
				// Embedded objects are not considered in the object graph limit
				if(!type.isEmbedded()) {
					List<JSONObject> list = typeObjectMap.get(type);
					if (list == null) {
						list = new ArrayList<JSONObject>();
						typeObjectMap.put(type, list);
					}
					list.add(object);
					objectStateMap.put(object, state);
				} else {
					embeddedObjectStateMap.put(object, state);
				}
				q.add(object);

				if (objectPath != null) {
					object.put(Constants.XOR.GEN_PATH, objectPath);
				}
			}

			return object;
		}

		private EntityType getEntityType(JSONObject jsonObject) {
			if(!jsonObject.has(Constants.XOR.TYPE)) {
				throw new IllegalStateException("Generator entity should have the type information");
			}

			return (EntityType)stateGraph.shape.getType(
				jsonObject.getString(
					Constants.XOR.TYPE));
		}

		private void init() {
			//Set all nodes to "not visited". This is by having the visited set as empty
			visited = new HashSet<JSONObject>();
			typeObjectMap = new HashMap<Type, List<JSONObject>>();
			objectStateMap = new ConcurrentHashMap<JSONObject, State>();
			embeddedObjectStateMap = new HashMap<JSONObject, State>();
			entityKeyMap = new HashMap<>();
			visitor = new ObjectGenerationVisitor(objectStateMap, settings, stateGraph);
			q = new LinkedList();
		}

		/**
		 * Generates a random object graph using JSON objects.
		 *
		 * @return the generated object graph
		 */
		public JSONObject generateObjectGraph ()
		{
		/*
		 * Use BFS to traverse the graph since we want to give all states an opportunity to participate in the object graph.
		 * Keep a map between the state and the list of JSONObjects
		 * When traversing an ASSOCIATION, randomly choose a JSONObject based on the target state.
		 * If the ASSOCIATION is a COMPOSITION, then create a new JSONObject.
		 *
		 * Assume no other behavior on a relationship.
		 */

			init();

			JSONObject result = (JSONObject)((EntityType)stateGraph.getRootState().getType()).generate(
				settings,
				null,
				null,
				null,
				this.visitor);

			// Add the root entity first
			addObject(stateGraph.getRootState(), result, null);
			visitor.setRoot(result);

			// Needed to flush the remaining objects in the queue
			boolean flush = false;

			while (!q.isEmpty()) {
				flush = visitor.hasReachedLimit();

				JSONObject entity = q.remove();
				if (!visited.contains(entity)) {
					// Mark as visited
					visited.add(entity);
					String path = entity.has(Constants.XOR.GEN_PATH) ?
						entity.getString(Constants.XOR.GEN_PATH) : null;

					Type entityType;
					State parentState;
					if(objectStateMap.containsKey(entity)) {
						parentState = objectStateMap.get(entity);
					} else if(embeddedObjectStateMap.containsKey(entity)) {
						parentState = embeddedObjectStateMap.get(entity);
					} else {
						throw new RuntimeException("Unable to find entity - check if it was added using addObject() method");
					}
					entityType = parentState.getType();

					for (Property property : entityType.getProperties()) {

						// target type
						ExtendedProperty extendedProperty = (ExtendedProperty)property;
						if (extendedProperty.isDataType()) {
							continue;
						}

						Edge edge = stateGraph.getOutEdge(
							parentState,
							extendedProperty.getName());
						State childState = null;
						if(edge != null) {
							childState = (State)edge.getEnd();
							if(stateGraph.hasSubStates(childState)) {
								Generator gen = extendedProperty.getGenerator();
								if(gen == null) {
									// Needed for inheritance handling
									gen = new DefaultGenerator(null);
									extendedProperty.setGenerator(gen);
								}
								EntityType childType = (EntityType)GraphUtil.getPropertyEntityType(extendedProperty, stateGraph.shape);
								childType = gen.getSubType(childType, stateGraph);
								childState = stateGraph.getVertex(childType);
							}
						}

						// Update visitor with current path
						String objectPath = Constants.XOR.walkDown(path, property);

						// Is the state out of scope
						if (childState == null) {
							if (!property.isNullable() && !property.isMany()) {
								(new RuntimeException(
									"Skipped type is a required property and needs to be part of the view: "
										+ property.getContainingType().getName() + "#"
										+ property.getName() + ", type: "
										+ property.getType().getName()
										+ ", path: " + objectPath)).printStackTrace();
							}
							continue;
						}

						Type targetEntityType = childState.getType();
						Type targetType = (extendedProperty.isMany()) ? GraphUtil.getPropertyType(extendedProperty, stateGraph.shape) : targetEntityType;

						logger.info("Path: " + objectPath + ", type: " + targetType.getName());

						visitor.setProperty(property);
						visitor.setSourceEntityType((EntityType)entityType);
						visitor.setParent(entity);

						Object target = ((BasicType)targetType).generate(
							settings,
							extendedProperty,
							entity,
							typeObjectMap.get(targetType),
							visitor);

						// Add this object only if it is a required relationship
						if (target instanceof JSONObject && (!flush || !extendedProperty.isNullable())) {
							target = addObject(childState, (JSONObject)target, objectPath);
							entity.put(property.getName(), target);
						}
						else if (target instanceof JSONArray && !flush) {
							JSONArray jsonArray = new JSONArray();
							for (int i = 0, j = 0; i < ((JSONArray)target).length(); i++) {
								JSONObject jsonObject = (JSONObject)((JSONArray)target).get(i);

								// Add it to the right state
								State collectionElementState = stateGraph.getVertex(
									getEntityType(jsonObject));

								// Is the state out of scope
								if (collectionElementState == null) {
									continue;
								}

								jsonArray.put(
									j++,
									addObject(collectionElementState, jsonObject, objectPath));
							}
							if(jsonArray.length() > 0) {
								entity.put(property.getName(), jsonArray);
							}
						}
					}
				}
			}

			return result;
		}
	}

	/**
	 * Generates a random object graph using JSON objects.
	 *
	 * @param settings used to control the size of the generated object graph
	 * @return the generated object graph
	 */
	public JSONObject generateObjectGraph (Settings settings) {
		RandomInstance ri = new RandomInstance(settings, this);
		return ri.generateObjectGraph();
	}

	public void generateVisual (Settings settings) {
		settings.exportGraph(this);
	}

	@Override
	protected void writeGMLEdges(BufferedWriter writer) throws IOException
	{
		for(Map.Entry<String, E> entry: getEdgeMap().entrySet()) {
			E edge = entry.getValue();

			writer.write("\tedge\n\t[\n");
			writer.write("\t\tsource " + getId((V)edge.getStart()) + "\n");
			writer.write("\t\ttarget " + getId((V)edge.getEnd()) + "\n");
			writer.write("\t\tlabel \"" + entry.getKey() + "\"\n");
			writer.write("\t]\n");
		}
	}

	private String getLabel(V vertex) {
		Type type = vertex.getType();

		// Type name
		StringBuilder label = new StringBuilder();
		label.append(Settings.getBaseName(type.getName()));

		if(!vertex.isReference()) {
			label.append("|");

			if (type instanceof EntityType) {
				// List the natural key parts
				EntityType entityType = (EntityType)type;
				StringBuilder keyString = new StringBuilder();
				if (entityType.getNaturalKey() != null) {
					for (String key : entityType.getNaturalKey()) {
						if (keyString.length() > 0) {
							keyString.append("\\n");
						}
						keyString.append(key);
					}
					label.append(keyString);
				}

				// Check for required entity types, as this affects stategraph scope
				// Simple required types do not have to be listed here
				StringBuilder requiredEntities = new StringBuilder();
				if(entityType.getProperties() != null) {
					for (Property property : entityType.getProperties()) {
						if (!property.isNullable()) {
							Type requiredEntityType = GraphUtil.getPropertyEntityType(
								property,
								getShape());
							if (requiredEntityType instanceof EntityType) {
								if (requiredEntities.length() > 0) {
									requiredEntities.append("\\n");
								}
								requiredEntities.append(
									property.getName() + " : " + Settings.getBaseName(
										requiredEntityType.getName()));
							}
						}
					}
				}

				if (requiredEntities.length() > 0) {
					label.append("|").append(requiredEntities);
				}
			}
		}

		return vertex.isReference() ? label.toString() : ("{" + label.toString() + "}");
	}

	protected void writeDOTVertices(BufferedWriter writer) throws IOException
	{
		Iterator vertexIter = getConnectedVertices().iterator();
		while (vertexIter.hasNext()) {
			V vertex = (V)vertexIter.next();

			String referenceStyle = vertex.isReference() ? "shape=component, " : "";
			String abstractStyle = vertex.getType().isAbstract() ? "style=filled,fillcolor=white," : "";
			writer.write(String.format("  %s[%s%slabel = \"%s\"]\n", getId(vertex), abstractStyle, referenceStyle, getLabel(vertex)));
		}
	}

	protected String getGraphName() {
		return Settings.getBaseName(this.getClass().getName());
	}

	@Override
	protected void writeGraphvizDotHeader(BufferedWriter writer) throws IOException
	{
		writer.write("  node[shape=record,style=filled,fillcolor=burlywood1,minlen=2]\n"); // ivory is also a good option
	}

	@Override
	public void writeGraphvizDot(BufferedWriter writer) throws IOException
	{
		// write the vertices
		writeDOTVertices(writer);

		// Write the edges
		// Do not constrain all the types that have been explicitly expanded
		for(Map.Entry<String, E> entry: getEdgeMap().entrySet()) {
			E edge = entry.getValue();

			Type startType = edge.getStart().getType();
			Type endType = edge.getEnd().getType();
			Property property = startType.getProperty(edge.getName());
			
			String edgeLabels = "";
			String edgeLabelsBack = "";
			if(property != null) {
			    String tailLabel = property.isContainment() ? "1" : "0..*";
			    String headLabel = property.isNullable() ? (property.isMany() ? "0..*" : "0..1") : (property.isMany() ? "1..*" : "1..1");
			    edgeLabels = String.format(", headlabel=\"%s\", taillabel=\"%s\"", headLabel, tailLabel);
			    edgeLabelsBack = String.format(", headlabel=\"%s\", taillabel=\"%s\"", tailLabel, headLabel);
			}

			boolean constrain = true;
			if(getRootState() == edge.getStart() && !(endType instanceof EntityType && ((EntityType)endType).isEmbedded())) {
				constrain = false;
			}

			StringBuilder result = new StringBuilder("  " + getId((V)edge.getStart()) + " -> " + getId((V)edge.getEnd()));
			result.append("[");

			if(!constrain) {
				result.append("constraint=false, ");
			}

			// Inheritance edge
			if(edge.getName() == null || DFAtoNFA.UNLABELLED.equals(edge.getName())) {
				result.append("dir=back, arrowtail=empty, weight=2");
			}
			// Aggregation edge
			else if(property != null && property.isContainment()) {
				result.append("dir=back, arrowtail=diamond, style=dashed");
                edgeLabels = edgeLabelsBack;				
			}
			// Association edge
			else {
			    boolean isMany = (property != null && property.isMany()) ? true : false;
				result.append("arrowhead=open");
				result.append(isMany ? "open" : ""); // double headed arrow
				result.append(", style=dashed");
			}

			// Required edge is marked in a different color
			if(property != null && !property.isNullable()) {
				result.append(", color=red");
			}

			// Edge labels should be configurable
			//result.append(edgeLabels);
			result.append("]\n");

			writer.write(result.toString());
		}
	}

	protected Map<String, E> getEdgeMap() {
		Map<String, E> result = new HashMap<>();
		Integer dup = 0;
		Integer unknown = 0;
		Iterator edgeIter = getEdges().iterator();
		while(edgeIter.hasNext()) {
			E edge = (E)edgeIter.next();
			String edgeName = edge.getName();
			edgeName = (edgeName == null) ? (unknown++).toString() : edgeName;
			edgeName += (edge.getEndCardinality() == null) ? "" : " [" + edge.getEndCardinality() + "]";

			if (result.containsKey(edgeName)) {
				edgeName += "." + (dup++).toString();
			}

			result.put(edgeName, edge);
		}

		return result;
	}

	@Override
	public Graph getGraph() {

		Iterator vertexIter = getVertices().iterator();
		Graph<V, String> g = new SparseMultigraph<V, String>();
		while(vertexIter.hasNext()) {
			V vertex = (V)vertexIter.next();
			g.addVertex(vertex);
		}

		for(Map.Entry<String, E> entry: getEdgeMap().entrySet()) {
			E edge = entry.getValue();
			String edgeName = entry.getKey();

			g.addEdge(edgeName, edge.getStart(), edge.getEnd(), EdgeType.DIRECTED);
		}

		return g;
	}

	@Override
	public String toString() {

		StringBuilder result = new StringBuilder("****** States in the StateGraph *******\n");
		for(State s: this.states.values()) {
			result.append("   " + s.getName() + "\n");
		}

		return result.toString();
	}
}
