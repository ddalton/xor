package tools.xor.util.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.AssociationSetting;
import tools.xor.BasicType;
import tools.xor.EntitySize;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.MatchType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AggregateManager;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoRE.Expression;
import tools.xor.util.DFAtoRE.LiteralExpression;
import tools.xor.util.DFAtoRE.TypedExpression;
import tools.xor.util.DFAtoRE.UnionExpression;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.State;
import tools.xor.view.QueryView;

public class StateGraph<V extends State, E extends Edge<V>> extends DirectedSparseGraph<V, E> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger sgLogger = LogManager.getLogger(Constants.Log.STATE_GRAPH);

	private static final String EMPTY_EDGE = "";

	private Type root; // aggregate rooted at this type
	private Map<Type, V> states = new HashMap<Type, V>();
	private Map<V, Map<String, E>> outTransitions = new HashMap<V, Map<String, E>>(); 
	private Map<Type, List<Property>> attrByType = new HashMap<Type, List<Property>>();
	
	public StateGraph(Type aggregateRoot) {
		super();
		this.root = aggregateRoot; 
	}
	
	public V getRootState() {
		return states.get(root);
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
	
	public E getOutEdge(V vertex, String name) {
		if(!outTransitions.containsKey(vertex)) {
			return null;
		}
		
		return outTransitions.get(vertex).get(name);
	}

	@Override
	public void addVertex(V vertex) {
		super.addVertex(vertex);
		this.states.put(vertex.getType(), vertex);
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
	
	public Map<Type, V> getStates() {
		return this.states;
	}
	
	/**
	 * 
	 * @param mergeStates map of states to be merged. Needs to be the original states map and not a copy, since it is needed to properly remove duplicates
	 * @return merged graph
	 */
	public StateGraph<V, E> copy(Map<Type, V> mergeStates) {
		
		StateGraph<V, E> result = new StateGraph<V, E>(this.root);
		
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
	
	/**
	 * Get the set of child attributes anchored at the state
	 * referenced by pathAnchor
	 * @param type of the vertex
	 * @return list of properties
	 */
	public List<Property> next(Type type) {
		if(attrByType.containsKey(type)) {
			return attrByType.get(type);
		}
		
		V vertex = getVertex(type);
		if(vertex == null) {
			for(Map.Entry<Type, V> entry: states.entrySet()) {
				System.out.println("The type " + entry.getKey() + " has vertex " + entry.getValue() + " with name " + entry.getKey().getName());
			}
			System.out.println("Type: " + type + ", Cannot find the vertex of type " + type.getName() + " in the state graph of entity " + getRootState().getName());
			
			throw new IllegalArgumentException("Type: " + type + ", Cannot find the vertex of type " + type.getName() + " in the state graph of entity " + getRootState().getName());
		} 
		
		List<Property> result = new ArrayList<Property>();
		
		// Add simple attributes
		for(String simpleAttribute: vertex.getAttributes()) {
			result.add(vertex.getType().getProperty(simpleAttribute));
		}

		// Add entity attributes
		for(Edge e: getOutEdges(vertex)) {
			if(EMPTY_EDGE.equals(e.getName())) {
				continue;
			}
			result.add(vertex.getType().getProperty(e.getName()));
		}
		attrByType.put(type, result);
		
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
	
	/**
	 * This method is to enhance the state graph since the states are reused across other state graph entities.
	 * We cannot just rebuild a part of the state graph with new state graph if we don't account for the sharing.
	 * 
	 * @param associations new properties e.g., open properties being added to the state graph
	 * @param am used to obtain the type service 
	 */
	public void enhance(List<AssociationSetting> associations, AggregateManager am) {

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

		List<EntityType> types = new ArrayList<EntityType>();
		for(AssociationSetting assoc: associations) {
			if(assoc.getMatchType() == MatchType.TYPE) {
				Type type = am.getDAS().getType(assoc.getEntityClass());
				if(type == null) {
					throw new RuntimeException("Unable to get the type for class: " + assoc.getClass().getName());
				}
				if(!EntityType.class.isAssignableFrom(type.getClass())) {
					throw new RuntimeException("Can only extend an entity type");
				}
				types.add((EntityType)type);
			} else {
				extend(assoc.getPathSuffix(), getRootState(), true);
			}
		}

		for(EntityType type: types) {
			//System.out.println("Type: " + type.getName());
			extend(type, am);
		}

	}
	
	/** Extend the state graph with the new type if needed and create all associations referencing this type in the state graph
	 *  Probably should be called before create(String path...)
	 * 
	 * @param additionalType to extend
	 * @param am AggregateManager
	 */
	public void extend(EntityType additionalType, AggregateManager am) {
		if(this.states.containsKey(additionalType)) {
			logger.warn("Already includes the type being extended: " + additionalType.getName());
		}
		
		if(additionalType.isDataType()) {
			throw new RuntimeException("Type " + additionalType.getName() + " has to be an entity type");
		}
		StateGraph<State, Edge<State>> addendum = am.getDAS().getView((EntityType) additionalType)
				.getStateGraph((EntityType) additionalType)
				.copy((Map<Type, State>) this.states);
		
		// We pass in the current type's state graph so the common states between the two entities
		// are not duplicated
		// We make a copy, since we want to later
		// iterate only through the states of the original map below
		Map<Type, State> oldVertices = new HashMap<Type, State>(this.states);

		if(sgLogger.isDebugEnabled()) {
			sgLogger.debug("Enhancing type: " + 
					additionalType.getName() + "[" + additionalType + "] with type: " + addendum.root.getName() + "[" + addendum.root + "]");
		}

		for(State state: oldVertices.values()) {
			sgLogger.debug(Constants.Format.getIndentString(1) + "Processing state: " + state.getType().getName());
			link(state, addendum.getRootState());
			
			// Ensure the state for the added type is linked to the existing states
			link(addendum.getRootState(), state);
		}
	}

	// TODO: this does not handle inheritance. @see DFAtoNFA
	private void link(State from, State to) {
		for(Property property: from.getType().getProperties()) {
			Type propertyType = GraphUtil.getPropertyType(property);
			sgLogger.debug(Constants.Format.getIndentString(2) + "Processing property: " + property.getName() + ", property type: " + propertyType.getName());
			if(propertyType.getName().equals(to.getType().getName()) ) {
				// add the transition
				if(getOutEdge((V) from, property.getName()) == null) {
					sgLogger.debug(Constants.Format.getIndentString(3) + "Adding association for property: " + property.getName() + " and type: " + to.getType().getName());
					addEdge((E) new Edge(property.getName(), from, to, true));
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
			Type propertyType = GraphUtil.getPropertyType(childProperty);
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
		for(E transition: getInEdges((V) state))
			scopeStart(transition.getStart());
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
			if(p.getType().isDataType() && !p.isMany()) {
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
							new QueryView(regionNode, p.getName(), p.getType() ),
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
	 * Cascaded relationships are reversed because it represents
	 * IS_DEPENDED_BY and required relationships model
	 * IS_DEPENDED_ON
	 */
	@Override
	public List<V> toposort() {
		// Go through all the types and reverse the cascaded relationships
		for(V state: getVertices()) {
			if(state.getType().getProperties() != null) {
				for(Property p: state.getType().getProperties()) {
					if(outTransitions.get(state) != null && outTransitions.get(state).containsKey(p.getName())) {
						if(!p.isContainment() && p.isNullable()) {
							unlinkEdge(outTransitions.get(state).get(p.getName()));
						}
						if(p.isContainment()) {
							reverseEdge(outTransitions.get(state).get(p.getName()));
						}
					}
				}
			}
		}

		// Remove all self loops
		unlinkSelfLoops();
		
		List<V> sorted = super.toposort();
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
	
	public void populateEdges() {
		// Go through every state and add the edges
		for(V state: getVertices()) {
			if(!state.getType().isDataType()) {
				for(Property p: state.getType().getProperties()) {
					//sgLogger.debug("checking edge " + p.getName() + " of type " + p.getType().getName() );
					if(states.containsKey(p.getType()) ) {
						Edge<State> edge = new Edge<State>(p.getName(), state, states.get(p.getType()));
						
						sgLogger.debug("Adding edge " + p.getName() + " to type " + state.getType().getName() );
						addEdge((E) edge);
					}
				}
			}
		}
	}
	
	public boolean supportsDynamicUpdate() {
		for(V state: getVertices()) {
			if(!((EntityType)state.getType()).supportsDynamicUpdate()) {
				return false;
			}
		}
		
		return true;
	}
	
	private void addObject(
			Map<State, List<JSONObject>> stateObjectMap,
			Map<JSONObject, State> objectStateMap,
			State state,
			JSONObject object) {

		List<JSONObject> list = stateObjectMap.get(state);
		if(list == null) {
			list = new ArrayList<JSONObject>();
			stateObjectMap.put(state, list);
		}
		list.add(object);
		objectStateMap.put(object, state);
	}
			
	
	/**
	 * Generates a random object graph using JSON objects.
	 *
	 * @return the generated object graph
	 */
	public JSONObject generateObjectGraph (Settings settings)
	{
		/*
		 * Use BFS to traverse the graph since we want to give all states an opportunity to participate in the object graph.
		 * Keep a map between the state and the list of JSONObjects
		 * When traversing an ASSOCIATION, randomly choose a JSONObject based on the target state.
		 * If the ASSOCIATION is a COMPOSITION, then create a new JSONObject.
		 * 
		 * Assume no other behavior on a relationship.
		 */

		//Set all nodes to "not visited". This is by having the visited set as empty
		Set<JSONObject> visited = new HashSet<JSONObject>();
		Map<State, List<JSONObject>> stateObjectMap = new HashMap<State, List<JSONObject>>();
		Map<JSONObject, State> objectStateMap = new HashMap<JSONObject, State>();

		Queue<JSONObject> q = new LinkedList();

		JSONObject result = (JSONObject)((EntityType)getRootState().getType()).generate(settings, null);
		addObject(
			stateObjectMap,
			objectStateMap,
			getRootState(),
			result);
		q.add(result);

		while (!q.isEmpty()) {
			// Check limits
			if(objectStateMap.size() > settings.getEntitySize().size()) {
				break;
			}

			JSONObject entity = q.remove();
			if (!visited.contains(entity)) {
				// Mark as visited
				visited.add(entity);

				for (Property property : objectStateMap.get(entity).getType().getProperties()) {
					// target type
					ExtendedProperty extendedProperty = (ExtendedProperty)property;
					if( extendedProperty.isDataType()) {
						continue;
					}

					Type targetType = extendedProperty.getType();
					Type targetEntityType = targetType;
					if (extendedProperty.isMany()) {
						targetEntityType = extendedProperty.getElementType();
					}
					int size = stateObjectMap.get(targetEntityType) != null ?
						stateObjectMap.get(targetEntityType).size() :
						0;
					Object target;
					if (property.isContainment() || size == 0) {

						State state = states.get(targetEntityType);

						// Check if the state is out of scope
						if(state == null) {
							continue;
						}

						target = ((BasicType)targetType).generate(settings, extendedProperty);
						if(target instanceof JSONObject) {
							addObject(
								stateObjectMap,
								objectStateMap,
								state,
								(JSONObject)target);
							q.add((JSONObject)target);
						} else if(target instanceof JSONArray) {
							for(int i = 0; i < ((JSONArray)target).length(); i++) {
								JSONObject jsonObject = (JSONObject)((JSONArray)target).get(i);
								addObject(
									stateObjectMap,
									objectStateMap,
									state,
									jsonObject);
								q.add(jsonObject);
							}
						}
					}
					else {
						List<JSONObject> entitiesToChooseFrom = stateObjectMap.get(targetType);
						target = entitiesToChooseFrom.get((int)(Math.random() * (size - 1)));
					}
					entity.put(property.getName(), target);
				}
			}
		}

		return result;
	}	
}
