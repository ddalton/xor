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

package tools.xor.jpa;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.CounterGenerator;
import tools.xor.ExtendedProperty;
import tools.xor.JDBCType;
import tools.xor.Settings;
import tools.xor.ToOneGenerator;
import tools.xor.Type;
import tools.xor.db.pm.PriorityTask;
import tools.xor.db.pm.Task;
import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.Generator;
import tools.xor.generator.GeneratorRecipient;
import tools.xor.generator.StringTemplate;
import tools.xor.logic.DefaultQueryOperation;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.AggregateManager;
import tools.xor.service.Shape;
import tools.xor.service.Transaction;
import tools.xor.view.AggregateView;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-jdbc-test.xml" })
@Transactional
public class JPAQueryOperationTest extends DefaultQueryOperation {

	@Resource(name = "amJDBCjpa")
	protected AggregateManager amJDBC;	// Useful for generating data using JDBC

	@Test
	public void queryPerson() {
		super.queryPerson();
	}
	
	@Test
	public void queryPersonNative() {
		super.queryPersonNative();
	}

	@Test
	public void queryPersonOQL() {
		super.queryPersonOQL();
	}

	//@Test
	public void queryPersonStoredProcedure() {
		super.queryPersonStoredProcedure();
	}		
	
	@Test
	public void queryPersonType() {
		super.queryPersonType();
	}
	
	@Test
	public void queryNarrowPersonType() {
		super.queryNarrowPersonType();
	}	
	
	@Test
	public void queryTaskChildren() {
		super.queryTaskChildren();
	}
	
	@Test
	public void queryTaskGrandChildren() {
		super.queryTaskGrandChildren();
	}

	@Test
	public void queryTaskGrandChildrenRegEx() {
		super.queryTaskGrandChildrenRegEx();
	}
	
	@Test
	public void queryTaskGrandChildrenSkip() {
		super.queryTaskGrandChildrenSkip();
	}		
	
	@Test
	public void queryTaskGrandChildSetList() {
		super.queryTaskGrandChildSetList();
	}		
	
	@Test
	public void queryTaskDependencies() {
		super.queryTaskDependencies();
	}	
	
	@Test
	public void querySubProjects() {
		super.querySubProjects();
	}
	
	@Test
	public void querySetListMap() {
		super.querySetListMap();
	}		
	
	@Test
	public void queryTaskEmptyFilter() {
		super.queryTaskEmptyFilter();
	}		
	
	@Test
	public void queryTaskNameFilter() {
		super.queryTaskNameFilter();
	}		
	
	@Test
	public void queryTaskUnionNameFilter() {
		super.queryTaskUnionNameFilter();
	}	
	
	@Test
	public void queryTaskUnionSet1() {
		super.queryTaskUnionSet1();
	}	
	
	@Test
	public void queryTaskUnionSet2() {
		super.queryTaskUnionSet2();
	}
	
	@Test
	public void queryTaskParallel() {
		super.queryTaskParallel();
	}
	
	@Test
	public void queryTaskUnionOverlap() {
		super.queryTaskUnionOverlap();
	}			
	
	@Test
	public void queryEntityProperties() {
		super.queryEntityProperties();
	}	
	
	@Test
	public void listPatents() {
		super.listPatents();
	}	
	
	@Test
	public void listCatalogOfPatents() {
		super.listCatalogOfPatents();
	}

	@Test
	public void validateComplex() {
		super.validateComplex();
	}

	@Test
	public void querySplitToRoot() {
		super.querySplitToRoot();
	}

	@Test
	public void querySplitToRootNoSplit() {
		super.querySplitToRootNoSplit();
	}

	@Test
	public void querySplitToAnchor() {
		super.querySplitToAnchor();
	}

	@Test
	public void querySplitToAnchorNoSplit() {
		super.querySplitToAnchorNoSplit();
	}

	@Test
	public void querySplitToAnchorParallel() {
		super.querySplitToAnchorParallel();
	}

	@Test
	public void oqlQuery() {
		super.oqlQuery();
	}

