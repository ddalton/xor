package tools.xor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * List elements are placed by their index regardless of the order of the query rows.
 */
public class ListPlacementTest {

    private static JSONObject element(String name) {
        return new JSONObject().put("name", name);
    }

    private static List<String> names(JSONArray array) {
        List<String> result = new ArrayList<>();
        for(int i = 0; i < array.length(); i++) {
            result.add(array.getJSONObject(i).getString("name"));
        }
        return result;
    }

    @Test
    public void sortedRows() {
        ListPlacement placement = new ListPlacement();
        List<String> list = new ArrayList<>();
        placement.add(list, "e0", 0);
        placement.add(list, "e1", 1);
        placement.add(list, "e2", 2);
        assertEquals(Arrays.asList("e0", "e1", "e2"), list);
    }

    @Test
    public void unsortedRows() {
        ListPlacement placement = new ListPlacement();
        List<String> list = new ArrayList<>();
        placement.add(list, "e2", 2);
        placement.add(list, "e0", 0);
        placement.add(list, "e1", "1"); // index as returned by some drivers
        assertEquals(Arrays.asList("e0", "e1", "e2"), list);
    }

    @Test
    public void repeatedRows() {
        // An element repeats when the query joins another collection
        ListPlacement placement = new ListPlacement();
        List<String> list = new ArrayList<>();
        String e0 = "e0", e1 = "e1";
        placement.add(list, e1, 1);
        placement.add(list, e0, 0);
        placement.add(list, e1, 1);
        placement.add(list, e0, 0);
        assertEquals(Arrays.asList("e0", "e1"), list);
    }

    @Test
    public void noIndex() {
        // Without an index the elements keep the order of the rows
        ListPlacement placement = new ListPlacement();
        List<String> list = new ArrayList<>();
        placement.add(list, "b", null);
        placement.add(list, "a", null);
        placement.add(list, "b", null);
        assertEquals(Arrays.asList("b", "a"), list);
    }

    @Test
    public void sparseIndex() {
        // Gaps in the index do not create empty slots
        ListPlacement placement = new ListPlacement();
        List<String> list = new ArrayList<>();
        placement.add(list, "e10", 10);
        placement.add(list, "e5", 5L);
        placement.add(list, "e20", 20);
        assertEquals(Arrays.asList("e5", "e10", "e20"), list);
    }

    @Test
    public void separateLists() {
        ListPlacement placement = new ListPlacement();
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        placement.add(list1, "a1", 1);
        placement.add(list2, "b0", 0);
        placement.add(list1, "a0", 0);
        assertEquals(Arrays.asList("a0", "a1"), list1);
        assertEquals(Arrays.asList("b0"), list2);
    }

    @Test
    public void jsonArray() {
        ListPlacement placement = new ListPlacement();
        JSONArray array = new JSONArray();
        JSONObject e0 = element("e0"), e1 = element("e1"), e2 = element("e2"), e3 = element("e3");
        placement.add(array, e3, 3);
        placement.add(array, e1, 1);
        placement.add(array, e1, 1);
        placement.add(array, e0, 0);
        placement.add(array, e2, 2);
        assertEquals(Arrays.asList("e0", "e1", "e2", "e3"), names(array));
    }
}
