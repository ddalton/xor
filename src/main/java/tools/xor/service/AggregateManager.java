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

package tools.xor.service;

import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import tools.xor.AbstractBO;
import tools.xor.AbstractType;
import tools.xor.AggregateAction;
import tools.xor.AssociationSetting;
import tools.xor.BusinessObject;
import tools.xor.DefaultTypeMapper;
import tools.xor.DefaultTypeNarrower;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.MapperDirection;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SimpleType;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.core.Interceptor;
import tools.xor.custom.AssociationStrategy;
import tools.xor.custom.DefaultAssociationStrategy;
import tools.xor.custom.DefaultDetailStrategy;
import tools.xor.custom.DetailStrategy;
import tools.xor.service.exim.CSVExportImport;
import tools.xor.service.exim.ExcelExportImport;
import tools.xor.service.exim.ExportImport;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;
import tools.xor.util.PersistenceType;
import tools.xor.util.excel.ExcelExporter;
import tools.xor.view.AggregateView;
import tools.xor.view.AggregateViewFactory;
import tools.xor.view.Filter;
import tools.xor.view.TypeVersion;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AggregateManager implements Xor
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);
	private static final Logger sgLogger = LogManager.getLogger(Constants.Log.STATE_GRAPH);

	private static int BULK_BATCH_SIZE = 1000; // Flush the session for every batch to reduce memory pressure during bulk CSV import

	private DASFactory dasFactory;
	private List<String> viewFiles;           // A list of files to read the view information from
	private List<TypeVersion> typeVersions;        // A list of the types and the versions it is valid it
	private int viewVersion = TypeVersion.MIN_VERSION_VALUE;
	private MetaModel metaModel;           // Meta model exposed to the user
	private boolean autoFlushNative;     // Flag to indicate if a flush should be issued before a native query is executed
	private Interceptor interceptor;         // A user provided interceptor to be notified of framework events
	private AssociationStrategy associationStrategy; // Determine if any extra object need to be processed
	private DetailStrategy detailStrategy;      // Allows the user to define a custom detail strategy
	private PersistenceType persistenceType;
	private TypeNarrower typeNarrower;
	private String viewsDirectory;

	// This is maintained per thread, because the persistence orchestrator holds
	// the session that is thread specific
	private ThreadLocal<PersistenceOrchestrator> persistenceOrchestrator = new ThreadLocal<PersistenceOrchestrator>();

	// Custom type mapper to map between differnt types. 
	// NOTE: The user is restricted from changing the path specified in the MetaModel.
	// A truly custom solution is to provide a map between the meta model path and the custom path.
	// Configured with the DefaultTypeMapper
	private TypeMapper typeMapper;

	private void reloadViews ()
	{
		// Load the default view file
		(new AggregateViewFactory()).load(this);

		// Load the configured view files
		syncViews();
	}

	@PostConstruct
	protected void init ()
	{

		if (associationStrategy == null)
			associationStrategy = new DefaultAssociationStrategy();

		if (detailStrategy == null)
			detailStrategy = new DefaultDetailStrategy();

		if (typeMapper == null)
			typeMapper = new DefaultTypeMapper();

		if (typeNarrower == null) {
			typeNarrower = new DefaultTypeNarrower();
			typeNarrower.setAggregateManager(this);
		}

		if (dasFactory != null) {
			metaModel = new MetaModel(this);
			dasFactory.setAggregateManager(this);
		}
		else {
			logger.error(
				"DASFactory instance is not set on the AggregateManager instance with the default name.");
		}

		reloadViews();
	}

	public DASFactory getDasFactory ()
	{
		return dasFactory;
	}

	public void setDasFactory (DASFactory dasFactory)
	{
		this.dasFactory = dasFactory;
	}

	public MetaModel getMetaModel ()
	{
		return metaModel;
	}

	public void setMetaModel (MetaModel metaModel)
	{
		this.metaModel = metaModel;
	}

	private void syncViews ()
	{
		if (viewFiles == null)
			return;

		// Load the views
		AggregateViewFactory viewFactory = new AggregateViewFactory();

		for (String viewFile : viewFiles)
			viewFactory.load(viewFile, this);
	}

	public TypeNarrower getTypeNarrower ()
	{
		return typeNarrower;
	}

	public void setTypeNarrower (TypeNarrower typeNarrower)
	{
		this.typeNarrower = typeNarrower;
		typeNarrower.setAggregateManager(this);
	}

	public String getViewsDirectory ()
	{
		return viewsDirectory;
	}

	public void setViewsDirectory (String viewsDirectory)
	{
		this.viewsDirectory = viewsDirectory;
	}

	public AssociationStrategy getAssociationStrategy ()
	{
		return associationStrategy;
	}

	public void setAssociationStrategy (AssociationStrategy associationStrategy)
	{
		this.associationStrategy = associationStrategy;
	}

	public void setDetailStrategy (DetailStrategy detailStrategy)
	{
		this.detailStrategy = detailStrategy;
	}

	public PersistenceType getPersistenceType ()
	{
		return persistenceType;
	}

	public void setPersistenceType (PersistenceType persistenceType)
	{
		this.persistenceType = persistenceType;
	}

	public List<String> getViewFiles ()
	{
		return viewFiles;
	}

	public void setViewFiles (List<String> viewFiles)
	{
		this.viewFiles = viewFiles;
	}

	public List<TypeVersion> getTypeVersions ()
	{
		return typeVersions;
	}

	public void setTypeVersions (List<TypeVersion> typeVersions)
	{
		this.typeVersions = typeVersions;
	}

	public boolean isAutoFlushNative ()
	{
		return autoFlushNative;
	}

	public void setAutoFlushNative (boolean autoFlushNative)
	{
		this.autoFlushNative = autoFlushNative;
	}

	public Interceptor getInterceptor ()
	{
		return interceptor;
	}

	public void setInterceptor (Interceptor interceptor)
	{
		this.interceptor = interceptor;
	}

	public TypeMapper getTypeMapper ()
	{
		return typeMapper;
	}

	public void setTypeMapper (TypeMapper customTypeMapper)
	{
		this.typeMapper = customTypeMapper;
	}

	public PersistenceOrchestrator getPersistenceOrchestrator ()
	{
		return persistenceOrchestrator.get();
	}

	/**
	 * This method sets the current persistence orchestrator, irrespective
	 * of whether the thread had one previously. This way we do
	 * not have to bother about clearing an obsolete persistence orchestrator
	 *
	 * @param po value to set
	 */
	public void setPersistenceOrchestrator (
		PersistenceOrchestrator po)
	{
		this.persistenceOrchestrator.set(po);
	}

	public DataAccessService getDAS ()
	{
		return dasFactory.create();
	}

	public Settings getSettings ()
	{
		Settings result = new Settings();
		result.setAssociationStrategy(associationStrategy);

		return result;
	}

	private class FlushHandler
	{
		Object oldFlushMode;
		BusinessObject businessObject;
		Settings settings;

		FlushHandler (Settings settings)
		{
			this.settings = settings;
			oldFlushMode = getPersistenceOrchestrator().disableAutoFlush();
		}

		void register (BusinessObject bo)
		{
			this.businessObject = bo;
		}

		Object instance ()
		{
			return businessObject.getInstance();
		}

		Object done ()
		{
			try {
				return (businessObject != null) ? businessObject.getInstance() : null;
			} finally {
				if(oldFlushMode != null) {
					getPersistenceOrchestrator().setFlushMode(oldFlushMode);
				}
				if (settings.doPostFlush()) {
					getPersistenceOrchestrator().flush();
				}
			}
		}
	}

	private void checkAndSet (Settings settings, Object inputObject)
	{
		Class<?> inputObjectClass = getEntityClass(inputObject, settings);

		if (getPersistenceOrchestrator() == null)
			setPersistenceOrchestrator(dasFactory.getPersistenceOrchestrator(settings.getSessionContext()));

		if (settings.getAssociationStrategy() == null)
			settings.setAssociationStrategy(associationStrategy);

		if (settings.getEntityType() == null) {

			// Try to infer the type from the input object
			DataAccessService das = getDAS();

			Class<?> domainClass = settings.getEntityClass();
			if (domainClass == null && inputObjectClass != null) {
				try {
					domainClass = das.getTypeMapper().toDomain(inputObjectClass);
				}
				catch (UnsupportedOperationException e) {
					domainClass = null;
				}
			}
			if (domainClass == null) {
				throw new RuntimeException(
					"Unable to identify the type on which to perform the operation. Need to explicitly specify the domain type.");
			}
			settings.setEntityType(das.getType(domainClass.getName()));
			settings.init(das.getShape()); // populate the view from the type if necessary
			owLogger.debug("Operation on Entity Type: " + settings.getEntityType().getName());
			if (owLogger.isTraceEnabled()) {
				owLogger.trace(getStackTrace(10));
			}

			if (sgLogger.isDebugEnabled()) {
				if (settings.getView().getStateGraph() != null) {
					if (sgLogger.isTraceEnabled()) {
						sgLogger.trace(getStackTrace(10));
					}
					sgLogger.debug(
						"State graph of Entity: " + settings.getEntityType().getName()
							+ " for view: " + settings.getView().getName());
					sgLogger.debug(settings.getView().getStateGraph((EntityType)settings.getEntityType()).dumpState());
				}
			}
		}

		if (owLogger.isDebugEnabled()) {
			if (settings.getAssociationSettings() != null
				&& settings.getAssociationSettings().size() > 0) {
				owLogger.debug("List of Association settings for this operation:");
				for (AssociationSetting assoc : settings.getAssociationSettings()) {
					owLogger.debug(Constants.Format.INDENT_STRING + assoc.toString());
				}
			}
		}

		if (settings.doPreClear())
			getPersistenceOrchestrator().clear();
	}

	private String getStackTrace (int numStackElements)
	{
		Exception e = new Exception();
		StringBuilder sb = new StringBuilder();

		int skip = 2; // skip first 2
		for (StackTraceElement element : e.getStackTrace()) {
			if (skip-- > 0) {
				continue;
			}
			else if (skip + numStackElements == 0) {
				break;
			}
			sb.append(element.toString());
			sb.append("\r\n");
		}
		return sb.toString();
	}

	@Override
	public Object clone (Object entity, Settings settings)
	{
		owLogger.debug("Performing clone operation");
		checkAndSet(settings, entity);
		DataAccessService das = getDAS();

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
			ObjectCreator oc = new ObjectCreator(
				settings,
				das,
				getPersistenceOrchestrator(),
				MapperDirection.DOMAINTODOMAIN);
			BusinessObject from = oc.createDataObject(
				entity,
				(EntityType)das.getType(entity.getClass()),
				null,
				null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject)from.clone(settings));

		} finally {
			flushHandler.done();
		}

		return flushHandler.instance();
	}

	private BusinessObject queryOne (Object entity, Settings settings)
	{
		return (BusinessObject)queryInternal(entity, settings).get(0);
	}

	private Class<?> getEntityClass (Object inputObject, Settings settings)
	{
		return inputObject == null ?
			(settings.getEntityClass() != null ?
				settings.getEntityClass() :
				settings.getEntityType().getInstanceClass()) :
			inputObject.getClass();
	}

	private List<?> queryInternal (Object entity, Settings settings)
	{
		owLogger.debug("Performing query operation");
		checkAndSet(settings, entity);

		DataAccessService das = getDAS();
		if (settings.doPreFlush())
			getPersistenceOrchestrator().flush();

		MapperDirection direction = MapperDirection.EXTERNALTOEXTERNAL;
		if ((settings.getEntityType() != null && settings.getEntityType().isOpen()) ||
			das.getTypeMapper().isDomain(getEntityClass(entity, settings)))
			direction = MapperDirection.DOMAINTOEXTERNAL;
		if (settings.doBaseline()) {
			direction = direction.toDomain();
		}

		ObjectCreator oc = new ObjectCreator(settings, das, getPersistenceOrchestrator(), direction);
		oc.setReadOnly(true);

		Type fromType = settings.getEntityType();
		if (fromType == null || !fromType.isOpen()) {
			fromType = oc.getType(getEntityClass(entity, settings));
		}
		BusinessObject from = oc.createDataObject(entity, fromType, null, null);

		// Get the narrowed class, if this is not an open type
		if (!(settings.getEntityType() != null && settings.getEntityType().isOpen())) {
			settings.initNarrowClass(getTypeNarrower(), entity, typeMapper);
		}

		List<?> dataObjects = from.query(settings);

		return dataObjects;
	}

	public int getViewVersion ()
	{
		return viewVersion;
	}

	public void setViewVersion (int version)
	{
		this.viewVersion = version;
	}

	/**
	 * Convenience method to quickly get access to the view meta object
	 *
	 * @param viewName name of view
	 * @return AggregateView
	 */
	public AggregateView getView (String viewName)
	{
		return getDAS().getView(viewName);
	}

	@Override
	public Object create (Object entity, Settings settings)
	{
		owLogger.debug("Performing create operation");
		checkAndSet(settings, entity);

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
			ObjectCreator oc = new ObjectCreator(
				settings,
				getDAS(),
				getPersistenceOrchestrator(),
				MapperDirection.EXTERNALTODOMAIN);

			Type type = null;
			// Collection type is used for bulk import/batching
			if(entity instanceof Collection) {
				type = getDAS().getType(entity.getClass());
			} else {
				type = oc.getType(entity.getClass(), settings.getEntityType());
			}
			BusinessObject from = oc.createDataObject(
				entity,
				type,
				null,
				null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject)from.create(settings));

		} finally {
			flushHandler.done();
		}

		return flushHandler.instance();
	}

	public BusinessObject readBO (Object entity, Settings settings)
	{
		owLogger.debug("Performing read operation");

		boolean isWrapper = false;
		Object wrapper = null;
		if (AbstractType.isWrapperType(entity.getClass())) {
			wrapper = entity;
			if (settings.getEntityClass() == null) {
				throw new IllegalArgumentException("The entity class needs to be provided");
			}
			else {
				EntityType entityType = (EntityType)getDAS().getType(settings.getEntityClass());
				if (entityType != null) {
					entity = ClassUtil.newInstance(settings.getEntityClass());
					isWrapper = true;
				}
				else {
					throw new IllegalArgumentException(
						"The entity class " + settings.getEntityClass().getName() +
							" does not refer to a domain class");
				}
			}
		}

		checkAndSet(settings, entity);

		if (settings.doPreRefresh())
			getPersistenceOrchestrator().refresh(entity);

		ObjectCreator oc = new ObjectCreator(
			settings,
			getDAS(),
			getPersistenceOrchestrator(),
			MapperDirection.DOMAINTOEXTERNAL);
		oc.setReadOnly(true);
		BusinessObject from = oc.createDataObject(
			entity,
			oc.getType(entity.getClass()),
			null,
			null);
		if (isWrapper) {
			ExtendedProperty property = (ExtendedProperty)((EntityType)from.getType()).getIdentifierProperty();
			property.setValue(from, wrapper);
		}
		from = from.load(settings);  // Get the persistent object

		//  perform read on it
		BusinessObject to = (BusinessObject)from.read(settings);

		return to;
	}

	@Override
	public Object read (Object entity, Settings settings)
	{
		BusinessObject to = readBO(entity, settings);
		return to.getNormalizedInstance(settings);
	}

	public void exportCSV(String filePath, Object inputObject, Settings settings) throws IOException
	{
		ExportImport exim = new CSVExportImport(this);
		exim.exportAggregate(filePath, inputObject, settings);
	}

	@Override
	public void exportAggregate (String filePath, Object inputObject, Settings settings) throws
		IOException
	{
		ExportImport exim = new ExcelExportImport(this);
		exim.exportAggregate(filePath, inputObject, settings);
	}

	private Map<String, Integer> getHeaderMap (Sheet sheet)
	{
		Map<String, Integer> colMap = new HashMap<String, Integer>();
		Row headerRow = sheet.getRow(0);
		for (int i = 0; i < headerRow.getLastCellNum(); i++) {
			Cell headerCell = headerRow.getCell(i);
			colMap.put(headerCell.getStringCellValue(), i);
		}

		return colMap;
	}

	@Override
	/**
	 *  For now we handle only one aggregate entity in the document. 
	 *  Later on we can update it handle multiple entities.
	 *
	 *  Ideally, we would want each entity to be in a separate document, 
	 *  so we can process it efficiently using streaming.
	 */
	public Object importAggregate (String filePath, Settings settings) throws IOException
	{
		ExportImport exim = new ExcelExportImport(this);
		return exim.importAggregate(filePath, settings);
	}

	private static boolean isEmbeddedPath (String propertyPath)
	{
		if (propertyPath.indexOf(Settings.PATH_DELIMITER) != -1 &&
			!propertyPath.startsWith(Constants.XOR.XOR_PATH_PREFIX)) {
			return true;
		}

		return false;
	}

	public static JSONObject getJSON (Map<String, Integer> colMap, CSVRecord row)
	{
		JSONObject entity = new JSONObject();

		for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
			String value = row.get(entry.getValue());
			if (isEmbeddedPath(entry.getKey())) {
				setEmbeddableValue(entity, entry.getKey(), value);
			}
			else {
				// set direct value
				if (value != null) {
					//if(NumberUtils.isNumber(value)) {
					//	entity.put(entry.getKey(), NumberUtils.toDouble(value));
					//} else {
						entity.put(entry.getKey(), value);
					//}
				}
				else {
					//entity.put(entry.getKey(), JSONObject.NULL);
					entity.put(entry.getKey(), "");
				}
			}
		}

		return entity;
	}

	public static JSONObject getJSON (Map<String, Integer> colMap, Row row)
	{
		JSONObject entity = new JSONObject();

		for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
			Cell cell = row.getCell(entry.getValue(), Row.RETURN_BLANK_AS_NULL);
			if (isEmbeddedPath(entry.getKey())) {
				if(cell != null) {
					setEmbeddableValue(entity, entry.getKey(), cell.getStringCellValue());
				}
			}
			else {
				// set direct value
				if (cell != null) {
					try {
						entity.put(entry.getKey(), cell.getStringCellValue());
					}
					catch (Exception e) {
						// Numeric entry
						entity.put(entry.getKey(), cell.getNumericCellValue());
					}
				}
				else {
					// Skip processing null values
				}
			}
		}

		return entity;
	}

	private static void setEmbeddableValue (JSONObject base, String path, String value)
	{
		setEmbeddableValue(base, path, value, null);
	}

	public static void setEmbeddableValue (JSONObject base,
											String path,
											Object value,
											String replacedProperty)
	{
		JSONObject embeddable = base;

		// Loop through each part of the path
		while (path.indexOf(Settings.PATH_DELIMITER) != -1) {
			String rootpart = path.substring(0, path.indexOf(Settings.PATH_DELIMITER));
			if (base.has(rootpart)) {
				embeddable = base.getJSONObject(rootpart);
			}
			else {
				embeddable = new JSONObject();
				base.put(rootpart, embeddable);
			}

			path = path.substring(rootpart.length() + 1);
		}
		embeddable.put(path, value);
		if (replacedProperty != null) {
			embeddable.remove(replacedProperty);
		}
	}

	@Override
	public Object update (Object entity, Settings settings)
	{
		owLogger.debug("Performing update operation");
		checkAndSet(settings, entity);

		if (settings.getAction() != AggregateAction.MERGE
			&& settings.getAction() != AggregateAction.UPDATE)
			throw new IllegalStateException("The default action should either be UPDATE or MERGE");

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
			ObjectCreator oc = new ObjectCreator(
				settings,
				getDAS(),
				getPersistenceOrchestrator(),
				MapperDirection.EXTERNALTODOMAIN);
			BusinessObject from = oc.createDataObject(
				entity,
				(EntityType)oc.getType(entity.getClass(), settings.getEntityType()),
				null,
				null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject)from.update(settings));

		} finally {
			flushHandler.done();
		}

		return flushHandler.instance();
	}

	@Override
	public Object update (Object inputObject, Class<?> entityClass)
	{
		return update(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}

	@Override
	public void delete (Object entity, Settings settings)
	{
		getPersistenceOrchestrator().delete(entity);
	}

	@Override
	public Object patch (Object entity, Settings settings)
	{
		owLogger.debug("Performing update operation");
		checkAndSet(settings, entity);
		settings.setBaseline(true);

		// attach it to the persistence layer
		ObjectCreator oc = new ObjectCreator(
			settings,
			getDAS(),
			getPersistenceOrchestrator(),
			MapperDirection.EXTERNALTODOMAIN);

		BusinessObject bo = oc.createDataObject(
			entity,
			//entityType,
			oc.getType(entity.getClass(), settings.getEntityType()),
			null,
			null);

		getPersistenceOrchestrator().attach(bo, settings);

		// update the just attached object with the original object
		// update the id and the version information
		return update(entity, settings);
	}

	@Override
	public List<?> query (Object entity, Settings settings)
	{
		List<Object> result = new ArrayList<Object>();
		List<?> dataObjects = queryInternal(entity, settings);

		Object lastObject = null;
		Object firstObject = null;
		for (Object obj : dataObjects) {
			firstObject = firstObject == null ? obj : firstObject;
			if (!settings.isDenormalized()) {
				result.add(((BusinessObject)obj).getNormalizedInstance(settings));
			}
			else {
				result.add(obj);
			}

			lastObject = obj;
		}

		if (settings.getLimit() != null && firstObject != lastObject) {

			// Extract the columns postions from the first object
			Map<String, Integer> colPositions = new HashMap<String, Integer>();
			if (lastObject.getClass().isArray()) {
				int i = 0;
				for (Object col : (Object[])firstObject) {
					colPositions.put((String)col, i++);
				}
			}

			// Ensure we capture the order by field values for the last object in the nextToken
			Map<String, Object> nextTokenValues = new HashMap<String, Object>();
			List<Filter> consolidated = new ArrayList<Filter>(settings.getAdditionalFilters());
			// Also Look for the filters in the view
			if (settings.getView().getFilter() != null) {
				consolidated.addAll(settings.getView().getFilter());
			}

			for (Filter filter : consolidated) {
				if (filter.isOrderBy()) {
					if (lastObject instanceof BusinessObject) {
						nextTokenValues.put(
							filter.getAttribute(),
							((BusinessObject)lastObject).get(filter.getAttribute()));
					}
					else if (lastObject.getClass().isArray()) {
						nextTokenValues.put(
							filter.getAttribute(),
							((Object[])lastObject)[colPositions.get(filter.getAttribute())]);
					}
				}
			}
			settings.setNextToken(nextTokenValues);
		}

		return result;
	}

	public static class AggregateManagerBuilder
	{

		private DASFactory nestedDasFactory;
		private boolean nestedAutoFlushNative;
		private Interceptor nestedInterceptor;
		private AssociationStrategy nestedAssociationStrategy;
		private DetailStrategy nestedDetailStrategy;
		private PersistenceType nestedPersistenceType;
		private TypeNarrower nestedTypeNarrower;
		private TypeMapper nestedTypeMapper;
		private int nestedViewVersion;

		public AggregateManagerBuilder dasFactory (DASFactory nestedDasFactory)
		{
			this.nestedDasFactory = nestedDasFactory;
			return this;
		}

		public AggregateManagerBuilder autoFlushNative (boolean nestedAutoFlushNative)
		{
			this.nestedAutoFlushNative = nestedAutoFlushNative;
			return this;
		}

		public AggregateManagerBuilder interceptor (Interceptor nestedInterceptor)
		{
			this.nestedInterceptor = nestedInterceptor;
			return this;
		}

		public AggregateManagerBuilder associationStrategy (
			AssociationStrategy nestedAssociationStrategy)
		{
			this.nestedAssociationStrategy = nestedAssociationStrategy;
			return this;
		}

		public AggregateManagerBuilder detailStrategy (DetailStrategy nestedDetailStrategy)
		{
			this.nestedDetailStrategy = nestedDetailStrategy;
			return this;
		}

		public AggregateManagerBuilder persistenceType (PersistenceType nestedPersistenceType)
		{
			this.nestedPersistenceType = nestedPersistenceType;
			return this;
		}

		public AggregateManagerBuilder typeNarrower (TypeNarrower nestedTypeNarrower)
		{
			this.nestedTypeNarrower = nestedTypeNarrower;
			return this;
		}

		public AggregateManagerBuilder typeMapper (TypeMapper nestedTypeMapper)
		{
			this.nestedTypeMapper = nestedTypeMapper;
			return this;
		}

		public AggregateManagerBuilder viewVersion (int nestedViewVersion)
		{
			this.nestedViewVersion = nestedViewVersion;
			return this;
		}

		public AggregateManagerBuilder init (AggregateManager other)
		{

			this.nestedDasFactory = other.getDasFactory();
			this.nestedAutoFlushNative = other.isAutoFlushNative();
			this.nestedInterceptor = other.getInterceptor();
			this.nestedAssociationStrategy = other.getAssociationStrategy();
			this.nestedDetailStrategy = other.detailStrategy;
			this.nestedPersistenceType = other.getPersistenceType();
			this.nestedTypeNarrower = other.getTypeNarrower();
			this.nestedTypeMapper = other.getTypeMapper();
			this.nestedViewVersion = other.getViewVersion();

			return this;
		}

		public AggregateManager build ()
		{
			AggregateManager result = new AggregateManager();

			if (nestedDasFactory != null)
				result.setDasFactory(nestedDasFactory);

			result.setAutoFlushNative(nestedAutoFlushNative);

			if (nestedInterceptor != null)
				result.setInterceptor(nestedInterceptor);

			if (nestedAssociationStrategy != null)
				result.setAssociationStrategy(nestedAssociationStrategy);

			if (nestedDetailStrategy != null)
				result.setDetailStrategy(nestedDetailStrategy);

			if (nestedPersistenceType != null)
				result.setPersistenceType(nestedPersistenceType);

			if (nestedTypeNarrower != null)
				result.setTypeNarrower(nestedTypeNarrower);

			if (nestedTypeMapper != null)
				result.setTypeMapper(nestedTypeMapper);

			result.setViewVersion(nestedViewVersion);

			result.init();

			return result;
		}
	}

	@Override
	public void exportDenormalized (OutputStream outputStream, Settings settings)
	{

		// Make sure this is a denormalized query
		settings.setDenormalized(true);
		List<?> result = query(null, settings);

		// Currently only address one sheet, additional sheets will handle
		// dependencies
		ExcelExporter e = new ExcelExporter(outputStream, settings);

		// The first row is the column names
		e.writeRow(result.get(0));

		for (int i = 1; i < result.size(); i++) {
			e.writeRow(result.get(i));
		}
		// TODO: add validation support
		e.writeValidations();

		e.finish();
	}

	private static Object getCellValue (Cell cell)
	{
		if (cell != null) {
			try {
				return cell.getStringCellValue();
			}
			catch (Exception e) {
				// Numeric entry
				return cell.getNumericCellValue();
			}
		}
		else {
			return "";
		}
	}

	public void importCSV (String filePath, Settings settings) throws Exception
	{
		ExportImport exim = new CSVExportImport(this);
		exim.importAggregate(filePath, settings);
	}

	@Override
	public void importDenormalized (InputStream is, Settings settings) throws
		IOException
	{

		try {
			Workbook wb = WorkbookFactory.create(is);

			// First create the object based on the denormalized values
			// The callInfo should have root BusinessObject
			// clone the task object using a DataObject

			// Create an object creator for the target root
			ObjectCreator oc = new ObjectCreator(
				settings,
				getDAS(),
				getPersistenceOrchestrator(),
				MapperDirection.EXTERNALTODOMAIN);

			// The Excel should have a single sheet containing the denormalized data
			// Create a JSONObject for each row
			Sheet entitySheet = wb.getSheetAt(0);
			Map<String, Integer> colMap = getHeaderMap(wb.getSheetAt(0));

			Map<BusinessObject, Object> roots = new IdentityHashMap<BusinessObject, Object>();
			for (int i = 1; i <= entitySheet.getLastRowNum(); i++) {
				Row row = entitySheet.getRow(i);

				Property idProperty = ((EntityType)settings.getEntityType()).getIdentifierProperty();
				String idName = idProperty.getName();
				if(!colMap.containsKey(idName)) {
					throw new RuntimeException("The Excel sheet needs to have the entity identifier column");
				}

				// TODO: Create a JSON object and then extract the value with the correct type
				Object idValue = getCellValue(row.getCell(colMap.get(idName)));
				if(idProperty.getType() instanceof SimpleType) {
					idValue = ((SimpleType)idProperty.getType()).unmarshall(idValue.toString());
				}

				// Get a child business object of the same type
				// TODO: Get by user key
				//EntityKey ek = oc.getTypeMapper().getEntityKey(idValue, settings.getEntityType());
				EntityKey ek = oc.getTypeMapper().getSurrogateKey(idValue, settings.getEntityType());
				BusinessObject bo = oc.getByEntityKey(ek);
				if (bo == null) {
					
					bo = oc.createDataObject(
						AbstractBO.createInstance(oc, idValue, settings.getEntityType()),
						settings.getEntityType(),
						null,
						null);
					BusinessObject potentialRoot = (BusinessObject)bo.getRootObject();
					if(!roots.containsKey(potentialRoot)) {
						roots.put(potentialRoot, null);
					}
				}

				for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
					Cell cell = row.getCell(entry.getValue());
					Object cellValue = getCellValue(cell);
					Property property = ((EntityType)settings.getEntityType()).getProperty(entry.getKey());
					cellValue = ((SimpleType)property.getType()).unmarshall(cellValue.toString());
					bo.set(entry.getKey(), cellValue);
				}
			}
			for(BusinessObject root: roots.keySet()) {
				this.update(root.getInstance(), settings);
			}

		}
		catch (EncryptedDocumentException e) {
			throw new RuntimeException("Document is encrypted, provide a decrypted inputstream", e);
		}
		catch (InvalidFormatException e) {
			throw new RuntimeException("The provided inputstream is not valid. ", e);
		}
		catch (Exception e) {
			throw new RuntimeException("An error occurred during update.", e);
		}
	}

	public File getGeneratedViewsDirectory() {
		File f = null;
		try {
			f = new File( URLDecoder.decode( viewsDirectory, "UTF-8" ) );
			if(!f.exists()) {
				f.mkdirs();
			}
		} catch (UnsupportedEncodingException e) {
			ClassUtil.wrapRun(e);
		}

		return f;
	}	
}