	@Test
	/**
	 * @see tools.xor.jpa.JPAMutableJsonTest#testSubtypeSubquery
	 */
	public void testSubtypeSubquery() {
		Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
		if(shape == null) {
			shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
		}


		JDBCType task = (JDBCType)shape.getType("TASK");
		task.clearGenerators();

		ExtendedProperty taskparent = (ExtendedProperty)task.getProperty("TASKPARENT_UUID");
		Generator parentgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
		taskparent.setGenerator(parentgen);
		Generator rootidgen = new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"});
		ExtendedProperty rootid = (ExtendedProperty)task.getProperty("UUID");
		rootid.setGenerator(rootidgen);
		Generator namegen = new StringTemplate(new String[] {"NAME_[VISITOR_CONTEXT]"});
		ExtendedProperty namep = (ExtendedProperty)task.getProperty("NAME");
		namep.setGenerator(namegen);
		ExtendedProperty dtype = (ExtendedProperty)task.getProperty("DTYPE");
		Generator dtypegen = new DefaultGenerator(new String[] {"Task"});
		dtype.setGenerator(dtypegen);
		ExtendedProperty priority = (ExtendedProperty)task.getProperty("PRIORITY");
		Generator pgen = new DefaultGenerator(new String[] {"0", "0"});
		priority.setGenerator(pgen);

		ToOneGenerator toonegen = new ToOneGenerator(new String[] { "0",
																	"0,0:0", // root task
																	"1,4:4", // 4 children
																	"5,8:2", // 2 children with 2 grand children
																	"9,14:3" // 2 children with 3 grand children
		});

		CounterGenerator gensettings = new CounterGenerator(15);
		gensettings.addListener(toonegen);
		task.addGenerator(gensettings);
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
				(GeneratorRecipient)parentgen));


		// Now add priority task grand children
		toonegen = new ToOneGenerator(new String[] { "1",
													 "15,17:1" // 3 children with 1 PriorityTask grand child
		});
		gensettings = new CounterGenerator(3, 15);
		gensettings.addListener(toonegen);
		task.addGenerator(gensettings);
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen,
				(GeneratorRecipient)parentgen));
		Generator priotypegen = new DefaultGenerator(new String[] {"PriorityTask"});
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(priotypegen, dtype));
		Generator priogen = new DefaultGenerator(new String[] {"2", "5"});
		gensettings.addVisit(new DefaultGenerator.GeneratorVisit(priogen, priority));

		String[] types = new String[] {
			"TASK"
		};

		Settings settings = new Settings();
		//settings.setImportMethod(ImportMethod.CSV);
		Transaction tx = amJDBC.createTransaction(settings);
		tx.begin();
		try {
			amJDBC.generate(shape.getName(), Arrays.asList(types), settings);

			List<String> paths = new ArrayList<>();
			paths.add("id");
			paths.add("name");
			paths.add("taskChildren.id");
			paths.add("taskChildren.name");
			paths.add("taskChildren.description");
			paths.add("taskChildren.taskChildren.id");
			paths.add("taskChildren.taskChildren.name");
			paths.add("taskChildren.taskChildren.description");
			paths.add("taskChildren.taskChildren.priority");
			AggregateView priorityView = new AggregateView("PRIORITYTASK");
			priorityView.setAttributeList(paths);
			priorityView.setSplitToRoot(false);
			settings = new Settings();
			settings.setView(priorityView);

			shape = aggregateService.getDataModel().getShape();
			Type type = shape.getType(Task.class);
			settings.setEntityType(type);
			settings.init(shape);

			List<?> result = aggregateService.query(null, settings);
			assert(result != null);
			assert(result.size() == 18);
			Task root = (Task)result.get(0);
			assert(root.getTaskChildren() != null);
			Set<Task> children = root.getTaskChildren();
			assert(children.size() == 4);

			Task task15 = null;
			for(Task child: children) {
				for(Task grandchild: child.getTaskChildren()) {
					if(grandchild.getName().equals("NAME_15")) {
						task15 = grandchild;
						break;
					}
				}
			}

			assert(task15 != null);
			assert(task15 instanceof PriorityTask);

		} finally {

			JDBCDataStore po = (JDBCDataStore)amJDBC.getDataStore();
			JDBCSessionContext sc = po.getSessionContext();

			try (Statement stmt = sc.getConnection().createStatement()) {
				stmt.execute("DELETE from TASK");
				sc.getConnection().commit();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			tx.rollback();
			// We don't close as the connection belongs to Spring
		}
	}
}
