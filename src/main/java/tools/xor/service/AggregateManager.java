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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AbstractType;
import tools.xor.AggregateAction;
import tools.xor.AssociationSetting;
import tools.xor.BusinessObject;
import tools.xor.DataGenerator;
import tools.xor.DefaultTypeNarrower;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.MapperSide;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.core.Interceptor;
import tools.xor.custom.AssociationStrategy;
import tools.xor.custom.DefaultAssociationStrategy;
import tools.xor.custom.DefaultDetailStrategy;
import tools.xor.custom.DetailStrategy;
import tools.xor.operation.AbstractOperation;
import tools.xor.operation.DenormalizedModifyOperation;
import tools.xor.operation.DenormalizedQueryOperation;
import tools.xor.operation.MigrateOperation;
import tools.xor.providers.jdbc.CustomPersister;
import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.providers.jdbc.JDBCSessionContext;
import tools.xor.service.exim.CSVExportImport;
import tools.xor.service.exim.ExcelExportImport;
import tools.xor.service.exim.ExportImport;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;
import tools.xor.util.PersistenceType;
import tools.xor.util.graph.ObjectGraph;
import tools.xor.view.AggregateViewFactory;
import tools.xor.view.Function;
import tools.xor.view.QueryFragment;
import tools.xor.view.TypeVersion;
import tools.xor.view.View;

