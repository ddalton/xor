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

package tools.xor.generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.IteratorListener;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateTree;

public class DefaultGenerator implements Generator
{
    public static final String PARENT_LINK = "[PARENT]";
    public static final String ROOT_LINK = "[ROOT]";
    public static final String GLOBAL_SEQ = "[GLOBAL_SEQ]";
    public static final String THREAD_NO = "[THREAD_NO]";
    public static final String ENTITY_SIZE = "[ENTITY_SIZE]";
    public static final String VISITOR_CONTEXT = "[VISITOR_CONTEXT]";
    public static final String GENERATOR = "[GENERATOR]";
    public static final String GENERATOR_HIER_ID = "[GENERATOR_HIER_ID]";
    public static final String GENERATOR_PARENT_ID = "[GENERATOR_PARENT_ID]";
    public static final String QUERY_DATA = "QUERY_DATA.";
    public static final int MAX_VISITOR_CONTEXT = 3;    

    // Used for collection sizes and range percent choices
    private static final String SIZE_DELIM = ":";
    protected static final String RANGE_DELIM = ",";
    protected static final String PERCENT_DELIM = ":";
    protected static final String PLACEHOLDER = "[__]";
    protected static final String TOKEN_END = "]";

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    protected String[] values;

    // Used if the generator is a driver
    private Set<IteratorListener> listeners = new HashSet<>();

    // Used to cache the fanout value for a FixedSet generator of domain values
    // It's value has the following interpretation:
    // null  Not evaluated
    // -1    The collection element does not have any FixedSet generator
    // > 0   Size of the fixed set collection
    private Integer fixedFanOut;
    private List<GeneratorVisit> visits;

    public DefaultGenerator (String[] arguments)
    {
        this.values = arguments;
    }
    
    /**
     * This constructor is used if the min and max value
     * is the same. i.e., the generator can produce only 1 value.
     * 
     * @param argument single value
     */
    public DefaultGenerator (int argument)
    {
        String strval = String.valueOf(argument);
        this.values = new String[] {strval, strval};
    }    

