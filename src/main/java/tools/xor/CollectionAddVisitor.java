package tools.xor;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CollectionAddVisitor
{
    Map<String, AddEvent> collectionEvents = new HashMap<>();
    Set<Property> initialized = new HashSet<>();

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
     * @param property that holds this collection. Useful for eliminating duplicate additions.
     */
    public void add(String path, AddEvent event, Property property) {
        if(initialized.contains(property)) {
            return;
        } else {
            initialized.add(property);
        }
        this.collectionEvents.put(path, event);
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
