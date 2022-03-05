package tools.xor;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

/**
 * Used to reconstitute the collection elements for all the changed fields in a record.
 */
public class ReconstituteRecordVisitor
{
    Map<String, AddEvent> collectionEvents = new HashMap<>();

    public static final class AddEvent {
        JSONArray collection;
        Object element;

        public AddEvent(JSONArray collection, Object element) {
            this.collection = collection;
            this.element = element;
        }

        public void execute() {
            collection.put(element);
        }
    }

    /**
     * Add an event to mark the addition of an element to its collection
     * @param path full property path that triggered this addition
     * @param event that contains details on the element and its collection
     */
    public void add(String path, AddEvent event) {
        if(collectionEvents.containsKey(path)) {
            return;
        }
        collectionEvents.put(path, event);
    }

    /**
     * Add all the elements to their respective collections if
     * the path is anchored at the lcp
     *
     * @param lcp longest common prefix
     */
    public void process(String lcp) {

        for(Map.Entry<String, AddEvent> entry: collectionEvents.entrySet()) {
            if(entry.getKey().startsWith(lcp)) {
                entry.getValue().execute();
            }
        }
    }
}