    public String[] getValues ()
    {
        return values;
    }

    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        byte minimum = Byte.MIN_VALUE;
        byte maximum = Byte.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Byte.parseByte(values[0]);
        }
        if (values.length >= 2) {
            maximum = Byte.parseByte(values[1]);
        }

        byte range = (byte) (maximum - minimum);
        return (byte) (minimum + ((byte)(range != 0 ? ClassUtil.nextDouble() * range : 0)));
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        short minimum = Short.MIN_VALUE;
        short maximum = Short.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Short.parseShort(values[0]);
        }
        if (values.length >= 2) {
            maximum = Short.parseShort(values[1]);
        }

        short range = (short) (maximum - minimum);
        return (short) (minimum + ((short)(range != 0 ? ClassUtil.nextDouble() * range : 0)));
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        char minimum = Character.MIN_VALUE;
        char maximum = Character.MAX_VALUE;

        if (values.length >= 1) {
            minimum = (char) Integer.valueOf(values[0]).intValue();
        }
        if (values.length >= 2) {
            maximum = (char) Integer.valueOf(values[1]).intValue();
        }

        char range = (char) (maximum - minimum);
        return (char) (minimum + ((char)(range != 0 ? ClassUtil.nextDouble() * range : 0)));
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int minimum = Integer.MIN_VALUE;
        int maximum = Integer.MAX_VALUE;

        if (values.length >= 1) {
            try {
                minimum = Double.valueOf(values[0]).intValue();
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("getIntValue, parsing error with min input: " + values[0], nfe);
            }
        }
        if (values.length >= 2) {
            try {
                maximum = Double.valueOf(values[1]).intValue();
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("getIntValue, parsing error with max input: " + values[1], nfe);
            }
        }

        int range = maximum - minimum;
        return minimum + (range != 0 ? ((int)(ClassUtil.nextDouble() * range)) : 0);
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        long minimum = Long.MIN_VALUE;
        long maximum = Long.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Long.parseLong(values[0]);
        }
        if (values.length >= 2) {
            maximum = Long.parseLong(values[1]);
        }

        long range = maximum - minimum;
        return minimum + (range != 0 ? ((long)(ClassUtil.nextDouble() * range)) : 0);
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        long minimum = 0;
        long maximum = (new Date()).getTime() + (1000*3600*24*365*2); // 2 years in future

        if (values.length >= 1) {
            try {
                minimum = Long.parseLong(values[0]);
            } catch (NumberFormatException nfe) {
                minimum = new BigDecimal(values[0]).longValueExact();
            }
        }
        if (values.length >= 2) {
            try {
                maximum = Long.parseLong(values[1]);
            } catch (NumberFormatException nfe) {
                maximum = new BigDecimal(values[1]).longValueExact();
            }            
        }        
        
        long  range = maximum - minimum;
        return new Date((long) (minimum + (range != 0 ? (ClassUtil.nextDouble() * range) : 0)));
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        double minimum = Double.MIN_VALUE;
        double maximum = Double.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Double.parseDouble(values[0]);
        }
        if (values.length >= 2) {
            maximum = Double.parseDouble(values[1]);
        }

        double range = maximum - minimum;
        return minimum + (range != 0 ? (ClassUtil.nextDouble() * range) : 0);
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        float minimum = Float.MIN_VALUE;
        float maximum = Float.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Float.parseFloat(values[0]);
        }
        if (values.length >= 2) {
            maximum = Float.parseFloat(values[1]);
        }

        float range = maximum - minimum;
        return minimum + ((float)(range != 0 ? ClassUtil.nextDouble() * range : 0));
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        BigInteger value = getBigInteger(visitor);
        
        int scale = 0;
        if(values.length >= 3) {
            scale = Integer.valueOf(values[2]);
        }
        
        return new BigDecimal(value, scale);
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        BigInteger minimum = BigInteger.ONE;
        BigInteger maximum = new BigInteger((new Long(Long.MAX_VALUE)).toString());

        if (values.length >= 1) {
            minimum = new BigInteger(values[0]);
        }
        if (values.length >= 2) {
            maximum = new BigInteger(values[1]);
        }

        long range = maximum.longValue() - minimum.longValue();
        return new BigInteger((new Long((long) (minimum.longValue() + (range != 0 ? ClassUtil.nextDouble() * range : 0)))).toString());
    }

    /**
     * Calculates a random index position from 0 to values.size-1
     * @return an int value
     */
    protected int getPosition() {
        if(getValues().length == 0) {
            throw new RuntimeException("Choices generator needs to have a minimum of 1 value.");
        }

        // Optimization to avoid invoking random()
        if(getValues().length == 1) {
            return 0;
        }

        int result =  (int) (ClassUtil.nextDouble() * getValues().length);
        if(result == getValues().length) {
            result--;
        }

        return result;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        int pos = getPosition();
        return getValues()[pos];
    }

    protected EntityType getSubType(List<EntityType> subTypes, StateGraph stateGraph) {

        List<EntityType> types = new ArrayList<EntityType>();
        for(EntityType et: subTypes) {
            if(et.isAbstract()) {
                continue;
            } else if(stateGraph.getVertex(et) == null) {
                // SubType not in scope
                continue;
            } else {
                types.add(et);
            }
        }

        int index =  (int) (ClassUtil.nextDouble() * types.size());
        if(index == types.size()) {
            index--;
        }

        return (index >= 0) ? types.get(index) : null;

    }

    @Override public EntityType getSubType (EntityType entityType, StateGraph stateGraph)
    {
        List<EntityType> subTypes = new ArrayList<EntityType>(entityType.getSubtypes());
        subTypes.add(entityType);
        return getSubType(subTypes, stateGraph);
    }

    @Override public void validate (ExtendedProperty property)
    {

    }

    @Override public int getFanout (Property property, Settings settings, String path, StateGraph.ObjectGenerationVisitor visitor)
    {
        // Calculate the fixedFanOut if applicable
        if(this.fixedFanOut == null) {
            this.fixedFanOut = -1;
            if(property.isMany()) {
                Type type = ((ExtendedProperty)property).getElementType();
                if(type instanceof EntityType) {
                    EntityType entityType = (EntityType) type;
                    for(Property p: entityType.getProperties()) {
                        Generator generator = ((ExtendedProperty)p).getGenerator(visitor.getRelationshipName());
                        if(generator instanceof FixedSet) {
                            // Checking the first occurrence is sufficient, as all other
                            // fixed set should be of same size
                            fixedFanOut = ((FixedSet)generator).getValues().length;
                            break;
                        }
                    }
                }
            }
        }

        if(this.fixedFanOut != null && this.fixedFanOut != -1) {
            return fixedFanOut;
        }

        float sparseness = settings.getSparseness(path);
        return (int)(ClassUtil.nextDouble() * settings.getEntitySize().size() * sparseness);
    }

    @Override public boolean isApplicableToCollectionElement ()
    {
        return false;
    }

    protected JSONObject getComponent(StateGraph.ObjectGenerationVisitor visitor, JSONObject current, String tokenName) {
        if(current == null) {
            return null;
        }

        if(tokenName.equals(PARENT_LINK)) {
            return current.getJSONObject(Constants.XOR.GEN_PARENT);
        } else if(tokenName.equals(ROOT_LINK)) {
            return visitor.getRoot();
        } else if(current.has(tokenName) && current.get(tokenName) instanceof JSONObject) {
            return current.getJSONObject(tokenName);
        }

        return null;
    }

    protected int getMaxCollectionElements() {
        return new Integer(new Double(getValues()[1]).intValue());
    }

    protected String getDependencyValue(StateGraph.ObjectGenerationVisitor visitor) {

        JSONObject parent = visitor.getParent();
        if(parent == null) {
            return null;
        }

        String path = getValues()[0];

        // For each path, loop through each component and see if the value is available
        while (path != null) {
            // Extract the next attribute in the path
            String component = AggregateTree.getTopAttribute(path);

            // Get the path ready for the next round if applicable
            path = AggregateTree.getNext(path);

            JSONObject current = parent;
            parent = getComponent(visitor, current, component);
            if (parent == null) {
                // Maybe we are at the last component
                if (path == null) {

                    // Unable to find the dependency, probably accessed through another path
                    if(!current.has(component)) {
                        return null;
                    }

                    //perform conversion if necessary
                    Object value = current.get(component);
                    if (value != null && value instanceof Number) {
                        return ((Number)value).toString();
                    }

                    return current.getString(component);
                }
                else {
                    // If the object has not yet been created, then try the next path
                    continue;
                }
            }
        }

        return null;
    }

    @Override
    public void init (StateGraph.ObjectGenerationVisitor visitor) {
        // overridden by subclasses
    }

    public static class ModRange {
        Long newstart; // start for the new range
        Long newend;   // end for the new range
        long start;    // start of existing range
        long end;      // end of existing range
        long newRangeDiff;

        public void parse(String text) {
            if(text.indexOf(SIZE_DELIM) == -1) {
                throw new RuntimeException("Expected input of format <new start>,<new end>:<start>,<end>");
            }

            String newRange = text.substring(0, text.indexOf(SIZE_DELIM)).trim();
            String range = text.substring(text.indexOf(SIZE_DELIM)+SIZE_DELIM.length()).trim();

            if(!("".equals(newRange))) {
                this.newstart = new Long(newRange.substring(0, newRange.indexOf(RANGE_DELIM)).trim());
                this.newend = new Long(newRange.substring(newRange.indexOf(RANGE_DELIM)+RANGE_DELIM.length()).trim());
                newRangeDiff = newend - newstart;
            }

            this.start = new Integer(range.substring(0, range.indexOf(RANGE_DELIM)).trim());
            this.end = new Integer(range.substring(range.indexOf(RANGE_DELIM)+RANGE_DELIM.length()).trim());
        }

        public Long getNewValue(Long value) {
            if(newstart == null) {
                return null;
            }

            assert(value >= start && value <= end) : "value should be within range inclusive";

            // convert value from old range to new range
            return start + (value%newRangeDiff);
        }

        public boolean inRange(Long value) {
            return (value >= start && value <= end);
        }
    }

    public static class RangeNode {
        int start; // inclusive
        int end;   // inclusive
        int sizemin;  // or sizemin
        int sizemax = -1; // if it represents a range of sizes
        boolean noOwner;
        RangeNode next;

        public void parse(String text, boolean zeroSupported) {
            if(text.indexOf(SIZE_DELIM) == -1) {
                throw new RuntimeException(String.format("Unable to find size delimiter '%s' in input: %s", SIZE_DELIM, text));
            }
            String rangeStr = text.substring(0, text.indexOf(SIZE_DELIM)).trim();
            String sizeStr = text.substring(text.indexOf(SIZE_DELIM)+SIZE_DELIM.length()).trim();

            if(sizeStr.indexOf(RANGE_DELIM) == -1) {
                this.sizemin = Integer.parseInt(sizeStr);
            } else {
                this.sizemin = new Integer(sizeStr.substring(0, sizeStr.indexOf(RANGE_DELIM)).trim());
                this.sizemax = new Integer(sizeStr.substring(sizeStr.indexOf(RANGE_DELIM)+RANGE_DELIM.length()).trim());
            }

            if(rangeStr.indexOf(RANGE_DELIM) == -1) {
                throw new RuntimeException(String.format("Unable to find range delimiter '%s' in input: %s", RANGE_DELIM, rangeStr));
            }
            this.start = new Integer(rangeStr.substring(0, rangeStr.indexOf(RANGE_DELIM)).trim());
            this.end = new Integer(rangeStr.substring(rangeStr.indexOf(RANGE_DELIM)+RANGE_DELIM.length()).trim());

            if(this.start < 0 || this.end < 0) {
                noOwner = true;
            }

            int oldStart = this.start;
            int oldEnd = this.end;
            this.start = Math.abs(this.start);
            this.end = Math.abs(this.end);

            if(this.end < start) {
                throw new RuntimeException(String.format("Start value %s needs to be less than end value %s. Original start and end values are %s and %s respectively",
                    this.start, this.end, oldStart, oldEnd));
            }

            if(!zeroSupported && this.sizemin == 0) {
                throw new RuntimeException("minimum size should be greater than 0");
            }

            if(this.sizemin < 0) {
                throw new RuntimeException(zeroSupported ? "minimum size should be greater than or equal to 0" : "minimum size should be greater than 0");
            }
        }

        public boolean isNoOwner() {
            return noOwner;
        }

        public int getStart() {
            return this.start;
        }

        public int getEnd() {
            return this.end;
        }

        public RangeNode getNext() {
            return this.next;
        }

        public int getSize() {
            if(sizemax == -1) {
                return sizemin;
            }

            return sizemin + ((int)(ClassUtil.nextDouble() * (sizemax-sizemin)));
        }
    }

    public void buildNodes(List<RangeNode> nodeList, int offset, boolean zeroSupported) {

        RangeNode previous = null;
        for(int i = offset; i < values.length; i++) {
            RangeNode node = new RangeNode();
            node.parse(values[i], zeroSupported);
            addNode(previous, node, nodeList);

            previous = node;
        }
    }

    private void addNode(RangeNode previous, RangeNode current, List<RangeNode> nodeList) {
        if(previous != null) {
            // See if an empty node needs to be added
            if(previous.end+1 < current.start) {
                RangeNode empty = new RangeNode();
                empty.start = previous.end+1;
                empty.end = current.start-1;
                nodeList.add(empty);

                previous.next = empty;
                empty.next = current;
            } else {
                previous.next = current;
            }
        }

        nodeList.add(current);
    }

    public static class GeneratorVisit {
        Generator generator;
        GeneratorRecipient recipient;

        public GeneratorVisit(Generator gen, GeneratorRecipient recipient) {
            this.generator = gen;
            this.recipient = recipient;
        }

        public GeneratorRecipient getRecipient() {
            return this.recipient;
        }

        public Generator getGenerator() {
            return this.generator;
        }
    }

    public void addVisit(GeneratorVisit visit) {
        if(this.visits == null) {
            this.visits = new LinkedList<>();
        }

        this.visits.add(visit);
    }

    public void processVisitors() {
        if(this.visits != null) {
            for (GeneratorVisit visit : visits) {
                visit.recipient.accept(visit.generator);
            }
        }
    }

    public abstract static class PercentNode {
        BigDecimal startPercent; // exclusive, exception is 0
        BigDecimal endPercent; // inclusive
        PercentNode left;
        PercentNode right;

        protected PercentNode findNode(BigDecimal random) {
            if( (random.equals(0) && startPercent.equals(0))
                || (random.compareTo(startPercent) == 1 && random.compareTo(endPercent) != 1) ) {
                return this;
            }

            // walk the left tree
            if(random.compareTo(startPercent) != 1) {
                return left.findNode(random);
            } else {
                return right.findNode(random);
            }
        }

        protected void parse(String text, PercentNode previous) {
            if(previous == null) {
                startPercent = new BigDecimal(0);
            } else {
                startPercent = previous.endPercent;
            }

            String percent = text.substring(text.indexOf(PERCENT_DELIM)+PERCENT_DELIM.length());
            endPercent = new BigDecimal(percent);

            if(endPercent.compareTo(new BigDecimal(0)) <= 0) {
                throw new RuntimeException(String.format("The percent value %s needs to be greater than 0", endPercent));
            }

            if(endPercent.compareTo(new BigDecimal(1)) > 0) {
                throw new RuntimeException(String.format("The percent value %s needs to be <= 1.00", endPercent));
            }
        }

        public abstract Long getLong ();

        public abstract String getString ();
    }

    /**
     * Given a start and end index of the values array, it returns
     * a node, that is the root of the binary tree.
     *
     * @param startIndex of the values array representing the start of the tree
     * @param endIndex of the values array representing the end of the tree
     * @param nodeList the list of nodes converted to a tree data structure
     * @return root node of binary tree
     */
    protected PercentNode buildTree(int startIndex, int endIndex, List<PercentNode> nodeList)
    {
        if(startIndex > endIndex) {
            return null;
        }

        int mid = (startIndex+endIndex)/2;
        PercentNode root = nodeList.get(mid);

        root.left = buildTree(startIndex, mid-1, nodeList);
        root.right = buildTree(mid+1, endIndex, nodeList);

        return root;
    }

    public String getCurrentValue(StateGraph.ObjectGenerationVisitor visitor) {
        return getStringValue(null, visitor);
    }

    public void addListener(IteratorListener listener) {
        listeners.add(listener);
    }

    protected void notifyListeners(int sourceId, StateGraph.ObjectGenerationVisitor visitor) {
        for(IteratorListener listener: listeners) {
            listener.handleEvent(sourceId, visitor);
        }
    }
}
