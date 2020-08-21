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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.AssociationSetting;
import tools.xor.EntityType;
import tools.xor.HierarchyGenerator;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SharedCounterGenerator;
import tools.xor.Type;
import tools.xor.db.base.Directory;
import tools.xor.db.base.Employee;
import tools.xor.db.base.Patent;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.generator.LocalizedString;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.service.MetaModel;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.DFAtoRE;
import tools.xor.util.DFAtoRE.Expression;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.TypeGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.View;

public class DefaultAggregatePaths extends AbstractDBTest {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	@Autowired
	protected AggregateManager aggregateManager;

	@Test
	public void checkDirPath() {
		DataModel das = aggregateManager.getDataModel();

		Type dir = das.getShape().getType(Directory.class);
		View view = aggregateManager.getDataModel().getShape().getView((EntityType) dir);

		Set<String> paths = AggregatePropertyPaths.enumerateRegEx(dir, das.getShape());
		System.out.println("********* Directory model paths **********");
		for(String path: paths) {
			System.out.println(path);
		}

		TypeGraph<State, Edge<State>> sg = view.getTypeGraph((EntityType)dir);
		if(sg != null) {
			System.out.println(sg.toString());
		}
	}

	public void checkPaths() {	
		DataModel das = aggregateManager.getDataModel(); 

		Type person = das.getShape().getType(Person.class);
		Set<String> paths = AggregatePropertyPaths.enumerateRegEx(person, das.getShape());

		assert(paths.size() > 0);
		String path = paths.iterator().next();
		assert(path.contains("userName"));
		assert(path.contains("name"));
		assert(path.contains("version"));
	}

	@Test
	public void checkLocale() {
		LocalizedString obj = new LocalizedString(new String[]{"abc"});

		for(int i=0; i < 10; i++) {
			System.out.println("Random: " + obj.randomString(10, "ru"));
		}
	}
	
	@Test
	public void checkBasePaths() {	
		DataModel das = aggregateManager.getDataModel(); 

		Type task = das.getShape().getType(Task.class);
		Set<String> paths = AggregatePropertyPaths.enumerateBase(task);

		assert(paths.size() > 0);
		assert(paths.contains("name"));
		assert(paths.contains("version"));
	}	
	
	@Test
	public void checkStateGraph() {
		DataModel das = aggregateManager.getDataModel(); 

		Type task = das.getShape().getType(Task.class);
		View view = aggregateManager.getDataModel().getShape().getView((EntityType) task);
		List<Property> properties = view.getTypeGraph((EntityType)task).next(task, null, null);
		
		List<String> propertyNames = new ArrayList<String>();
		for(Property p: properties) {
			propertyNames.add(p.getName());
		}
		
		assert(propertyNames.contains("version"));
		assert(propertyNames.contains("quote"));
		assert(propertyNames.contains("dependants"));
		assert(properties.size() == 19);
	}

