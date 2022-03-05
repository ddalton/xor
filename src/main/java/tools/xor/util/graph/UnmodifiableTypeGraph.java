package tools.xor.util.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import tools.xor.AssociationSetting;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.Edge;
import tools.xor.util.State;

public class UnmodifiableTypeGraph<V extends State, E extends Edge<V>> implements TypeGraph<V, E>, Tree<V, E> {

	private TypeGraph<V, E> typeGraph;

	public UnmodifiableTypeGraph (TypeGraph stateGraph) {
		this.typeGraph = stateGraph;
	}
	
	public V getRootState() {
		return typeGraph.getRootState();
	}

	private void raiseException() {
		throw new UnsupportedOperationException("Changes are not allowed on this state graph, make a copy of the view holding this graph to make changes.");
	}

	@Override public Collection<V> getVertices ()
	{
		return typeGraph.getVertices();
	}

	@Override public int getId (V vertex)
	{
		return typeGraph.getId(vertex);
	}

	@Override public V getVertex (int id)
	{
		return typeGraph.getVertex(id);
	}

	@Override public Collection<E> getEdges ()
	{
		return typeGraph.getEdges();
	}

	public void addEdge(E edge, V start, V end) {
		raiseException();
	}

	public void removeEdge(E edge) {
		raiseException();
	}
	
	public E getOutEdge(V vertex, String name) {
		return typeGraph.getOutEdge(vertex, name);
	}

	@Override public Collection<E> getOutEdges (V vertex)
	{
		return typeGraph.getOutEdges(vertex);
	}

	public void addVertex(V vertex) {
		raiseException();
	}

	@Override public void removeVertex (V vertex)
	{
		typeGraph.removeVertex(vertex);
	}

	@Override public Collection<V> getRoots ()
	{
		return typeGraph.getRoots();
	}

	public void addEdge(E edge) {
		addEdge(edge, edge.getStart(), edge.getEnd());
	}

	public V getVertex(Type t) {
		return typeGraph.getVertex(t);
	}

	public V getVertex(String name) {
		return typeGraph.getVertex(name);
	}
	
	public TypeGraph<V, E> getFullStateGraph() {
		typeGraph.getFullStateGraph();
		return this;
	}
	
	public StateGraph<V, E> copy() {
		return typeGraph.copy();
	}

	public StateGraph<V, E> copy(Map<Type, V> mergeStates) {
		return typeGraph.copy(mergeStates);
	}

	@Override public List<Property> next (State state, String propertyPath, Set<String> exactSet)
	{
		return typeGraph.next(state, propertyPath, exactSet);
	}

	@Override public List<Property> next (Type type, String propertyPath, Set<String> exactSet)
	{
		return typeGraph.next(type, propertyPath, exactSet);
	}

	@Override public String dumpState ()
	{
		return typeGraph.dumpState();
	}

	@Override public void prune (List<AssociationSetting> associations)
	{
		typeGraph.prune(associations);
	}

	@Override public void markReferences (List<String> references)
	{
		typeGraph.markReferences(references);
	}

	@Override public void enhance (List<AssociationSetting> associations)
	{
		typeGraph.enhance(associations);
	}

	@Override public void extend (String path, V anchor, boolean initialize)
	{
		typeGraph.extend(path, anchor, initialize);
	}

	@Override public boolean supportsDynamicUpdate ()
	{
		return typeGraph.supportsDynamicUpdate();
	}

	@Override public JSONObject generateObjectGraph (Settings settings)
	{
		return typeGraph.generateObjectGraph(settings);
	}

	@Override public void generateVisual (Settings settings)
	{
		typeGraph.generateVisual(settings);
	}

	@Override
	public boolean hasPath(String path) {
		return typeGraph.hasPath(path);
	}

	@Override public void exportToDOT (String filename)
	{
		typeGraph.exportToDOT(filename);
	}

	@Override public V getParent (V node)
	{
		if(typeGraph instanceof Tree) {
			return ((Tree<V, E>)typeGraph).getParent(node);
		} else {
			throw new UnsupportedOperationException("getParent not supported on a graph");
		}
	}

	@Override public List<V> getChildren (V node)
	{
		if(typeGraph instanceof Tree) {
			return ((Tree<V, E>)typeGraph).getChildren(node);
		} else {
			throw new UnsupportedOperationException("getChildren not supported on a graph");
		}
	}

	@Override public V getRoot ()
	{
		if(typeGraph instanceof Tree) {
			return ((Tree<V, E>)typeGraph).getRoot();
		} else {
			throw new UnsupportedOperationException("getRoot not supported on a graph");
		}
	}

	@Override public int getHeight ()
	{
		if(typeGraph instanceof Tree) {
			return ((Tree<V, E>)typeGraph).getHeight();
		} else {
			throw new UnsupportedOperationException("getHeight not supported on a graph");
		}
	}

	@Override public String getPathToRoot (V node)
	{
		if(typeGraph instanceof Tree) {
			return ((Tree<V, E>)typeGraph).getPathToRoot(node);
		} else {
			throw new UnsupportedOperationException("getPathToRoot not supported on a graph");
		}
	}

	@Override public <Q extends TreeOperations<V, E>> void split (E splitAtEdge,
																  E newEdge,
																  Q target)
	{
		if(typeGraph instanceof Tree) {
			((Tree<V, E>)typeGraph).split(splitAtEdge, newEdge, target);
		} else {
			throw new UnsupportedOperationException("split not supported on a graph");
		}
	}
}
