package tools.xor.util.graph;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
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
import tools.xor.view.AggregateView;
import tools.xor.view.QueryView;
import tools.xor.view.QueryViewProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

public class StateGraph<V extends State, E extends Edge<V>> extends DirectedSparseGraph<V, E> implements TypeGraph<V, E> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger sgLogger = LogManager.getLogger(Constants.Log.STATE_GRAPH);

	public  static final String EMPTY_EDGE = "";
	private static final String ALL = "_all_";

	private Type root; // aggregate rooted at this type
	private Map<Type, V> states = new HashMap<Type, V>();
	private Map<V, Map<String, E>> outTransitions = new HashMap<V, Map<String, E>>(); 
	//private Map<Type, List<Property>> attrByType = new HashMap<Type, List<Property>>();
	private Map<Type, Map<String, List<Property>>> attrByType = new HashMap<Type, Map<String, List<Property>>>();
	private Shape shape; // The shape of type system on which this state graph is based
	
	public StateGraph(Type aggregateRoot, Shape shape) {
		super();
		this.root = aggregateRoot;
		this.shape = shape;
	}
	
	public V getRootState() {
		return states.get(root);
	}

	protected Shape getShape () {
		return this.shape;
	}
	
	@Override
	public void addEdge(E edge, V start, V end) {
		if(edge.getStart() != start) {
			if(edge.getEnd() == start) {
				logger.debug("Adding a reversed edge");
			} else {
				throw new IllegalStateException("Transition object start is not the same as given start");
			}
		}
		if(edge.getEnd() != end) {
			if(edge.getStart() == end) {
				logger.debug("Adding a reversed edge");
			} else {
				throw new IllegalStateException("Transition object end is not the same as given end");
			}
		}
		
		Map<String, E> outLinks = outTransitions.get(start);
		if(outLinks == null) {
			outLinks = new HashMap<String, E>();
			outTransitions.put(start, outLinks);
		}
		outLinks.put(edge.getName(), edge);
		
		super.addEdge(edge, start, end);
	}

	@Override
	public void removeEdge(E edge) {
		Map<String, E> outLinks = outTransitions.get(edge.getStart());
		if(outLinks == null) {
			throw new IllegalStateException("Unable to find outTransition entry for edge");
		}
		outLinks.remove(edge.getName());

		super.removeEdge(edge);
	}

	@Override
	public E getOutEdge(V vertex, String name) {
		if(!outTransitions.containsKey(vertex)) {
			// vertex is not in scope
			return null;
		}

		if(outTransitions.get(vertex).containsKey(name)) {
			return outTransitions.get(vertex).get(name);
		} else {
			// The graph only has unlabelled edges from parent to child
			// and not vice-versa so we have to explicitly get the parent
			// vertex and check it

			// check parent if there is an inheritance relationship
			V parentVertex = getParentVertex(vertex);
			return parentVertex == null ? null : getOutEdge(parentVertex, name);
		}
	}

	private V getParentVertex(V vertex) {

		if(vertex.getType() != null && vertex.getType() instanceof EntityType) {
			EntityType entityType = (EntityType)vertex.getType();
			if(entityType.getSuperType() != null) {
				return getVertex(entityType.getSuperType());
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
	}

	@Override
	public void removeVertex(V vertex) {
		if(!containsVertex(vertex)) {
			return;
		}

		super.removeVertex(vertex);
		this.states.remove(vertex.getType());
	}
	
	public void addEdge(E edge) {
		addEdge(edge, edge.getStart(), edge.getEnd());
	}

	public V getVertex(Type t) {
		return states.get(t);
	}
	
	public StateGraph<V, E> getFullStateGraph() {

		for(State state: states.values()) {
			state.initDataTypes();
		}
		
		return this;
	}
	
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
	public StateGraph<V, E> copy(Map<Type, V> mergeStates) {
		
		StateGraph<V, E> result = new StateGraph<V, E>(this.root, this.shape);
		
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
			result.addVertex(newState);
		}

		for(E edge: getEdges()) {
			result.addEdge(
				(E) new Edge<State>(
					edge.getName(),
					oldNew.get(edge.getStart()),
					oldNew.get(edge.getEnd()),
					edge.isQualified()));
		}

		return result;
	}

	public List<Property> next(Type type, String propertyPath, Set<String> exactSet) {
		State state = getVertex(type);

		return next(state, propertyPath, exactSet);
	}

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
						Property p = type.getProperty(QueryViewProperty.getRootName(remaining));
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
			for (String simpleAttribute : vertex.getAttributes()) {
				result.add(vertex.getType().getProperty(simpleAttribute));
			}
		}

		// Addresses scope extension using AssociationSetting with MatchType.TYPE
		for (Edge e : getOutEdges((V)vertex)) {
			if (EMPTY_EDGE.equals(e.getName())) {
				continue;
			}
			result.add(vertex.getType().getProperty(e.getName()));
		}

		return result;
	}
	
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

	public void prune(List<AssociationSetting> associations, Shape shape) {
		for(AssociationSetting assoc: associations) {
			if (assoc.getMatchType() == MatchType.TYPE) {
				V state = states.get(shape.getType(assoc.getEntityClass()));
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
	
	/**
	 * This method is to enhance the state graph since the states are reused across other state graph entities.
	 * We cannot just rebuild a part of the state graph with new state graph if we don't account for the sharing.
	 * 
	 * @param associations new properties e.g., open properties being added to the state graph
	 * @param shape of the type being enhanced
	 */
	public void enhance(List<AssociationSetting> associations, Shape shape) {

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
				Type type = shape.getType(assoc.getEntityClass());
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
			extend(type, shape, false);
		}
		for(EntityType type: exactTypes) {
			extend(type, shape, true);
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
	 * @param shape of the type being extended
	 * @param isExact true if extending only by an exact type
	 */
	public void extend(EntityType additionalType, Shape shape, boolean isExact) {
		if(this.states.containsKey(additionalType)) {
			logger.warn("Already includes the type being extended: " + additionalType.getName());
		}
		
		if(additionalType.isDataType()) {
			throw new RuntimeException("Type " + additionalType.getName() + " has to be an entity type");
		}
		StateGraph<State, Edge<State>> addendum = shape.getView(additionalType)
			.getTypeGraph(additionalType, isExact)
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

	public void extend(String path, V current, boolean initialize) {

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
			Property childProperty = ((EntityType)current.getType()).getProperty(attribute);
			if(childProperty == null) {
				logger.error("Unable to add unknown attribute to state graph: " + attribute + " to state: " + current.getType().getName());
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
	public List<QueryView> getQueryableRegions() {
		QueryView regionRoot = new QueryView(null, null, root);
		List<QueryView> result = new LinkedList<QueryView>();
		
		processRegion(regionRoot, regionRoot, result, new HashSet<Type>());
		
		for(QueryView view: result) {
			view.initViewProperties();
		}
		
		return result;
	}
	
	private void processRegion(QueryView regionRoot, QueryView regionNode, List<QueryView> result, Set<Type> processed) {
		Type type = regionNode.getAggregateType();
		if(processed.contains(type)) {
			return;
		} else {
			processed.add(type);
		}
		
		for(Property p: type.getProperties()) {
			Type propertyType = GraphUtil.getPropertyType(p, shape);
			if(propertyType.isDataType() && !p.isMany()) {
				StringBuilder attrPath = new StringBuilder(regionNode.getAnchorPath());
				attrPath.append(Settings.PATH_DELIMITER);
				attrPath.append(p.getName());
				
				// All attributes are added to the region root
				regionRoot.addAttribute(attrPath.toString());
			} else if(p.isContainment()) {
				if(!p.isMany() ) {
					// Process a type interior to a region
					processRegion(
							regionRoot, 
							new QueryView(regionNode, p.getName(), propertyType ),
							result,
							processed);
				} else {
					// process a new region
					QueryView newRegion = new QueryView(regionNode, p.getName(), ((ExtendedProperty)p).getElementType() );
					result.add(newRegion);
					processRegion(
							newRegion,
							newRegion,
							result,
							processed);
				}
			}

		}	
	}

	/**
	 * Renumber the vertices based on topological order
	 * Cascaded relationships are reversed because it represents IS_DEPENDED_BY and
	 * required relationships models IS_DEPENDED_ON
	 */
	@Override
	public List<V> toposort(Shape shape) {

		List<E> edgesToReverse = new ArrayList<E>();

		// Go through all the types and reverse the cascaded relationships
		for(V state: getVertices()) {
			Collection<Property> properties = (shape == null) ?
				state.getType().getProperties() :
				((shape.getProperties((EntityType)state.getType()) != null) ?
					shape.getProperties((EntityType)state.getType()).values() : null);

			if( properties != null) {
				for(Property p: properties) {
					if(outTransitions.get(state) != null && outTransitions.get(state).containsKey(p.getName())) {
						if(p.isContainment()) {
							// We process them later as we don't want to be thrashing the edge
							// reversal depending upon how we traverse the graph
							edgesToReverse.add(outTransitions.get(state).get(p.getName()));
						} else if (p.isNullable()) {
							unlinkEdge(outTransitions.get(state).get(p.getName()));
						}
					}
				}
			}

			// Handle inheritance edges, these need to be reversed
			if(getOutEdges(state) != null) {
				for(E edge: getOutEdges(state)) {
					// Is this an inheritance edge
					if(DFAtoNFA.UNLABELLED.equals(edge.getName())) {
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
		System.out.println("******* TOPOLOGICAL ORDER *******");
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
					for (Property p : shape.getProperties((EntityType)state.getType()).values()) {
						Type propertyType = GraphUtil.getPropertyType(p, shape);
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
			if(DFAtoNFA.UNLABELLED.equals(edge.getName()) ) {
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

	public static class ObjectGenerationVisitor
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

		public ObjectGenerationVisitor (Map<JSONObject, State> objectStateMap, Settings settings, StateGraph stateGraph) {
			this.objectStateMap = objectStateMap;
			this.settings = settings;
			this.stateGraph = stateGraph;
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
			if (this.sourceEntityType == null || this.getProperty() == null) {
				return AbstractProperty.TYPE_GENERATOR;
			}

			return
				Constants.XOR.getRelationshipName(
					getSourceEntityType(),
					getProperty());
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
		}
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

		private Object getKeyValue(JSONObject object, String keyPath) {
			Object result = null;

			do {
				String root = QueryViewProperty.getRootName(keyPath);
				keyPath = QueryViewProperty.getNext(keyPath);

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

	@Override
	protected void writeDOTEdges(BufferedWriter writer) throws IOException
	{
		writeGraphvizDot(writer);
	}

	private String getLabel(V vertex) {
		Type type = vertex.getType();

		// Type name
		StringBuilder label = new StringBuilder();
		label.append(QueryViewProperty.getBaseName(type.getName()))
		.append("|");

		if(type instanceof EntityType) {
			// List the natural key parts
			EntityType entityType = (EntityType) type;
			StringBuilder keyString = new StringBuilder();
			if(entityType.getNaturalKey() != null) {
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
			for(Property property: entityType.getProperties()) {
				if(!property.isNullable()) {
					Type requiredEntityType = GraphUtil.getPropertyEntityType(property, getShape());
					if(requiredEntityType instanceof EntityType ) {
						if(requiredEntities.length() > 0) {
							requiredEntities.append("\\n");
						}
						requiredEntities.append(property.getName() + " : " + QueryViewProperty.getBaseName(requiredEntityType.getName()));
					}
				}
			}

			if(requiredEntities.length() > 0) {
				label.append("|").append(requiredEntities);
			}
		}

		return label.toString();
	}

	protected void writeDOTVertices(BufferedWriter writer) throws IOException
	{
		Iterator vertexIter = getConnectedVertices().iterator();
		while(vertexIter.hasNext()) {
			V vertex = (V)vertexIter.next();

			writer.write("  " + getId(vertex) + "[label = \"{" + getLabel(vertex) + "}\"]\n");
		}
	}

	protected String getGraphName() {
		return QueryViewProperty.getBaseName(this.getClass().getName());
	}

	protected void writeGraphvizDot(BufferedWriter writer) throws IOException
	{
		writer.write("  node[shape=record,style=filled,fillcolor=ivory]\n");

		// write the vertices
		writeDOTVertices(writer);

		// Write the edges
		// Do not constrain all the types that have been explicitly expanded
		for(Map.Entry<String, E> entry: getEdgeMap().entrySet()) {
			E edge = entry.getValue();

			EntityType startType = (EntityType)edge.getStart().getType();
			EntityType endType = (EntityType)edge.getEnd().getType();
			Property property = startType.getProperty(edge.getName());

			boolean constrain = true;
			if(getRootState() == edge.getStart() && !endType.isEmbedded()) {
				constrain = false;
			}

			StringBuilder result = new StringBuilder("  " + getId((V)edge.getStart()) + " -> " + getId((V)edge.getEnd()));
			result.append("[");

			if(!constrain) {
				result.append("constraint=false, ");
			}

			// Inheritance edge
			if(edge.getName() == null || "".equals(edge.getName())) {
				result.append("dir=back, arrowtail=empty, weight=2");
			}
			// Aggregation edge
			else if(property.isContainment()) {
				result.append("dir=back, arrowtail=diamond, style=dashed");
			}
			// Association edge
			else {
				result.append("arrowhead=open, style=dashed");
			}

			// Required edge is marked in a different color
			if(property != null && !property.isNullable()) {
				result.append(", color=red");
			}

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
