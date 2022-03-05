package tools.xor.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.xor.AbstractDBTest.TypeTest;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.DirectedSparseGraph;

/**
 * Unit test the Johnson algorithm.
 */

public class JohnsonTest {
	
	@Test
	public void testDisjointCycles() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateA), stateB, stateA);
		dg.addEdge(new Edge("edge3", stateC, stateD), stateC, stateD);
		dg.addEdge(new Edge("edge4", stateD, stateC), stateD, stateC);
		
		List<List<Vertex>> cycles = dg.getCircuits();
		
		System.out.println(GraphUtil.printCycles(cycles));
		assertTrue(cycles.size() == 2);
		
		List<List<Edge>> loops = dg.getLoops();		
		assertTrue(loops.size() == 2);
		
	}
	
	/**
	 * We test restore by invoking getCircuits() on the same graph more than once
	 */
	@Test
	public void testOverlappingCycles() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateC), stateB, stateC);
		dg.addEdge(new Edge("edge3", stateB, stateD), stateB, stateD);
		dg.addEdge(new Edge("edge4", stateC, stateA), stateC, stateA);
		dg.addEdge(new Edge("edge5", stateD, stateA), stateD, stateA);
		
		List<List<Vertex>> cycles = dg.getCircuits();
		
		assertTrue(cycles.size() == 2);
		
		cycles = dg.getCircuits();
		assertTrue(cycles.size() == 2);
		
		System.out.println(GraphUtil.printCycles(cycles));
	}
	
	@Test
	public void testOverlappingCycles2() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateC), stateB, stateC);
		dg.addEdge(new Edge("edge3", stateB, stateD), stateB, stateD);
		dg.addEdge(new Edge("edge4", stateC, stateA), stateC, stateA);
		dg.addEdge(new Edge("edge5", stateD, stateA), stateD, stateA);
		dg.addEdge(new Edge("edge6", stateC, stateD), stateC, stateD);
		
		List<List<Vertex>> cycles = dg.getCircuits();
		
		assertTrue(cycles.size() == 3);
		
		cycles = dg.getCircuits();
		assertTrue(cycles.size() == 3);
		
		System.out.println(GraphUtil.printCycles(cycles));
	}
	
	@Test
	public void testOverlappingCycles3() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateC), stateB, stateC);
		dg.addEdge(new Edge("edge3", stateB, stateD), stateB, stateD);
		dg.addEdge(new Edge("edge4", stateC, stateA), stateC, stateA);
		dg.addEdge(new Edge("edge5", stateD, stateA), stateD, stateA);
		dg.addEdge(new Edge("edge6", stateC, stateD), stateC, stateD);
		dg.addEdge(new Edge("edge7", stateB, stateA), stateB, stateA);
		
		List<List<Vertex>> cycles = dg.getCircuits();
		
		assertTrue(cycles.size() == 4);
		
		cycles = dg.getCircuits();
		assertTrue(cycles.size() == 4);
		
		System.out.println(GraphUtil.printCycles(cycles));
		
		List<List<Edge>> loops = dg.getLoops();
		assertTrue(loops.size() == 4);
	}
	
	@Test
	public void testOverlappingCycles4() {
		State stateA = new State(new TypeTest("A"), false);	
		State stateB = new State(new TypeTest("B"), false);
		State stateC = new State(new TypeTest("C"), false);	
		State stateD = new State(new TypeTest("D"), false);
		State stateE = new State(new TypeTest("E"), false);
		
		DirectedGraph<State, Edge> dg = new DirectedSparseGraph<State, Edge>();
		dg.addEdge(new Edge("edge1", stateA, stateB), stateA, stateB);
		dg.addEdge(new Edge("edge2", stateB, stateC), stateB, stateC);
		dg.addEdge(new Edge("edge3", stateD, stateB), stateD, stateB);
		dg.addEdge(new Edge("edge4", stateC, stateD), stateC, stateD);
		dg.addEdge(new Edge("edge5", stateD, stateA), stateD, stateA);
		dg.addEdge(new Edge("edge6", stateA, stateE), stateA, stateE);
		dg.addEdge(new Edge("edge7", stateE, stateB), stateE, stateB);
		dg.addEdge(new Edge("edge8", stateC, stateE), stateC, stateE);
		
		List<List<Vertex>> cycles = dg.getCircuits();
		
		assertTrue(cycles.size() == 4);
		
		cycles = dg.getCircuits();
		assertTrue(cycles.size() == 4);
		
		System.out.println(GraphUtil.printCycles(cycles));
	}	
}