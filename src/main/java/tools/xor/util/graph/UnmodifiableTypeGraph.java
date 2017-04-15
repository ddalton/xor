package tools.xor.util.graph;

import org.json.JSONObject;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.view.QueryView;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnmodifiableTypeGraph<V extends State, E extends Edge<V>> implements TypeGraph<V, E> {

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

	public void addVertex(V vertex) {
		raiseException();
	}
	
	public void addEdge(E edge) {
		addEdge(edge, edge.getStart(), edge.getEnd());
	}

	public V getVertex(Type t) {
		return typeGraph.getVertex(t);
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

	@Override public List<Property> next (Type type, String propertyPath, Set<String> exactSet)
	{
		return typeGraph.next(type, propertyPath, exactSet);
	}

	@Override public String dumpState ()
	{
		return typeGraph.dumpState();
	}

	@Override public List<QueryView> getQueryableRegions ()
	{
		return typeGraph.getQueryableRegions();
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
}
