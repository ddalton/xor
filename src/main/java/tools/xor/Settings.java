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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import tools.xor.core.EmptyInterceptor;
import tools.xor.core.Interceptor;
import tools.xor.custom.AssociationStrategy;
import tools.xor.custom.DetailStrategy;
import tools.xor.providers.jdbc.ImportMethod;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataStore;
import tools.xor.service.Shape;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.Detector;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.TypeGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.BindParameter;
import tools.xor.view.Function;
import tools.xor.view.NativeQuery;
import tools.xor.view.ObjectResolver;
import tools.xor.view.QueryFragment;
import tools.xor.view.View;
import tools.xor.view.ViewType;

/**
 * @author Dilip Dalton
 *
 */
public class Settings {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public static final String PATH_DELIMITER = ".";
	public static final String PATH_DELIMITER_REGEX = "\\" + PATH_DELIMITER; // The '.' character is a special character in regular expressions, so it has to be escaped
	public static final String URI_PATH_DELIMITER = "_";	
	public static final String ACTION_OVERRIDE = URI_PATH_DELIMITER + "ACTION" + URI_PATH_DELIMITER;
	public static final String USERKEY_OVERRIDE = URI_PATH_DELIMITER + "USERKEY" + URI_PATH_DELIMITER;	
	
	public static final int    INITIAL_API_VERSION = 1;
	public static final int    CURRENT_API_VERSION = 1;

	public enum DateForm {
		FORMATTED,
		NUMBER
	};

	public enum GraphFormat {
		PNG,
		DOT,
		GML
	};

	protected DateForm dateForm = DateForm.FORMATTED;
	public DateForm getDateForm ()
	{
		return dateForm;
	}

	public void setDateForm (DateForm dateForm)
	{
		this.dateForm = dateForm;
	}

	protected String dateFormat = JSONObjectProperty.ISO8601_FORMAT; // this is the default format and can be overridden
	public String getDateFormat ()
	{
		return dateFormat;
	}

	public void setDateFormat (String dateFormat)
	{
		this.dateFormat = dateFormat;
	}

	protected AggregateManager aggregateManager;
	
	protected boolean supportsPostLogic;
	
	// The entity type on which the data is based
	protected Type entityType;

	protected View view;

	protected Shape shape;

	protected StateGraph.Scope scope = StateGraph.Scope.EDGE;

	private AggregateAction action; // specifies the type of action being performed that involves data change in the database

	private AggregateAction mainAction; // represents action, unless it is explicitly set. This is needed since the main action can spawn sub actions

	private Map<String, Object> params = new HashMap<String, Object>(); // Used for filtering the result

	private Map<String, AggregateAction> actionOverrides; // A collection can have a specific action override set

	private Map<String, String> userkeyOverrides; // An association can be resolved using a userkey	

	private Class<?> entityClass;

	private List<String> references;
	private List<AssociationSetting> expandedAssociations;
	private List<AssociationSetting> prunedAssociations;
	private Set<String> pruneRelative; // optimization field to quickly check attributes to prune

	private Interceptor interceptor = EmptyInterceptor.INSTANCE; // Allow user code to inspect or tweak the processing

	private boolean preFlush;      // Setting this flag to true, flushes the session before it executes the operation
	private boolean postFlush;     // Setting this flag to true, flushes the session after it executes the operation	
	private boolean preClear; // Setting this flag to true clears the session cache before it executes the operation
	private boolean preRefresh;   // Setting this flag to true refreshes the object before it executes the operation	
	private boolean persist;     // Setting this flag will do an automatic saveupdate on the object graph after the UPDATE phase

	private AssociationStrategy associationStrategy;
	
	private DetailStrategy detailStrategy;
	
	private boolean denormalized; // specifies if the query should return a denormalized result
	                              // Also can be used for update, specifying if the input data is denormalized
	
	private List<Function> additionalFunctions = new ArrayList<Function>();

	private String downcastName;
	
	private List<String> tags = new ArrayList<String>();
	
	// Paging related attributes
	private Integer limit;
	private Integer offset;
	private Map<String, Object> nextToken;

	private int apiVersion = getCurrentApiVersion();
	
	private boolean baseline; // Retrieve a domain based object, when using the query API
	
	private boolean autoWire;
	
	private Object sessionContext;

	// A negative value indicates that the globalSeq has not been initialized
	private AtomicLong globalSeq = new AtomicLong(1);

	private int batchSize;
	
	// User provided data that is made available to callbacks
	private Object externalData;

	private ObjectResolver.Type resolverType = ObjectResolver.Type.SHARED;
	
	// User provided data that is efficiently obtained
	private PrefetchCache prefetchCache;

	private ImportMethod importMethod = ImportMethod.PREPARED_STATEMENT;

	// Settings related to data generation
	private EntitySize entitySize = EntitySize.MEDIUM;
	private float sparseness = 1.0f;
	private String graphFileName;
	private Map<String, Float> collectionSparseness = new HashMap<String, Float>(); // Currently the logic is based on exact match

	private Detector detector;

	// Transient
	private Map<String, Boolean> shouldCreateIfMissing;

