package tools.xor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Locate the FROM clause of the outermost query.
 */
public class QueryStringHelperTest {

    private static String fromClause(String query) {
        int index = QueryStringHelper.indexOfFromClause(query);
        return index == -1 ? null : query.substring(index);
    }

    @Test
    public void simple() {
        assertEquals("FROM Task t", fromClause("SELECT t.id FROM Task t"));
    }

    @Test
    public void caseAndWhitespace() {
        assertEquals("from Task t", fromClause("select t.id from Task t"));
        assertEquals("FROM\n  Task t", fromClause("SELECT t.id,\n  t.name\nFROM\n  Task t"));
        assertEquals("FROM\tTask t", fromClause("SELECT t.id\tFROM\tTask t"));
    }

    @Test
    public void subqueryInSelectList() {
        assertEquals("FROM Task t",
            fromClause("SELECT t.id, (SELECT COUNT(*) FROM Task c WHERE c.parent = t.id) FROM Task t"));
    }

    @Test
    public void quotedAndCommented() {
        assertEquals("FROM Task t", fromClause("SELECT 'from' AS label, \"FROM\" FROM Task t"));
        assertEquals("FROM Task t", fromClause("SELECT 'it''s from here' FROM Task t"));
        assertEquals("FROM Task t", fromClause("SELECT t.id -- from comment\nFROM Task t"));
        assertEquals("FROM Task t", fromClause("SELECT t.id /* from comment */ FROM Task t"));
    }

    @Test
    public void identifiersContainingFrom() {
        assertEquals("FROM Task t", fromClause("SELECT t.fromDate, fromDate, t.from_x FROM Task t"));
    }

    @Test
    public void notFound() {
        assertEquals(null, fromClause("SELECT 1"));
        assertEquals(null, fromClause("SELECT (SELECT 1 FROM dual)"));
    }
}
