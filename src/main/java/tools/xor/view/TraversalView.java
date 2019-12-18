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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.json.JSONObject;
import org.springframework.util.StringUtils;
import tools.xor.AbstractType;
import tools.xor.AggregateAction;
import tools.xor.AssociationSetting;
import tools.xor.EntityType;
import tools.xor.FunctionScope;
import tools.xor.FunctionType;
import tools.xor.MatchType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.DataAccessService;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.DFAtoRE;
import tools.xor.util.Edge;
import tools.xor.util.InterQuery;
import tools.xor.util.State;
import tools.xor.util.Vertex;
import tools.xor.util.graph.StateTree;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.TypeGraph;
import tools.xor.view.AggregateTree.QueryKey;
import tools.xor.view.expression.AliasHandler;

/**
 *
 Views are interpreted based on the following:

 a.b.[VIEW_NAME] - would expand the properties list to include that in the VIEW_NAME

 child view:
 1. It has a generated name
 2. Used to represent a separate query
 3. Used to narrow a type
 4. Specify relative properties

 An alias cannot be specified without the anchor path
 By default all properties are absolute paths

 So with regards to the child view, we have 2 dimensions, and let us look at how they affect the child view functionality:
 A ‘✓’ marks the presence of that dimension

 These values are specified using the ALIAS function on the parent view.
 If the alias refers to a view then the alias type is VIEW, if it refers to a property then the alias type is PROPERTY.
 For a view alias, the view name is required, the view entity type or anchor path is optional.

 The table below shows how the view alias is interpreted depending upon if the EntityType and/or the anchor path is provided.

 child EntityType      anchor path          Outcome
 =======================================================================================================

 ✓                  ✓                 Used for reasons (2), (3) and (4)
 ✓                 Used for reasons (2) and (4)


 Question: What if I want the child query to be part of the parent query and to have type narrowing on the child
 Solution: A NARROW function needs to be created and a child view is not necessary

 Question: An ALIAS function is provided, but the queries are not created using FragmentBuilder
 Solution: We don’t have to create Fragments, but we have to have QueryField objects for each query

 */

@XmlRootElement(name="TraversalView")
public class TraversalView implements Comparable<TraversalView>, Vertex, View {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public static final String  VIEW_REFERENCE_START = "[";
    public static final String  VIEW_REFERENCE_NAME_REGEX = "^.*\\[\\s*(\\w+)\\s*\\].*";
    public static final String  VIEW_REFERENCE_MULTIPLE_REGEX = "^(.*)(\\[\\s*(\\w+)\\s*\\]).*(\\[\\s*\\w+\\s*\\].*)";
    public static final String  REGEX_STRING = "[\\*\\?\\+\\[\\{\\|\\(\\)\\^\\$]";
    public static final Pattern REGEX_STRING_MATCHER = Pattern.compile(REGEX_STRING);

    public static final String DOMAIN = "DOMAIN:";
    public static final String REGEX = "_REGEX_";

    protected String            name;
    protected String            anchorPath;
    protected String            typeName;   // represents the root entity type name
    protected List<Join>        join;
    protected int               version;    // The version from which this view is effective
    protected int               resultPosition;

    // The primary key attribute name needed for linking with child views
    // This is a list because a primary key can be composite
    protected List<String>      primaryKeyAttribute;

    // A dotted notation list of attributes representing the view scope
    protected List<String>      attributeList;

    // Alternative representation using a richer format, i.e., JSON
    protected String            jsonString;

    // A JSONObject of the jsonString
    // With be built either from attributeList or jsonString
    // jsonString has richer information and is the recommended approach
    @XmlTransient
    protected JSONObject        json;

    // Each function should be independent of one another
    // TODO: Should this be pushed down to AggregateView?
    protected List<Function> function = new ArrayList<>();

    @XmlTransient
    private boolean expanded;

    @XmlTransient
    private Set<String> exactAttributes; // These do not have the recursive operand (*)
    // and exact match can be performed

    @XmlTransient
    private Map<String, Pattern> regexAttributes;

    @XmlTransient
    private boolean isSplitToRoot = true;

    @XmlTransient
    private Shape shape; // The Shape with which this view is associated

    @XmlTransient
    private final Map<QueryKey, AggregateTree>  queryCache = new ConcurrentHashMap<>();