	/** decides how sparse the graph is. This depicts the ratio between the number of vertices vs the number of edges
	 *    So greater the number, the more dense the graph is.
	 *    Takes a value between 0.0f and 1.0f
	 * @return the sparseness value
	 */
	public float getSparseness ()
	{
		return sparseness;
	}

	public float getSparseness(String rootedAt) {
		if(rootedAt != null && collectionSparseness.containsKey(rootedAt)) {
			return collectionSparseness.get(rootedAt);
		}

		return getSparseness();
	}

	public boolean hasCollectionSparseness(String rootedAt) {
		return collectionSparseness.containsKey(rootedAt);
	}

	public AggregateManager getAggregateManager() {
		return this.aggregateManager;
	}

	public DataStore getDataStore ()
	{
		return this.aggregateManager == null ? null : this.aggregateManager.getDataStore();
	}

	public void setAggregateManager(AggregateManager aggregateManager) {
		this.aggregateManager = aggregateManager;
	}

	/*
	 * we need to get the persistence orchestrator specific to that thread
	 */
	public void initDataStore (DataStore persistenceOrchestrator)
	{
		if(persistenceOrchestrator instanceof JDBCDataStore) {

			// Need this to be true for JDBCPersistenceOrchestrator
			this.postFlush = true;

			// Needed for id to be copied
			this.baseline = true;
		}
	}

	public ImportMethod getImportMethod() {
		return this.importMethod;
	}

	public void setImportMethod (ImportMethod importMethod)
	{
		this.importMethod = importMethod;
	}

	public boolean isShouldCreate(Type type) {

		if(action == AggregateAction.LOAD || action == AggregateAction.DELETE) {
			return false;
		}

		if(type instanceof EntityType) {
			if (shouldCreateIfMissing == null && expandedAssociations.size() > 0) {
				// populate
				shouldCreateIfMissing = new HashMap<>();
				for (AssociationSetting setting : expandedAssociations) {
					if (setting.getEntityName() != null) {
						shouldCreateIfMissing.put(
							setting.getEntityName(),
							setting.doCreateIfMissing());
					}
				}
			}

			if (shouldCreateIfMissing != null
				&& shouldCreateIfMissing.containsKey(type.getName())) {
				return shouldCreateIfMissing.get(type.getName());
			}
		}

		return true;
	}

	public void setSparseness (float sparseness)
	{
		this.sparseness = sparseness;
	}

	public EntitySize getEntitySize ()
	{
		return entitySize;
	}

	public void setEntitySize (EntitySize entitySize)
	{
		this.entitySize = entitySize;
	}

	public Object getSessionContext() {
		return sessionContext;
	}

	public void setSessionContext(Object sessionContext) {
		this.sessionContext = sessionContext;
	}

	public long getAndIncrGlobalSeq ()
	{
		return globalSeq.getAndIncrement();
	}

	public void setGlobalSeq (long globalSeq)
	{
		this.globalSeq.set(globalSeq);;
	}

