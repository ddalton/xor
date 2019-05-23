package tools.xor.util.graph;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
import tools.xor.view.AggregateView.PropertyAlias;

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
		SubtypeState parent;
		Map<String, Resolver> resolvers;

		public SubtypeState(Type type, boolean startState) {
			super(type, startState);

			subtypeStates = new HashMap<>();
		}

		public SubtypeState getParent() {
			return this.parent;
		}

		public void setParent(SubtypeState sts) {
			this.parent = sts;
		}

		public void addSupertype (SubtypeState ancestorState, TypeGraph tg) {
			ancestorState.addSubtype(this, tg, this);
		}

		private void markSubType(String typeName, SubtypeState sts) {
			this.subtypeStates.put(typeName, sts);
		}

		public void addSubtype (SubtypeState descendantState, TypeGraph tg, SubtypeState bookkeepingState) {
			// first check that the input state is a descendant of the current state
			EntityType type = (EntityType)getType();
			if(type instanceof QueryType) {
				type = ((QueryType) type).getBasedOn();
			}

			EntityType childType = (EntityType) descendantState.getType();
			if(childType instanceof QueryType) {
				childType = ((QueryType) childType).getBasedOn();
			}

			if(type.getName().equals(childType.getName())) {
				return;
			}

			SubtypeState start = this; // start of inheritance edge
			if(type.isSameOrAncestorOf(childType)) {
				for(EntityType d: type.getDescendantsTo(childType)) {
					SubtypeState sts = new SubtypeState(new QueryType(d, null), false);
					sts.setParent(start);
					bookkeepingState.markSubType(sts.getTypeName(), sts);

					addInheritanceEdge(tg, start, sts);
					start = sts;
				}
			}

			bookkeepingState.markSubType(descendantState.getTypeName(), descendantState);
			descendantState.setParent(start);
			addInheritanceEdge(tg, start, descendantState);
		}

		private void addInheritanceEdge(TypeGraph tg, SubtypeState start, SubtypeState end) {
			tg.addEdge(
				new Edge(DFAtoNFA.UNLABELLED, start, end),
				start,
				end);
		}

		public State findState (EntityType entityType) {

			if(subtypeStates.containsKey(entityType.getName())) {
				return subtypeStates.get(entityType.getName());
			}

			return this;
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
		if(entityType == null && view.getTypeName() != null) {
			entityType = (EntityType)view.getShape().getType(view.getTypeName());
		}

		// Allow it to host aliases
		entityType = new QueryType(entityType, null);

		// Scan through all the child views and build a map between a root state and its
		// view and between the view name and the state.
		Map<PropertyAlias, State> viewAliasStateMap = new HashMap<>();
		Map<String, AggregateView> nameViewMap = new HashMap<>();

		State startState = new SubtypeState(entityType, true);
		viewAliasStateMap.put(new PropertyAlias(view.getName()), startState);
		nameViewMap.put(view.getName(), view);
		buildMaps(viewAliasStateMap, nameViewMap, view, entityType);

		// Create the graph and add all states to it
		StateTree result = new StateTree<>(entityType, view.getShape(), startState);
		for(State state: viewAliasStateMap.values()) {
			result.addVertex(state);
		}

		result.linkEdges(viewAliasStateMap, nameViewMap);

		return result;
	}

	// populates both the simple and relationship attributes in a State
	private void linkEdges(Map<PropertyAlias, State> viewAliasStateMap, Map<String, AggregateView> nameViewMap) {
		// Iterate through each view and add the edges by each attribute path
		for(PropertyAlias pa: viewAliasStateMap.keySet()) {

			V current = (V)viewAliasStateMap.get(pa);

			AggregateView view = nameViewMap.get(pa.getViewName());
			if(view.getAttributeList() == null) {
				throw new RuntimeException("View does not have any attributes set");
			}

			// Iterate through each attribute path
			for(String attrPath: view.getAttributeList()) {
				extend(view, "", attrPath, (V) current, viewAliasStateMap);
			}
		}
	}

	private V getFragmentState(PropertyAlias alias, Map<PropertyAlias, State> viewNameStateMap) {
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
	 * @param path that extends this type graph
	 * @param current root state of the path. The path is relative.
	 * @param viewAliasStateMap map between view name and the root state
	 */
	private void extend (AggregateView view, String processed, String path, V current, Map<PropertyAlias, State> viewAliasStateMap)
	{
		if (path == null || "".equals(path.trim())) {
			return; // terminating condition
		}

		String attribute = State.getNextAttr(path);
		processed = ((processed.length() > 0) ? Settings.PATH_DELIMITER : "") + attribute;

		// subtypes based on function alias declared in the view for a property
		PropertyAlias alias = view.getAlias(path);
		if(alias != null && alias.isViewReference()) {
			V subTypeState = getFragmentState(alias, viewAliasStateMap);
			current.addSubtype(subTypeState, this, current);
			return;
		}
		if(processed.length() > 0) {
			alias = view.getAlias(processed);
		}

		String remaining = State.getRemaining(path);
		E t = getOutEdge(current, attribute);

		// Not in the current state graph, let us find and add it
		if (t == null) {
			// Extend along two ways
			// 1. By inheritance - find the correct subtype from Property Alias
			Property childProperty = (current.getType()).getProperty(attribute);
			if (childProperty == null) {
				if(alias != null) {
					QueryType queryType = (QueryType)current.getType();
					ExtendedProperty original = (ExtendedProperty)queryType.getProperty(alias.getOriginal());
					Type newPropertyType = getShape().getType(alias.getSubclassName());

					if(original != null) {
						if (!newPropertyType.isDataType() || original.isMany()) {
							newPropertyType = new QueryType((EntityType)newPropertyType, null);
						}

						childProperty = original.refine(
							alias.getAlias(),
							newPropertyType,
							queryType);
					} else {
						queryType.addSelfJoin(alias);
					}

					queryType.addProperty(childProperty);
				} else {
					logger.error(
						"Unable to add unknown attribute to state graph: " + attribute
							+ " to state: "
							+ current.getType() + ". Is it a sub-type attribute?");
					return;
				}
			}

			// 2. By the path - All new states by walking along the property path
			// This approach could also involve inheritance, but the correct
			// type needs to be deduced
			t = extendByPath(current, attribute, false);
		}

		if(t != null) {
			extend(view, processed, remaining, t.getEnd(), viewAliasStateMap);
		}
	}

	private E extendByPath (V current, String attribute, boolean initialize) {
		E edge = null;

		Property childProperty = current.getType().getProperty(attribute);
		if(childProperty == null) {
			// TODO: Find if the property is in the subtype or supertype
			// TODO: If so, add either SubtypeState#addSubtype or SubtypeState#addSupertype

			logger.error("Unable to add unknown attribute to state graph: " + attribute + " to state: " + current.getType().getName() + ". Does this attribute belong to a subtype?");
			return edge;
		}
		Type propertyType = GraphUtil.getPropertyEntityType(childProperty, getShape());
		if(propertyType.isDataType()) {
			// This is an attribute of this state
			current.addAttribute(childProperty.getName());
			return edge;
		} else {
			V end = (V)new SubtypeState(propertyType, false);
			if (initialize) {
				end.setAttributes(((EntityType)propertyType).getInitializedProperties());
			}
			addVertex(end);

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
		if(t == null) {
			t = extendByPath(current, attribute, initialize);
		}

		if(t != null) {
			extend(State.getRemaining(path), t.getEnd(), initialize);
		}
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

	private static void buildMaps (Map<PropertyAlias, State> viewAliasStateMap,
								   Map<String, AggregateView> nameViewMap,
								   AggregateView view,
								   EntityType entityType)
	{
		if(view.getChildren() != null) {
			for (AggregateView child : view.getChildren()) {
				nameViewMap.put(child.getName(), child);
			}

			for (PropertyAlias viewAlias : view.getViewAliases()) {
				AggregateView childView = nameViewMap.get(viewAlias.getViewName());
				if (childView != null) {
					EntityType childEntityType = (EntityType)entityType.getDAS().getType(viewAlias.getSubclassName());

					SubtypeState childRootState = new SubtypeState(childEntityType, true);
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
}
