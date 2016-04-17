package tools.xor.util;

import org.junit.Assert;
import org.junit.Test;

import tools.xor.AbstractDBTest.TypeTest;
import tools.xor.Type;
import tools.xor.util.graph.StateGraph;

/**
 * Unit test the TopoSort algorithm.
 */

public class TopoSortTest {

    /**
     * Test for a binary strongly connected component.
     */
	@Test
	public void test1() {

		Type root = new TypeTest("A");
		State stateA = new State(root, false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);
		StateGraph<State, Edge<State>> dg = new StateGraph<State, Edge<State>>(root);
		
		dg.addVertex(stateA);
		dg.addVertex(stateB);
		dg.addVertex(stateC);
		dg.addEdge(new Edge("edge1", stateB, stateA), stateB, stateA);
		dg.addEdge(new Edge("edge2", stateC, stateB), stateC, stateB);
		
		// The states should be in order A, B, C
		Assert.assertTrue(dg.getVertex(1).getName().equals("A"));
		Assert.assertTrue(dg.getVertex(2).getName().equals("B"));
		Assert.assertTrue(dg.getVertex(3).getName().equals("C"));
		
		// Do topological sorting and the order should be C, B, A
		dg.toposort();

		Assert.assertTrue(dg.getVertex(1).getName().equals("C"));
		Assert.assertTrue(dg.getVertex(2).getName().equals("B"));
		Assert.assertTrue(dg.getVertex(3).getName().equals("A"));
	}

}