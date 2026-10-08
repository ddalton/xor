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

package tools.xor.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps the columns of a query result row to the positions expected by the view.
 *
 * A user provided query (e.g., native SQL or a stored procedure tuned by a DBA) can
 * either return the columns in the order of the view attributes, or label each column
 * with the attribute path it populates using a column alias, in which case the
 * columns can be in any order:
 *
 * <pre>
 *   SELECT t.NAME AS name, t.UUID AS id, td.UUID AS "taskDetails.id" ...
 * </pre>
 *
 * Labels are matched to the attribute paths ignoring case. A path containing a '.'
 * needs a quoted alias. The columns are mapped by name only if every expected attribute
 * has exactly one matching label, otherwise they are mapped by position.
 */
public class ColumnMapping
{
    private final int[] sourceIndex; // sourceIndex[expected position] = position in the result row
    private final boolean byName;

    private ColumnMapping (int[] sourceIndex, boolean byName)
    {
        this.sourceIndex = sourceIndex;
        this.byName = byName;
    }

    /**
     * @return true if the columns are mapped using the column labels
     */
    public boolean isByName ()
    {
        return byName;
    }

    /**
     * Rearrange the row so that each value is at the position expected by the view.
     *
     * @param row as returned by the query
     * @return row in the order expected by the view
     */
    public Object[] apply (Object[] row)
    {
        if (!byName) {
            return row;
        }

        Object[] result = new Object[sourceIndex.length];
        for (int i = 0; i < sourceIndex.length; i++) {
            if (sourceIndex[i] != -1) {
                result[i] = row[sourceIndex[i]];
            }
        }

        return result;
    }

    private static String normalize (String name)
    {
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Create the mapping between the query result columns and the columns expected by the view.
     *
     * @param viewName used in error messages
     * @param expected attribute path at each position expected by the view, an entry can be null if the
     *                 position is unused
     * @param labels   label of each column in the result row, or null if not provided by the query
     * @param rowWidth number of columns in the result row
     * @param query    query string used in error messages
     * @return the column mapping
     * @throws IllegalStateException if the result columns do not match the view
     */
    public static ColumnMapping create (String viewName,
                                        List<String> expected,
                                        List<String> labels,
                                        int rowWidth,
                                        String query)
    {
        if (labels != null && labels.size() == rowWidth) {
            ColumnMapping byName = mapByName(expected, labels);
            if (byName != null) {
                return byName;
            }
        }

        if (rowWidth != expected.size()) {
            throw new IllegalStateException(String.format(
                "The query for view %s returned %d columns, but %d columns are expected in the order: %s. "
                    + "Alternatively, label each column with the attribute it populates using a column alias. Query: %s",
                viewName,
                rowWidth,
                expected.size(),
                expected,
                query));
        }

        if (labels != null && labels.size() == rowWidth) {
            checkPositions(viewName, expected, labels, query);
        }

        return new ColumnMapping(null, false);
    }

    /**
     * Map the columns by name if every expected attribute has exactly one matching label
     * and every label matches an expected attribute.
     */
    private static ColumnMapping mapByName (List<String> expected, List<String> labels)
    {
        Map<String, Integer> labelIndex = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = normalize(labels.get(i));
            if (label == null || labelIndex.put(label, i) != null) {
                // missing or duplicate label
                return null;
            }
        }

        int[] sourceIndex = new int[expected.size()];
        int matched = 0;
        for (int i = 0; i < expected.size(); i++) {
            sourceIndex[i] = -1;
            String path = normalize(expected.get(i));
            if (path == null) {
                continue;
            }
            Integer index = labelIndex.get(path);
            if (index == null) {
                return null;
            }
            sourceIndex[i] = index;
            matched++;
        }

        if (matched != labels.size()) {
            // Some columns are not used by the view
            return null;
        }

        return new ColumnMapping(sourceIndex, true);
    }

    /**
     * When mapping by position, detect columns whose labels indicate they have been swapped.
     * Labels often coincide with attribute names, e.g., a NAME column for the name attribute,
     * so a column that is labeled with an attribute name at a different position, while
     * neither its own position nor that attribute's position has a matching label, is
     * likely to be at the wrong position.
     */
    private static void checkPositions (String viewName, List<String> expected, List<String> labels, String query)
    {
        Map<String, Integer> expectedIndex = new HashMap<>();
        for (int i = 0; i < expected.size(); i++) {
            String path = normalize(expected.get(i));
            if (path != null) {
                expectedIndex.putIfAbsent(path, i);
            }
        }

        List<String> problems = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = normalize(labels.get(i));
            Integer j = expectedIndex.get(label);
            if (j == null || j == i) {
                continue;
            }
            boolean ownPositionMatches = label.equals(normalize(expected.get(i)));
            boolean otherPositionMatches = normalize(labels.get(j)) != null
                && normalize(labels.get(j)).equals(normalize(expected.get(j)));
            if (!ownPositionMatches && !otherPositionMatches) {
                problems.add(String.format(
                    "column %d (%s) looks like it populates '%s' at position %d, but populates '%s'",
                    i + 1,
                    labels.get(i),
                    expected.get(j),
                    j + 1,
                    expected.get(i)));
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(String.format(
                "The columns of the query for view %s appear to be out of order: %s. "
                    + "The columns are mapped by position in the order: %s. "
                    + "Either reorder the columns or label each column with the attribute it populates using a column alias. Query: %s",
                viewName,
                String.join("; ", problems),
                expected,
                query));
        }
    }
}