    // A view can be valid for multiple entity types, especially if the view refers
    // to the attributes in a base class. Then in this case, the view can be
    // used by all the subtypes of that base class.
    @XmlTransient
    private final Map<String, StateGraph<State, Edge<State>>> stateGraph =  new ConcurrentHashMap<>();

    @XmlTransient
    private final Map<String, PropertyAlias> aliasMap = new ConcurrentHashMap<>();

    @XmlTransient
    private final Map<String, PropertyAlias> viewAliasMap = new ConcurrentHashMap<>();

    /**********************  C O N S T R U C T O R S ***************************/

    public TraversalView(QueryTree queryTree) {
        initQuery(queryTree);
    }

    public TraversalView(Type type, String viewName) {
        this(viewName);
        this.typeName = type.getName();
    }

    public TraversalView(String viewName) {
        setName(viewName);
    }

    /**
     * No-args constructor required for Unmarshalling purpose and also internally by the framework.
     * Don't use this directly.
     */
    public TraversalView() {
        // Name is required
        setName(UUID.randomUUID().toString());
    }

    private void initQuery(QueryTree queryTree) {
        this.name = queryTree.getName();
        this.typeName = queryTree.getAggregateType().getName();

        // We create the OQLQuery object and populate it with information from the query view
        // such as ColumnMeta information
        // The query builder should have populated the OQLQuery object
    }

    @Override
    public List<String> getPrimaryKeyAttribute () {
        return this.primaryKeyAttribute;
    }

    @Override public boolean isTempTablePopulated ()
    {
        return false;
    }

    @Override public void setTempTablePopulated (boolean tempTablePopulated)
    {
        throw new UnsupportedOperationException("This method cannot be invoked on TraversalView");
    }

    @Override public int getResultPosition ()
    {
        return resultPosition;
    }

