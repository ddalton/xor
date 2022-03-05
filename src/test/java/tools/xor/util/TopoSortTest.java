package tools.xor.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tools.xor.AbstractDBTest.TypeTest;
import tools.xor.Type;
import tools.xor.service.DomainShape;
import tools.xor.service.Shape;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.TreeOperations;
import tools.xor.view.ReconstituteVisitor;

/**
 * Unit test the TopoSort algorithm.
 */

public class TopoSortTest {

    /**
     * Test for a binary strongly connected component.
     */
	@Test
	public void test1() {

		Shape shape = new DomainShape("Test", null, null);

		Type root = new TypeTest("A");
		State stateA = new State(root, false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);
		StateGraph<State, Edge<State>> dg = new StateGraph<State, Edge<State>>(root, shape);
		
		dg.addVertex(stateA);
		dg.addVertex(stateB);
		dg.addVertex(stateC);
		dg.addEdge(new Edge("edge1", stateB, stateA), stateB, stateA);
		dg.addEdge(new Edge("edge2", stateC, stateB), stateC, stateB);
		
		// The states should be in order A, B, C
		assertTrue(dg.getVertex(1).getName().equals("A"));
		assertTrue(dg.getVertex(2).getName().equals("B"));
		assertTrue(dg.getVertex(3).getName().equals("C"));
		
		// Do topological sorting and the order should be C, B, A
		dg.renumber(dg.toposort(null));

		assertTrue(dg.getVertex(1).getName().equals("C"));
		assertTrue(dg.getVertex(2).getName().equals("B"));
		assertTrue(dg.getVertex(3).getName().equals("A"));
	}

	/**
	 * The algorithm to test reconstitution will first process nodes that are
	 * subtypes using a pre-order DFS traversal and then process the remaining nodes
	 * using BFS
	 */
	@Test
	public void testReconstitution() {
		// create 10 nodes
		State n1 = new State(new TypeTest("N1"), false);
		State n2 = new State(new TypeTest("N2"), false);
		State n3 = new State(new TypeTest("N3"), false);
		State n4 = new State(new TypeTest("N4"), false);
		State n5 = new State(new TypeTest("N5"), false);
		State n6 = new State(new TypeTest("N6"), false);
		State n7 = new State(new TypeTest("N7"), false);
		State n8 = new State(new TypeTest("N8"), false);
		State n9 = new State(new TypeTest("N9"), false);
		State n10 = new State(new TypeTest("N10"), false);

		TreeOperations aggregateTree = new TreeOperations();

		// Named edges
		aggregateTree.addEdge(new Edge("e1", n1, n2), n1, n2);
		aggregateTree.addEdge(new Edge("e2", n2, n5), n2, n5);
		aggregateTree.addEdge(new Edge("e3", n4, n6), n4, n6);
		aggregateTree.addEdge(new Edge("e4", n6, n9), n6, n9);

		// Inheritance edges - goes from supertype to subtype
		aggregateTree.addEdge(new Edge(DFAtoNFA.UNLABELLED, n2, n3), n2, n3);
		aggregateTree.addEdge(new Edge(DFAtoNFA.UNLABELLED, n2, n4), n2, n4);
		aggregateTree.addEdge(new Edge(DFAtoNFA.UNLABELLED, n4, n10), n4, n10);

		aggregateTree.addEdge(new Edge(DFAtoNFA.UNLABELLED, n6, n7), n6, n7);
		aggregateTree.addEdge(new Edge(DFAtoNFA.UNLABELLED, n7, n8), n7, n8);

		aggregateTree.reconstitute(new ReconstituteVisitor()
		{
			@Override public void visit (Object node, boolean isSubtype)
			{
				System.out.println(String.format("Node: %s, isSubtype: %s", node.toString(), isSubtype));
			}
		});
	}
}
