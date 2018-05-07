package tools.xor.util.graph;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Resolver;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.State;
import tools.xor.view.AggregateView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * State specific to a particular edge. This graph might have multiple states for the same type,
 * depending on how that state is accessed.
 *
 * @param <V> Vertex
 * @param <E> Edge
 */
public class StateTree<V extends StateTree.SubtypeState, E extends StateTree.AutonomousEdge<V>> extends StateGraph<V, E> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private V rootState;
	
	public StateTree(Type aggregateRoot, Shape shape, State rootState) {
		super(aggregateRoot, shape);
		this.rootState = (V) rootState;
	}
	
	@Override
	public V getRootState() {
		return rootState;
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
		Map<String, SubtypeState> subtypeStates;
		State parent;
		boolean needsRebuild; // is rebuilding of inheritance hierarchy necessary?
		Map<String, Resolver> resolvers;

		public SubtypeState(Type type, boolean startState) {
			super(type, startState);

			subtypeStates = new HashMap<>();
		}

		public void setNeedsRebuild (boolean needsRebuild)
		{
			this.needsRebuild = needsRebuild;
		}

		public State getParent() {
			return this.parent;
		}

		public void setParent(State parent) {
			this.parent = parent;
		}

		public void addSubtypeState(SubtypeState state) {
			subtypeStates.put(state.getName(), state);
			state.setParent(this);
		}

		public void rebuild(TypeGraph tg) {
			if(needsRebuild) {
				Map<String, SubtypeState> allSubtypes = subtypeStates;
				this.subtypeStates = new HashMap<>();

				for(SubtypeState subtypeState: allSubtypes.values()) {
					// Ensure that only subtypes are being added
					EntityType subType = (EntityType)subtypeState.getType();
					if(!getType().getInstanceClass().isAssignableFrom(subType.getInstanceClass())) {
						// not a subtype
						continue;
					}

					// Find and add it to the most immediate supertype (i.e., the parent type)
					boolean foundParent = false;
					while(subType != getType()) {
						if(allSubtypes.containsKey(subType.getSuperType().getName())) {
							allSubtypes.get(subType.getSuperType().getName()).addSubtypeState(subtypeState);
							foundParent = true;
							break;
						}
						subType = subType.getSuperType();
					}
					if(!foundParent) {
						// current type is the parent
						addSubtypeState(subtypeState);
					}
				}

				// Finally add the inheritance edges
				addInheritanceEdge(tg);
			}
		}

		private void addInheritanceEdge(TypeGraph tg) {
			for(SubtypeState subtypeState: subtypeStates.values()) {
				tg.addEdge(
					new Edge(DFAtoNFA.UNLABELLED, this, subtypeState),
					this,
					subtypeState);
			}
		}

		public State getState(EntityType entityType) {
			State result = this;

			EntityType current = entityType;
			while(current != null && !subtypeStates.containsKey(current.getName())) {
				current = current.getSuperType();
			}

			if(current != null) {
				result = subtypeStates.get(current.getName());
			}

			return result;
		}

		public Set<String> getAttributes() {
			if(parent != null) {
				Set<String> result = new HashSet<>(super.getAttributes());
				result.addAll(parent.getAttributes());
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
	public static StateGraph build(AggregateView view, EntityType entityType) {
		// Prerequistie - all child views need to have a type

		if(view.getTypeName() != null) {
			entityType = (EntityType)view.getShape().getType(view.getTypeName());
		}

		// Scan through all the child views and build a map between a root state and its
		// view and between the view name and the state.
		Map<State, AggregateView> stateViewMap = new HashMap<>();
		Map<String, State> viewNameStateMap = new HashMap<>();

		State startState = new SubtypeState(entityType, true);
		stateViewMap.put(startState, view);
		viewNameStateMap.put(view.getName(), startState);
		buildMaps(stateViewMap, viewNameStateMap, view);

		// Create the graph and add all states to it
		StateTree result = new StateTree<>(entityType, view.getShape(), startState);
		for(State state: stateViewMap.keySet()) {
			result.addVertex(state);
		}

		result.linkEdges(stateViewMap, viewNameStateMap);

		return result;
	}

	private void linkEdges(Map<State, AggregateView> stateViewMap, Map<String, State> viewNameStateMap) {
		// Iterate through each view and add the edges by each attribute path
		for(AggregateView view: stateViewMap.values()) {

			V current = (V)viewNameStateMap.get(view.getName());

			if(view.getAttributeList() == null) {
				throw new RuntimeException("View does not have any attributes set");
			}

			// Iterate through each attribute path
			for(String attrPath: view.getAttributeList()) {
				extend(attrPath, (V) current, viewNameStateMap);
			}
		}

		for(V state: getVertices()) {
			((SubtypeState)state).rebuild(this);
		}
	}

	private V getFragmentState(String path, Map<String, State> viewNameStateMap) {
		V result = null;

		String viewFragmentName = AggregateView.getViewReference(path);
		if(viewNameStateMap.containsKey(viewFragmentName)) {
			result = (V)viewNameStateMap.get(viewFragmentName);
		} else {
			throw new RuntimeException("Unable to find fragment with name: " + viewFragmentName);
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
	 * @param path that extends this type graph
	 * @param current root state of the path. The path is relative.
	 * @param viewNameStateMap map between view name and the root state
	 */
	public void extend (String path, V current, Map<String, State> viewNameStateMap)
	{

		if (path == null || "".equals(path.trim())) {
			return; // terminating condition
		}

		String attribute = State.getNextAttr(path);

		// subtypes
		if(attribute.startsWith(AggregateView.VIEW_REFERENCE_START)) {
			V subTypeState = getFragmentState(attribute, viewNameStateMap);
			current.addSubtypeState(subTypeState);
			current.setNeedsRebuild(true);
			return;
		}

		String remaining = State.getRemaining(path);
		E t = getOutEdge(current, attribute);

		// Not in the current state graph, let us find and add it
		if (t == null) {
			Property childProperty = (current.getType()).getProperty(attribute);
			if (childProperty == null) {
				logger.error(
					"Unable to add unknown attribute to state graph: " + attribute + " to state: "
						+ current.getType().getName());
				return;
			}
			Type propertyType = GraphUtil.getPropertyEntityType(childProperty, getShape());
			if (propertyType.isDataType()) {
				// This is an attribute of this state
				current.addAttribute(childProperty.getName());
				return;
			}
			else {
				V end = null;

				// Check if the remaining type is a view fragment
				boolean isViewRef = false;
				if(remaining != null && remaining.startsWith(AggregateView.VIEW_REFERENCE_START)) {
					end = getFragmentState(remaining, viewNameStateMap);
					isViewRef = true;
				} else {
					// Add a new state to the state graph
					end = (V)new SubtypeState(propertyType, false);
					addVertex(end);
				}

				// add the transition
				t = (E)new AutonomousEdge(childProperty.getName(), current, end, true);
				addEdge(t);

				if(isViewRef) {
					// No further processing necessary
					return;
				}
			}
		}

		extend(remaining, t.getEnd(), viewNameStateMap);
	}

	@Override
	protected List<Property> getPropertiesInScope (State vertex,
												   Type type,
												   String propertyPath,
												   Set<String> exactSet)
	{
		List<Property> result = new ArrayList<Property>();

		for (String simpleAttribute : vertex.getAttributes()) {
			result.add(vertex.getType().getProperty(simpleAttribute));
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

	private static void buildMaps (Map<State, AggregateView> stateViewMap,
								   Map<String, State> viewNameStateMap,
								   AggregateView view)
	{
		SubtypeState parentState = null;
		EntityType parentEntityType = (EntityType)view.getShape().getType(view.getTypeName());
		if (!viewNameStateMap.containsKey(view.getName())) {
			if (view.getTypeName() == null) {
				throw new RuntimeException("EntityType is required in view: " + view.getName());
			}
			parentState = new SubtypeState(parentEntityType, true);
			stateViewMap.put(parentState, view);
			viewNameStateMap.put(view.getName(), parentState);
		}
		else {
			parentState = (SubtypeState)viewNameStateMap.get(view.getName());
		}

		if(parentState.subtypeStates.isEmpty() && view.getChildren() != null) {
			// process child view fragments if any
			for (AggregateView child : view.getChildren()) {
				buildMaps(stateViewMap, viewNameStateMap, child);
			}
		}
	}
}
