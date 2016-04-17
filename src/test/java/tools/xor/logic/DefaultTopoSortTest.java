package tools.xor.logic;

import java.util.List;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.Type;
import tools.xor.db.base.Chapter;
import tools.xor.db.base.Facet;
import tools.xor.db.base.MetaEntityType;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataAccessService;
import tools.xor.util.State;
import tools.xor.util.Edge;
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
		DataAccessService das = aggregateManager.getDAS(); 

		Type chapterType = das.getType(Chapter.class);
		Type categoryType = das.getType(MetaEntityType.class);
		Type FacetType = das.getType(Facet.class);
		
		State task = new State(chapterType, false);	
		State category = new State(categoryType, false);
		State facet = new State(FacetType, false);
		StateGraph<State, Edge<State>> dg = new StateGraph<State, Edge<State>>(chapterType);
		
		dg.addVertex(category);
		dg.addVertex(task);
		dg.addVertex(facet);
		dg.populateEdges();
		
		List<State> states = dg.toposort();
		
		Assert.assertTrue(states.size() == 3);
		Assert.assertTrue(states.get(0).getType() == chapterType);
		Assert.assertTrue(states.get(1).getType() == categoryType);
		Assert.assertTrue(states.get(2).getType() == FacetType);
	}

}