	public int getBatchSize() {
		return this.batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public Settings() {
		this.params = new HashMap<String, Object>();
		this.actionOverrides = new HashMap<String, AggregateAction>();
		this.userkeyOverrides = new HashMap<String, String>();	
		this.expandedAssociations = new ArrayList<AssociationSetting>();
		this.prunedAssociations = new ArrayList<>();
		this.pruneRelative = new HashSet<>();
		this.references = new ArrayList<>();
		this.tags.add(AbstractProperty.EMPTY_TAG);
	}

	public StateGraph.Scope getScope() {
		return this.scope;
	}
	
	public int getCurrentApiVersion() {
		return CURRENT_API_VERSION;
	}
	
	public boolean isCurrentApi() {
		return apiVersion == getCurrentApiVersion();
	}

	public void addTag(String value) {
		tags.add(value);
	}

	public List<String> getTags() {
		return this.tags;
	}

	public String getDowncastName() {
		return this.downcastName;
	}

	public void setDowncastName(String typeName) {
		this.downcastName = typeName;
	}

	public AssociationStrategy getAssociationStrategy() {
		return associationStrategy;
	}

	public void setAssociationStrategy(
			AssociationStrategy associationStrategy) {
		this.associationStrategy = associationStrategy;
	}	

	public DetailStrategy getDetailStrategy() {
		return detailStrategy;
	}

	public void setDetailStrategy(DetailStrategy detailStrategy) {
		this.detailStrategy = detailStrategy;
	}

	public void expand (AssociationSetting associationSetting) {
		this.expandedAssociations.add(associationSetting);
	}

	public void prune(AssociationSetting associationSetting) {
		this.prunedAssociations.add(associationSetting);
	}

	public List<AssociationSetting> getExpandedAssociations () {
		return this.expandedAssociations;
	}

	public Shape getShape() {
		return this.shape;
	}
	
	public void init(Shape shape) {
		init(this.view, null, shape);
	}

	private boolean hasExpandedAssociations () {
		return (expandedAssociations != null && expandedAssociations.size() > 0);
	}

	public boolean hasPrunedAssociations() {
		return (prunedAssociations != null && prunedAssociations.size() > 0);
	}

	public boolean hasReferences() {
		return (references != null && references.size() > 0);
	}

	public void addReference(String typeName) {
		this.references.add(typeName);
	}

	public List<String> getReferences()
	{
		return this.references;
	}

	public void init(View aView, Map<String, String> queryParams, Shape shape) {

		this.shape = shape;
		this.view = aView;
		if(this.view == null) {
			if(entityType == null) {
				throw new RuntimeException("EntityType is required to resolve the default view");
			}
			//this.view = am.getDAS().getView( AbstractType.getViewName(entityType) );
			this.view = shape.getView((EntityType)entityType);

		} else if(view.getName() == null || "".equals(view.getName().trim())) {
			throw new IllegalStateException("A name for the AggregateView is required");
		}
		if(this.view.getShape() == null) {
			this.view.setShape(shape);
		}

		if(hasExpandedAssociations() || hasPrunedAssociations()) {
			// If the view is going to be modified make a copy of the built-in view
			// We don't need to make a copy of a user provided view
			if(AggregateView.isBuiltInView(view.getName())) {
				view = view.copy();
			}
		}
		if(!view.isTree(this)) {
			this.scope = StateGraph.Scope.TYPE_GRAPH;
		} else {
			this.scope = StateGraph.Scope.EDGE;
		}

		if(entityType != null) {
			TypeGraph sg = getTypeGraph();
			if (hasExpandedAssociations()) {
				sg.enhance(expandedAssociations);
			}
			if (hasPrunedAssociations()) {
				for (AssociationSetting as : prunedAssociations) {
					if (as.getMatchType() == MatchType.RELATIVE_PATH) {
						pruneRelative.add(as.getPathSuffix());
					}
				}
				// remove this from the StateGraph
				sg.prune(prunedAssociations);
			}
			if (hasReferences()) {
				// Mark the appropriate states as references
				sg.markReferences(references);
			}
		}

		this.params = populateFilters(queryParams);
		this.actionOverrides = getActionOverrides(queryParams);
		this.userkeyOverrides = getUserkeyOverrides(queryParams);

		initMutableAction(queryParams);
	}

	public TypeGraph getTypeGraph() {
		return view.getTypeGraph(((EntityType)entityType), this.scope);
	}

	public boolean shouldPrune(String path) {
		return hasPrunedAssociations() && pruneRelative.contains(path);
	}

	public Map<String, Object> populateFilters(Map<String, String> queryParams) {
		if(view == null || queryParams == null)
			return params;

		// TODO: skip recurse attributes and display a warning as this is not yet supported
		for(String key: view.getConsolidatedAttributes()) {
			String encodedKey = encodeParam(key);
			if(queryParams.containsKey(encodedKey))
				params.put(key, queryParams.get(encodedKey));
		}

		return params;
	}

	protected void initMutableAction(Map<String, String> queryParams) {
		if(queryParams == null)
			return;

		for(Entry<String, String> entry: queryParams.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if(key.toUpperCase().equals(ACTION_OVERRIDE.toUpperCase())) {
				action = AggregateAction.valueOf(value.toUpperCase());
				break;
			}
		}

		if(action == null)
			throw new IllegalArgumentException("Action has an invalid value should be one of the following values - CREATE, UPDATE, MERGE.");
	}

	public Map<String, AggregateAction> getActionOverrides(Map<String, String> queryParams) {

		if(queryParams == null)
			return new HashMap<String, AggregateAction>();

		Map<String, AggregateAction> result = new HashMap<String, AggregateAction>();
		for(Entry<String, String> entry: queryParams.entrySet()) {
			String key = entry.getKey();
			String mutableAction = entry.getValue();
			String toUpperKey = key.toUpperCase();
			if(!toUpperKey.endsWith(ACTION_OVERRIDE))
				continue;

			int endIndex = toUpperKey.indexOf(ACTION_OVERRIDE);
			String propertyPathSuffix = toUpperKey.substring(0, endIndex);
			AggregateAction actionOverride = AggregateAction.valueOf(mutableAction.toUpperCase());			
			result.put(decodeParam(propertyPathSuffix), actionOverride);
		}		


		return result;
	}

	public boolean doMerge(CallInfo ci) {
		return doAction(ci, AggregateAction.MERGE);
	}	

	public boolean doUpdate(CallInfo ci) {
		return doAction(ci, AggregateAction.UPDATE);
	}	

	public boolean doAction(CallInfo ci, AggregateAction aggregateAction) {
		if(!(actionOverrides == null || actionOverrides.isEmpty() || ci.getInputPropertyPath() == null) ){
			if(actionOverrides.get(ci.getInputPropertyPath().toUpperCase()) != null){
				if (actionOverrides.get(ci.getInputPropertyPath().toUpperCase()) == aggregateAction){
					return true;
				}else{
					return false;
				}				
			}
		}
		if(action == aggregateAction)
			return true;

		return false;
	}	

	public Map<String, String> getUserkeyOverrides(Map<String, String> queryParams) {
		if(queryParams == null)
			return new HashMap<String, String>();

		Map<String, String> toUpperParams = new HashMap<String, String>();
		for(Map.Entry<String, String> entry: queryParams.entrySet())
			toUpperParams.put(entry.getKey().toUpperCase(), entry.getValue());

		Map<String, String> result = new HashMap<String, String>();
		for(String key: toUpperParams.keySet()) {
			if(!key.endsWith(USERKEY_OVERRIDE))
				continue;

			int endIndex = key.indexOf(USERKEY_OVERRIDE);
			String propertyPathSuffix = key.substring(0, endIndex);
			result.put(decodeParam(propertyPathSuffix), toUpperParams.get(key));
		}

		return result;
	}		

	public static String decodeParam(String param) {
		return param.replace(URI_PATH_DELIMITER, PATH_DELIMITER);
	}

	public static String encodeParam(String param) {
		return param.replace(PATH_DELIMITER, URI_PATH_DELIMITER);
	}

	public View getView () {
		return view;
	}

	/**
	 * Set a simple view, i.e., one that does not need a StateGraph @see init
	 * @param view in effect
	 */
	public void setView(View view) {
		this.view = view;
	}

	public AggregateAction getAction() {
		return action;
	}

	public void setAction(AggregateAction mutableAction) {
		// this.mutableAction can never by null
		if(mutableAction != null) {
			this.action = mutableAction;
		}
	}

	public AggregateAction getMainAction ()
	{
		return mainAction == null ? action : mainAction;
	}

	public void setMainAction (AggregateAction mainAction)
	{
		this.mainAction = mainAction;
	}

	/**
	 * This can be augmented by the framework if the function is going to
	 * transform the value.
	 *
	 * @return The user specified parameter values
	 */
	public Map<String, Object> getParams () {
		return params;
	}

	public ObjectResolver.Type getResolverType () {
		return resolverType;
	}

	public void setParams (Map<String, Object> params) {
		this.params = params;
	}

	public Map<String, AggregateAction> getActionOverrides() {
		return actionOverrides;
	}

	public void setActionOverrides(Map<String, AggregateAction> overrides) {
		this.actionOverrides = overrides;
	}

	public Map<String, String> getUserkeyOverrides() {
		return userkeyOverrides;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<?> desiredSubclass) {
		this.entityClass = desiredSubclass;
	}

	public Interceptor getInterceptor() {
		return this.interceptor;
	}

	public void setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	public boolean doPreFlush() {
		return preFlush;
	}

	public void setPreFlush(boolean flush) {
		this.preFlush = flush;
	}

	public boolean doPostFlush() {
		return postFlush;
	}	

	public void setPostFlush(boolean flush) {
		this.postFlush = flush;
	}	
	
	public void setPreClear(boolean value) {
		this.preClear = value;
	}
	
	public boolean doPreClear() {
		return this.preClear;
	}
	
	public void setPreRefresh(boolean value) {
		this.preRefresh = value;
	}
	
	public boolean doPreRefresh() {
		return this.preRefresh;
	}	

	public boolean doPersist() {
		return persist;
	}

	public void setPersist(boolean persist) {
		this.persist = persist;
	}

	public void setParam (String name, Object value) {
		params.put(name, value);
	}

	public void initDowncast(TypeNarrower typeNarrower, Object entity, TypeMapper typeMapper)
	{
		if(getView() == null)
			return;

		if(entity != null) {
			if(getView().getRegexAttributes() == null) {
				if(getShape() == null) {
					throw new RuntimeException("Shape needs to be provided in settings!");
				}
				this.downcastName = typeNarrower.downcast(getShape(), entity, getView());
				if (downcastName == null) {
					throw new IllegalArgumentException(
						"The entityClass is not applicable for this view. Check if the entity object for the correct class was passed in.");
				}
			} else {
				throw new IllegalArgumentException(
					"Type narrowing is not supported on a view containing RegEx attributes. Use read() instead of query().");
			}
		} else {
			downcastName = getEntityType().getName();
		}
	}

	public List<Function> getAdditionalFunctions () {
		return this.additionalFunctions;
	}
	
	public Function addFunction (FunctionType type, String arg) {
		List<String> args = new LinkedList<>();
		args.add(arg);
		return addFunction(null, type, 1, args);
	}

	public Function addFunction (String name, String arg) {
		List<String> args = new LinkedList<>();
		args.add(arg);
		return addFunction(name, FunctionType.COMPARISON, 1, args);
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

		Function newFunction = new Function(name, type, FunctionScope.ANY, position, args, null);
		this.additionalFunctions.add(newFunction);

		return newFunction;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	/**
	 * This is an efficient paging mechanism if 
	 * the paging query is based of the order by column
	 * This value should represent the last term in the order by clause
	 * for this chunk
	 * 
	 * @return last term in current chunk
	 */

	public Map<String, Object> getNextToken() {
		return nextToken;
	}

	public void setNextToken(Map<String, Object> nextToken) {
		this.nextToken = nextToken;
	}
	
	public Set<String> getAllParameters() {
	    Set<String> params = new HashSet<>();
	    
	    params.addAll(this.params.keySet());
	    
	    if(this.nextToken != null) {
	        for(String tokenName: this.nextToken.keySet()) {
	            params.add(QueryFragment.NEXTTOKEN_PARAM_PREFIX + tokenName);
	        }
	    }
	    
	    return params;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Type getEntityType() {
		return entityType;
	}

	public void setEntityType(Type entityType) {
		this.entityType = entityType;
	}

	public boolean isDenormalized() {
		return denormalized;
	}

	public void setDenormalized(boolean denormalized) {
		this.denormalized = denormalized;
	}

	public boolean doBaseline() {
		return baseline;
	}

	public void setBaseline(boolean domain) {
		this.baseline = domain;
	}
	
	public boolean isAutoWire() {
		return autoWire;
	}

	public void setAutoWire(boolean autoWire) {
		this.autoWire = autoWire;
	}

	public static class SettingsIterator<T> implements Iterator<T>
	{
		private InputStream jsonStream;
		private SettingsBuilder builder;
		private JsonParser cursor;
		private T current;

		public SettingsIterator(InputStream jsonStream, SettingsBuilder builder) {
			this.jsonStream = jsonStream;
			this.builder = builder;

			try {
				initCursor();
			} catch (IOException e) {
				throw new RuntimeException("Unable to create JSON from inputstream", e);
			} catch (JSONException e) {
				throw new RuntimeException("Unable to create JSON from inputstream", e);
			}
		}

		private void initCursor() throws IOException, JSONException
		{
			cursor = getJsonParser();
			if (cursor == null) {
				current = null;
				return;
			}

			// First token should be an array
			boolean isArray = false;
			if(cursor.nextToken() == JsonToken.START_ARRAY) {
				isArray = true;
			}

			// Avoid the start object token
			if(!isArray || cursor.nextToken() == JsonToken.START_OBJECT) {
				current = extractCurrent();
			}
		}

		private void advance() {
			try {
				if(cursor.nextToken() == JsonToken.START_OBJECT) {
                    current = extractCurrent();
                } else {
                    // end of input
                    current = null;
                }
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to create get next json value from input", e);
			}
		}

		private T extractCurrent () {
			JSONObject json = (JSONObject)extractJson(false);

			if(json.has(Constants.XOR.REST_SETTINGS)) {
				return (T)json;
			} else {
				return (T)builder.json(json).build();
			}
		}

		public Settings extractSettings(JSONObject json) {
			if(json.has(Constants.XOR.REST_SETTINGS)) {
				json = json.getJSONObject(Constants.XOR.REST_SETTINGS);
			}
			return builder.json(json).build();
		}

		/**
		 * 	A value is referenced either using a jsonobject and its key or is part of an array
		 * @param isArray true if we are creating an array false if we are creating a JSONObject
		 *                instance
		 * @return
		 */
		private Object extractJson (boolean isArray)
		{

			Object result = isArray ? new JSONArray() : new JSONObject();
			JSONObject ownerObject = isArray ? null : (JSONObject)result;
			JSONArray ownerArray = isArray ? (JSONArray) result : null;
			try {
				String key = null; // populated only if isArray is false
				while ((isArray && cursor.nextToken() != JsonToken.END_ARRAY) || (!isArray
					&& cursor.nextToken() != JsonToken.END_OBJECT)) {
					switch (cursor.getCurrentToken()) {
					case START_OBJECT:
						JSONObject jsonObject = (JSONObject)extractJson(false);
						setValue(ownerObject, key, ownerArray, jsonObject);
						break;
					case START_ARRAY:
						JSONArray jsonArray = (JSONArray)extractJson(true);
						setValue(ownerObject, key, ownerArray, jsonArray);
						break;
					case FIELD_NAME:
						key = cursor.getCurrentName();
						break;
					case VALUE_STRING:
						if (isArray) {
							ownerArray.put(cursor.getText());
						}
						else {
							ownerObject.put(key, cursor.getText());
						}
						break;
					case VALUE_NUMBER_INT:
						if (isArray) {
							ownerArray.put(cursor.getIntValue());
						}
						else {
							ownerObject.put(key, cursor.getIntValue());
						}
						break;
					case VALUE_NUMBER_FLOAT:
						if (isArray) {
							ownerArray.put(cursor.getFloatValue());
						}
						else {
							ownerObject.put(key, cursor.getFloatValue());
						}
						break;
					case VALUE_TRUE:
					case VALUE_FALSE:
						if (isArray) {
							ownerArray.put(cursor.getBooleanValue());
						}
						else {
							ownerObject.put(key, cursor.getBooleanValue());
						}
						break;
					case VALUE_NULL:
						if (!isArray) {
							ownerObject.put(key, JSONObject.NULL);
						}
						break;
					}
				}
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to create get next json value from input", e);
			}

			return result;
		}

		private void setValue(JSONObject current, String key, JSONArray array, Object value) {
			if(current != null) {
				current.put(key, value);
			} else {
				array.put(value);
			}
		}

		private JsonParser getJsonParser ()
		{
			JsonFactory jsonfactory = new JsonFactory();
			JsonParser jsonParser = null;

			try {
				jsonParser = jsonfactory.createParser(this.jsonStream);
			} catch (IOException e) {
				throw new RuntimeException("Unable to create JSON from blob", e);
			}

			return jsonParser;
		}

		@Override public boolean hasNext ()
		{
			return current != null;
		}

		@Override public T next ()
		{
			T result = current;
			// advance to the next item
			this.advance();

			return result;
		}

		@Override public void remove ()
		{
			throw new UnsupportedOperationException();
		}
	}

	public static class SettingsBuilder {
		private Shape shape;
		Settings settings;

		public SettingsBuilder(Shape shape) {
			this.shape = shape;
			this.settings = new Settings();
		}

		public SettingsIterator iterator(InputStream jsonStream) {
			return new SettingsIterator(jsonStream, this);
		}

		private void createView(Class clazz, ViewType builtInType) {
			createView(clazz, builtInType, null);
		}

		private void createView(Class clazz, ViewType builtInType, Property property) {
			Type type = shape.getType(clazz);
			EntityType entityType;
			if(type instanceof EntityType) {
				entityType = (EntityType) type;
			} else {
				throw new RuntimeException("Class is not an entity: " + clazz.getName());
			}

			settings.setEntityType(type);

			switch(builtInType) {
			case BASE:
				settings.setView(shape.getBaseView(entityType));
				break;
			case MIGRATE:
				settings.setView(shape.getMigrateView(entityType));
				break;
			case RELATIONSHIP:
				assert(property != null);
				settings.setView(shape.getRelationshipView(entityType, property));
				break;
			case AGGREGATE:
				settings.setView(shape.getView(entityType));
				break;
			case REF:
				settings.setView(shape.getRefView(entityType));
				break;
			default:
				throw new RuntimeException("Unknown built-in view type: " + builtInType);
			}
		}
		
		public SettingsBuilder base(Class<?> clazz) {
			createView(clazz, ViewType.BASE);

			return this;
		}

		public SettingsBuilder migrate(Class<?> clazz) {
			createView(clazz, ViewType.MIGRATE);

			return this;
		}

		public SettingsBuilder migrateRelationship(Class<?> clazz, Property property) {
			createView(clazz, ViewType.RELATIONSHIP, property);

			return this;
		}

		public SettingsBuilder aggregate(Class<?> clazz) {
			createView(clazz, ViewType.AGGREGATE);

			return this;
		}

		public SettingsBuilder expand(AssociationSetting setting) {
			this.settings.expand(setting);
			return this;
		}

		public SettingsBuilder prune(AssociationSetting setting) {
			this.settings.prune(setting);
			return this;
		}

		public SettingsBuilder reference(String typeName) {
			this.settings.addReference(typeName);
			return this;
		}

		public SettingsBuilder globalSeq(int globalSeq) {
			this.settings.setGlobalSeq(globalSeq);
			return this;
		}

		public SettingsBuilder json(String jsonString) {
			JSONObject json = new JSONObject(jsonString);
			this.json(json);
			return this;
		}

		// Extract many of the input fields from the json object
		private SettingsBuilder json(JSONObject json) {

			// Create a fresh settings object
			this.settings = new Settings();

			List<BindParameter> parameters = null;
			try {
				for (String key : JSONObject.getNames(json)) {
					String keyName = key.toUpperCase();
					switch (keyName) {
					case "BASE":
						String className = json.getString(key);
						Class clazz = Class.forName(className);
						base(clazz);
						break;
					case "AGGREGATE":
						className = json.getString(key);
						clazz = Class.forName(className);
						aggregate(clazz);
						break;
					case "MIGRATE":
						className = json.getString(key);
						clazz = Class.forName(className);
						migrate(clazz);
						break;
					case "GRAPHFILENAME":
						this.settings.setGraphFileName(json.getString(key));
						break;
					case "EXPANDBYCLASS":
						JSONArray extensions = json.getJSONArray(key);
						for(int i = 0; i < extensions.length(); i++) {
							clazz = Class.forName(extensions.getString(i));
							AssociationSetting extension = new AssociationSetting(clazz);
							expand(extension);
						}
						break;
					case "EXPANDBYCLASSEXACT":
						extensions = json.getJSONArray(key);
						for(int i = 0; i < extensions.length(); i++) {
							AssociationSetting extension = AssociationSetting.getExact(extensions.getString(i));
							expand(extension);
						}
						break;
					case "EXPANDRELATIVE":
						extensions = json.getJSONArray(key);
						for(int i = 0; i < extensions.length(); i++) {
							String path = extensions.getString(i);
							AssociationSetting prune = new AssociationSetting(path, MatchType.RELATIVE_PATH);
							expand(prune);
						}
						break;
					case "EXPANDABSOLUTE":
						extensions = json.getJSONArray(key);
						for(int i = 0; i < extensions.length(); i++) {
							String path = extensions.getString(i);
							AssociationSetting prune = new AssociationSetting(path, MatchType.ABSOLUTE_PATH);
							expand(prune);
						}
						break;							
					case "PRUNEBYCLASS":
						JSONArray prunes = json.getJSONArray(key);
						for(int i = 0; i < prunes.length(); i++) {
							clazz = Class.forName(prunes.getString(i));
							AssociationSetting extension = new AssociationSetting(clazz);
							prune(extension);
						}
						break;						
					case "PRUNERELATIVE":
						prunes = json.getJSONArray(key);
						for(int i = 0; i < prunes.length(); i++) {
							String path = prunes.getString(i);
							AssociationSetting prune = new AssociationSetting(path, MatchType.RELATIVE_PATH);
							prune(prune);
						}
						break;
					case "PRUNEABSOLUTE":
						prunes = json.getJSONArray(key);
						for(int i = 0; i < prunes.length(); i++) {
							String path = prunes.getString(i);
							AssociationSetting prune = new AssociationSetting(path, MatchType.ABSOLUTE_PATH);
							prune(prune);
						}
						break;
					case "REFERENCE":
						JSONArray references = json.getJSONArray(key);
						for(int i = 0; i < references.length(); i++) {
							reference(references.getString(i));
						}
						break;
					case "FILTERS":
						Object obj = json.get(key);
						if(obj instanceof JSONObject) {
							JSONObject filters = (JSONObject) obj;
							for (String filterName : JSONObject.getNames(filters)) {
								Object filterValue = filters.get(filterName);
								this.settings.setParam(filterName, filterValue);
							}
						} else if(obj instanceof JSONArray) {
							JSONArray array = (JSONArray) obj;
							for(int i = 0; i < array.length(); i++) {
								this.settings.setParam(new Integer(i + 1).toString(), array.get(i));
							}
						}
						break;
					case "BINDPARAMETERS":
						JSONArray array = json.getJSONArray(key);;
						parameters = new ArrayList<>(array.length());
						for(int i = 0; i < array.length(); i++) {
							BindParameter bp = new BindParameter();
							bp.setType(array.getString(i));
							parameters.add(bp);
						}
						break;
					case "GLOBALSEQ":
						globalSeq(json.getInt(key));
						break;
					case "ENTITYSIZE":
						String entitySize = json.getString(key);
						this.settings.setEntitySize(EntitySize.valueOf(entitySize));
						break;
					case "ENTITYCLASS":
						String entityClassName = json.getString(key);
						clazz = Class.forName(entityClassName);
						this.settings.setEntityClass(clazz);
						break;
					case "ACTION":
						String action = json.getString(key);
						this.settings.setAction(AggregateAction.valueOf(action));
						break;
					case "DATEFORMAT":
						String dateformat = json.getString(key);
						this.settings.setDateFormat(dateformat);
						break;
					case "NORMALIZED":
						boolean isNormalized = json.getBoolean(key);
						this.settings.setDenormalized(!isNormalized);
						break;
					case "VIEW":
						ObjectMapper mapper = new ObjectMapper();
						JSONObject jsonView = json.getJSONObject(key);
						View view = mapper.readValue(jsonView.toString(), AggregateView.class);

						// If this is a native query we generate the view name as MD5 digest
						// of the SQL query string
						NativeQuery nq = view.getNativeQuery();
						if(nq != null) {
							view.setName(DigestUtils.md5Hex(nq.getSelectClause()));
						}

						if(view.getName() != null) {
							// check for existing view with same name
							View existing = shape != null ? shape.getView(view.getName()) : null;
							if(existing != null) {
								view = existing;
							}
						}

						this.settings.setView(view);
					}
				}
			} catch (Exception e) {
				throw ClassUtil.wrapRun(e);
			}

			// If action is not provided, then try to infer the action for
			if(settings.getAction() == null) {
				if(settings.getView() != null && settings.getView().getNativeQuery() != null) {
					NativeQuery nq = settings.getView().getNativeQuery();
					String qs = nq.getSelectClause().trim().toUpperCase();
					if(qs.startsWith("SELECT")) {
						settings.setAction(AggregateAction.READ);
					} else if(qs.startsWith("INSERT")) {
						settings.setAction(AggregateAction.CREATE);
					} else if(qs.startsWith("UPDATE")) {
						settings.setAction(AggregateAction.UPDATE);
					} else if(qs.startsWith("DELETE")) {
						settings.setAction(AggregateAction.DELETE);
					}
				}
			}

			if(settings.getView() != null && settings.getView().getNativeQuery() != null && parameters != null) {
				settings.getView().getNativeQuery().setParameterList(parameters);
			}

			return this;
		}
		
		public Settings build() {
			settings.init(shape);
			return this.settings;
		}
	}
	
	public static String convertToBOPath(String input) {
		return input.replace(PATH_DELIMITER, AbstractBO.SDO_PATH_DELIMITER);
	}
	

	public boolean isSupportsPostLogic() {
		return supportsPostLogic;
	}

	public void setSupportsPostLogic(boolean supportsPostLogic) {
		this.supportsPostLogic = supportsPostLogic;
	}

	public Object getExternalData() {
		return externalData;
	}

	public void setExternalData(Object externalData) {
		this.externalData = externalData;
	}

	public PrefetchCache getPrefetchCache() {
		return prefetchCache;
	}

	public void setPrefetchCache(PrefetchCache prefetchCache) {
		this.prefetchCache = prefetchCache;
	}

	public boolean isGenerateVisual ()
	{
		return this.graphFileName != null && !"".equals(this.graphFileName.trim());
	}

	public Detector getDetector ()
	{
		return detector;
	}

	public void setDetector (Detector detector)
	{
		this.detector = detector;
	}

	/**
	 * Set the file name used for graph visuals of the state and object graphs.
	 * Currently supports 3 formats.
	 * 1. PNG format
	 * 2. DOT format
	 * 3. GML format
	 *
	 * @param graphFileName with the appropriate extension (.png, .dot, .gml)
	 */
	public void setGraphFileName (String graphFileName)
	{
		this.graphFileName = graphFileName;
	}

	public String getGraphFileName() {
		return this.graphFileName;
	}

	public Map<String, Float> getCollectionSparseness ()
	{
		return collectionSparseness;
	}

	public void setCollectionSparseness (Map<String, Float> collectionSparseness)
	{
		this.collectionSparseness = collectionSparseness;
	}

	/**
	 * Generates a PNG file or an export in some populate graph formats (.dot and .gml)
	 * @param dg DirectedGraph instance that needs to be exported
	 */
	public void exportGraph (DirectedGraph dg) {
		String extension = null;

		if (getGraphFileName() != null) {
			if (getGraphFileName().lastIndexOf(PATH_DELIMITER) == -1) {
				throw new RuntimeException(
					"The filename should have the extension specified.");
			}

			extension = getGraphFileName().substring(
				getGraphFileName().lastIndexOf(PATH_DELIMITER) + 1);
			extension = extension.toUpperCase();
		}

		if(extension.equals(GraphFormat.PNG.name())) {
			generateVisual(dg.getGraph());
		} else if(extension.equals(GraphFormat.GML.name())) {
			dg.exportToGML(getGraphFileName());
		} else if(extension.equals(GraphFormat.DOT.name())) {
			dg.exportToDOT(getGraphFileName());
		} else {
			throw new RuntimeException(
				"One of the supported extensions need to be supported in the filename (.png, .dot or .gml)");
		}
	}

	/**
	 * Generates a PNG visual of the graph
	 * @param graph data structure
	 */
	public void generateVisual (Graph graph) {
		final Dimension SMALL = new Dimension(1280, 1024);
		final Dimension MEDIUM = new Dimension(3840, 2160);
		final Dimension LARGE = new Dimension(5120, 2880);
		final Dimension XLARGE = new Dimension(7680, 4320);

		FRLayout layout = new FRLayout(graph);
		//Layout layout = new ISOMLayout<>(graph);

		int multiplicationFactor = 1;
		if (layout instanceof FRLayout && ApplicationConfiguration.config().containsKey(Constants.Config.REPULSION_MULTIPLIER)) {
			double multiplier = ApplicationConfiguration.config().getDouble(Constants.Config.REPULSION_MULTIPLIER);
			((FRLayout)layout).setRepulsionMultiplier(multiplier);

			// Square the increase
			multiplicationFactor = (int)(multiplier/0.75D);
			multiplicationFactor *= multiplicationFactor;
			if(multiplicationFactor < 1) {
				multiplicationFactor = 1;
			}
		}

		Dimension graphSize;
		int size = graph.getVertices().size();
		size *= multiplicationFactor;

		if(size < EntitySize.SMALL.size()*2) {
			graphSize = SMALL;
		} else if(size < EntitySize.MEDIUM.size()*1.2) {
			graphSize = MEDIUM;
		} else if(size < EntitySize.LARGE.size()*1.1) {
			graphSize = LARGE;
		} else {
			graphSize = XLARGE;
		}

		VisualizationViewer<Integer,String> vv =
			new VisualizationViewer<Integer,String>(layout, graphSize);

		// Create the VisualizationImageServer
		// vv is the VisualizationViewer containing my graph
		VisualizationImageServer<Integer, String> vis =
			new VisualizationImageServer<Integer, String>(vv.getGraphLayout(),
				vv.getGraphLayout().getSize());

		// Configure the VisualizationImageServer the same way
		// you did your VisualizationViewer. In my case e.g.

		vis.setBackground(Color.WHITE);
		vis.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<String>());
		vis.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<Integer, String>());
		vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Integer>());
		vis.getRenderer().getVertexLabelRenderer()
			.setPosition(Renderer.VertexLabel.Position.CNTR);

		// Create the buffered image
		BufferedImage image = (BufferedImage) vis.getImage(
			new Point2D.Double(vv.getGraphLayout().getSize().getWidth() / 2,
				vv.getGraphLayout().getSize().getHeight() / 2),
			new Dimension(vv.getGraphLayout().getSize()));

		// Write image to a png file
		File outputfile = new File(getGraphFileName());

		try {
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			// Exception handling
		}
	}

	public static String getBaseName(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(propertyPath.lastIndexOf(Settings.PATH_DELIMITER)+1);
		else
			return propertyPath;
	}

	public static String getAnchorName(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(0, propertyPath.lastIndexOf(Settings.PATH_DELIMITER));
		else
			return propertyPath;
	}

	public static String getRootName(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(0, propertyPath.indexOf(Settings.PATH_DELIMITER));
		else
			return propertyPath;
	}

	public static String getNext(String propertyPath) {
		if(propertyPath.indexOf(Settings.PATH_DELIMITER) != -1)
			return propertyPath.substring(propertyPath.indexOf(Settings.PATH_DELIMITER)+1);
		else
			return null;
	}

	public static boolean doSQL(DataStore po) {
		return po instanceof JDBCDataStore;
	}
}
