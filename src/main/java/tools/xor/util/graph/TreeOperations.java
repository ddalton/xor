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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import tools.xor.Settings;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.Edge;
import tools.xor.util.Vertex;
import tools.xor.view.ReconstituteVisitor;

public class TreeOperations<V extends Vertex, E extends Edge<V>> extends DirectedSparseGraph<V, E> implements Tree<V, E>
{
    @Override
    public V getParent(V node) {
        return getParent(this, node);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree & DirectedGraph<V, E>> V getParent(T tree, V node) {
        Collection<E> inEdges = tree.getInEdges(node);

        assert inEdges.size() <= 1 : "A tree can have at most one incoming edge";

        return (inEdges.size() == 1) ? inEdges.iterator().next().getStart(): null;
    }

    public V getsuperType(V node) {
        return getSupertype(this, node);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree & DirectedGraph<V, E>> V getSupertype(T tree, V node) {
        Collection<E> inEdges = tree.getInEdges(node);

        assert inEdges.size() <= 1 : "A tree can have at most one incoming edge";

        Edge<V> incoming = (inEdges.size() == 1) ? inEdges.iterator().next() : null;

        // check if it is an inheritance edge
        if(incoming != null && incoming.isUnlabelled()) {
            return incoming.getStart();
        }

        return null;
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
    @Override
    public <Q extends TreeOperations<V, E>> void split(E splitAtEdge, E newEdge, Q target) {
        split(this, splitAtEdge, newEdge, target);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree & DirectedGraph<V, E>> void split(T tree, E splitAtEdge, E newEdge, T target) {
        List<V> vertices = new LinkedList<>();
        List<V> verticesToRemove = new LinkedList<>();

        // mark the vertex and all its descendants from the tree
        vertices.add(splitAtEdge.getEnd());
        while(!vertices.isEmpty()) {
            V vertex = vertices.remove(0);
            verticesToRemove.add(vertex);

            vertices.addAll(tree.getChildren(vertex));
        }

        // The vertices can only be added to the target after they are first
        // removed from the source, due to the constraint on Tree having one incoming edge
        List<E> edgesToAdd = new LinkedList<>();
        if(newEdge != null) {
            edgesToAdd.add(newEdge);
        }
        for(V vertex: verticesToRemove) {
            if(vertex == splitAtEdge.getEnd()) {
                continue;
            }
            edgesToAdd.add(tree.getInEdges(vertex).iterator().next());
        }

        // Now the vertices can be removed
        for(V vertex: verticesToRemove) {
            tree.removeVertex(vertex);
        }

        // Add the edges to the target
        assert(target.getRoot() != null);

        for(E edge: edgesToAdd) {
            target.addEdge(edge, edge.getStart(), edge.getEnd());
        }
    }

    @Override
    public List<V> getChildren(V node) {
        return getChildren(this, node);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree & DirectedGraph<V, E>> List<V> getChildren(T tree, V node) {
        Collection<E> outEdges = tree.getOutEdges(node);

        List<V> result = new LinkedList();
        for(Edge<V> edge: outEdges) {
            result.add(edge.getEnd());
        }

        return result;
    }

    public List<V> getSubtypes(V node) {
        return getSubtypes(this, node);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree & DirectedGraph<V, E>> List<V> getSubtypes(T tree, V node) {
        Collection<E> outEdges = tree.getOutEdges(node);

        List<V> result = new LinkedList();
        for(Edge<V> edge: outEdges) {
            if(edge.isUnlabelled()) {
                result.add(edge.getEnd());
            }
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

    public boolean isRoot(V vertex) {
        if(getInEdges(vertex).size() == 0) {
            return true;
        }

        return false;
    }

    @Override
    public V getRoot() {

        return getRoot(this);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree<V, E> & DirectedGraph<V, E>> V getRoot(T tree) {

        if(tree.getVertices().size() == 0) {
            return null;
        }

        // take any node and go up the ancestor path until the first node is reached.
        // This node is the root of the tree
        V current = tree.getVertices().iterator().next();
        V parent = tree.getParent(current);

        while(parent != null) {
            current = tree.getParent(current);
            parent = tree.getParent(current);
        }

        return current;
    }

    @Override
    public int getHeight() {
        return getHeight(this);
    }

    @Override public String getPathToRoot (V node)
    {
        return getPathToRoot(this, node);
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree<V, E> & DirectedGraph<V, E>> String getPathToRoot(T tree, V node) {
        // A node in a tree has only one incoming edge
        // We walk up the tree from the given node until we reach the root node

        V root = tree.getRoot();
        Stack<String> pathSteps = new Stack<>();
        while(node != root) {
            Collection<E> inEdges = tree.getInEdges(node);

            assert inEdges.size() == 1 : "Non-root node should have only one incoming edge";

            Edge<V> incoming = inEdges.iterator().next();
            pathSteps.push(incoming.getName());

            node = incoming.getStart();
        }

        // Build the path
        StringBuilder path = new StringBuilder("");
        while(!pathSteps.isEmpty()) {
            if(path.length() > 0) {
                path.append(Settings.PATH_DELIMITER);
            }
            String step = pathSteps.pop();
            path.append(step);
        }

        return path.toString();
    }

    public static <V extends Vertex, E extends Edge<V>, T extends Tree<V, E> & DirectedGraph<V, E>> int getHeight(T tree) {
        return getHeight(tree, tree.getRoot());
    }

    private static <V extends Vertex, E extends Edge<V>, T extends Tree<V, E> & DirectedGraph<V, E>> int getHeight(T tree, V node) {
        int result = 0;

        if(node == null) {
            return result;
        }

        for(Edge<V> edge: tree.getOutEdges(node)) {
            result = Math.max(result, getHeight(tree, edge.getEnd()));
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
        return vertex.getDisplayName();
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

            String label = edge.getDisplayName();
            if(DFAtoNFA.UNLABELLED.equals(label)) {
                result.append("[dir=back, arrowtail=empty, weight=2]\n");
            } else {
                result.append("[label=").append(edge.getDisplayName()).append("]\n");
            }


            writer.write(result.toString());
        }
    }

    public void reconstitute(ReconstituteVisitor visitor) {
        List<V> nodes = new LinkedList<>();
        Map<V, Object> processed = new IdentityHashMap<V, Object>();


        nodes.addAll(getRoots());
        for(V node: nodes) {
            reconstituteSubtype(node, visitor, processed);
        }

        // Do a BFS traversal of the remaining nodes
        while(!nodes.isEmpty()) {
            // remove the first node
            V node = nodes.remove(0);
            if(!processed.containsKey(node)) {
                visitor.visit(node, false);
            } else {
                processed.put(node, null);
            }

            // We need to go through all children including
            // inheritance relationships because some associations can only be reached
            // through an inheritance relationship
            nodes.addAll(getChildren(node));
        }
    }

    private void reconstituteSubtype(V node, ReconstituteVisitor visitor, Map<V, Object> processed) {
        // do a pre-order DFS of all inheritance edges
        Collection<E> outEdges = getOutEdges(node);
        for(E edge: outEdges) {
            reconstituteSubtype(edge.getEnd(), visitor, processed);
        }

        Collection<E> inEdges = getInEdges(node);
        if(inEdges.size() == 1) {
            E edge = inEdges.iterator().next();
            if(DFAtoNFA.UNLABELLED.equals(edge.getName())) {
                // visit the subtype
                visitor.visit(node, true);
                processed.put(node, null);
            }
        }
    }
}
