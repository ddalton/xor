package tools.xor.view;

import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.InterQuery;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QueryJoinAction implements Action
{
    Set<InterQuery> edgesToProcess;

    public final static String JOIN_TABLE_NAME;

    static {
        if (ApplicationConfiguration.config().containsKey(Constants.Config.QUERY_JOIN_TABLE)) {
            JOIN_TABLE_NAME = ApplicationConfiguration.config().getString(Constants.Config.QUERY_JOIN_TABLE);
        } else {
            JOIN_TABLE_NAME = Query.QUERY_JOIN_TABLENAME;
        }
    }

    public QueryJoinAction(Set<InterQuery> edgesToProcess) {
        this.edgesToProcess = edgesToProcess;
    }

    public static boolean needsQueryJoinTable(QueryTree child) {
        boolean result = false;

        View view = child.getView();
        if(view.isCustom() && view.getUserOQLQuery() == null) {
            if(view.getNativeQuery() == null || view.getNativeQuery().contains(JOIN_TABLE_NAME)) {
                result = true;
            }
        }

        return result;
    }

    @Override public void execute (QueryTreeInvocation qti, PersistenceOrchestrator po)
    {
        // To make it work with all types of parent queries, we
        // will batch insert the values into the temp table
        // The ids are not necessarily the root of the query tree as it depends on the
        // source of the InterQuery edge

        // Child queries need to refer to the session id/invocation id while refering to the parent ids

        Set<QueryFragment> processed = new HashSet<>();
        for(InterQuery<QueryTree> edge: edgesToProcess) {
            if(processed.contains(edge.getSource())) {
                continue;
            }

            QueryFragment source = edge.getSource();
            String invocationId = qti.getOrCreateInvocationId(source);
            Set ids = qti.getParentIds(edge);

            // perform the insert (will choose the correct id column based on the type of the id object)
            // We do this only if the parent query has not already populated
            QueryTree queryTree = edge.getStart();
            if(queryTree.getView() == null || !queryTree.getView().isTempTablePopulated()) {
                po.populateQueryJoinTable(invocationId, ids);
            }

            processed.add(source);
        }
    }

    @Override public Action copy (Object context)
    {
        Map<InterQuery, InterQuery> edgeMap = (Map<InterQuery, InterQuery>)context;

        Set<InterQuery> edges = new HashSet<>();
        for(InterQuery edge: edgesToProcess) {
            if(!edgeMap.containsKey(edge)) {
                throw new RuntimeException("Unable to find edge mapping in the copy");
            }

            edges.add(edgeMap.get(edge));
        }

        return new QueryJoinAction(edges);
    }
}
