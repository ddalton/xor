package tools.xor.view;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Mapping of query result columns to view attributes by label or position.
 */
public class ColumnMappingTest {

    private static final List<String> EXPECTED = Arrays.asList("id", "name", "displayName", "taskDetails.id");

    private static ColumnMapping create(List<String> labels, int width) {
        return ColumnMapping.create("TESTVIEW", EXPECTED, labels, width, "SELECT ...");
    }

    @Test
    public void byNameInAnyOrder() {
        ColumnMapping mapping = create(Arrays.asList("taskDetails.id", "NAME", "id", "DisplayName"), 4);
        assertTrue(mapping.isByName());
        assertArrayEquals(
            new Object[] { "ID1", "Task", "Task 1", "TD1" },
            mapping.apply(new Object[] { "TD1", "Task", "ID1", "Task 1" }));
    }

    @Test
    public void byPositionWithoutLabels() {
        ColumnMapping mapping = create(null, 4);
        assertFalse(mapping.isByName());
        Object[] row = new Object[] { "ID1", "Task", "Task 1", "TD1" };
        assertSame(row, mapping.apply(row));
    }

    @Test
    public void byPositionWithColumnNames() {
        // Typical legacy native query: some column names coincide with attribute names
        ColumnMapping mapping = create(Arrays.asList("UUID", "NAME", "DISPLAYNAME", "UUID"), 4);
        assertFalse(mapping.isByName());
    }

    @Test
    public void sameNameForDifferentAttributes() {
        // NAME at position 2 is used for an attribute other than name, but the name attribute has its own NAME column
        List<String> expected = Arrays.asList("id", "ownedBy.name", "name");
        ColumnMapping mapping = ColumnMapping.create("TESTVIEW", expected, Arrays.asList("UUID", "NAME", "NAME"), 3, "SELECT ...");
        assertFalse(mapping.isByName());
    }

    @Test
    public void swappedColumns() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> create(Arrays.asList("UUID", "DISPLAYNAME", "NAME", "UUID"), 4));
        assertTrue(e.getMessage().contains("out of order"), e.getMessage());
        assertTrue(e.getMessage().contains("column 2 (DISPLAYNAME)"), e.getMessage());
    }

    @Test
    public void tooFewColumns() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> create(Arrays.asList("UUID", "NAME", "DISPLAYNAME"), 3));
        assertTrue(e.getMessage().contains("returned 3 columns, but 4 columns are expected"), e.getMessage());
    }

    @Test
    public void unusedLabeledColumn() {
        // Every attribute is labeled, but there is an additional column, so it is mapped by position
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> create(Arrays.asList("id", "name", "displayName", "taskDetails.id", "extra"), 5));
        assertTrue(e.getMessage().contains("returned 5 columns"), e.getMessage());
    }

    @Test
    public void duplicateLabels() {
        // Duplicate labels cannot be mapped by name
        ColumnMapping mapping = create(Arrays.asList("id", "name", "name", "taskDetails.id"), 4);
        assertFalse(mapping.isByName());
    }

    @Test
    public void unusedPosition() {
        // A position not used by the view is left empty
        List<String> expected = Arrays.asList("id", null, "name");
        ColumnMapping mapping = ColumnMapping.create("TESTVIEW", expected, Arrays.asList("name", "id"), 2, "SELECT ...");
        assertTrue(mapping.isByName());
        assertArrayEquals(new Object[] { "ID1", null, "Task" }, mapping.apply(new Object[] { "Task", "ID1" }));
    }
}
