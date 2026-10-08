package tools.xor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tools.xor.Settings;

/**
 * A large IN list is split into batches of at most MAX_INLIST_SIZE values.
 */
public class InListBatchTest {

    /**
     * Records the parameter values of each execution
     */
    private static class RecordingQuery extends AbstractQuery {
        private final Map<String, Object> params = new HashMap<>();
        private final List<Map<String, Object>> executions = new ArrayList<>();

        RecordingQuery() {
            super("SELECT id FROM Task WHERE id IN (" + Query.INTERQUERY_JOIN_PLACEHOLDER + ")");
        }

        @Override protected List getResultListInternal(View view, Settings settings) {
            executions.add(new HashMap<>(params));
            return new ArrayList<>();
        }

        @Override public void setParameter(String name, Object value) { params.put(name, value); }
        @Override public boolean hasParameter(String name) { return true; }
        @Override public Object execute(Settings settings) { return null; }
        @Override public Object getSingleResult(View view, Settings settings) { return null; }
        @Override public void setMaxResults(int limit) { }
        @Override public void setFirstResult(int offset) { }
        @Override public void updateParamMap(List<BindParameter> bindParams) { }
        @Override public boolean isOQL() { return false; }
        @Override public boolean isSQL() { return true; }
        @Override public boolean isDeferred() { return false; }
    }

    private static Set<Object> ids(int count) {
        Set<Object> result = new LinkedHashSet<>();
        for(int i = 0; i < count; i++) {
            result.add("id" + i);
        }
        return result;
    }

    private void checkBatches(int numValues, int expectedBatches) {
        RecordingQuery query = new RecordingQuery();
        Set<Object> values = ids(numValues);
        query.processLargeInList(values);
        query.getResultList(null, null);

        assertEquals(expectedBatches, query.executions.size());

        // Every value is bound in some batch, and every bind parameter has a value
        Set<Object> bound = new HashSet<>();
        for(Map<String, Object> execution: query.executions) {
            assertEquals(QueryTreeInvocation.MAX_INLIST_SIZE, execution.size());
            for(int i = QueryTreeInvocation.OFFSET; i <= QueryTreeInvocation.MAX_INLIST_SIZE; i++) {
                Object value = execution.get(QueryFragment.PARENT_INLIST + i);
                assertTrue(values.contains(value));
                bound.add(value);
            }
        }
        assertEquals(values, bound);
    }

    @Test
    public void justOverLimit() {
        checkBatches(QueryTreeInvocation.MAX_INLIST_SIZE + 1, 2);
    }

    @Test
    public void exactMultipleOfLimit() {
        // Previously an extra batch containing only padding was executed
        checkBatches(QueryTreeInvocation.MAX_INLIST_SIZE * 2, 2);
    }

    @Test
    public void partialLastBatch() {
        checkBatches(QueryTreeInvocation.MAX_INLIST_SIZE * 2 + 5, 3);
    }
}
