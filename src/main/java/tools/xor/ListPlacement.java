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

package tools.xor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;

/**
 * Places the elements of a list collection according to their list index while the query
 * results are being reconstituted.
 *
 * The rows of a query need not be sorted by the list index, e.g., a native query tuned to use
 * a different join order, and a list element can appear in multiple rows when the query also
 * joins other collections. So an element is added only once and is inserted at the position
 * given by its index relative to the elements already added. If the index is not available,
 * the elements are kept in the order they are first seen.
 */
public class ListPlacement
{
    private static class State
    {
        final List<Integer> indices = new ArrayList<>(); // index of each element in the collection, in order
        final Set<Object> elements = Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private final Map<Object, State> stateByCollection = new IdentityHashMap<>();

    private State getState (Object collection)
    {
        return stateByCollection.computeIfAbsent(collection, k -> new State());
    }

    private static Integer toIndex (Object indexValue)
    {
        if (indexValue == null) {
            return null;
        }
        if (indexValue instanceof Number) {
            return ((Number)indexValue).intValue();
        }
        return Integer.valueOf(indexValue.toString().trim());
    }

    /**
     * Find the position to insert an element with the given index. Elements without an
     * index and elements with the same index keep their relative order.
     */
    private static int getPosition (State state, Integer index)
    {
        int position = state.indices.size();
        if (index == null) {
            return position;
        }
        while (position > 0) {
            Integer previous = state.indices.get(position - 1);
            if (previous == null || previous <= index) {
                break;
            }
            position--;
        }
        return position;
    }

    /**
     * Add an element to a list.
     *
     * @param list       collection
     * @param element    to add
     * @param indexValue list index of the element, can be null
     */
    @SuppressWarnings("unchecked")
    public void add (List list, Object element, Object indexValue)
    {
        State state = getState(list);
        if (state.elements.isEmpty() && !list.isEmpty()) {
            // The list was populated outside of this reconstitution, so track its contents
            for (Object existing : list) {
                state.elements.add(existing);
                state.indices.add(null);
            }
        }
        if (!state.elements.add(element)) {
            return;
        }

        Integer index = toIndex(indexValue);
        int position = getPosition(state, index);
        state.indices.add(position, index);
        list.add(position, element);
    }

    /**
     * Add an element to a JSON array.
     *
     * @param array      collection
     * @param element    to add
     * @param indexValue list index of the element, can be null
     */
    public void add (JSONArray array, Object element, Object indexValue)
    {
        State state = getState(array);
        if (state.elements.isEmpty() && array.length() > 0) {
            for (int i = 0; i < array.length(); i++) {
                state.elements.add(array.get(i));
                state.indices.add(null);
            }
        }
        if (!state.elements.add(element)) {
            return;
        }

        Integer index = toIndex(indexValue);
        int position = getPosition(state, index);
        state.indices.add(position, index);

        // JSONArray does not support insertion, so shift the elements after the position
        int length = array.length();
        if (position == length) {
            array.put(element);
        } else {
            array.put(array.get(length - 1));
            for (int i = length - 1; i > position; i--) {
                array.put(i, array.get(i - 1));
            }
            array.put(position, element);
        }
    }
}
