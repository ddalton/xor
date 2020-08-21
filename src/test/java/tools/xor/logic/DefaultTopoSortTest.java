package tools.xor.logic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.Type;
import tools.xor.db.base.Chapter;
import tools.xor.db.base.Facet;
import tools.xor.db.base.MetaEntityType;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;

/**
 * Unit test the TopoSort algorithm.
 */

public class DefaultTopoSortTest {

	@Autowired
	protected AggregateManager aggregateManager;
	
    /**
     * Test for a binary strongly connected component.
     */
	public void testCategoryOrder() {
		DataModel das = aggregateManager.getDataModel(); 

		Type chapterType = das.getShape().getType(Chapter.class);
		Type categoryType = das.getShape().getType(MetaEntityType.class);
		Type FacetType = das.getShape().getType(Facet.class);
		
		State task = new State(chapterType, false);	
		State category = new State(categoryType, false);
		State facet = new State(FacetType, false);
		StateGraph<State, Edge<State>> dg = new StateGraph<State, Edge<State>>(chapterType, das.getShape());
		
		dg.addVertex(category);
		dg.addVertex(task);
		dg.addVertex(facet);
		dg.populateEdges(das.getShape());
		
		List<State> states = dg.toposort(das.getShape());
		
		assertTrue(states.size() == 3);
		assertTrue(states.get(0).getType() == chapterType);
		assertTrue(states.get(1).getType() == categoryType);
		assertTrue(states.get(2).getType() == FacetType);
	}

}
