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

package tools.xor.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.db.base.Directory;
import tools.xor.db.base.Patent;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.DFAtoRE;
import tools.xor.util.DFAtoRE.Expression;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;


public class DefaultAggregatePaths extends AbstractDBTest {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	@Autowired
	protected AggregateManager aggregateManager;

	@Test
	public void checkDirPath() {
		DataAccessService das = aggregateManager.getDAS();

		Type dir = das.getType(Directory.class);
		AggregateView view = aggregateManager.getDAS().getView((EntityType) dir);

		Set<String> paths = AggregatePropertyPaths.enumerate(dir);
		System.out.println("********* Directory model paths **********");
		for(String path: paths) {
			System.out.println(path);
		}

		Map<String, StateGraph<State, Edge<State>>> sgAll = view.getStateGraph();
		System.out.println("********* Key **********");
		for(Map.Entry<String, StateGraph<State, Edge<State>>> entry: sgAll.entrySet()) {
			System.out.println(entry.getKey());
			printGraph(entry.getValue());
		}
	}

	private void printGraph(StateGraph<State, Edge<State>> sg) {

		Map<Type, State> v = sg.getStates();
		for(State s: v.values()) {
			System.out.println("   " + s.getName());
		}
	}

	public void checkPaths() {	
		DataAccessService das = aggregateManager.getDAS(); 

		Type person = das.getType(Person.class);
		Set<String> paths = AggregatePropertyPaths.enumerate(person);

		assert(paths.size() > 0);
		assert(paths.contains("userName"));
		assert(paths.contains("name"));
		assert(paths.contains("version"));
	}
	
	@Test
	public void checkBasePaths() {	
		DataAccessService das = aggregateManager.getDAS(); 

		Type task = das.getType(Task.class);
		Set<String> paths = AggregatePropertyPaths.enumerateBase(task);

		assert(paths.size() > 0);
		assert(paths.contains("name"));
		assert(paths.contains("version"));
	}	
	
	@Test
	public void checkStateGraph() {
		DataAccessService das = aggregateManager.getDAS(); 

		Type task = das.getType(Task.class);
		AggregateView view = aggregateManager.getDAS().getView((EntityType) task);
		List<Property> properties = view.getStateGraph((EntityType) task).next(task);
		
		List<String> propertyNames = new ArrayList<String>();
		for(Property p: properties) {
			propertyNames.add(p.getName());
		}
		
		assert(propertyNames.contains("version"));
		assert(propertyNames.contains("quote"));
		assert(propertyNames.contains("dependants"));
		assert(properties.size() == 18);
	}

	public void checkCyclicPaths() {	
		DataAccessService das = aggregateManager.getDAS(); 

		Type task = das.getType(Task.class);
		Set<String> paths = AggregatePropertyPaths.enumerate(task);

		assert(paths.size() > 0);
		
		assert(paths.contains("(taskChildren. + dependants. + auditTask.)*scheduledFinish") ||
				paths.contains("(dependants. + taskChildren. + auditTask.)*scheduledFinish") ||
				paths.contains("(taskChildren. + auditTask. + dependants.)*scheduledFinish") ||
				paths.contains("(auditTask. + dependants. + taskChildren.)*scheduledFinish") ||
				paths.contains("(dependants. + auditTask. + taskChildren.)*scheduledFinish") ||
				paths.contains("(auditTask. + taskChildren. + dependants.)*scheduledFinish")
				);
		
		assert(paths.contains("(taskChildren. + dependants. + auditTask.)*name") ||
				paths.contains("(dependants. + taskChildren. + auditTask.)*name") ||
				paths.contains("(taskChildren. + auditTask. + dependants.)*name") ||
				paths.contains("(auditTask. + dependants. + taskChildren.)*name") ||
				paths.contains("(dependants. + auditTask. + taskChildren.)*name") ||
				paths.contains("(auditTask. + taskChildren. + dependants.)*name")
				);
		
		Type patent = das.getType(Patent.class);
		paths = AggregatePropertyPaths.enumerate(patent);
		// Print the paths
		Level oldLevel = logger.getLevel();
		//logger.setLevel(Level.DEBUG);
		StringBuilder pathstr = new StringBuilder("\n");
		for(String path: paths) {
			pathstr.append(path + "\n");
		}
		logger.debug(pathstr);
		logger.setLevel(oldLevel);
	}	

