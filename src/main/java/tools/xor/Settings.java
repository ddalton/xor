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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.core.EmptyInterceptor;
import tools.xor.core.Interceptor;
import tools.xor.custom.AssociationStrategy;
import tools.xor.custom.DetailStrategy;
import tools.xor.service.AggregateManager;
import tools.xor.view.AggregateView;
import tools.xor.view.Filter;

import javax.imageio.ImageIO;

/**
 * TODO: Convert the fields to final and only use the builder to create the Settings object
 * @author family
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

	protected DateForm dateForm = DateForm.FORMATTED;
	public DateForm getDateForm ()
	{
		return dateForm;
	}

	public void setDateForm (DateForm dateForm)
	{
		this.dateForm = dateForm;
	}

	protected String dateFormat = MutableJsonProperty.ISO8601_FORMAT; // this is the default format and can be overridden
	public String getDateFormat ()
	{
		return dateFormat;
	}

	public void setDateFormat (String dateFormat)
	{
		this.dateFormat = dateFormat;
	}
	
	protected boolean supportsPostLogic;
	
	// The entity type on which the data is based
	// TODO: can evolve to an IDL operation
	protected Type entityType;

	// If the scope is ContentScope.VIEW then the actual view is referenced in this property
	protected AggregateView view;

	private AggregateAction action = AggregateAction.UPDATE; // specifies the type of action being performed that involves data change in the database

	private Map<String, Object> filters = new HashMap<String, Object>(); // Used for filtering the result

	private Map<String, AggregateAction> actionOverrides; // A collection can have a specific action override set

	private Map<String, String> userkeyOverrides; // An association can be resolved using a userkey	

	private Class<?> entityClass;

	private List<AssociationSetting> associationSettings;

	private Interceptor interceptor = EmptyInterceptor.INSTANCE; // Allow user code to inspect or tweak the processing

	private boolean preFlush;      // Setting this flag to true, flushes the session before it executes the operation
	private boolean postFlush;     // Setting this flag to true, flushes the session after it executes the operation	
	private boolean preClear; // Setting this flag to true clears the session cache before it executes the operation
	private boolean preRefresh;   // Setting this flag to true refreshes the object before it executes the operation	
	private boolean persist;     // Setting this flag will do an automatic saveupdate on the object graph after the UPDATE phase

	private boolean narrow; // Request the query operation to narrow the object to the appropriate type. This can have a performance impact

	private boolean crossAggregate; // Flag to allow an operation to cross aggregate boundaries. Relevant when a view is used.

	private AssociationStrategy associationStrategy;
	
	private DetailStrategy detailStrategy;
	
	private boolean denormalized; // specifies if the query should return a denormalized result
	                              // Also can be used for update, specifying if the input data is denormalized
	
	private List<Filter> additionalFilters = new ArrayList<Filter>();

	private Class<?> narrowedClass;
	
	private List<String> tags = new ArrayList<String>();
	private Map<String, Object> context = new HashMap<String, Object>(); // used by clients to pass data to the logic methods
	
	// Paging related attributes
	private Integer limit;
	private Integer offset;
	private Map<String, Object> nextToken;

	private int apiVersion = getCurrentApiVersion();
	
	private boolean baseline; // Retrieve a domain based object, when using the query API
	
	private boolean autoWire;
	
	private Object sessionContext;
	
	// User provided data that is made available to callbacks
	private Object externalData;
	
	// User provided data that is efficiently obtained
	private PrefetchCache prefetchCache;

	// Settings related to data generation
	private EntitySize entitySize = EntitySize.MEDIUM;
	private float sparseness = 1.0f;
	private String graphFileName;
	private Map<String, Float> collectionSparseness = new HashMap<String, Float>(); // Currently the logic is based on exact match

	// Transient
	private Map<Class<?>, Boolean> shouldCreateIfMissing;

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

	public boolean isShouldCreate(Class<?> clazz) {
		if(shouldCreateIfMissing == null && associationSettings.size() > 0) {
			// populate
			shouldCreateIfMissing = new HashMap<>();
			for(AssociationSetting setting: associationSettings) {
				if(setting.getEntityClass() != null) {
					shouldCreateIfMissing.put(setting.getEntityClass(), setting.getCreateIfMissing());
				}
			}
		}

		if(shouldCreateIfMissing != null && shouldCreateIfMissing.containsKey(clazz)) {
			return shouldCreateIfMissing.get(clazz);
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

	public Settings() {
		this.action = AggregateAction.UPDATE;
		this.filters = new HashMap<String, Object>();
		this.actionOverrides = new HashMap<String, AggregateAction>();
		this.userkeyOverrides = new HashMap<String, String>();	
		this.associationSettings = new ArrayList<AssociationSetting>();
		this.tags.add(AbstractProperty.EMPTY_TAG);
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
	
	public void addContext(String key, Object value){
		context.put(key, value);
	}
	
	public Object getContext(String key) {
		return context.get(key);
	}

	public Class<?> getNarrowedClass() {
		return narrowedClass;
	}

	public void setNarrowedClass(Class<?> narrowedClass) {
		this.narrowedClass = narrowedClass;
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

	public void addAssociation(AssociationSetting associationSetting) {
		this.associationSettings.add(associationSetting);
	}

	public List<AssociationSetting> getAssociationSettings() {
		return this.associationSettings;
	}
	
	public void init(AggregateManager am) {
		init(this.view, null, am);
	}

	public void init(AggregateView aView, Map<String, String> queryParams, AggregateManager am) {

		this.view = aView;
		if(this.view == null) {
			if(entityType == null) {
				throw new RuntimeException("EntityType is required to resolve the default view");
			}
			//this.view = am.getDAS().getView( AbstractType.getViewName(entityType) );
			this.view = am.getDAS().getView((EntityType) entityType);
		} else if(view.getName() == null || "".equals(view.getName().trim())) {
			throw new IllegalStateException("A name for the AggregateView is required");
		}

		if(associationSettings != null && associationSettings.size() > 0) {
			// Make a copy of the view and enhance it with the associations needed to be traversed
			view = view.copy();
			view.getStateGraph((EntityType) entityType).enhance(associationSettings, am);
		}

		this.filters = populateFilters(queryParams);
		this.actionOverrides = getActionOverrides(queryParams);
		this.userkeyOverrides = getUserkeyOverrides(queryParams);

		initMutableAction(queryParams);
	}

	public Map<String, Object> populateFilters(Map<String, String> queryParams) {
		if(view == null || queryParams == null)
			return filters;

		// TODO: skip recurse attributes and display a warning as this is not yet supported
		for(String key: view.getAttributeList()) {
			String encodedKey = encodeParam(key);
			if(queryParams.containsKey(encodedKey))
				filters.put(key, queryParams.get(encodedKey));					
		}

		return filters;
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

	public AggregateView getView() {
		return view;
	}

	public void setView(AggregateView view) {
		// TODO: call init instead
		this.view = view;
	}

	public AggregateAction getAction() {
		return action;
	}

	public void setAction(AggregateAction mutableAction) {
		// this.mutableAction can never by null
		if(mutableAction != null)
			this.action = mutableAction;
	}

	public Map<String, Object> getFilters() {
		return filters;
	}

	public void setFilters(Map<String, Object> filters) {
		this.filters = filters;
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

	public boolean doNarrow() {
		return narrow;
	}

	public void setNarrow(boolean narrow) {
		this.narrow = narrow;
	}

	public boolean permitCrossAggregate() {
		return crossAggregate;
	}

	public void setCrossAggregate(boolean crossAggregate) {
		this.crossAggregate = crossAggregate;
	}

	public void addFilter(String name, Object value) {
		filters.put(name, value);
	}

	public void initNarrowClass(TypeNarrower typeNarrower, Object entity, TypeMapper typeMapper)
	{
		if(getView() == null)
			return;

		if(entity != null) {
			narrowedClass = typeNarrower.narrow(entity, getView().getName());
			if(narrowedClass == null) {
				throw new IllegalArgumentException("The entityClass is not applicable for this view. Check if the entity object for the correct class was passed in.");
			}
		} else {
			narrowedClass = getEntityType().getInstanceClass();
		}
		
		narrowedClass = typeMapper.toDomain(narrowedClass);
	}

	public List<Filter> getAdditionalFilters() {
		return this.additionalFilters;
	}
	
	public void addFunctionFilter(String filterExpression) {
		addFunctionFilter(filterExpression, 0);
	}
	
	public void addFunctionFilter(String filterExpression, int position) {
		Filter newFilter = new Filter(filterExpression, position);
		this.additionalFilters.add(newFilter);
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

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public int getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(int apiVersion) {
		this.apiVersion = apiVersion;
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
	
	private Settings(SettingsBuilder builder) {
		this.entityClass = builder.entityClass;
	}
	
	public boolean isAutoWire() {
		return autoWire;
	}

	public void setAutoWire(boolean autoWire) {
		this.autoWire = autoWire;
	}

	public static class SettingsBuilder {
		private Class<?> entityClass;
		
		public SettingsBuilder entityClass(Class<?> clazz) {
			this.entityClass = clazz;
			return this;
		}
		
		public Settings build() {
			return new Settings(this);
		}
	}
	
	public static String convertToBOPath(String input) {
		return input.replace(PATH_DELIMITER, AbstractBO.PATH_DELIMITER);
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

	/**
	 * Set the file name used for graph visuals of the state and object graphs.
	 * Generation in the PNG format is the only one supported.
	 *
	 * @param graphFileName with the png extension.
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

	public void generateVisual (Graph graph) {
		final Dimension SMALL = new Dimension(1280, 1024);
		final Dimension MEDIUM = new Dimension(3840, 2160);
		final Dimension LARGE = new Dimension(5120, 2880);
		final Dimension XLARGE = new Dimension(7680, 4320);

		Dimension graphSize;
		if(graph.getVertices().size() < EntitySize.SMALL.size()*2) {
			graphSize = SMALL;
		} else if(graph.getVertices().size() < EntitySize.MEDIUM.size()*1.2) {
			graphSize = MEDIUM;
		} else if(graph.getVertices().size() < EntitySize.LARGE.size()*1.1) {
			graphSize = LARGE;
		} else {
			graphSize = XLARGE;
		}

		VisualizationViewer<Integer,String> vv =
			new VisualizationViewer<Integer,String>(new FRLayout(graph), graphSize);

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


}