public class AggregateManager implements Xor
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);
	private static final Logger sgLogger = LogManager.getLogger(Constants.Log.STATE_GRAPH);

	private static int BULK_BATCH_SIZE = 1000; // Flush the session for every batch to reduce memory pressure during bulk CSV import

	private DataModelFactory dataModelFactory;
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
	private ForeignKeyEnhancer foreignKeyEnhancer = new DefaultForeignKeyEnhancer();

	// This is maintained per thread, because the dataStore holds
	// the session that is thread specific
	private ThreadLocal<DataStore> dataStore = new ThreadLocal<DataStore>();

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

	public AggregateManager(PersistenceType persistenceType) {
		this.persistenceType = persistenceType;
	}

	public AggregateManager() {}

	@PostConstruct
	protected void init ()
	{

		if (associationStrategy == null) {
			associationStrategy = new DefaultAssociationStrategy();
		}

		if (detailStrategy == null) {
			detailStrategy = new DefaultDetailStrategy();
		}

		if (typeNarrower == null) {
			typeNarrower = new DefaultTypeNarrower();
			typeNarrower.setAggregateManager(this);
		}

		if (dataModelFactory != null) {
			metaModel = new MetaModel(this);
			dataModelFactory.setAggregateManager(this);
			
			// create or make a copy if typeMapper already initialized
		    typeMapper = this.getDataModel().getTypeMapper();
		}
		else {
			logger.error(
				"DASFactory instance is not set on the AggregateManager instance with the default name.");
		}

		reloadViews();
	}

	public DataModelFactory getDataModelFactory ()
	{
		return dataModelFactory;
	}

	@Override
	public void setDataModelFactory (DataModelFactory dasFactory)
	{
		this.dataModelFactory = dasFactory;
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

	public ForeignKeyEnhancer getForeignKeyEnhancer () {
		return this.foreignKeyEnhancer;
	}

	public void setForeignKeyEnhancer (ForeignKeyEnhancer enhancer) {
		this.foreignKeyEnhancer = enhancer;
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

	@Override
	public void setTypeMapper (TypeMapper typeMapper)
	{
		this.typeMapper = typeMapper;
	}

	@Override
	public DataStore getDataStore ()
	{
		return dataStore.get();
	}

	/**
	 * This method sets the current dataStore, irrespective
	 * of whether the thread had one previously. This way we do
	 * not have to bother about clearing an obsolete dataStore.
	 *
	 * @param po value to set
	 */
	public void setDataStore (
		DataStore po)
	{
		this.dataStore.set(po);
	}

	@Override
	public DataModel getDataModel ()
	{
		return dataModelFactory.create(this.typeMapper);
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
		boolean existingTransaction;
		Settings settings;

		FlushHandler (Settings settings)
		{
			this.settings = settings;
			oldFlushMode = getDataStore().disableAutoFlush();

			if(getDataStore() instanceof JDBCDataStore) {
				JDBCDataStore po = (JDBCDataStore) getDataStore();
				try {
					CustomPersister cp = po.getSessionContext();
					if(cp.getConnection() != null && !cp.getConnection().isClosed()) {
                        existingTransaction = true;
                    }

					if(!existingTransaction) {
                        po.getSessionContext().beginTransaction();
                    }
				}
				catch (SQLException e) {
					throw ClassUtil.wrapRun(e);
				}
			}
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
					getDataStore().setFlushMode(oldFlushMode);
				}
				if (settings.doPostFlush()) {
					getDataStore().flush();
				}

				if(!existingTransaction && getDataStore() instanceof JDBCDataStore) {
					JDBCDataStore po = (JDBCDataStore) getDataStore();
					po.getSessionContext().commit();
					po.getSessionContext().close();
				}
			}
		}
	}

	@Override
	public Transaction createTransaction(Settings settings) {
		configure(settings);

		if(getDataStore() instanceof JDBCDataStore) {
			JDBCDataStore po = (JDBCDataStore)getDataStore();
			JDBCSessionContext sc = po.getSessionContext();
			return new JDBCTransaction(sc);
		} else if (getDataStore() instanceof JPAPersistenceXMLPO) {
			JPAPersistenceXMLPO jpaPO = (JPAPersistenceXMLPO) getDataStore();
			return new JPAManualTransaction(jpaPO.getEntityManager().getTransaction());
		} else if (getDataStore() instanceof JPASpringPO) {
			JPASpringPO jpaPO = (JPASpringPO) getDataStore();
			return new JPATransaction(jpaPO.getTxManager());
		} else {
			throw new UnsupportedOperationException("createTransaction is supported only on JDBC provider");
		}
	}

	@Override
	public void configure (Settings settings) {

		if (getDataStore() == null) {
			setDataStore(dataModelFactory.createDataStore(settings != null ? settings.getSessionContext() : null));
		}
		if(settings != null) {
			if(settings.getDataStore() == null) {
				settings.setAggregateManager(this);
				settings.initDataStore(getDataStore());
				if (settings.getSessionContext() != null
					&& getDataStore() instanceof JDBCDataStore) {
					JDBCDataStore po = ((JDBCDataStore)getDataStore());
					po.getSessionContext().init((JDBCSessionContext)settings.getSessionContext());
				}
			} else if(settings.getDataStore() != getDataStore()) {
				throw new IllegalStateException("DataStore in settings is different from AggregateManager!");
			}
		}
	}

	private void checkAndSet (Settings settings, Object inputObject)
	{
		Class<?> inputObjectClass = getEntityClass(inputObject, settings);

		configure(settings);

		if (settings.getAssociationStrategy() == null) {
			settings.setAssociationStrategy(associationStrategy);
		}

		if (settings.getEntityType() == null) {

			// Try to infer the type from the input object
			DataModel das = getDataModel();

			Class<?> domainClass = settings.getEntityClass();
			String entityTypeName = null;
			if (domainClass == null && inputObjectClass != null) {
			    entityTypeName = das.getTypeMapper().toDomain(inputObjectClass.getName());
			    if(entityTypeName != null) {
			        try {
			            domainClass = Class.forName(entityTypeName);
			        } catch (ClassNotFoundException e) {
			            e.printStackTrace();
			        }   
			        settings.setEntityClass(domainClass);
			    }
			} else if(domainClass != null) {
			    entityTypeName = domainClass.getName();
			}

			// check if the view is a built-in view. The domain class can be inferred from
			// the view name
			if(entityTypeName == null) {
				domainClass = settings.getView().inferDomainClass();
				settings.setEntityClass(domainClass);
			}

			if (domainClass == null) {
				throw new RuntimeException(
					"Unable to identify the type on which to perform the operation. Need to explicitly specify the domain type.");
			} else {
			    entityTypeName = domainClass.getName();
			}
			Type entityType = das.getShape().getType(entityTypeName);
			if(entityType == null) {
			    throw new RuntimeException("EntityType needs to be provided in the settings");
			}
			settings.setEntityType(entityType);
			settings.init(das.getShape()); // populate the view from the type if necessary
			owLogger.debug("Operation on Entity Type: " + settings.getEntityType().getName());
			if (owLogger.isTraceEnabled()) {
				owLogger.trace(getStackTrace(10));
			}

			if (sgLogger.isDebugEnabled()) {
				if (settings.getView().getTypeGraph((EntityType)settings.getEntityType()) != null) {
					if (sgLogger.isTraceEnabled()) {
						sgLogger.trace(getStackTrace(10));
					}
					sgLogger.debug(
						"State graph of Entity: " + settings.getEntityType().getName()
							+ " for view: " + settings.getView().getName());
					sgLogger.debug(settings.getView().getTypeGraph((EntityType)settings.getEntityType()).dumpState());
				}
			}
		}

		if(settings.getView() != null && !settings.getView().isExpanded()) {
			settings.getView().expand();
		}

		if (owLogger.isDebugEnabled()) {
			if (settings.getExpandedAssociations() != null
				&& settings.getExpandedAssociations().size() > 0) {
				owLogger.debug("List of Association settings for this operation:");
				for (AssociationSetting assoc : settings.getExpandedAssociations()) {
					owLogger.debug(Constants.Format.INDENT_STRING + assoc.toString());
				}
			}
		}

		if (settings.doPreClear()) {
			getDataStore().clear();
		}
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
		DataModel das = getDataModel();

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
            TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.DOMAIN);
            ObjectCreator oc = new ObjectCreator(
                settings,
                getDataStore(),
                typeMapper);		    

			BusinessObject from = oc.createDataObject(
				entity,
				oc.getShape().getType(entity.getClass()),
				null,
				null);
			oc.setRoot(from);
			BusinessObject bo = (BusinessObject)from.clone(settings);
			flushHandler.register(bo);
			generateVisual(bo, settings);

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
		if(inputObject == null) {
			if(settings.getEntityClass() != null) {
				return settings.getEntityClass();
			} else {
				return settings.getEntityType().getInstanceClass();
			}
		} else if(inputObject instanceof List && ((List)inputObject).size() > 0) {
			return ((List)inputObject).get(0).getClass();
		} else {
			return inputObject.getClass();
		}
	}

	@Override
	public Object dml(Settings settings) {
		configure(settings);

		AbstractOperation operation = null;
		if(settings.getAction() == AggregateAction.READ) {
			operation = new DenormalizedQueryOperation();
		} else {
			operation = new DenormalizedModifyOperation();
		}

		operation.execute(settings);
		return operation.getResult();
	}
	
	private MapperSide findSide(Object entity, Settings settings) {
        MapperSide side = MapperSide.EXTERNAL;
        if ((settings.getEntityType() != null && settings.getEntityType().isOpen()) ||
            getDataModel().getTypeMapper().isDomain(getEntityClass(entity, settings).getName())) 
        {
            side = MapperSide.DOMAIN;
        }
        
        return side;
	}

	private List<?> queryInternal (Object entity, Settings settings)
	{
		owLogger.debug("Performing query operation");
		checkAndSet(settings, entity);

		if (settings.doPreFlush())
			getDataStore().flush();

		MapperSide side = findSide(entity, settings);
        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(side);
        ObjectCreator oc = new ObjectCreator(
            settings,
            getDataStore(),
            typeMapper);            
		oc.setReadOnly(true);

		Type fromType = settings.getEntityType();
		if (fromType == null || !fromType.isOpen()) {
			fromType = oc.getType(getEntityClass(entity, settings));
			if(!(fromType instanceof EntityType)) {
				fromType = settings.getEntityType();
			}
		}
		BusinessObject from = oc.createDataObject(entity, fromType, null, null);
		if(from.getIdentifierValue() != null) {
		    settings.getParams().put(QueryFragment.ID_PARAMETER_NAME, from.getIdentifierValue());
		}

		// Get the narrowed class, if this is not an open type
		if (!(settings.getEntityType() != null && settings.getEntityType().isOpen())) {
			settings.initDowncast(getTypeNarrower(), entity, typeMapper);
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
	public View getView (String viewName)
	{
		return getDataModel().getShape().getView(viewName);
	}

	@Override
	public Object create (Object entity, Settings settings)
	{
		owLogger.debug("Performing create operation");
		checkAndSet(settings, entity);

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
		    MapperSide side = findSide(entity, settings);
		    TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(side);
			ObjectCreator oc = new ObjectCreator(
				settings,
				getDataStore(),
				typeMapper);

			BusinessObject from = oc.createDataObject(
				entity,
				getEntityType(entity, oc, settings),
				null,
				null);
			oc.setRoot(from);
			settings.setAction(AggregateAction.CREATE);

			BusinessObject bo = (BusinessObject)from.create(settings);
			flushHandler.register(bo);
			generateVisual(bo, settings);

		} finally {
			flushHandler.done();
		}

		return flushHandler.instance();
	}

	@Override
	public Object toDomain (Object entity, Settings settings)
	{
		owLogger.debug("Performing object conversion from External to Domain");
		checkAndSet(settings, entity);

        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.EXTERNAL);
        ObjectCreator oc = new ObjectCreator(
            settings,
            getDataStore(),
            typeMapper);   		

		BusinessObject from = oc.createDataObject(
			entity,
			getEntityType(entity, oc, settings),
			null,
			null);
		oc.setRoot(from);
		BusinessObject to = (BusinessObject)from.toDomain(settings);

		return to.getInstance();
	}

	protected Type getEntityType(Object entity, ObjectCreator oc, Settings settings) {
		Type type = null;
		// Collection type is used for bulk import/batching
		if(entity instanceof Collection) {
			type = oc.getType(entity.getClass());
		} else {
			type = oc.getType(entity.getClass(), settings.getEntityType());
		}

		return type;
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
				EntityType entityType = (EntityType)getShape(settings).getType(settings.getEntityClass());
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

		if (settings.doPreRefresh()) {
			getDataStore().refresh(entity);
		}

		MapperSide side = findSide(entity, settings);
        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(side);
        ObjectCreator oc = new ObjectCreator(
            settings,
            getDataStore(),
            typeMapper);  		
		oc.setReadOnly(true);

		BusinessObject from = oc.createDataObject(
			entity,
			//oc.getType(entity.getClass()),
			oc.getType(entity.getClass(), settings.getEntityType()),
			null,
			null);
		
		if (isWrapper) {
			ExtendedProperty property = (ExtendedProperty)((EntityType)from.getType()).getIdentifierProperty();
			property.setValue(from, wrapper);
		}

		from = from.load(settings);  // Get the persistent object

		//  perform read on it
		settings.setAction(AggregateAction.READ);
		BusinessObject to = (from != null) ? (BusinessObject)from.read(settings) : null;

		return to;
	}

	private void generateVisual(BusinessObject bo, Settings settings) {
		if(settings.isGenerateVisual()) {
			ObjectGraph og = bo.getObjectCreator().getObjectGraph();
			og.generateVisual(settings);
		}
	}

	@Override
	public Object read (Object entity, Settings settings)
	{
		BusinessObject to = readBO(entity, settings);

		generateVisual(to, settings);

		return (to != null) ? to.getNormalizedInstance(settings) : null;
	}

	@Override
	public Object toExternal (Object entity, Settings settings)
	{
		owLogger.debug("Performing object conversion from Domain to External");
		checkAndSet(settings, entity);

        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.EXTERNAL);
        ObjectCreator oc = new ObjectCreator(
            settings,
            getDataStore(),
            typeMapper);    		
		oc.setReadOnly(true);

		BusinessObject from = oc.createDataObject(
			entity,
			oc.getType(entity.getClass()),
			null,
			null);

		BusinessObject to = (BusinessObject)from.toExternal(settings);

		if(settings.isGenerateVisual()) {
			ObjectGraph og = to.getObjectCreator().getObjectGraph();
			og.generateVisual(settings);
		}

		return (to != null) ? to.getNormalizedInstance(settings) : null;
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

	@Override
	public Object update (Object entity, Settings settings)
	{
		owLogger.debug("Performing de operation");
		checkAndSet(settings, entity);

		if (settings.getAction() == null) {
			settings.setAction(AggregateAction.UPDATE);
		} else if (settings.getAction() != AggregateAction.MERGE
			&& settings.getAction() != AggregateAction.UPDATE) {
			throw new IllegalStateException("The default action should either be UPDATE or MERGE");
		}

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
		    MapperSide side = findSide(entity, settings);
	        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(side);
	        ObjectCreator oc = new ObjectCreator(
	            settings,
	            getDataStore(),
	            typeMapper);        		    

			BusinessObject from = oc.createDataObject(
				entity,
				getEntityType(entity, oc, settings),
				null,
				null);
			oc.setRoot(from);

			BusinessObject bo = from.update(settings);
			flushHandler.register(bo);
			generateVisual(bo, settings);

		} finally {
			flushHandler.done();
		}

		return flushHandler.instance();
	}

	private String[] getEntitiesArray(String propertyName) {
		String[] entities = new String[0];

		if (ApplicationConfiguration.config().containsKey(propertyName)) {
			String entitiesToMigrate = ApplicationConfiguration.config().getString(propertyName);
			if (entitiesToMigrate == null || "".equals(entitiesToMigrate.trim())) {
				// Nothing to migrate
				return entities;
			}

			String[] input = entitiesToMigrate.split(",");
			entities = Arrays.stream(input).map(String::trim).toArray(String[]::new);
		}

		return entities;
	}

	private String[] getEntitiesToMigrate()
	{
		return getEntitiesArray(Constants.Config.MIGRATE_ENTITIES);
	}

	private String[] getEntitiesInRelationshipMigration() {
		return getEntitiesArray(Constants.Config.MIGRATE_RELATIONSHIPS);
	}

	@Override
	public void migrate (AggregateManager source, Settings settings)
	{
		if (source == null) {
			throw new IllegalArgumentException("Source database needs to be provided");
		}

		configure(settings);
		MigrateOperation operation = getDataStore().getMigrateOperation(
			source,
			this,
			null);

		// Migrate just the entity including embedded objects
		List<EntityType> orderedTypes = operation.getEntitiesInOrder(
			getEntitiesToMigrate(),
			settings);
		Iterator<EntityType> iterator = orderedTypes.iterator();
		while (iterator.hasNext()) {
			EntityType entityType = iterator.next();
			System.out.println("****** Migrating entity: " + entityType.getName());

			// Create a new settings based on migrate view
			Settings batchSettings = operation.build(entityType, settings);
			performMigration(source, batchSettings, operation);
		}

		//NOTE: Specific consideration for migrating relationships
		// If entity is not root concrete class then only the relationships in the
		// entity is migrated
		// If entity is root concreate class, then all the relationships are migrated
		// This above rule helps us to avoid migrating duplicate relationships, due to
		// inheritance.

		// Migrate collection of embedded objects
		List<Settings> embeddedRelationships = operation.getEmbeddedRelationships(
			getEntitiesInRelationshipMigration(),
			settings);
		Iterator<Settings> relationshipIterator = embeddedRelationships.iterator();
		while (relationshipIterator.hasNext()) {
			Settings relSettings = relationshipIterator.next();
			System.out.println(
				"****** Migrating embedded relationship: " + relSettings.getView().getName());

			performMigration(source, relSettings, operation);
		}

		// Migrate collection of entities
		List<Settings> entityRelationships = operation.getEntityRelationships(
			getEntitiesInRelationshipMigration(),
			settings);
		relationshipIterator = entityRelationships.iterator();
		while (relationshipIterator.hasNext()) {
			Settings relSettings = relationshipIterator.next();
			System.out.println(
				"****** Migrating entity relationship: " + relSettings.getView().getName());

			performMigration(source, relSettings, operation);
		}
	}

	private void performMigration(AggregateManager source, Settings settings, MigrateOperation operation) {
		settings.setPersist(true);
		settings.setMainAction(AggregateAction.MIGRATE);
		settings.setSessionContext(settings.getSessionContext());

		// Create a new operation for each entity,so we don't mix different entities
		// in the same queue
		operation = getDataStore().getMigrateOperation(source, this, null);
		operation.execute(settings);
	}

	@Override
	public void delete (Object entity, Settings settings)
	{
		owLogger.debug("Performing delete operation");
		checkAndSet(settings, entity);

		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
            TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.EXTERNAL);
            ObjectCreator oc = new ObjectCreator(
                settings,
                getDataStore(),
                typeMapper);		    

			BusinessObject from = oc.createDataObject(
				entity,
				getEntityType(entity, oc, settings),
				null,
				null);
			oc.setRoot(from);

			// Get the persistent managed object
			from = from.load(settings);

			if(from != null) {
			    from.delete(settings);
			}

		} finally {
			flushHandler.done();
		}
	}

	@Override
	public List patch (List entity, List snapshot, Settings settings)
	{
		owLogger.debug("Performing update operation");
		checkAndSet(settings, entity);
		settings.setBaseline(true);

		if (snapshot != null && !(snapshot instanceof List)) {
			throw new RuntimeException(
				"snapshot should also be a list mirroring the input entity list");
		}

		List entityList = (List)entity;
		List snapshotList = (List)snapshot;
		for (int i = 0; i < entityList.size(); i++) {
			attach(
				entityList.get(i),
				snapshot == null ? null : snapshotList.get(i),
				settings);
		}

		// Now that the optimized persistence managed objects are in the cache
		// we are ready to update them
		Object updatedObjects = update(entity, settings);
		if(updatedObjects instanceof List) {
			return (List) updatedObjects;
		} else {
			List result = new ArrayList();
			result.add(updatedObjects);
			return result;
		}
	}

	private Shape getShape(Settings settings) {
		if(settings.getShape() == null) {
			return getDataModel().getShape();
		}

		return settings.getShape();
	}

	private void attach(Object entity, Object snapshot, Settings settings) {

		// attach it to the persistence layer
        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.EXTERNAL);
        ObjectCreator oc = new ObjectCreator(
            settings,
            getDataStore(),
            typeMapper);	    

		BusinessObject bo = oc.createDataObject(
			entity,
			//entityType,
			oc.getType(entity.getClass(), settings.getEntityType()),
			null,
			null);

		BusinessObject snapshotBO = null;
		if(snapshot != null) {
			snapshotBO = oc.createDataObject(
				snapshot,
				//entityType,
				oc.getType(entity.getClass(), settings.getEntityType()),
				null,
				null);
		}

		// If the object is not present in the cache then we will perform
		// a short-circuit attach, i.e., avoid going to the Database
		// NOTE: Not all persistence managers support this and in that case
		// an exception is thrown and the update method should be used instead.
		Object instance = getDataStore().getCached(
			bo.getDomainType().getInstanceClass(),
			bo.getIdentifierValue());
		if(instance == null) {
			getDataStore().attach(bo, snapshotBO, settings);
		}
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
				// We don't normalize the instance as it can have a performance impact
				// do it only if absolutely necessary
				result.add(((BusinessObject)obj).getInstance());
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
			List<Function> consolidated = new ArrayList<Function>(settings.getAdditionalFunctions());
			// Also Look for the filters in the view
			if (settings.getView().getFunction() != null) {
				consolidated.addAll(settings.getView().getFunction());
			}

			for (Function function : consolidated) {
				if (function.isOrderBy()) {
					if (lastObject instanceof BusinessObject) {
						nextTokenValues.put(
							function.getAttribute(),
							((BusinessObject)lastObject).get(function.getAttribute()));
					}
					else if (lastObject.getClass().isArray()) {
						nextTokenValues.put(
							function.getAttribute(),
							((Object[])lastObject)[colPositions.get(function.getAttribute())]);
					}
				}
			}
			settings.setNextToken(nextTokenValues);
		}

		return result;
	}

	public static class DefaultForeignKeyEnhancer implements ForeignKeyEnhancer
	{

		@Override public List<JDBCDataModel.ForeignKey> process (List<JDBCDataModel.ForeignKey> foreignKeys)
		{
			return foreignKeys;
		}
	}

	public static class AggregateManagerBuilder
	{

		private DataModelFactory nestedDasFactory;
		private boolean nestedAutoFlushNative;
		private Interceptor nestedInterceptor;
		private AssociationStrategy nestedAssociationStrategy;
		private DetailStrategy nestedDetailStrategy;
		private PersistenceType nestedPersistenceType;
		private TypeNarrower nestedTypeNarrower;
		private TypeMapper nestedTypeMapper;
		private int nestedViewVersion;

		public AggregateManagerBuilder dasFactory (DataModelFactory nestedDasFactory)
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

			this.nestedDasFactory = other.getDataModelFactory();
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
				result.setDataModelFactory(nestedDasFactory);

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
        ExcelExportImport exim = new ExcelExportImport(this);
        exim.exportDenormalized(outputStream, settings);
	}

	public void importCSV (String filePath, Settings settings) throws Exception
	{
		ExportImport exim = new CSVExportImport(this);
		Object obj = exim.importAggregate(filePath, settings);
		assert(obj != null);
	}

	@Override
	public void importDenormalized (InputStream is, Settings settings) throws
		IOException
	{
	    ExcelExportImport exim = new ExcelExportImport(this);
	    exim.importDenormalized(is, settings);
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

	@Override
	public void generate (String name, List<String> types, Settings settings)
	{
		TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.DOMAIN, name);

		// Generate the data
		configure(settings);
		(new DataGenerator(types, typeMapper, settings, getDataModelFactory())).execute();
	}
	

    @Override
    public void generateSameTX (String name, List<String> types, Settings settings)
    {
        TypeMapper typeMapper = getDataModel().getTypeMapper().newInstance(MapperSide.DOMAIN, name);

        // Generate the data
        configure(settings);
        (new DataGenerator(types, typeMapper, settings, getDataModelFactory(), true)).execute();
    }	
}