	@Test
	public void testDFA1() {
		/*
		 * Has two states - A and B and 3 transitions:
		 * 
		 * 1) A ...a..> A
		 * 2) A ...b..> B
		 * 3) B ...b..> B
		 * 
		 */

		final String INPUT_A = "a";
		final String INPUT_B = "b";

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");

		State stateA = new State(A, true);	
		State stateB = new State(B, false);
		
		Edge t1 = new Edge(INPUT_A, stateA, stateA);
		Edge t2 = new Edge(INPUT_B, stateA, stateB);
		Edge t3 = new Edge(INPUT_B, stateB, stateB);

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);

		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);

		// Set B as the finish state
		stateB.setFinishState(true);
		
		re.createEquations(); 
		Expression result = re.processEquations();

		logger.debug("Resulting regular expresson: " + result.toString());
		assert(result.toString().equals("a*bb*"));
	}

	@Test
	public void testDFA2() {
		/*
		 * Has three states - A, B, C and 6 transitions:
		 * 
		 * 1) A ...a..> B
		 * 2) A ...b..> C
		 * 3) B ...a..> C
		 * 4) B ...b..> A
		 * 5) C ...a..> A
		 * 6) C ...b..> B
		 * 
		 */

		final String INPUT_A = "a";
		final String INPUT_B = "b";

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");
		Type C = new TypeTest("C");		

		State stateA = new State(A, true);
		State stateB = new State(B, false);
		State stateC = new State(C, false);
		
		Edge t1 = new Edge(INPUT_A, stateA, stateB);
		Edge t2 = new Edge(INPUT_B, stateA, stateC);
		Edge t3 = new Edge(INPUT_A, stateB, stateC);
		Edge t4 = new Edge(INPUT_B, stateB, stateA);
		Edge t5 = new Edge(INPUT_A, stateC, stateA);
		Edge t6 = new Edge(INPUT_B, stateC, stateB);		

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);

		// A is both start and finish state
		stateA.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "(ab + (b + aa)(ba)*(bb + a))*") ||
				result.toString().equals( "(ab + (b + aa)(ba)*(a + bb))*") ||
				result.toString().equals( "(ab + (aa + b)(ba)*(bb + a))*") ||
				result.toString().equals( "(ab + (aa + b)(ba)*(a + bb))*") ||
				result.toString().equals( "((aa + b)(ba)*(bb + a) + ab)*") ||
				result.toString().equals( "((aa + b)(ba)*(a + bb) + ab)*") ||
				result.toString().equals( "((b + aa)(ba)*(bb + a) + ab)*") ||
				result.toString().equals( "((b + aa)(ba)*(a + bb) + ab)*") ||
				result.toString().equals( "(ba + (bb + a)(ab)*(b + aa))*") ||
				result.toString().equals( "(ba + (bb + a)(ab)*(aa + b))*") ||
				result.toString().equals( "(ba + (a + bb)(ab)*(b + aa))*") ||
				result.toString().equals( "(ba + (a + bb)(ab)*(aa + b))*") ||
				result.toString().equals( "((bb + a)(ab)*(aa + b) + ba)*") ||
				result.toString().equals( "((a + bb)(ab)*(aa + b) + ba)*") ||
				result.toString().equals( "((bb + a)(ab)*(b + aa) + ba)*") ||
				result.toString().equals( "((a + bb)(ab)*(b + aa) + ba)*"));				

	}	

	@Test
	public void testDFA3() {
		/*
		 * Has four states - A, B, C, D and 6 transitions:
		 * 
		 * 1) A ...a..> A
		 * 2) A ...b..> B
		 * 3) B ...b..> B
		 * 4) B ...c..> C
		 * 5) C ...d..> D
		 * 6) D ...b..> B
		 * 
		 */

		final String INPUT_A = "a";
		final String INPUT_B = "b";
		final String INPUT_C = "c";
		final String INPUT_D = "d";		

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");
		Type C = new TypeTest("C");
		Type D = new TypeTest("D");			
		
		State stateA = new State(A, true);		
		State stateB = new State(B, false);
		State stateC = new State(C, false);
		State stateD = new State(D, false);	

		Edge t1 = new Edge(INPUT_A, stateA, stateA);
		Edge t2 = new Edge(INPUT_B, stateA, stateB);
		Edge t3 = new Edge(INPUT_B, stateB, stateB);
		Edge t4 = new Edge(INPUT_C, stateB, stateC);
		Edge t5 = new Edge(INPUT_D, stateC, stateD);
		Edge t6 = new Edge(INPUT_B, stateD, stateB);		

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);

		stateD.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);		

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*b(cdb + b)*cd") || result.toString().equals( "a*b(b + cdb)*cd"));		
	}	
	
	@Test
	/**
	 * Same as testDFA3, but the finish state is B
	 */
	public void testDFA4() {
		/*
		 * Has four states - A, B, C, D and 6 transitions:
		 * 
		 * 1) A ...a..> A
		 * 2) A ...b..> B
		 * 3) B ...b..> B
		 * 4) B ...c..> C
		 * 5) C ...d..> D
		 * 6) D ...b..> B
		 * 
		 */

		final String INPUT_A = "a";
		final String INPUT_B = "b";
		final String INPUT_C = "c";
		final String INPUT_D = "d";		

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");
		Type C = new TypeTest("C");
		Type D = new TypeTest("D");			
		
		State stateA = new State(A, true);	
		State stateB = new State(B, false);
		State stateC = new State(C, false);
		State stateD = new State(D, false);	

		Edge t1 = new Edge(INPUT_A, stateA, stateA);
		Edge t2 = new Edge(INPUT_B, stateA, stateB);
		Edge t3 = new Edge(INPUT_B, stateB, stateB);
		Edge t4 = new Edge(INPUT_C, stateB, stateC);
		Edge t5 = new Edge(INPUT_D, stateC, stateD);
		Edge t6 = new Edge(INPUT_B, stateD, stateB);		

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);
		
		stateB.setFinishState(true);

		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);			

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*b(cdb + b)*") || result.toString().equals( "a*b(b + cdb)*"));		
	}
	
	@Test
	/**
	 * Same as testDFA3, but the finish state is A
	 */
	public void testDFA5() {
		/*
		 * Has four states - A, B, C, D and 6 transitions:
		 * 
		 * 1) A ...a..> A
		 * 2) A ...b..> B
		 * 3) B ...b..> B
		 * 4) B ...c..> C
		 * 5) C ...d..> D
		 * 6) D ...b..> B
		 * 
		 */

		final String INPUT_A = "a";
		final String INPUT_B = "b";
		final String INPUT_C = "c";
		final String INPUT_D = "d";		

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");
		Type C = new TypeTest("C");
		Type D = new TypeTest("D");			
		
		State stateA = new State(A, true);	
		State stateB = new State(B, false);
		State stateC = new State(C, false);
		State stateD = new State(D, false);	

		Edge t1 = new Edge(INPUT_A, stateA, stateA);
		Edge t2 = new Edge(INPUT_B, stateA, stateB);
		Edge t3 = new Edge(INPUT_B, stateB, stateB);
		Edge t4 = new Edge(INPUT_C, stateB, stateC);
		Edge t5 = new Edge(INPUT_D, stateC, stateD);
		Edge t6 = new Edge(INPUT_B, stateD, stateB);		

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);
		
		// A is both start and finish state
		stateA.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);		

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*"));		
	}	
	
	@Test
	/**
	 * Same as testDFA3, but the finish state is C
	 */
	public void testDFA6() {
		/*
		 * Has four states - A, B, C, D and 6 transitions:
		 * 
		 * 1) A ...a..> A
		 * 2) A ...b..> B
		 * 3) B ...b..> B
		 * 4) B ...c..> C
		 * 5) C ...d..> D
		 * 6) D ...b..> B
		 * 
		 */

		final String INPUT_A = "a";
		final String INPUT_B = "b";
		final String INPUT_C = "c";
		final String INPUT_D = "d";		

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");
		Type C = new TypeTest("C");
		Type D = new TypeTest("D");			
		
		State stateA = new State(A, true);	
		State stateB = new State(B, false);
		State stateC = new State(C, false);
		State stateD = new State(D, false);	

		Edge t1 = new Edge(INPUT_A, stateA, stateA);
		Edge t2 = new Edge(INPUT_B, stateA, stateB);
		Edge t3 = new Edge(INPUT_B, stateB, stateB);
		Edge t4 = new Edge(INPUT_C, stateB, stateC);
		Edge t5 = new Edge(INPUT_D, stateC, stateD);
		Edge t6 = new Edge(INPUT_B, stateD, stateB);		

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);
		
		stateC.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*b(cdb + b)*c") || result.toString().equals( "a*b(b + cdb)*c") || result.toString().equals("a*bb*c(dbb*c)*"));		
	}
	
	@Test
	/**
	 * Simple DFA with no cycles. C is finish state.
	 */
	public void testDFA7() {
		/*
		 * Has three states - A, B, C and 2 transitions:
		 * 
		 * 1) A ...b..> B
		 * 2) B ...c..> C
		 * 
		 */

		final String INPUT_B = "b";
		final String INPUT_C = "c";

		Type A = new TypeTest("A");
		Type B = new TypeTest("B");
		Type C = new TypeTest("C");		

		State stateA = new State(A, true);
		State stateB = new State(B, false);
		State stateC = new State(C, false);
		
		Edge t1 = new Edge(INPUT_B, stateA, stateB);
		Edge t2 = new Edge(INPUT_C, stateB, stateC);	

		DFAtoRE re = new DFAtoRE();
		re.setAggregateType(A);

		stateC.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "bc"));				

	}	
	
	@Test
	public void testDFATask() {
		DataAccessService das = aggregateManager.getDAS(); 

		Type task = das.getType(Task.class);
		
		DFAtoRE re = new DFAtoRE(task);
		Map<State, Expression> regEx = re.getRegEx();
		
		for(State state: regEx.keySet()) {
			Expression exp = regEx.get(state);
			if(state.getName().endsWith("Task")) {
				assert(exp.toString().equals("(taskChildren. + auditTask. + dependants.)*") ||
						exp.toString().equals("(auditTask. + taskChildren. + dependants.)*") ||
						exp.toString().equals("(dependants. + taskChildren. + auditTask.)*") ||
						exp.toString().equals("(dependants. + auditTask. + taskChildren.)*") ||
						exp.toString().equals("(taskChildren. + dependants. + auditTask.)*") ||
						exp.toString().equals("(auditTask. + dependants. + taskChildren.)*"));						
			} else if (state.getName().endsWith("Quote")) {			
				assert(exp.toString().equals("(taskChildren. + auditTask. + dependants.)*quote.") ||
						exp.toString().equals("(auditTask. + taskChildren. + dependants.)*quote.") ||
						exp.toString().equals("(dependants. + taskChildren. + auditTask.)*quote.") ||
						exp.toString().equals("(dependants. + auditTask. + taskChildren.)*quote.") ||
						exp.toString().equals("(taskChildren. + dependants. + auditTask.)*quote.") ||
						exp.toString().equals("(auditTask. + dependants. + taskChildren.)*quote."));				
			}
		}
	}
	
	@Test
	public void checkOptionModel() {
		List<String> paths = aggregateManager.getMetaModel().getAttributePaths("tools.xor.db.base.MetaEntity");
		assert(paths != null && paths.size() > 0);
	}
}
