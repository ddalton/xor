package tools.xor.util.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.QueryType;
import tools.xor.Resolver;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.State;
import tools.xor.view.AggregateView;
import tools.xor.view.Field;
import tools.xor.view.TraversalView;

/**
 * State specific to a particular edge. This graph might have multiple states for the same type,
 * depending on how that state is accessed.
 * 
 * Needed for the construction of the QueryTree.
 * Each node/vertex corresponds to a QueryFragment in the QueryTree, where it represents a single entity that is part of a JOIN
 *
 * @param <V> Vertex
 * @param <E> Edge
 */
public class StateTree<V extends StateTree.SubtypeState, E extends StateTree.AutonomousEdge<V>> extends StateGraph<V, E> implements Tree<V, E> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private V rootState;

	// Needed for copy functionality
	StateTree(Type aggregateRoot, Shape shape) {
		super(aggregateRoot, shape);
	}
	
	public StateTree(Type aggregateRoot, Shape shape, State rootState) {
		super(aggregateRoot, shape);
		this.rootState = (V) rootState;

		addVertex(this.rootState);
	}
	
	@Override
	public V getRootState() {
		return rootState;
	}

	@Override public V getParent (V node)
	{
		return TreeOperations.getParent(this, node);
	}

	@Override public List<V> getChildren (V node)
	{
		return TreeOperations.getChildren(this, node);
	}

	@Override public V getRoot ()
	{
		return TreeOperations.getRoot(this);
	}

	@Override public int getHeight ()
	{
		return TreeOperations.getHeight(this);
	}

	@Override public String getPathToRoot (V node)
	{
		return TreeOperations.getPathToRoot(this, node);
	}

	@Override public <Q extends TreeOperations<V, E>> void split (E splitAtEdge,
																  E newEdge,
																  Q target)
	{
		TreeOperations.split(this, splitAtEdge, newEdge, target);
	}

	/**
	 * An edge that has logic embedded in it and can independently decide on the outcome of
	 * processing this edge.
	 *
	 * @param <V> between two SubtypeState vertices
	 */
	public static class AutonomousEdge<V extends StateTree.SubtypeState> extends Edge<V> {
		private Resolver resolver;
		public AutonomousEdge (String name, V start, V end, boolean qualify)
		{
			super(name, start, end, qualify, false);
		}
	}

	public static class SubtypeState extends State {
		Map<String, SubtypeState> descendants; // maintained at the root state
		SubtypeState supertype;
		Set<SubtypeState> children;
		Map<String, Resolver> resolvers;

		public SubtypeState(QueryType type, boolean startState) {
			super(type, startState);

			descendants = new HashMap<>();
			children = new HashSet<>();
		}

		@Override
		public State copy() {
			SubtypeState result = new SubtypeState((QueryType)getType(), this.isStartState());
			copyData(result);

			// Resolvers can be shared
			result.resolvers = resolvers;

			return result;
		}

		public void copyDependencies(SubtypeState copy, Map<SubtypeState, SubtypeState> oldNew) {

			if(supertype != null) {
				copy.supertype = oldNew.get(supertype);
			}

			if(children.size() > 0) {
				for(SubtypeState child: children) {
					copy.children.add(oldNew.get(child));
				}
			}

			if(descendants.size() > 0) {
				for(Map.Entry<String, SubtypeState> entry: descendants.entrySet()) {
					copy.descendants.put(entry.getKey(), oldNew.get(entry.getValue()));
				}
			}
		}

		public SubtypeState getSupertype () {
			return this.supertype;
		}

		public void setSupertype (SubtypeState superType, TypeGraph tg) {

			EntityType entityType = ((QueryType)getType()).getBasedOn();

			// check if any of the subtypes is a supertype to the current state
			for(SubtypeState sts: superType.children) {
				EntityType childType = ((QueryType)sts.getType()).getBasedOn();
				if( childType.isSameOrSupertypeOf(childType) ) {
					setSupertype(sts, tg);

					// The current state needs to be added in the correct place in
					// the inheritance hierarchy
					return;
				}
			}

			// Check if any of the subtypes of the supertype is a subtype of the current state
			// and adjust accordingly
			Set<SubtypeState> removedTypes = new HashSet<>();
			for(SubtypeState sts: superType.children) {
				EntityType childType = ((QueryType)sts.getType()).getBasedOn();
				if( entityType.isSameOrSupertypeOf(childType) ) {
					sts.supertype = this;
					children.add(sts);
					removedTypes.add(sts);

					// add inheritance edges from the new supertype to the children
					addInheritanceEdge(tg, this, sts);
				}
			}

			for(SubtypeState sts: removedTypes) {
				superType.children.remove(sts);

				// remove the inheritance edge from the old parent to the children
				removeInheritanceEdge(tg, superType, sts);
			}

			this.supertype = superType;

			// add the inheritance edge from the parent to the current state
			addInheritanceEdge(tg, superType, this);
			getRootState().markSubType(getTypeName(), this);
		}

		private void markSubType(String typeName, SubtypeState sts) {
			this.descendants.put(typeName, sts);
		}

		public boolean addSubtype (SubtypeState descendantState, TypeGraph tg) {
			// first check that the input state is a descendant of the current state
			EntityType type = (EntityType)getType();
			if(type instanceof QueryType) {
				type = ((QueryType) type).getBasedOn();
			}

			if(type.isAbstract()) {
				throw new RuntimeException("Abstract types cannot be queried, specify the desired concrete type");
			}

			EntityType childType = (EntityType) descendantState.getType();
			if(childType instanceof QueryType) {
				childType = ((QueryType) childType).getBasedOn();
			}

			if(type.getName().equals(childType.getName()) || findState(childType) != null) {
				return false;
			}

			if(type.isSameOrSupertypeOf(childType)) {
				descendantState.setSupertype(this, tg);
			} else {
				throw new RuntimeException("Trying to add a subtype to a state that is not a supertype");
			}

			return true;
		}

		public SubtypeState getRootState() {
			SubtypeState result = this;

			while(result.getSupertype() != null) {
				result = result.getSupertype();
			}

			return result;
		}

		private void addInheritanceEdge(TypeGraph tg, SubtypeState start, SubtypeState end) {
			tg.addEdge(
				new Edge(DFAtoNFA.UNLABELLED, start, end),
				start,
				end);
		}

		private void removeInheritanceEdge(TypeGraph tg, SubtypeState start, SubtypeState end) {
			tg.removeEdge(new Edge(DFAtoNFA.UNLABELLED, start, end));
		}

		public SubtypeState findState (EntityType entityType) {

			if(descendants.containsKey(entityType.getName())) {
				return descendants.get(entityType.getName());
			}

			return getTypeName().equals(entityType.getName()) ? this : null;
		}

		public Set<String> getAttributes() {
			if(supertype != null) {
				Set<String> result = new HashSet<>(super.getAttributes());
				result.addAll(supertype.getAttributes());
				return result;
			} else {
				return super.getAttributes();
			}
		}
	}

	/**
	 * Will build a Type graph using the type information from the view. If the view does
	 * not have the type specified, then it will use the provided entityType.
	 * The same type might occur more than once depending
	 * upon its position within the graph w.r.t to the root state.
	 *
	 * This structure is useful when modeling the type graph needed to support GraphQL queries.
	 *
	 * @param view whose type graph we need to build
	 * @param entityType fallback
	 * @return Type graph for the view
	 */
	public static StateGraph build(TraversalView view, EntityType entityType) {
		if(entityType == null && view.getTypeName() != null) {
			entityType = (EntityType)view.getShape().getType(view.getTypeName());
		}

		// Scan through all the child views and build a map between a root state and its
		// view and between the view name and the state.
		Map<Field, State> viewAliasStateMap = new HashMap<>();
		Map<String, TraversalView> nameViewMap = new HashMap<>();

		// Allow it to host aliases
		QueryType queryType = new QueryType(entityType, null);
		State startState = new SubtypeState(queryType, true);

		// Handle the root view
		viewAliasStateMap.put(new Field("root", null, null, view.getName()), startState);
		nameViewMap.put(view.getName(), view);
		buildMaps(viewAliasStateMap, nameViewMap, view, queryType);

		// Create the graph and add all states to it
		StateTree result = new StateTree<>(queryType, view.getShape(), startState);
		for(State state: viewAliasStateMap.values()) {
			result.addVertex(state);
		}

		result.linkEdges(viewAliasStateMap, nameViewMap, AggregateView.Format.PATHS);

		return result;
	}

	// populates both the simple and relationship attributes in a State
	private void linkEdges(Map<Field, State> viewAliasStateMap, Map<String, TraversalView> nameViewMap, AggregateView.Format format) {
		// Iterate through each view and add the edges by each attribute path
		for(Field pa: viewAliasStateMap.keySet()) {

			V current = (V)viewAliasStateMap.get(pa);

			TraversalView view = nameViewMap.get(pa.getViewName());
			if(view.getAttributeList() == null) {
				throw new RuntimeException("View does not have any attributes set");
			}

			if(format == AggregateView.Format.PATHS) {
				// Iterate through each attribute path
				for (String attrPath : view.getAttributeList()) {
					extend(
						view, "", attrPath, null,
						AggregateView.Format.PATHS, (V)current, viewAliasStateMap);
				}
			} else {
				Iterator<String> iter = view.getJson().keys();
				while(iter.hasNext()) {
					String attribute = iter.next();
					extend(view, "", attribute, view.getJson(), format, (V) current, viewAliasStateMap);
				}
			}
		}
	}

	private V getFragmentState(Field alias, Map<Field, State> viewNameStateMap) {
		V result = null;

		if(viewNameStateMap.containsKey(alias)) {
			result = (V)viewNameStateMap.get(alias);
		} else {
			throw new RuntimeException("Unable to find fragment with name: " + alias.getViewName());
		}

		return result;
	}

	@Override
	public List<Property> next(Type type, String propertyPath, Set<String> exactSet) {
		// We might support it in further by resolving from propertyPath and any additional info for subtypes
		throw new UnsupportedOperationException("next() method is not supported on type argument, as there can be multiple states for a type.");
	}

	/**
	 * Extend the StateGraph by adding the necessary states lying on the path being added
	 * Preference is given to JSON representation
	 *
	 * @param path that extends this type graph
	 * @param current root state of the path. The path is relative.
	 * @param viewAliasStateMap map between view name and the root state
	 */
	private void extend (TraversalView view, String processed, String path, JSONObject json, AggregateView.Format format, V current, Map<Field, State> viewAliasStateMap)
	{
		String attribute = null;
		if(format == AggregateView.Format.PATHS) {
			if (path == null || "".equals(path.trim())) {
				return; // terminating condition
			}
			attribute = State.getNextAttr(path);
		} else {
			if(json == null || json.length() == 0) {
				return;
			}
			attribute = path;
		}
		processed = ((processed.length() > 0) ? Settings.PATH_DELIMITER : "") + attribute;

		// subtypes based on function alias declared in the view for a property
		Field alias = view.getAlias(processed);
		if(alias != null && alias.isViewReference()) {
			// get a view alias
			V subTypeState = getFragmentState(alias, viewAliasStateMap);
			current.addSubtype(subTypeState, this);
			// The attributes of the view are later on added to the state using method linkEdges
			return;
		}
		if(processed.length() > 0) {
			// Get a property alias
			alias = view.getAlias(processed);
		}

		String remaining = null;
		if(format == AggregateView.Format.PATHS) {
			remaining = State.getRemaining(path);
		} else {
			if (json.has(attribute) && json.get(attribute) instanceof JSONObject) {
				json = json.getJSONObject(attribute);
			}
			else {
				json = null;
			}
		}
		E t = getOutEdge(current, attribute);

		// Not in the current state graph, let us find and add it
		if (t == null) {
			// Extend along three ways
			if(alias != null) {
				// 1. Extend by using a alias
				// find the correct subtype from Property Alias
				processAlias(alias, current);
			} else {
				// 2. By inheritance - Find the correct types
				processSubtypes(current, attribute, remaining, json, format, false);
			}
		}

		// Check if the property is a datatype, if so add it to the state
		Property childProperty = (current.getType()).getProperty(attribute);
		if (childProperty != null) {
			Type propertyType = GraphUtil.getPropertyEntityType(childProperty, getShape());
			if(propertyType.isDataType()) {
				// This is an attribute of this state
				current.addAttribute(childProperty.getName());
			}
		}

		// find the edge again, if found, then recurse
		t = getOutEdge(current, attribute);
		if(t != null) {
			if(format == AggregateView.Format.PATHS) {
				extend(view, processed, remaining, json, format, t.getEnd(), viewAliasStateMap);
			} else {
				Iterator<String> iter = json.keys();
				while(iter.hasNext()) {
					String childAttribute = iter.next();
					extend(view, processed, childAttribute, json, format, t.getEnd(), viewAliasStateMap);
				}
			}
		}
	}

	private void processAlias(Field alias, V current) {
		if(alias != null) {
			QueryType queryType = (QueryType)current.getType();
			ExtendedProperty original = (ExtendedProperty)queryType.getProperty(alias.getOriginal());
			Type originalPropertyType = GraphUtil.getPropertyEntityType(original, getShape());
			Type propertyType = alias.getTypeName() != null ?
				getShape().getType(alias.getTypeName()) : originalPropertyType;

			if(original != null) {
				Property childProperty = original.refine(
					alias.getAlias(),
					propertyType,
					queryType);

				// Enhance the type by adding this new property (alias)
				queryType.addProperty(childProperty);

				// add it to the scope (state)
				current.addAttribute(childProperty.getName());

				if(!originalPropertyType.isDataType()) {
					// check if the original has a state, if not create one
					E t = getOutEdge(current, original.getName());
					if(t == null) {
						// create a new state for the original property
						SubtypeState sts = new SubtypeState(new QueryType((EntityType) originalPropertyType, null), false);
						// add this edge to the graph
						t = addQueryEdge(current, original, (V)sts, false );
					}
					// alias could represent a subtype, so add it just to be safe
					SubtypeState aliasSts = new SubtypeState(new QueryType((EntityType) propertyType, null), false);
					aliasSts.addSubtype(aliasSts, this);
				}
			} else {
				// root alias
				queryType.addSelfJoin(alias);
			}
		}
	}

	private boolean foundSubtype(EntityType subType, String remaining, JSONObject json, AggregateView.Format format) {
		if(format == AggregateView.Format.PATHS) {
			return remaining == null || (remaining != null && subType.getProperty(remaining) != null);
		} else {
			// check the json
			if(json == null) {
				return true;
			}

			// check all the immediate properties are present in the subType
			Iterator<String> iter = json.keys();
			while(iter.hasNext()) {
				String propertyName = iter.next();
				if(subType.getProperty(propertyName) == null) {
					return false;
				}
			}
			return true;
		}
	}

	private E processSubtypes (V current, String attribute, String remaining, JSONObject json, AggregateView.Format format, boolean initialize) {
		E edge = null;

		Property childProperty = current.getType().getProperty(attribute);
		if(childProperty == null) {

			// Search and add the property from the subtype
			boolean found = false;
			if(current.getType() instanceof EntityType) {
				EntityType entityType = (EntityType)current.getType();
				List<EntityType> subTypes = entityType.findInSubtypes(attribute);

				List<SubtypeState> subtypeStates = new LinkedList<>();
				for(EntityType subType: subTypes) {
					if(!foundSubtype(subType, remaining, json, format)) {
						continue;
					}

					found = true;
					SubtypeState sts = current.findState(subType);
					if(sts == null) {
						sts = new SubtypeState(new QueryType(subType, null), false);
						if(current.addSubtype(sts, this)) {
							subtypeStates.add(sts);
						} else {
							throw new RuntimeException("Unnecessary to add a subtype");
						}
					}
					sts.addAttribute(attribute);
				}

				if(subtypeStates.size() > 1) {
					throw new RuntimeException(
						String.format(
							"Found multiple subtypes for attribute: %s in state: %s. This needs to be fixed by using aliases",
							attribute,
							current.getType().getName()));
				}

				// add an edge where required
				if(subtypeStates.size() == 1) {
					SubtypeState sts = subtypeStates.get(0);
					// If we have reached here then we should have a QueryType that is having a basedOn type populated
					// and is not DQOR (Dynamic Query Object Reconstitution) where basedOn is null
					Property p = ((QueryType)sts.getType()).getBasedOn().getProperty(attribute);
					return addQueryEdge(current, p, (V)sts, initialize);
				}
			}

			if(!found) {
				logger.error(
					"Unable to add unknown attribute to state tree: " + attribute + " to state: "
						+ current.getType().getName());
			}
			return null;
		} else {
			edge = addQueryEdge(current, childProperty, null, initialize );
		}

		return edge;
	}

	/*
	 * Adds an edge if the property represents a relationship
	 */
	private E addQueryEdge(V current, Property childProperty, V end, boolean initialize) {
		E edge = null;

		Type propertyType = GraphUtil.getPropertyEntityType(childProperty, getShape());
		if(!propertyType.isDataType()) {
			if(end == null) {
				end = (V)new SubtypeState(new QueryType((EntityType) propertyType, null), false);
			} else {
				// the edge is against the root state
				end = (V)end.getRootState();
			}
			if (initialize) {
				end.setAttributes(((EntityType)propertyType).getInitializedProperties());
			}

			// add the transition
			edge = (E)new AutonomousEdge(childProperty.getName(), current, end, true);
			addEdge(edge);
		}

		return edge;
	}

	@Override
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
		String remaining = State.getRemaining(path);
		if(t == null) {
			t = processSubtypes(current, attribute, remaining, null,
				AggregateView.Format.PATHS, initialize);
		}

		if(t != null) {
			extend(remaining, t.getEnd(), initialize);
		}
	}

	@Override
	protected List<Property> getPropertiesInScope (State vertex,
												   Type type,
												   String propertyPath,
												   Set<String> exactSet)
	{
		List<Property> result = new ArrayList<Property>();

		// The vertex is SubtypeState and contains both direct and supertype properties
		for (String simpleAttribute : vertex.getAttributes()) {
			result.add(vertex.getType().getProperty(simpleAttribute));
		}

		// Addresses scope extension using AssociationSetting with MatchType.TYPE
		collectEdges(result, (V)vertex);

		return result;
	}

	private static void buildMaps (Map<Field, State> viewAliasStateMap,
								   Map<String, TraversalView> nameViewMap,
								   TraversalView view,
								   EntityType entityType)
	{
		if(view.getChildren() != null) {
			for (TraversalView child : view.getChildren()) {
				nameViewMap.put(child.getName(), child);
			}

			for (Field viewAlias : view.getViewAliases()) {
				TraversalView childView = nameViewMap.get(viewAlias.getViewName());
				if (childView != null) {
					EntityType childEntityType = (EntityType)entityType.getShape().getType(viewAlias.getTypeName());
					QueryType queryType = new QueryType(childEntityType, null);
					SubtypeState childRootState = new SubtypeState(queryType, false);
					viewAliasStateMap.put(viewAlias, childRootState);

					buildMaps(viewAliasStateMap, nameViewMap, childView, childEntityType);
				}
				else {
					// TODO: check view in the general view list
					throw new RuntimeException(
						"view alias referring to a missing view: " + viewAlias.getViewName());
				}
			}
		}
	}

	public QueryType createQueryType() {
		// We need to augment the state tree with aliases
		return null;
	}

	@Override
	public StateGraph<V, E> copy() {
		return copy(null);
	}

	/**
	 *
	 * @param mergeStates map of states to be merged. Needs to be the original states map and not a copy, since it is needed to properly remove duplicates
	 * @return merged graph
	 */
	@Override
	public StateGraph<V, E> copy(Map<Type, V> mergeStates) {

		StateTree<V, E> result = new StateTree<V, E>(getAggregateRoot(), getShape());

		copyData(result, mergeStates);

		return result;
	}

	@Override
	protected Map<V, V> copyData(StateGraph<V, E> copy, Map<Type, V> mergeStates) {
		Map<V, V> oldNew = super.copyData(copy, mergeStates);

		// Copy dependencies
		for(Map.Entry<V, V> entry: oldNew.entrySet()) {
			entry.getKey().copyDependencies(entry.getValue(),
				(Map<SubtypeState, SubtypeState>)oldNew);
		}

		((StateTree)copy).rootState = (V) oldNew.get(getRootState());

		return oldNew;
	}
}