	public void checkCyclicPaths() {	
		DataModel das = aggregateManager.getDataModel(); 

		Type task = das.getShape().getType(Task.class);
		Set<String> paths = AggregatePropertyPaths.enumerateRegEx(task, das.getShape());

		assert(paths.size() > 0);
		String path = paths.iterator().next();
		
		assert(path.startsWith("(taskChildren.|dependants.|auditTask.)*quote.(") ||
			path.startsWith("(dependants.|taskChildren.|auditTask.)*quote.(") ||
			path.startsWith("(taskChildren.|auditTask.|dependants.)*quote.(") ||
			path.startsWith("(auditTask.|dependants.|taskChildren.)*quote.(") ||
			path.startsWith("(dependants.|auditTask.|taskChildren.)*quote.(") ||
			path.startsWith("(auditTask.|taskChildren.|dependants.)*quote.(") ||
			path.startsWith("(taskChildren.|dependants.|auditTask.)*(") ||
			path.startsWith("(dependants.|taskChildren.|auditTask.)*(") ||
			path.startsWith("(taskChildren.|auditTask.|dependants.)*(") ||
			path.startsWith("(auditTask.|dependants.|taskChildren.)*(") ||
			path.startsWith("(dependants.|auditTask.|taskChildren.)*(") ||
			path.startsWith("(auditTask.|taskChildren.|dependants.)*(")
				);
		
		Type patent = das.getShape().getType(Patent.class);
		paths = AggregatePropertyPaths.enumerateRegEx(patent, das.getShape());
		// Print the paths
		StringBuilder pathstr = new StringBuilder("\n");
		for(String p: paths) {
			pathstr.append(p + "\n");
		}
		logger.debug(pathstr);
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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());

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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());

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

		assert(result.toString().equals( "(ab|(b|aa)(ba)*(bb|a))*") ||
				result.toString().equals( "(ab|(b|aa)(ba)*(a|bb))*") ||
				result.toString().equals( "(ab|(aa|b)(ba)*(bb|a))*") ||
				result.toString().equals( "(ab|(aa|b)(ba)*(a|bb))*") ||
				result.toString().equals( "((aa|b)(ba)*(bb|a)|ab)*") ||
				result.toString().equals( "((aa|b)(ba)*(a|bb)|ab)*") ||
				result.toString().equals( "((b|aa)(ba)*(bb|a)|ab)*") ||
				result.toString().equals( "((b|aa)(ba)*(a|bb)|ab)*") ||
				result.toString().equals( "(ba|(bb|a)(ab)*(b|aa))*") ||
				result.toString().equals( "(ba|(bb|a)(ab)*(aa|b))*") ||
				result.toString().equals( "(ba|(a|bb)(ab)*(b|aa))*") ||
				result.toString().equals( "(ba|(a|bb)(ab)*(aa|b))*") ||
				result.toString().equals( "((bb|a)(ab)*(aa|b)|ba)*") ||
				result.toString().equals( "((a|bb)(ab)*(aa|b)|ba)*") ||
				result.toString().equals( "((bb|a)(ab)*(b|aa)|ba)*") ||
				result.toString().equals( "((a|bb)(ab)*(b|aa)|ba)*"));

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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());

		stateD.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);		

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*b(cdb|b)*cd") || result.toString().equals( "a*b(b|cdb)*cd"));
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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());
		
		stateB.setFinishState(true);

		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);			

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*b(cdb|b)*") || result.toString().equals( "a*b(b|cdb)*"));
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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());
		
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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());
		
		stateC.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);
		re.getGraph().addEdge(t3);
		re.getGraph().addEdge(t4);
		re.getGraph().addEdge(t5);
		re.getGraph().addEdge(t6);

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "a*b(cdb|b)*c") || result.toString().equals( "a*b(b|cdb)*c") || result.toString().equals("a*bb*c(dbb*c)*"));
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
		re.setAggregateType(A, aggregateManager.getDataModel().getShape());

		stateC.setFinishState(true);
		
		re.getGraph().addEdge(t1);
		re.getGraph().addEdge(t2);

		re.createEquations(); 
		Expression result = re.processEquations();

		assert(result.toString().equals( "bc"));				

	}	
	
	@Test
	public void testDFATask() {
		DataModel das = aggregateManager.getDataModel(); 

		Type task = das.getShape().getType(Task.class);
		
		DFAtoRE re = new DFAtoRE(task, das.getShape());
		Map<State, Expression> regEx = re.getRegEx();
		
		for(State state: regEx.keySet()) {
			Expression exp = regEx.get(state);
			if(state.getName().endsWith("Task")) {
				assert(exp.toString().equals("(taskChildren.|auditTask.|dependants.)*") ||
						exp.toString().equals("(auditTask.|taskChildren.|dependants.)*") ||
						exp.toString().equals("(dependants.|taskChildren.|auditTask.)*") ||
						exp.toString().equals("(dependants.|auditTask.|taskChildren.)*") ||
						exp.toString().equals("(taskChildren.|dependants.|auditTask.)*") ||
						exp.toString().equals("(auditTask.|dependants.|taskChildren.)*"));
			} else if (state.getName().endsWith("Quote")) {			
				assert(exp.toString().equals("(taskChildren.|auditTask.|dependants.)*quote.") ||
						exp.toString().equals("(auditTask.|taskChildren.|dependants.)*quote.") ||
						exp.toString().equals("(dependants.|taskChildren.|auditTask.)*quote.") ||
						exp.toString().equals("(dependants.|auditTask.|taskChildren.)*quote.") ||
						exp.toString().equals("(taskChildren.|dependants.|auditTask.)*quote.") ||
						exp.toString().equals("(auditTask.|dependants.|taskChildren.)*quote."));
			}
		}
	}
	
	@Test
	public void checkOptionModel() {
		List<String> paths = aggregateManager.getMetaModel().getAggregateAttributes(
			"tools.xor.db.base.MetaEntity");
		assert(paths != null && paths.size() > 0);
	}

	public void metaViewList() {
		MetaModel mm = aggregateManager.getMetaModel();
		List<String> viewList = mm.getViewList();

		System.out.println("||||||++++++++++ VIEWS +++++++++++++||||||||");
		for(String view: viewList) {
			System.out.println("View: " + view);
		}

		System.out.println("++++++++++ TASKCHILDREN +++++++++++++");
		List<String> attrs = mm.getViewAttributes("TASKCHILDREN");
		for(String attr: attrs) {
			System.out.println("ATTR: " + attr);
		}

		System.out.println("++++++++++ TASK aggregate +++++++++++++");
		attrs = mm.getAggregateAttributes(Task.class.getName());
		for(String attr: attrs) {
			System.out.println("ATTR: " + attr);
		}

		System.out.println("++++++++++ TYPES +++++++++++++");
		List<String> types = mm.getTypeList();
		for(String type: types) {
			System.out.println("TYPE: " + type);
		}
	}

	public void generateStateGraph() {
		//Settings settings = new Settings();
		DataModel das = aggregateManager.getDataModel();
		/*
		EntityType taskType = (EntityType)das.getType(Task.class);

		settings.setEntityType(taskType);
		settings.init(das.getShape());
		*/
		Settings settings = das.settings().aggregate(Task.class).build();
		TypeGraph sg = settings.getView().getTypeGraph((EntityType)settings.getEntityType());
		settings.setGraphFileName("TaskStateGraph.png");
		sg.generateVisual(settings);
	}

	public void generateStateInheritanceGraph() {
		Settings settings = new Settings();
		DataModel das = aggregateManager.getDataModel();
		EntityType taskType = (EntityType)das.getShape().getType(Task.class);

		settings.setEntityType(taskType);
		settings.expand(new AssociationSetting(Person.class));
		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(taskType);
		settings.setGraphFileName("TaskInheritanceStateGraph.png");
		sg.generateVisual(settings);
	}

	public void generateStatePersonGraph() {
		Settings settings = new Settings();
		DataModel das = aggregateManager.getDataModel();
		EntityType personType = (EntityType)das.getShape().getType(Person.class);

		settings.setEntityType(personType);
		settings.init(das.getShape());
		TypeGraph sg = settings.getView().getTypeGraph(personType);
		settings.setGraphFileName("PersonInheritanceStateGraph.png");
		sg.generateVisual(settings);
	}

	public void checkPersonSubTypes() {

		DataModel das = aggregateManager.getDataModel();
		EntityType personType = (EntityType)das.getShape().getType(Person.class);

		Set<EntityType> personSubTypes = personType.getSubtypes();
		assert(personSubTypes != null && personSubTypes.size() == 4);
		Set<EntityType> personChildSubTypes = personType.getChildTypes();
		assert(personChildSubTypes != null && personChildSubTypes.size() == 3);


		EntityType employeeType = (EntityType)das.getShape().getType(Employee.class);

		Set<EntityType> employeeSubTypes = employeeType.getSubtypes();
		assert(employeeSubTypes != null && employeeSubTypes.size() == 1);
		Set<EntityType> employeeChildSubTypes = employeeType.getChildTypes();
		assert(employeeChildSubTypes != null && employeeChildSubTypes.size() == 1);
	}

	public void checkJSON() {
		String[] paths = new String[] {
			"taskChildren.[TASKCHILDREN]",
			"[VERYBASIC]",
			"project.name",
			"dependants.[VERYBASIC]",
			"dependants.taskChildren.[VERYBASIC]",
			"dependants.taskChildren.auditTask.[VERYBASIC]",
			"assignedTo.name"
		};
		List attrs = Arrays.asList(paths);

		JSONObject json = new JSONObject();
		AggregateView.extractJSON(json, attrs);

		System.out.println("JSON: " + json);

		View view = aggregateManager.getView("PARALLEL_QUERY");
		System.out.println("PARALLEL_QUERY View Json: " + view.getJson().toString());
	}

	@Test
	public void generatePaths() {
		HierarchyGenerator root = HierarchyGenerator.build(3, 30);

		int count = 0;
		while(root.hasNext()) {
			root.next();
			//HierarchyGenerator gen = root.next();
			SharedCounterGenerator scg = root.getCurrentIdGenerator();
			String idValue = scg.getStringValue(null, null);

			HierarchyGenerator parentGen = root.getCurrentParent();
			SharedCounterGenerator parentscg = parentGen == null ? null : parentGen.getIdGenerator();
			String parentIdValue = parentscg == null ? null : parentscg.getStringValue(null, null);


			String str = root.getStringValue(null, null);
			System.out.println(String.format("path: %s, id: %s, parentid: %s", str, idValue, parentIdValue));

			count++;
		}

		assert(count == 30);
	}
}
