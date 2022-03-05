package tools.xor.view;

import java.util.HashSet;
import java.util.Set;

import tools.xor.service.DomainShape;
import tools.xor.service.Shape;
import tools.xor.util.InterQuery;

/**
 * This class will be responsible for adding a node to the AggregateTree to be
 * responsible for populating the Query Join table to be used by the child query trees
 */
public class JoinTableAmender implements TreeMutatorStrategy
{
    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;
    private Shape shape;

    public JoinTableAmender (AggregateTree aggregateTree, Shape shape) {
        this.aggregateTree = aggregateTree;
        this.shape = shape;
    }

    @Override public void execute ()
    {
        // algorithm
        // 1. For each vertex
        // 2. Examine the outgoing edge. If any edge points to custom SP or a SQL referencing an Query Join table
        //      then we add a new QueryJoinAction
        // 3. if (2) is true then continue step 1
        boolean addAction = false;
        for(QueryTree queryTree: aggregateTree.getVertices()) {
            // Does it have any out edges?
            if(aggregateTree.getOutEdges(queryTree).size() == 0) {
                continue;
            }

            Set<InterQuery> edgesToProcess = new HashSet<>();
            for(InterQuery<QueryTree> edge: aggregateTree.getOutEdges(queryTree)) {
                if(QueryJoinAction.needsQueryJoinTable(edge.getEnd())) {
                    addAction = true;
                    edgesToProcess.add(edge);
                }
            }

            if(addAction) {
                // We need to populate the query join table
                queryTree.addAction(new QueryJoinAction(edgesToProcess));
            }
        }

        // Check that the query join table is present in the shape
        if(addAction && shape instanceof DomainShape) {
            if(!((DomainShape)shape).hasTable(QueryJoinAction.JOIN_TABLE_NAME)) {
                throw new RuntimeException(String.format("Unable to find join table %s in database.", QueryJoinAction.JOIN_TABLE_NAME));
            }
        }
    }
}
