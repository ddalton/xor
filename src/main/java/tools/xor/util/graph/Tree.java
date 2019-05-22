/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package tools.xor.util.graph;

import tools.xor.Settings;
import tools.xor.util.Edge;
import tools.xor.util.Vertex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class Tree<V extends Vertex, E extends Edge<V>> extends DirectedSparseGraph<V, E>
{
    public V getParent(V node) {
        Collection<E> inEdges = getInEdges(node);

        assert inEdges.size() <= 1 : "A tree can have at most one incoming edge";

        return (inEdges.size() == 1) ? inEdges.iterator().next().getStart(): null;
    }

    @Override
    public void addEdge(E edge, V start, V end) {
        assert getInEdges(end).size() == 0 : "A node in a tree can have at most one incoming edge";

        super.addEdge(edge, start, end);
    }

    /**
     * Split the tree at the edge and add the new edge along with its descendants to
     * the target tree's root.
     *
     * @param splitAtEdge which is being split from this tree
     * @param target tree to which the edge and its descendants are grafted
     */
    public <Q extends Tree<V, E>> void split(E splitAtEdge, E newEdge, Q target) {
        List<V> vertices = new LinkedList<>();
        List<V> verticesToRemove = new LinkedList<>();

        vertices.add(splitAtEdge.getEnd());
        while(!vertices.isEmpty()) {
            V vertex = vertices.remove(0);
            verticesToRemove.add(vertex);

            vertices.addAll(getChildren(vertex));
        }

        // The vertices can only be added to the target after they are first
        // removed from the source, due to the constraint on Tree having one incoming edge
        List<E> edgesToAdd = new LinkedList<>();
        edgesToAdd.add(newEdge);
        for(V vertex: verticesToRemove) {
            if(vertex == newEdge.getEnd()) {
                continue;
            }
            edgesToAdd.add(getInEdges(vertex).iterator().next());
        }

        // Now the vertices can be removed
        for(V vertex: verticesToRemove) {
            removeVertex(vertex);
        }

        // Add the edges to the target
        assert(target.getRoot() != null);

        for(E edge: edgesToAdd) {
            target.addEdge(edge, edge.getStart(), edge.getEnd());
        }
    }

    public List<V> getChildren(V node) {
        Collection<E> outEdges = getOutEdges(node);

        List<V> result = new LinkedList();
        for(Edge<V> edge: outEdges) {
            result.add(edge.getEnd());
        }

        return result;
    }

    /**
     * Vertices with no incoming edges
     * @return roots of all trees
     */
    public List<V> getRoots() {
        List<V> result = new LinkedList<>();
        for(V vertex: getVertices()) {
            if(getInEdges(vertex).size() == 0) {
                result.add(vertex);
            }
        }

        return result;
    }

    public V getRoot() {

        if(getVertices().size() == 0) {
            return null;
        }

        // take any node and go up the ancestor path until the first node is reached.
        // This node is the root of the tree
        V current = getVertices().iterator().next();
        V parent = getParent(current);

        while(parent != null) {
            current = getParent(current);
            parent = getParent(current);
        }

        return current;
    }

    public int getHeight() {
        return getHeight(getRoot());
    }

    private int getHeight(V node) {
        int result = 0;

        if(node == null) {
            return result;
        }

        for(Edge<V> edge: getOutEdges(node)) {
            result = Math.max(result, getHeight(edge.getEnd()));
        }

        return result + 1;
    }

    @Override
    /**
     * We would like the getOutEdges to have order
     */
    protected Collection<E> newEdgeCollection() {
        // We expect the edges to be iterated in the order it was added/constructed
        return new LinkedHashSet<E>();
    }

    @Override
    protected Collection<E> newEdgeCollection(Collection<E> input) {
        return new LinkedHashSet<E>(input);
    }

    protected String getGraphName() {
        return Settings.getBaseName(this.getClass().getName());
    }

    protected String getLabel(V vertex) {
        return vertex.toString();
    }

    protected void writeDOTVertices(BufferedWriter writer) throws IOException
    {
        /*
         * Do a BFS, so the tree gets laid out in the correct order
         */
        List<V> bfsOrder = new ArrayList(getVertices().size());
        bfsOrder.add(getRoot());
        int currentPos = 0;

        while(currentPos != bfsOrder.size()) {
            V vertex = bfsOrder.get(currentPos++);
            bfsOrder.addAll(getChildren(vertex));

            writer.write("  " + vertex + "[label = \"" + getLabel(vertex) + "\"]\n");
        }
    }

    @Override
    protected void writeGraphvizDotHeader(BufferedWriter writer) throws IOException
    {
        writer.write("  ranksep=1.5; nodesep=1;\n");
        writer.write("  node[shape=record,height=.1];\n");
    }

    @Override
    public void writeGraphvizDot(BufferedWriter writer) throws IOException
    {
        // write the vertices in the desired order
        writeDOTVertices(writer);

        // Write the edges
        // Do not constrain all the types that have been explicitly expanded
        for(E edge: getEdges()) {

            StringBuilder result = new StringBuilder("  " + edge.getStart() + " -> " + edge.getEnd());
            result.append("[label=").append(edge.getName()).append("]\n");


            writer.write(result.toString());
        }
    }
}
