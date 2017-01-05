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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
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
import tools.xor.ExcelJsonTypeMapper;
import tools.xor.ExtendedProperty;
import tools.xor.MapperDirection;
import tools.xor.MutableBO;
import tools.xor.OpenType;
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
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ExcelJsonCreationStrategy;
import tools.xor.util.ObjectCreator;
import tools.xor.util.PersistenceType;
import tools.xor.util.excel.ExcelExporter;
import tools.xor.view.AggregateView;
import tools.xor.view.AggregateViewFactory;
import tools.xor.view.Filter;
import tools.xor.view.TypeVersion;

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
				getPersistenceOrchestrator().setFlushMode(oldFlushMode);
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
			settings.init(this); // populate the view from the type if necessary
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

		ObjectCreator oc = new ObjectCreator(das, getPersistenceOrchestrator(), direction);
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

	public void linkBackPointer (Object entity)
	{
		ObjectCreator oc = new ObjectCreator(
			getDAS(),
			getPersistenceOrchestrator(),
			MapperDirection.EXTERNALTOEXTERNAL);
		MutableBO dataObject = (MutableBO)oc.createDataObject(
			entity,
			(EntityType)oc.getType(entity.getClass()),
			null,
			null);
		oc.setShare(true);
		dataObject.createAggregate();
		dataObject.linkBackPointer();
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
				getDAS(),
				getPersistenceOrchestrator(),
				MapperDirection.EXTERNALTODOMAIN);
			BusinessObject from = oc.createDataObject(
				entity,
				(EntityType)oc.getType(entity.getClass(), settings.getEntityType()),
				null,
				null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject)from.create(settings));

		} finally {
			flushHandler.done();
		}

		return flushHandler.instance();
	}

	private BusinessObject readBO (Object entity, Settings settings)
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

	@Override
	public void exportAggregate (OutputStream os, Object inputObject, Settings settings) throws
		IOException
	{
		validateImportExport();

		BusinessObject to = readBO(inputObject, settings);
		Set<BusinessObject> dataObject = to.getObjectCreator().getDataObjects();

		// Get the container and the containment property and create a sheet of such objects
		Map<String, List<BusinessObject>> sheetBO = new HashMap<String, List<BusinessObject>>();
		for (BusinessObject bo : dataObject) {
			if (bo.getContainer() != null && bo.getContainmentProperty() != null) {
				String key = Constants.XOR.getExcelSheetFullName(
					bo.getContainer().getType(),
					bo.getContainmentProperty());
				if (!sheetBO.containsKey(key)) {
					sheetBO.put(key, new LinkedList<BusinessObject>());
				}
				List<BusinessObject> boList = sheetBO.get(key);
				boList.add(bo);
			}
		}

		Workbook wb = processSheetBO(to, sheetBO);
		wb.write(os);
		os.close();
		wb.close();

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

	private void validateImportExport ()
	{
		if (!ExcelJsonTypeMapper.class.isAssignableFrom(this.getTypeMapper().getClass())) {
			throw new RuntimeException("Import/Export can only work with ExcelJsonTypeMapper");
		}
	}

	@Override
	/**
	 *  For now we handle only one aggregate entity in the document. 
	 *  Later on we can update it handle multiple entities.
	 *
	 *  Ideally, we would want each entity to be in a separate document, 
	 *  so we can process it efficiently using streaming.
	 */
	public Object importAggregate (InputStream is, Settings settings) throws IOException
	{
		validateImportExport();

		try {
			Workbook wb = WorkbookFactory.create(is);

			Sheet entitySheet = wb.getSheet(Constants.XOR.EXCEL_ENTITY_SHEET);
			if (entitySheet == null) {
				throw new RuntimeException("The entity sheet is missing");
			}

			// Get the entity class name
			Map<String, Integer> colMap = getHeaderMap(entitySheet);
			if (!colMap.containsKey(Constants.XOR.TYPE)) {
				// TODO: Fallback to entity class in settings if provided
				throw new RuntimeException("XOR.type column is missing");
			}
			Row entityRow = entitySheet.getRow(1);
			if (entityRow == null) {
				throw new RuntimeException("Entity row is missing");
			}
			Cell typeCell = entityRow.getCell(colMap.get(Constants.XOR.TYPE));
			String entityClassName = typeCell.getStringCellValue();
			try {
				settings.setEntityClass(Class.forName(entityClassName));
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException("Class " + entityClassName + " is not found");
			}

			/******************************************************
			 * Algorithm
			 *
			 * 1. Create all objects with the XOR.id 
			 * 2. Create the collections
			 * 3. Associate the collections to their owners
			 * 4. Then finally call JSONTransformer.unpack to link the objects by XOR.id
			 *
			 ********************************************************/

			// 1. Create all objects with the XOR.id
			Map<String, String> collectionSheets = new HashMap<String, String>();
			Map<String, String> entitySheets = new HashMap<String, String>();
			entitySheets.put(Constants.XOR.EXCEL_ENTITY_SHEET, entityClassName);
			Map<String, JSONObject> idMap = parseEntities(wb, entitySheets, collectionSheets);

			// 2. Create the collections
			// The key in the collection property map is of the form <owner_xor_id>:<property>
			Map<String, JSONArray> collectionPropertyMap = parseCollections(
				wb,
				collectionSheets,
				idMap);

			// 3. Associate the collections to their owners
			// Replace all objectref prefix keys with the actual objects
			// Replace all collection properties with the array objects
			link(wb, idMap, collectionPropertyMap);

			// Find the root
			Cell idCell = entityRow.getCell(colMap.get(Constants.XOR.ID));
			String rootId = idCell.getStringCellValue();
			JSONObject root = idMap.get(rootId);

			// Finally persist the root object
			// call the update persistence method
			Class entityClass;
			try {
				entityClass = Class.forName(root.getString(Constants.XOR.TYPE));
			}
			catch (ClassNotFoundException | JSONException e) {
				throw new RuntimeException(
					"Unable to construct root entity. Either the class is not found or the class name is missing");
			}

			return update(root, entityClass);

		}
		catch (EncryptedDocumentException e) {
			throw new RuntimeException("Document is encrypted, provide a decrypted inputstream");
		}
		catch (InvalidFormatException e) {
			throw new RuntimeException("The provided inputstream is not valid. " + e.getMessage());
		}
	}

	private void link (Workbook wb,
					   Map<String, JSONObject> idMap,
					   Map<String, JSONArray> collectionPropertyMap)
	{
		// First link the collections to their owners
		for (Map.Entry<String, JSONArray> entry : collectionPropertyMap.entrySet()) {
			String collectionKey = entry.getKey();
			String[] tokens = collectionKey.split(":");
			String ownerId = tokens[0];
			String collectionProperty = tokens[1];

			JSONObject owner = idMap.get(ownerId);
			if (owner == null) {
				throw new RuntimeException(
					"Unable to find collection owner with XOR.id " + ownerId);
			}
			owner.put(collectionProperty, entry.getValue());
		}

		// Link all the object references
		for (JSONObject entity : idMap.values()) {
			// Iterate through all the object references
			JSONArray fields = entity.names();
			for (int i = 0; i < fields.length(); i++) {
				String property = fields.getString(i);
				if (property.startsWith(Constants.XOR.OBJECTREF)) {
					JSONObject toOne = idMap.get(entity.getString(property));
					if (toOne == null) {
						logger.info(
							"Unable to find object reference: " + entity.getString(property));
						continue;
					}
					String reference = property.substring(Constants.XOR.OBJECTREF.length());

					// replace the object reference with actual reference
					setEmbeddableValue(entity, reference, toOne, property);
				}
			}
		}
	}

	private Type getType (String entityInfo)
	{
		// Parse the entity classname from this
		String[] tokens = entityInfo.split(":");
		if (tokens.length != 2) {
			throw new RuntimeException(
				"The entity info column in sheet map is not in <classname>:<property> format: "
					+ entityInfo);
		}
		return getDAS().getType(tokens[0]);
	}

	private Property getProperty (String entityInfo)
	{
		Type type = getType(entityInfo);

		String[] tokens = entityInfo.split(":");
		return type.getProperty(tokens[1]);
	}

	/**
	 *
	 * @param wb
	 * @param collectionSheets Will be used in subsequent processing
	 * @return
	 */
	/**
	 * Parse the given Excel workbook and group the entity and collection sheets separately.
	 *
	 * @param wb               given workbook
	 * @param entitySheets     Sheets containing entity data
	 * @param collectionSheets Sheets capturing collection relationships. Will be used in subsequent processing
	 * @return a map of JSON entities keyed by their XOR id
	 */
	private Map<String, JSONObject> parseEntities (Workbook wb,
												   Map<String, String> entitySheets,
												   Map<String, String> collectionSheets)
	{
		// First find all the entity sheets
		Sheet sheetMap = wb.getSheet(Constants.XOR.EXCEL_INDEX_SHEET);

		// SheetName is in first column
		// Entity type and property is in second column
		for (int i = 0; i <= sheetMap.getLastRowNum(); i++) {
			Row row = sheetMap.getRow(i);
			String entityInfo = row.getCell(1).getStringCellValue();

			if (getProperty(entityInfo).isMany()) {
				collectionSheets.put(row.getCell(0).getStringCellValue(), entityInfo);
			}
			else {
				entitySheets.put(row.getCell(0).getStringCellValue(), entityInfo);
			}
		}

		Map<String, JSONObject> idMap = new HashMap<String, JSONObject>();
		for (Map.Entry<String, String> entry : entitySheets.entrySet()) {
			processEntitySheet(wb, entry.getKey(), idMap);
		}

		return idMap;
	}

	private void processEntitySheet (Workbook wb, String sheetName, Map<String, JSONObject> idMap)
	{
		// Ensure we have the XOR.id column in the entity sheet
		Sheet entitySheet = wb.getSheet(sheetName);
		Map<String, Integer> colMap = getHeaderMap(entitySheet);
		if (!colMap.containsKey(Constants.XOR.ID)) {
			throw new RuntimeException("XOR.id column is missing");
		}

		// process each entity
		for (int i = 1; i <= entitySheet.getLastRowNum(); i++) {
			JSONObject entityJSON = getJSON(colMap, entitySheet.getRow(i));
			idMap.put(entityJSON.getString(Constants.XOR.ID), entityJSON);
		}
	}

	private void processCollectionSheet (
		Workbook wb,
		String sheetName,
		String entityInfo, Map<String, JSONArray> collectionPropertyMap,
		Map<String, JSONObject> idMap)
	{
		// Ensure we have the XOR.id column in the entity sheet
		Sheet collectionSheet = wb.getSheet(sheetName);
		Map<String, Integer> colMap = getHeaderMap(collectionSheet);

		// A collection can have value objects, so XOR.ID is not mandatory
		// But a collection entry should have a collection owner
		if (!colMap.containsKey(Constants.XOR.OWNER_ID)) {
			throw new RuntimeException("XOR.owner.id column is missing");
		}

		// process each collection entry
		for (int i = 1; i <= collectionSheet.getLastRowNum(); i++) {
			JSONObject collectionEntryJSON = getJSON(colMap, collectionSheet.getRow(i));
			String key = getCollectionKey(
				collectionEntryJSON.getString(Constants.XOR.OWNER_ID),
				entityInfo);
			addCollectionEntry(collectionPropertyMap, key, collectionEntryJSON);

			// If the collection element is an entity add it to the idMap also
			if (collectionEntryJSON.has(Constants.XOR.ID)) {
				try {
					idMap.put(collectionEntryJSON.getString(Constants.XOR.ID), collectionEntryJSON);
				}
				catch (Exception e) {
					String longStr = new Long(collectionEntryJSON.getLong(Constants.XOR.ID)).toString();
					idMap.put(longStr, collectionEntryJSON);
				}
			}
		}
	}

	private String getCollectionKey (String ownerXorKey, String entityInfo)
	{
		return ownerXorKey + ":" + getProperty(entityInfo).getName();
	}

	private void addCollectionEntry (Map<String, JSONArray> collectionPropertyMap,
									 String key,
									 JSONObject collectionEntryJSON)
	{
		JSONArray collection = null;
		if (collectionPropertyMap.containsKey(key)) {
			collection = collectionPropertyMap.get(key);
		}
		else {
			collection = new JSONArray();
			collectionPropertyMap.put(key, collection);
		}

		collection.put(collectionEntryJSON);
	}

	private static boolean isEmbeddedPath (String propertyPath)
	{
		if (propertyPath.indexOf(Settings.PATH_DELIMITER) != -1 &&
			!propertyPath.startsWith(Constants.XOR.XOR_PATH_PREFIX)) {
			return true;
		}

		return false;
	}

	public static JSONObject getJSON (Map<String, Integer> colMap, Row row)
	{
		JSONObject entity = new JSONObject();

		for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
			Cell cell = row.getCell(entry.getValue());
			if (isEmbeddedPath(entry.getKey())) {
				setEmbeddableValue(entity, entry.getKey(), cell.getStringCellValue());
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
					//entity.put(entry.getKey(), JSONObject.NULL);
					entity.put(entry.getKey(), "");
				}
			}
		}

		return entity;
	}

	private static void setEmbeddableValue (JSONObject base, String path, String value)
	{
		setEmbeddableValue(base, path, value, null);
	}

	private static void setEmbeddableValue (JSONObject base,
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

	private Map<String, JSONArray> parseCollections (Workbook wb,
													 Map<String, String> collectionSheets,
													 Map<String, JSONObject> idMap)
	{
		Map<String, JSONArray> collectionPropertyMap = new HashMap<String, JSONArray>();
		for (Map.Entry<String, String> entry : collectionSheets.entrySet()) {
			processCollectionSheet(
				wb,
				entry.getKey(),
				entry.getValue(),
				collectionPropertyMap,
				idMap);
		}

		return collectionPropertyMap;
	}

	/**
	 * Generate the Excel sheets based on entities and collections
	 * TODO: Should the map be topologically ordered?
	 *
	 * @param to      root entity
	 * @param sheetBO map of the sheet name and the entities/relationships within that sheet
	 * @return the generated Excel workbook
	 */
	private Workbook processSheetBO (BusinessObject to, Map<String, List<BusinessObject>> sheetBO)
	{

		SXSSFWorkbook wb = new SXSSFWorkbook();
		wb.setCompressTempFiles(true);

		List<BusinessObject> entityBOList = new LinkedList<BusinessObject>();
		entityBOList.add(to);
		createBOSheet(wb, Constants.XOR.EXCEL_ENTITY_SHEET, null, entityBOList, null);

		int sheetNo = 1;
		Map<String, String> sheetMap = new HashMap<String, String>();
		for (Map.Entry<String, List<BusinessObject>> entry : sheetBO.entrySet()) {
			// Create a sheet
			String sheetName = Constants.XOR.EXCEL_SHEET_PREFIX + sheetNo++;
			sheetMap.put(entry.getKey(), sheetName);
			createBOSheet(wb, sheetName, entry.getKey(), entry.getValue(), null);
		}
		writeSheetMap(wb, sheetMap);

		return wb;
	}

	private void createBOSheet (Workbook wb,
								String sheetName,
								String entityInfo,
								List<BusinessObject> boList,
								BusinessObject owner)
	{

		int colNo = 0;
		int rowNo = 1;
		SXSSFSheet sh = (SXSSFSheet)wb.getSheet(sheetName);
		if (sh == null) {
			sh = (SXSSFSheet)wb.createSheet(sheetName);
		}
		else {
			rowNo = sh.getLastRowNum() + 1;
		}
		Map<String, Integer> propertyColIndex = new HashMap<String, Integer>();

		for (BusinessObject bo : boList) {
			if (bo.getContainmentProperty() != null && bo.getContainmentProperty().isMany()) {
				createBOSheet(
					wb,
					sheetName,
					entityInfo,
					bo.getList(),
					(BusinessObject)bo.getContainer());
				continue;
			}

			List<String> propertyPaths = new ArrayList<String>();

			// Based on polymorphism, the actual instance can be a different subtype
			// so we need to get a fresh property list and calculate the column indexes
			// as new properties might be present and would need to be mapped to additional columns
			if (owner == null && bo.getContainer() != null) {
				owner = (BusinessObject)bo.getContainer();
			}
			if (owner != null) {
				propertyPaths.add(Constants.XOR.OWNER_ID);
			}
			propertyPaths.add(Constants.XOR.ID);
			propertyPaths.add(Constants.XOR.TYPE);
			for (Property property : bo.getType().getProperties()) {
				if (property.isMany()) {
					propertyPaths.add(ExcelJsonCreationStrategy.getCollectionTypeKey(property));

					// Collections are handled separately
					continue;
				}
				// Skip open content until we come with a default serialized form for empty object
				// Currently it fails validation since empty string does not equal JSONObject
				if (property.isOpenContent()) {
					continue;
				}
				// Handle embedded objects and expand them if necessary
				propertyPaths.addAll(property.expand());
			}

			for (String propertyPath : propertyPaths) {
				if (!propertyColIndex.containsKey(propertyPath)) {
					propertyColIndex.put(propertyPath, colNo++);
				}
			}

			// TODO: add columns only if the value is not null
			Row row = sh.createRow(rowNo++);
			for (String propertyPath : propertyPaths) {
				Cell cell = row.createCell(propertyColIndex.get(propertyPath));
				Object value = null;
				if (Constants.XOR.OWNER_ID.equals(propertyPath)) {
					value = owner.getOpenProperty(Constants.XOR.ID);
				}
				else if (Constants.XOR.ID.equals(propertyPath) || propertyPath.startsWith(
					Constants.XOR.TYPE + Constants.XOR.SEP)) {
					value = bo.getOpenProperty(propertyPath);
				}
				else if (Constants.XOR.TYPE.equals(propertyPath)) {
					value = bo.getInstanceClassName();
				}
				else if (propertyPath.startsWith(Constants.XOR.OBJECTREF)) {
					String path = propertyPath.substring(Constants.XOR.OBJECTREF.length());
					value = bo.getExistingDataObject(Settings.convertToBOPath(path));
					if (value != null && value instanceof BusinessObject) {
						value = ((BusinessObject)value).getOpenProperty(Constants.XOR.ID);
					}
					else if (value != null) {
						throw new RuntimeException(
							"ObjectRef needs to refer to an Entity: " + value.toString());
					}
				}
				else {
					value = bo.get(Settings.convertToBOPath(propertyPath));
				}
				if (value != null) {
					cell.setCellValue(value.toString());
				}
			}
		}

		writeColumnNames(sh, propertyColIndex);
	}

	private void writeSheetMap (SXSSFWorkbook wb, Map<String, String> sheetMap)
	{
		SXSSFSheet sh = (SXSSFSheet)wb.createSheet(Constants.XOR.EXCEL_INDEX_SHEET);

		int rowNo = 0;
		for (Map.Entry<String, String> entry : sheetMap.entrySet()) {
			Row row = sh.createRow(rowNo++);
			Cell sheetNameCell = row.createCell(0);
			Cell propertyNameCell = row.createCell(1);
			sheetNameCell.setCellValue(entry.getValue());
			propertyNameCell.setCellValue(entry.getKey());
		}

		sh.autoSizeColumn(0);
		sh.autoSizeColumn(1);
		wb.setSheetOrder(Constants.XOR.EXCEL_INDEX_SHEET, 1);
	}

	private void writeColumnNames (SXSSFSheet sh, Map<String, Integer> propertyColIndex)
	{
		Row row = sh.getRow(0);
		if (row != null) {
			// Column names have already been populated
			return;
		}

		row = sh.createRow(0);
		for (Map.Entry<String, Integer> entry : propertyColIndex.entrySet()) {
			Cell cell = row.createCell(entry.getValue());
			cell.setCellValue(entry.getKey());
			sh.autoSizeColumn(entry.getValue());
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
		// TODO: 
	}

	@Override
	public Object patch (Object entity, Settings settings)
	{
		owLogger.debug("Performing update operation");
		checkAndSet(settings, entity);
		settings.setBaseline(true);

		BusinessObject o = queryOne(entity, settings);

		// attach it to the persistence layer
		getPersistenceOrchestrator().attach(o, settings.getView());

		// update the just attached object with the original object
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

	@Override
	public void importBulk (Reader csvData, Settings settings) throws Exception
	{
		CSVParser parser = new CSVParser(csvData, CSVFormat.DEFAULT.withHeader());
		Map<String, Integer> headerMap = parser.getHeaderMap();
		Map<String, Property> propertyMap = new HashMap<String, Property>();

		// Create view based on the CSV header fields
		AggregateView view = new AggregateView("CSV_IMPORT");
		List path = new ArrayList();
		view.setAttributeList(path);
		settings.setView(view);

		for(Map.Entry<String, Integer> entry : headerMap.entrySet()) {
			Property property = ((EntityType)settings.getEntityType()).getProperty(entry.getKey());
			if(property != null) {
				propertyMap.put(entry.getKey(), property);
				path.add(entry.getKey());
			}
		}

		// Create an object creator for the target root
		ObjectCreator oc = new ObjectCreator(
			getDAS(),
			getPersistenceOrchestrator(),
			MapperDirection.EXTERNALTODOMAIN);


		int count = 1;
		for(
			CSVRecord csvRecord
			:parser)

		{
			BusinessObject bo = oc.createDataObject(
				AbstractBO.createInstance(oc, null, settings.getEntityType()),
				settings.getEntityType(),
				null,
				null);
			for (Map.Entry<String, Property> entry : propertyMap.entrySet()) {
				Object cellValue = csvRecord.get(entry.getKey());
				Property property = propertyMap.get(entry.getKey());
				cellValue = ((SimpleType)property.getType()).unmarshall(cellValue.toString());

				bo.set(entry.getKey(), cellValue);
			}

			this.update(bo.getInstance(), settings);
			if(count++%BULK_BATCH_SIZE == 0) {
				getPersistenceOrchestrator().flush();
				getPersistenceOrchestrator().clear();
			}
		}
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
				EntityKey ek = oc.getTypeMapper().getEntityKey(idValue, settings.getEntityType());
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