    public void setPrimaryKeyAttribute (List<String> attribute) {
        this.primaryKeyAttribute = attribute;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public List<Join> getJoin() {
        return join;
    }

    public void setJoin(List<Join> join) {
        this.join = join;
    }

    public List<AggregateView> getChildren() {
        return null;
    }

    public void setChildren(List<AggregateView> children) {
        throw new UnsupportedOperationException("This method cannot be invoked on TraversalView");
    }

    @Override
    public List<Function> getFunction () {
        return function;
    }

    public void setFunction (List<Function> functions) {
        this.function = functions;
    }

    public Function addFunction (FunctionType type, String arg) {
        List<String> args = new LinkedList<>();
        args.add(arg);
        return addFunction(null, type, 1, args);
    }

    public Function addFunction (String name, String arg1, String arg2) {
        List<String> args = new LinkedList<>();
        args.add(arg1);
        args.add(arg2);

        return addFunction(name, FunctionType.COMPARISON, 1, args);
    }

    public Function addFunction (String name, String arg1, String arg2, String arg3) {
        List<String> args = new LinkedList<>();
        args.add(arg1);
        args.add(arg2);
        args.add(arg3);

        return addFunction(name, FunctionType.COMPARISON, 1, args);
    }

    public Function addFunction (FunctionType type, int position, String arg) {
        List<String> args = new LinkedList<>();
        args.add(arg);
        return addFunction(null, type, position, args);
    }

    public Function addFunction (String name, FunctionType type, int position, List<String> args) {
        // We prohibit directly adding condition by the user for security reasons
        if(type == FunctionType.FREESTYLE) {
            throw new IllegalStateException("Direct addition of query condition is prohibited from Settings");
        }

        Function newFunction = new Function(name, type, FunctionScope.ANY, position, args);
        this.function.add(newFunction);

        return newFunction;
    }

    public OQLQuery getSystemOQLQuery() {
        return null;
    }

    public void setSystemOQLQuery(OQLQuery systemOQLQuery) {
        throw new UnsupportedOperationException("This method cannot be invoked on TraversalView");
    }

    @Override
    public OQLQuery getUserOQLQuery() {
        return null;
    }

    @Override
    public void setUserOQLQuery(OQLQuery userOQLQuery) {
        throw new UnsupportedOperationException("This method cannot be invoked on TraversalView");
    }

    @Override
    public NativeQuery getNativeQuery() {
        return null;
    }

    public void setNativeQuery(NativeQuery nativeQuery) {
        throw new UnsupportedOperationException("This method cannot be invoked on TraversalView");
    }

    @Override
    public StoredProcedure getStoredProcedure(final AggregateAction action) {
        return null;
    }

    @Override
    public List<StoredProcedure> getStoredProcedure() {
        return null;
    }

    @Override
    public void setStoredProcedure(List<StoredProcedure> storedProcedure) {
        throw new UnsupportedOperationException("This method cannot be invoked on TraversalView");
    }

    @XmlTransient
    @Override
    public Shape getShape() {
        return this.shape;
    }

    @Override
    public void setShape(Shape shape) {
        this.shape = shape;
    }

    @Override
    public List<String> getConsolidatedAttributes () {
        List<String> result = new LinkedList<>();

        if(getAttributeList() != null) {
            result.addAll(getAttributeList());
        }

        if(getChildren() != null) {
            for (View child : getChildren()) {
                result.addAll(child.getConsolidatedAttributes());
            }
        }

        return result;
    }

    @Override
    public JSONObject getJson() {
        return this.json;
    }

    @Override
    public List<String> getAttributeList() {
        return attributeList;
    }

    @Override
    public void setAttributeList(List<String> attributeList) {
        this.attributeList = attributeList;
    }

    @Override public String getTypeName() {
        return typeName;
    }

    @Override
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAnchorPath() {
        return anchorPath;
    }

    @Override
    public void setAnchorPath(String anchorPath) {
        this.anchorPath = anchorPath;
    }

    private AggregateTree getAggregateTree (QueryKey viewKey) {
        if(!queryCache.containsKey(viewKey)) {

            // Build the QueryTree
            AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = new AggregateTree(this);
            new FragmentBuilder(aggregateTree).build((EntityType)viewKey.type);

            // run through the cartesian join splitter
            if (isSplitToRoot()) {
                (new SplitToRoot(aggregateTree)).execute();
            }
            else {
                (new SplitToAnchor(aggregateTree)).execute();
                (new SplitSubtype(aggregateTree)).execute();
            }

            Shape shape = ((EntityType)viewKey.type).getShape();
            (new JoinTableAmender(aggregateTree, shape)).execute();

            queryCache.put(viewKey, aggregateTree);
        }

        return queryCache.get(viewKey).copy();
    }

    @Override
    public AggregateTree getAggregateTree (Type type) {
        return getAggregateTree(new QueryKey(type, name));
    }

    public static boolean isBuiltInView(String viewName) {
        for(ViewType viewType: ViewType.values()) {
            if(viewName.endsWith(viewType.toString())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAggregateView(String viewName) {
        return viewName.endsWith(ViewType.AGGREGATE.toString());
    }

    @Override
    public Class inferDomainClass() {
        String suffix = null;
        Class result = null;

        for(ViewType viewType: ViewType.values()) {
            if(name.endsWith(viewType.toString())) {
                suffix = Settings.URI_PATH_DELIMITER + viewType.toString();
                break;
            }
        }

        if(suffix != null) {
            String encodedName = name.substring(0, name.indexOf(suffix));
            String className = Settings.decodeParam(encodedName);
            try {
                result = Class.forName(className);
            }
            catch (ClassNotFoundException e) {
                result = null;
            }
        }

        return result;
    }

    @Override
    public TraversalView copy()
    {
        TraversalView copy = new TraversalView();
        copyInto(copy);

        return copy;
    }

    protected void copyInto(TraversalView copy) {

        copy.setName(name);
        copy.setAnchorPath(anchorPath);
        copy.setTypeName(typeName);
        copy.setExpanded(expanded);
        copy.jsonString = jsonString;
        if(json != null) {
            copy.json = ClassUtil.copyJson(json);
        }
        copy.setSplitToRoot(isSplitToRoot());
        if(attributeList != null) {
            copy.setAttributeList(new ArrayList<>(attributeList));
        }

        if(primaryKeyAttribute != null) {
            copy.primaryKeyAttribute = new ArrayList<>(primaryKeyAttribute);
        }

        if(function != null) {
            List<Function> functionCopy = new ArrayList<>();
            for(Function f: function) {
                functionCopy.add(new Function(f));
            }
            copy.setFunction(functionCopy);
        }

        copy.setJoin(join);

        if(regexAttributes != null) {
            copy.regexAttributes = new HashMap(regexAttributes);
        }
        if(exactAttributes != null) {
            copy.exactAttributes = new HashSet(exactAttributes);
        }

        // NOTE: If a copy is taken and the values changed then the state graph could become invalidated
        if(stateGraph != null) {
            for(Map.Entry<String, StateGraph<State, Edge<State>>> entry: stateGraph.entrySet()) {
                copy.stateGraph.put(entry.getKey(), entry.getValue().copy());
            }
        }

        copy.setShape(getShape());
    }

    @Override
    public Set<String> getViewReferences() {
        Set<String> result = new HashSet<>();

        for(String attribute: getConsolidatedAttributes()) {
            String viewName = getViewReference(attribute);
            if(viewName != null)
                result.add(viewName);
        }

        return result;
    }

    public static String getViewReference(String attribute) {
        if(attribute.matches(VIEW_REFERENCE_NAME_REGEX)) {
            Scanner s = new Scanner(attribute);
            try {
                s.findInLine(VIEW_REFERENCE_NAME_REGEX);
                MatchResult result = s.match();
                if(result.groupCount() == 1)
                    return result.group(1);
            } finally {
                s.close();
            }
        }

        return null;
    }

    @Override
    public boolean hasViewReference() {

        for (String attribute : getConsolidatedAttributes()) {
            if (getViewReference(attribute) != null)
                return true;
        }

        return false;
    }

    // Extract JSON from the attributes list
    public JSONObject extractJSON(List<String> expanding)
    {
        JSONObject result = new JSONObject();

        if(jsonString != null && attributeList != null) {
            throw new RuntimeException("Cannot configure view with both json and path representation. Choose one.");
        }

        if(jsonString != null) {
            result = new JSONObject(jsonString);
        } else if(attributeList != null) {
            extractJSON(result, attributeList);
        }

        if(!isCompositionView()) {
            expand(result, "", expanding);
        }

        return result;
    }

    // TODO: make private
    public static void extractJSON(JSONObject json, List<String> attributes) {
        for(String path: attributes) {
            String remaining = State.getRemaining(path);
            String field = State.getNextAttr(path);

            // create intermediate objects if needed
            JSONObject owner = json;
            while(remaining != null) {
                if(!owner.has(field)) {
                    owner.put(field, new JSONObject());
                }
                owner = owner.getJSONObject(field);
                field = State.getNextAttr(remaining);
                remaining = State.getRemaining(remaining);
            }
            owner.put(field, "");
        }
    }

    protected void addChildView(View view, String anchor) {
        throw new UnsupportedOperationException("A TraversalView cannot have children");
    }

    private void expand(JSONObject owner, String anchor, List<String> expanding) {
        List<String> toRemove = new LinkedList<>();
        Map<String, Object> toAdd = new HashMap<>();

        Iterator iter = owner.keys();
        while(iter.hasNext()) {
            String key = (String)iter.next();
            anchor += (StringUtils.isEmpty(anchor) ? "" : Settings.PATH_DELIMITER) + key;

            Object child = owner.get(key);
            if(child instanceof JSONObject) {
                expand((JSONObject) child, anchor, expanding);
            } else {
                JSONObject reference = null;
                boolean isValidReference = false;
                if (getViewReference(key) != null) {
                    View view = getView(getShape(), key);

                    if(view != null) {
                        isValidReference = true;

                        // This will become a child AggregateView, since it is targeted for querying
                        if(view.isCustom()) {
                            addChildView(view, anchor);
                        } else {
                            view.expand(expanding);
                            reference = view.getJson();
                        }
                    }
                }

                if(isValidReference) {
                    // remove the view reference field
                    toRemove.add(key);

                    if(reference != null) {
                        // merge the reference object with the owner
                        Iterator childIter = reference.keys();
                        while (childIter.hasNext()) {
                            String childKey = (String)childIter.next();
                            toAdd.put(childKey, reference.get(childKey));
                        }
                    }
                }

            }
        }

        for(String key: toRemove) {
            owner.remove(key);
        }

        for(Map.Entry<String, Object> entry: toAdd.entrySet()) {
            owner.put(entry.getKey(), entry.getValue());
        }
    }

    public boolean isCustom() {
        return false;
    }

    @Override
    public boolean isCompositionView() {

        if(getAttributeList() != null) {
            for (String attribute : getAttributeList()) {
                if (isCompositionReference(attribute)) {
                    View view = getView(shape, attribute);
                    if(view.isCustom()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isCompositionReference(String path) {
        if(path.trim().startsWith(TraversalView.VIEW_REFERENCE_START)) {
            return true;
        }

        return false;
    }

    // TODO: A view is expanded for 2 reasons
    // 1. QUERY - to be used in a query
    //            Any user provided child queries (or view references) should not be expanded
    // 2. TRAVERSAL - to be used for either update operations or read by graph traversal
    @Override
    public void expand() {
        this.expand(new LinkedList<>());
    }

    @Override
    public void expand(List<String> expanding) {

        // If it is already expanded then return
        if(isExpanded()) {
            return;
        }

        if(expanding.contains(getName())) {
            throw new RuntimeException("Cyclic view reference detected: " + String.join(" -> ", expanding));
        } else {
            expanding.add(getName());
        }

        this.json = extractJSON(expanding);

        // We don't expand composition views
        if(!isCompositionView()) {
            // Find and substitute the view references
            // TODO: save a copy of the original attributeList to help with identifiying if
            //       it is expandable
            attributeList = getExpandedList(getAttributeList(), expanding);

            // Get the RegEx attributes
            Map<String, Pattern> regexMap = new HashMap<>();
            Set<String> exactSet = new HashSet<>();
            for (String attrPath : this.attributeList) {
                if (DFAtoRE.isRegex(attrPath)) {
                    regexMap.put(attrPath, Pattern.compile(attrPath));
                }
                else {
                    exactSet.add(attrPath);
                }
            }
            if (regexMap.size() > 0) {
                this.setRegexAttributes(regexMap);
            }
            if (exactSet.size() > 0) {
                this.setExactAttributes(exactSet);
            }
        }

        setExpanded(true);
    }

    private boolean hasRegex() {
        if(this.attributeList != null) {
            for (String attrPath : this.attributeList) {
                if (DFAtoRE.isRegex(attrPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<String> getExpandedList(List<String> input, List<String> expanding) {
        // Find and substitute the view references
        List<String> newList = new ArrayList<>();

        if(input != null) {
            for (String attribute : input) {
                if (getViewReference(attribute) != null) {
                    newList.addAll(expand(getShape(), attribute, expanding));
                }
                else {
                    newList.add(attribute);
                }
            }
        }

        return newList;
    }

    @Override
    public Set<String> getFunctionAttributes() {
        Set<String> functionAttributes = new HashSet<>();

        if(getFunction() != null) {
            for (Function function : getFunction()) {
                functionAttributes.addAll(function.getAttributes());
            }
        }

        return functionAttributes;
    }

    private static View getView(Shape shape, String attribute) {
        if(attribute.matches(VIEW_REFERENCE_MULTIPLE_REGEX)) {
            throw new IllegalStateException("Cannot refer to more than one view in an attribute: " + attribute);
        }
        String viewName = getViewReference(attribute);
        View view = shape.getView(viewName);

        return view;
    }

    private static List<String> expand(Shape shape, String attribute, List<String> expanding) {
        View view = getView(shape, attribute);

        // Attributes might be referring to a RegEx expression
        if(view == null) {
            List<String> unchanged = new LinkedList<>();
            unchanged.add(attribute);
            return unchanged;
        }

        if(!view.isExpanded()) {
            view.expand(expanding);
        }

        String prefix = extractAnchor(attribute);
        List<String> expandedAttributes = new ArrayList<>();

        if(view.isCustom()) {
            List<String> pkAttribute = view.getPrimaryKeyAttribute();
            if(pkAttribute == null) {
                throw new RuntimeException("primaryKeyAttribute needs to be specified to use a custom view. " +
                    "This is needed for efficieny reasons as the whole point of the custom view is to avoid inefficient fetching by the parent");
            }
            for (String suffix : pkAttribute) {
                expandedAttributes.add(prefix + suffix);
            }
        } else {
            for (String suffix : view.getAttributeList()) {
                expandedAttributes.add(prefix + suffix);
            }
        }

        return expandedAttributes;
    }

    @XmlTransient
    @Override
    public Set<String> getExactAttributes ()
    {
        return exactAttributes;
    }

    public void setExactAttributes (Set<String> exactAttributes)
    {
        this.exactAttributes = exactAttributes;
    }

    @XmlTransient
    @Override
    public Map<String, Pattern> getRegexAttributes() {
        return this.regexAttributes;
    }

    public void setRegexAttributes(Map<String, Pattern> regexAttributes) {
        this.regexAttributes = regexAttributes;
    }

    @Override
    public boolean matches(String path) {
        if(regexAttributes != null) {
            for(Pattern p: regexAttributes.values()) {
                Matcher matcher = p.matcher(path);
                if(matcher.matches() || matcher.hitEnd()) {
                    return true;
                }
            }
        }

        return false;
    }

    @XmlTransient
    public Map<String, StateGraph<State, Edge<State>>> getStateGraph() {
        return this.stateGraph;
    }

    private String getEntityName(EntityType type, StateGraph.Scope scope) {
        String namePrefix = scope.name() + ":";

        if(type.isDomainType()) {
            return namePrefix + DOMAIN + type.getName();
        } else {
            return namePrefix + type.getName();
        }
    }

    @Override
    public void addTypeGraph (EntityType type, TypeGraph<State, Edge<State>> value, StateGraph.Scope scope) {

        stateGraph.put(getEntityName(type, scope), (StateGraph<State, Edge<State>>)value);
    }

    @Override
    public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType) {
        return getTypeGraph(entityType, StateGraph.Scope.TYPE_GRAPH);
    }

    public TypeGraph<State, Edge<State>> getTypeGraph () {
        if(typeName == null) {
            throw new IllegalStateException("The type for the view needs to be provided");
        }

        // Return the type graph related to the view EntityType
        return getTypeGraph(null);
    }

    @Override
    public boolean isTree (Settings settings) {
        if(settings.getExpandedAssociations() != null && settings.getExpandedAssociations().size() > 0) {
            for(AssociationSetting assoc: settings.getExpandedAssociations()) {
                if(assoc.getMatchType() == MatchType.TYPE ||
                    assoc.getMatchType() == MatchType.RELATIVE_PATH) {
                    return false;
                }
            }
        }

        if(isAggregateView(getName())) {
            return false;
        }

        if(hasRegex()) {
            return false;
        }

        return true;
    }

    @Override
    public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType, StateGraph.Scope scope) {

        // If entityType is provided, it takes precedence over typeName
        // Ensure entityType is same or subType of typeName
        EntityType type = null;
        if(typeName != null) {
            type = (EntityType)shape.getType(typeName);

            if(entityType != null) {
                assert type.isSameOrSupertypeOf(entityType) :
                    "EntityType should be of the same type as " + typeName;
            } else {
                entityType = type;
            }
        }
        String entityName = getEntityName(entityType, scope);

        // This is not a default view, then we need to construct the type graph for this view
        if(!stateGraph.containsKey(entityName) ) {
            if(typeName != null) {

                // If EntityType is not provided, then use type as the EntityType
                if(entityType == null && type instanceof EntityType) {
                    entityType = (EntityType) type;
                }
                if(entityType == null) {
                    throw new RuntimeException("The given type should be an entityType: " + typeName);
                }
                if (!type.isSameOrSupertypeOf(entityType)) {
                    throw new RuntimeException(
                        "The view type " + type.getName()
                            + " should either be the same or a supertype of the given type: "
                            + entityType.getName());
                }
            }

            switch(scope) {
            case EDGE:
                stateGraph.put(entityName, StateTree.build(this, entityType));
                break;
            case VIEW_GRAPH:
                stateGraph.put(entityName, DFAtoRE.build(this, entityType));
                break;
            case TYPE_GRAPH:
                DFAtoRE dfaRE = new DFAtoRE(entityType, shape);
                stateGraph.put(entityName, dfaRE.getExactStateGraph());
                break;
            case FULL_GRAPH:
                dfaRE = new DFAtoRE(entityType, shape);
                stateGraph.put(entityName, dfaRE.getFullStateGraph());
                break;
            }
        }

        return stateGraph.get(entityName);
    }

    public static boolean isEdgeGraph(View view) {
        return !view.isExpanded() && view.getTypeName() != null;
    }

    @Override
    public int compareTo(TraversalView o) {
        return (name.equals(o.getName())) ? (version - o.getVersion()) : name.compareTo(o.getName());
    }

    public boolean isMigrateView(Type type) {
        String migrateViewName = AbstractType.getMigrateViewName(type);
        return migrateViewName != null && migrateViewName.equals(getName());
    }

    @Override
    public boolean isValid() {
        TypeGraph typeGraph = getTypeGraph();

        for(String path: attributeList) {
            if(!typeGraph.hasPath(path)) {
                logger.info("Invalid path: " + path);
                return false;
            }
        }
        return true;
    }

    /**
     * propertyName - optional. If the propertyName is not provided then it represents an alias on the root object
     * alias - required
     * typeName - alias type, can be a subtype of the type of propertyName
     * viewName - optional, represents the properties added to the alias
     */
    public static class PropertyAlias {
        String alias;
        String propertyName;
        String typeName;
        String viewName;
        boolean isInterQuery; // If present, then this alias represents an interquery edge

        public PropertyAlias(String alias, String propertyName, String typeName, String viewName, boolean isInterQuery) {
            this.alias = alias;
            this.propertyName = propertyName;
            this.typeName = typeName;
            this.viewName = viewName;
            this.isInterQuery = isInterQuery;

            assert(this.alias != null);
        }

        public boolean isViewReference() {
            return this.viewName != null && !"".equals(this.viewName.trim());
        }

        public String getViewName() {
            return this.viewName;
        }

        public String getTypeName () {
            return this.typeName;
        }

        public String getAlias() {
            return this.alias;
        }

        public String getOriginal() {
            return Settings.getBaseName(this.propertyName);
        }

        @Override
        public boolean equals(Object o) {
            if(this == o)
                return true;

            if(!PropertyAlias.class.isAssignableFrom(o.getClass()))
                return false;

            PropertyAlias other = (PropertyAlias) o;
            if(!alias.equals(other.alias)) {
                return false;
            }

            if( (propertyName != null ? propertyName.equals(other.propertyName) : other.propertyName == null) &&
                (typeName != null ? typeName.equals(other.typeName) : other.typeName == null) &&
                (viewName != null ? viewName.equals(other.viewName) : other.viewName == null)
                ) {
                if(isInterQuery == other.isInterQuery) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public int hashCode() {
            int h = 17;

            h = 31 * h + alias.hashCode();
            h = propertyName != null ? (31 * h + propertyName.hashCode()) : h;
            h = typeName != null ? (31 * h + typeName.hashCode()) : h;
            h = viewName != null ? (31 * h + viewName.hashCode()) : h;
            h = isInterQuery ? 31 * h : h;

            return h;
        }
    }

    /**
     * Return the aliases of a property
     * @param path the path for which to check for alias
     * @return the alias if present, null otherwise
     */
    public PropertyAlias getAlias(String path) {

        if(aliasMap.size() == 0) {
            // check and initialize if needed
            if(getFunction() != null) {
                for (Function function : getFunction()) {
                    if (function.type == FunctionType.ALIAS) {
                        AliasHandler ah = (AliasHandler)function.getHandler();
                        PropertyAlias pa = new PropertyAlias(
                            function.getName(),
                            function.getAttribute(),
                            ah.getSubclassName(),
                            ah.getViewName(),
                            ah.isInterQuery());

                        if(pa.isViewReference()) {
                            viewAliasMap.put(function.getAttribute(), pa);
                        } else {
                            aliasMap.put(function.getAttribute(), pa);
                        }
                    }
                }
            }
        }

        // Get the anchor path for the view reference
        path = extractAnchor(path);

        return aliasMap.get(path);
    }

    public static String extractAnchor(String propertyPath) {
        if(propertyPath.contains(VIEW_REFERENCE_START)) {
            propertyPath = propertyPath.substring(0, propertyPath.indexOf(VIEW_REFERENCE_START));
        }

        return propertyPath;
    }

    public Set<PropertyAlias> getViewAliases() {
        return new HashSet(viewAliasMap.values());
    }

    @Override public boolean isSplitToRoot ()
    {
        return this.isSplitToRoot;
    }

    @Override public void setSplitToRoot (boolean value)
    {
        this.isSplitToRoot = value;
    }
}
