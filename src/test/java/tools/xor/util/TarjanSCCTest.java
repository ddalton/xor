package tools.xor.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.Stack;

import org.junit.jupiter.api.Test;

import tools.xor.AbstractDBTest.TypeTest;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.DirectedSparseGraph;

/**
 * Unit test the Tarjan algorithm.
 * Refer: https://github.com/1123/johnson/blob/master/src/test/java/jgraphalgos/tarjan/TestTarjan.java
 */

public class TarjanSCCTest {

    /**
     * Test for a binary strongly connected component.
     */

	@Test
	public void test1() {

		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateA), stateB, stateA);
		
		Stack<Set<State>> sccs = dg.getSCC();

		assertTrue(sccs.size() == 1);
		assertTrue(sccs.get(0).contains(stateA));
		assertTrue(sccs.get(0).contains(stateB));
	}

    /**
     * This test asserts that a graph with a single edge does not contain strongly connected components.
     */

	@Test
	public void test2() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		
		Stack<Set<State>> sccs = dg.getSCC();
		
		assertTrue(sccs.size() == 0);
	}

    /**
     * Test for discovery of a strongly connected component with 4 nodes (a cycle).
     */

	@Test
	public void test3() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateC), stateB, stateC);
		dg.addEdge(new Edge("edge3", stateC, stateD), stateC, stateD);
		dg.addEdge(new Edge("edge4", stateD, stateA), stateD, stateA);
		
		Stack<Set<State>> sccs = dg.getSCC();
		
		assertTrue(sccs.size() == 1);
		assertTrue(sccs.get(0).contains(stateA));
		assertTrue(sccs.get(0).contains(stateB));
		assertTrue(sccs.get(0).contains(stateC));
		assertTrue(sccs.get(0).contains(stateD));
	}

    /**
     * Test for discovery of two binary strongly connected components.
     */

	@Test
	public void test4() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateA), stateB, stateA);
		dg.addEdge(new Edge("edge3", stateC, stateD), stateC, stateD);
		dg.addEdge(new Edge("edge4", stateD, stateC), stateD, stateC);
		
		Stack<Set<State>> sccs = dg.getSCC();
		
		System.out.println(sccs.get(0));
		System.out.println(sccs.get(1));
		assertTrue(sccs.size() == 2);
		assertTrue( 
				(sccs.get(0).contains(stateA) && sccs.get(0).contains(stateB) && sccs.get(1).contains(stateC) && sccs.get(1).contains(stateD) ) ||
				(sccs.get(1).contains(stateA) && sccs.get(1).contains(stateB) && sccs.get(0).contains(stateC) && sccs.get(0).contains(stateD) )	
			);
	}
	
	
}