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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import tools.xor.AbstractType;
import tools.xor.AggregateAction;
import tools.xor.AssociationSetting;
import tools.xor.BusinessObject;
import tools.xor.DefaultTypeMapper;
import tools.xor.DefaultTypeNarrower;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.MapperDirection;
import tools.xor.MutableBO;
import tools.xor.Settings;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.core.Interceptor;
import tools.xor.custom.AssociationStrategy;
import tools.xor.custom.DefaultAssociationStrategy;
import tools.xor.custom.DefaultDetailStrategy;
import tools.xor.custom.DetailStrategy;
import tools.xor.util.PersistenceType;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;
import tools.xor.util.excel.ExcelExporter;
import tools.xor.view.AggregateView;
import tools.xor.view.AggregateViewFactory;
import tools.xor.view.TypeVersion;

@Component
public class AggregateManager implements Xor {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);
	private static final Logger sgLogger = LogManager.getLogger(Constants.Log.STATE_GRAPH);

	private DASFactory              dasFactory;	
	private List<String>            viewFiles;           // A list of files to read the view information from
	private List<TypeVersion>       typeVersions;        // A list of the types and the versions it is valid it
	private int                     viewVersion = TypeVersion.MIN_VERSION_VALUE; 
	private MetaModel               metaModel;           // Meta model exposed to the user
	private boolean                 autoFlushNative;     // Flag to indicate if a flush should be issued before a native query is executed
	private Interceptor             interceptor;         // A user provided interceptor to be notified of framework events
	private AssociationStrategy     associationStrategy; // Determine if any extra object need to be processed
	private DetailStrategy          detailStrategy;      // Allows the user to define a custom detail strategy
	private PersistenceType persistenceType;
	private TypeNarrower            typeNarrower;
	private String                  viewsDirectory;

	// This is maintained per thread, because the persistence orchestrator holds
	// the session that is thread specific
	private ThreadLocal<PersistenceOrchestrator> persistenceOrchestrator = new ThreadLocal<PersistenceOrchestrator>();
	
	// Custom type mapper to map between differnt types. 
    // NOTE: The user is restricted from changing the path specified in the MetaModel.
    // A truly custom solution is to provide a map between the meta model path and the custom path.
    // Configured with the DefaultTypeMapper
	private TypeMapper              typeMapper;          
	           
	
	private void reloadViews() {
		// Load the default view file
		(new AggregateViewFactory()).load(this);

		// Load the configured view files
		syncViews();
	}

	@PostConstruct
	protected void init() {

		if(associationStrategy == null)
			associationStrategy = new DefaultAssociationStrategy();

		if(detailStrategy == null)
			detailStrategy = new DefaultDetailStrategy();		
		
		if(typeMapper == null)
			typeMapper = new DefaultTypeMapper();

		if(typeNarrower == null) {
			typeNarrower = new DefaultTypeNarrower();
			typeNarrower.setAggregateManager(this);
		}

		if(dasFactory != null) {
			metaModel = new MetaModel(this);
			dasFactory.setAggregateManager(this);
		} else {
			logger.error("DASFactory instance is not set on the AggregateManager instance with the default name.");
		}	
		
		reloadViews();	
	}

	public DASFactory getDasFactory() {
		return dasFactory;
	}

	public void setDasFactory(DASFactory dasFactory) {
		this.dasFactory = dasFactory;
	}

	public MetaModel getMetaModel() {
		return metaModel;
	}

	public void setMetaModel(MetaModel metaModel) {
		this.metaModel = metaModel;
	}

	private void syncViews() {
		if(viewFiles == null)
			return;

		// Load the views
		AggregateViewFactory viewFactory = new AggregateViewFactory();

		for(String viewFile: viewFiles)
			viewFactory.load(viewFile, this);		
	}

	public TypeNarrower getTypeNarrower() {
		return typeNarrower;
	}

	public void setTypeNarrower(TypeNarrower typeNarrower) {
		this.typeNarrower = typeNarrower;
		typeNarrower.setAggregateManager(this);
	}	
	
	public String getViewsDirectory() {
		return viewsDirectory;
	}

	public void setViewsDirectory(String viewsDirectory) {
		this.viewsDirectory = viewsDirectory;
	}	

	public AssociationStrategy getAssociationStrategy() {
		return associationStrategy;
	}

	public void setAssociationStrategy(AssociationStrategy associationStrategy) {
		this.associationStrategy = associationStrategy;
	}	
	
	public void setDetailStrategy(DetailStrategy detailStrategy) {
		this.detailStrategy = detailStrategy;
	}

	public PersistenceType getPersistenceType () {
		return persistenceType;
	}

	public void setPersistenceType (PersistenceType persistenceType) {
		this.persistenceType = persistenceType;
	}	

	public List<String> getViewFiles() {
		return viewFiles;
	}

	public void setViewFiles(List<String> viewFiles) {
		this.viewFiles = viewFiles;
	}

	public List<TypeVersion> getTypeVersions() {
		return typeVersions;
	}

	public void setTypeVersions(List<TypeVersion> typeVersions) {
		this.typeVersions = typeVersions;
	}

	public boolean isAutoFlushNative() {
		return autoFlushNative;
	}

	public void setAutoFlushNative(boolean autoFlushNative) {
		this.autoFlushNative = autoFlushNative;
	}

	public Interceptor getInterceptor() {
		return interceptor;
	}

	public void setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	public TypeMapper getTypeMapper() {
		return typeMapper;
	}

	public void setTypeMapper(TypeMapper customTypeMapper) {
		this.typeMapper = customTypeMapper;
	}	

	public PersistenceOrchestrator getPersistenceOrchestrator() {
		return persistenceOrchestrator.get();
	}

	/**
	 * This method sets the current persistence orchestrator, irrespective
	 * of whether the thread had one previously. This way we do
	 * not have to bother about clearing an obsolete persistence orchestrator
	 * 
	 * @param po
	 */
	public void setPersistenceOrchestrator(
			PersistenceOrchestrator po) {
		this.persistenceOrchestrator.set(po);
	}	

	public DataAccessService getDAS() {
		return dasFactory.create();
	}

	public Settings getSettings() {
		Settings result = new Settings();
		result.setAssociationStrategy(associationStrategy);

		return result;
	}
	
	private class FlushHandler {
		Object oldFlushMode;
		BusinessObject businessObject;
		Settings settings;
		
		FlushHandler(Settings settings) 
		{
			this.settings = settings;
			oldFlushMode = getPersistenceOrchestrator().disableAutoFlush();
		}
		
		void register(BusinessObject bo) {
			this.businessObject = bo;
		}
		
		Object instance() {
			return businessObject.getInstance();
		}
		
		Object done() {
			try {
				return (businessObject != null) ? businessObject.getInstance() : null;
			}
			finally {
				getPersistenceOrchestrator().setFlushMode(oldFlushMode);
				if(settings.doPostFlush()) {
					getPersistenceOrchestrator().flush();
				}
			}
		}
	}

	private void checkAndSet(Settings settings, Object inputObject) {
		Class<?> inputObjectClass = getEntityClass(inputObject, settings);
		
		
		if(getPersistenceOrchestrator() == null)
			setPersistenceOrchestrator(dasFactory.getPersistenceOrchestrator(settings.getSessionContext()));	
		
		if(settings.getAssociationStrategy() == null)
			settings.setAssociationStrategy(associationStrategy);
		
		if(settings.getEntityType() == null) {
			
			// Try to infer the type from the input object
			DataAccessService das = getDAS();

			Class<?> domainClass = settings.getEntityClass();
			if(domainClass == null && inputObjectClass != null) {
				try {
					domainClass = das.getTypeMapper().toDomain(inputObjectClass);
				} catch(UnsupportedOperationException e) {
					domainClass = null;
				}
			}
			if(domainClass == null) {
				throw new RuntimeException("Unable to identify the type on which to perform the operation. Need to explicitly specify the domain type.");
			}
			settings.setEntityType(das.getType(domainClass.getName()));
			settings.init(this); // populate the view from the type if necessary
			owLogger.debug("Operation on Entity Type: " + settings.getEntityType().getName());
			if(owLogger.isTraceEnabled()) {
				owLogger.trace( getStackTrace(10));
			}
			
			if(sgLogger.isDebugEnabled()) {
				if(settings.getView().getStateGraph() != null) {
					if(sgLogger.isTraceEnabled()) {
						sgLogger.trace( getStackTrace(10));
					}
					sgLogger.debug("State graph of Entity: " + settings.getEntityType().getName() + " for view: " + settings.getView().getName());
					sgLogger.debug(settings.getView().getStateGraph((EntityType) settings.getEntityType()).dumpState());
				}
			}
		}
		
		if(owLogger.isDebugEnabled()) {
			if(settings.getAssociationSettings() != null && settings.getAssociationSettings().size() > 0) {
				owLogger.debug("List of Association settings for this operation:");
				for(AssociationSetting assoc: settings.getAssociationSettings()) {
					owLogger.debug(Constants.Format.INDENT_STRING + assoc.toString());
				}
			}
		}
		
		if(settings.doPreClear())
			getPersistenceOrchestrator().clear();
	}
	
	private String getStackTrace(int numStackElements) {
		Exception e = new Exception();
		StringBuilder sb = new StringBuilder();
		
		int skip = 2; // skip first 2
		for (StackTraceElement element : e.getStackTrace()) {
			if(skip-- > 0) {
				continue;
			} else if(skip+numStackElements == 0) {
				break;
			}
			sb.append(element.toString());
			sb.append("\r\n");
		}
		return sb.toString();
	}

	@Override
	public Object clone(Object entity, Settings settings) {
		owLogger.debug("Performing clone operation");
		checkAndSet(settings, entity);
		DataAccessService das = getDAS();
		
		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);

		try {
			ObjectCreator oc = new ObjectCreator(das, getPersistenceOrchestrator(), MapperDirection.DOMAINTODOMAIN);
			BusinessObject from = oc.createDataObject(entity, (EntityType) das.getType(entity.getClass()), null, null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject) from.clone(settings));	
			
		} finally {
			flushHandler.done();
		}
		
		return flushHandler.instance();
	}

	private BusinessObject queryOne(Object entity, Settings settings) {
		return (BusinessObject) queryInternal(entity, settings).get(0);
	}
	
	private Class<?> getEntityClass(Object inputObject, Settings settings) {
		return inputObject == null ? settings.getEntityType().getInstanceClass() : inputObject.getClass();
	}

	private List<?> queryInternal(Object entity, Settings settings) {
		owLogger.debug("Performing query operation");
		checkAndSet(settings, entity);

		DataAccessService das = getDAS();
		if(settings.doPreFlush())
			getPersistenceOrchestrator().flush();

		MapperDirection direction = MapperDirection.EXTERNALTOEXTERNAL;
		if(das.getTypeMapper().isDomain( getEntityClass(entity, settings) ))
			direction = MapperDirection.DOMAINTOEXTERNAL;

		ObjectCreator oc = new ObjectCreator(das, getPersistenceOrchestrator(), direction);
		BusinessObject from = oc.createDataObject(entity, (EntityType) oc.getType( getEntityClass(entity, settings) ), null, null);

		// Get the narrowed class
		settings.initNarrowClass(getTypeNarrower(), entity, typeMapper);

		List<?> dataObjects = from.query(settings);

		return dataObjects;
	}

	public void linkBackPointer(Object entity) {
		ObjectCreator oc = new ObjectCreator(getDAS(), getPersistenceOrchestrator(), MapperDirection.EXTERNALTOEXTERNAL);
		MutableBO dataObject = (MutableBO) oc.createDataObject(entity, (EntityType) oc.getType(entity.getClass()), null, null);
		oc.setShare(true);		
		dataObject.createAggregate();
		dataObject.linkBackPointer();
	}

	public int getViewVersion() {
		return viewVersion;
	}

	public void setViewVersion(int version) {
		this.viewVersion = version;
	}

	/**
	 * Convenience method to quickly get access to the view meta object
	 *
	 * @param viewName
	 * @return
	 */
	public AggregateView getView(String viewName) {
		return getDAS().getView(viewName);
	}

	@Override
	public Object create(Object entity, Settings settings) {
		owLogger.debug("Performing create operation");
		checkAndSet(settings, entity);	
		
		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);
	
		try {
			ObjectCreator oc = new ObjectCreator(getDAS(), getPersistenceOrchestrator(), MapperDirection.EXTERNALTODOMAIN);
			BusinessObject from = oc.createDataObject(entity, (EntityType) oc.getType(entity.getClass(), settings.getEntityType()), null, null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject) from.create(settings));	
		
		} finally {
			flushHandler.done();
		}
		
		return flushHandler.instance();
	}

	@Override
	public <T> Object create(Object inputObject, Class<T> entityClass) {
		return create(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}

	@Override
	public Object read(Object entity, Settings settings) {
		owLogger.debug("Performing read operation");
		
		boolean isWrapper = false;
		Object wrapper = null;
		if(AbstractType.isWrapperType(entity.getClass())) {
			wrapper = entity;
			if( settings.getEntityClass() == null ) {
				throw new IllegalArgumentException("The entity class needs to be provided");
			} else {
				EntityType entityType = (EntityType) getDAS().getType(settings.getEntityClass());
				if(entityType != null) {
					entity = ClassUtil.newInstance(settings.getEntityClass());
					isWrapper = true;
				} else {
					throw new IllegalArgumentException("The entity class " + settings.getEntityClass().getName() +
							" does not refer to a domain class");
				}
			}
		}
		
		checkAndSet(settings, entity);
		
		if(settings.doPreRefresh())
			getPersistenceOrchestrator().refresh(entity);		
		
		ObjectCreator oc = new ObjectCreator( getDAS(), getPersistenceOrchestrator(), MapperDirection.DOMAINTOEXTERNAL);
		BusinessObject from = oc.createDataObject(entity, oc.getType(entity.getClass()), null, null);
		if(isWrapper) {
			ExtendedProperty property = (ExtendedProperty) ((EntityType)from.getType()).getIdentifierProperty();
			property.setValue(from, wrapper);
		}
		from = from.load(settings);  // Get the persistent object
	
		//  perform read on it
		BusinessObject to = (BusinessObject) from.read(settings);		
	
		return to.getNormalizedInstance(settings);
	}

	@Override
	public <T> Object read(Object inputObject, Class<T> entityClass) {
		return read(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}

	@Override
	public Object update(Object entity, Settings settings) {
		owLogger.debug("Performing update operation");
		checkAndSet(settings, entity);		
	
		if(settings.getAction() != AggregateAction.MERGE && settings.getAction() != AggregateAction.UPDATE)
			throw new IllegalStateException("The default action should either be UPDATE or MERGE");
		
		// Not necessary as we manage the back-pointers
		FlushHandler flushHandler = new FlushHandler(settings);
	
		try {
			ObjectCreator oc = new ObjectCreator(getDAS(), getPersistenceOrchestrator(), MapperDirection.EXTERNALTODOMAIN);
			BusinessObject from = oc.createDataObject(entity, (EntityType) oc.getType(entity.getClass(), settings.getEntityType()), null, null);
			oc.setRoot(from);
			flushHandler.register((BusinessObject) from.update(settings));	
			
		} finally {
			flushHandler.done();
		}
		
		return flushHandler.instance();
	}

	@Override
	public <T> Object update(Object inputObject, Class<T> entityClass) {
		return update(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}

	@Override
	public void delete(Object entity, Settings settings) {
		// TODO: 
	}

	@Override
	public <T> void delete(Object inputObject, Class<T> entityClass) {
		delete(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}

	@Override
	public Object patch(Object entity, Settings settings) {
		owLogger.debug("Performing update operation");
		checkAndSet(settings, entity);				
		settings.setBaseline(true);
		
		BusinessObject o = queryOne(entity, settings);
		System.out.println("TYpe: " + o.getType().getName() + ", instance: " + o.getInstance());
		
		// attach it to the persistence layer
		getPersistenceOrchestrator().attach(o, settings.getView());
		System.out.println("patch object: " + o.getInstance());
		
		// update the just attached object with the original object
		return update(entity, settings);		
	}

	@Override
	public <T> Object patch(Object inputObject, Class<T> entityClass) {
		return patch(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}

	@Override
	public List<?> query(Object entity, Settings settings) {
		List<Object> result = new ArrayList<Object>();
		List<?> dataObjects = queryInternal(entity, settings);
	
		for(Object obj: dataObjects) {
			if(!settings.isDenormalized())
				result.add( ((BusinessObject)obj).getNormalizedInstance(settings) );
			else
				result.add(obj);
		}
	
		return result;
	}

	@Override
	public <T> List<?> query(Object inputObject, Class<T> entityClass) {
		return query(inputObject, new Settings.SettingsBuilder().entityClass(entityClass).build());
	}
	
	public static class AggregateManagerBuilder
	{

		private DASFactory              nestedDasFactory;	              
		private boolean                 nestedAutoFlushNative;     
		private Interceptor             nestedInterceptor;         
		private AssociationStrategy     nestedAssociationStrategy; 
		private DetailStrategy          nestedDetailStrategy;      
		private PersistenceType nestedPersistenceType;
		private TypeNarrower            nestedTypeNarrower;
		private TypeMapper              nestedTypeMapper;          		
		private int                     nestedViewVersion;	
		
		public AggregateManagerBuilder dasFactory(DASFactory nestedDasFactory) {
			this.nestedDasFactory = nestedDasFactory;
			return this;
		}
		public AggregateManagerBuilder autoFlushNative(boolean nestedAutoFlushNative) {
			this.nestedAutoFlushNative = nestedAutoFlushNative;
			return this;
		}
		public AggregateManagerBuilder interceptor(Interceptor nestedInterceptor) {
			this.nestedInterceptor = nestedInterceptor;
			return this;
		}
		public AggregateManagerBuilder associationStrategy(
				AssociationStrategy nestedAssociationStrategy) {
			this.nestedAssociationStrategy = nestedAssociationStrategy;
			return this;
		}
		public AggregateManagerBuilder detailStrategy(DetailStrategy nestedDetailStrategy) {
			this.nestedDetailStrategy = nestedDetailStrategy;
			return this;
		}
		public AggregateManagerBuilder persistenceType(PersistenceType nestedPersistenceType) {
			this.nestedPersistenceType = nestedPersistenceType;
			return this;
		}
		public AggregateManagerBuilder typeNarrower(TypeNarrower nestedTypeNarrower) {
			this.nestedTypeNarrower = nestedTypeNarrower;
			return this;
		}
		public AggregateManagerBuilder typeMapper(TypeMapper nestedTypeMapper) {
			this.nestedTypeMapper = nestedTypeMapper;
			return this;
		}
		public AggregateManagerBuilder viewVersion(int nestedViewVersion) {
			this.nestedViewVersion = nestedViewVersion;
			return this;
		}		
		
		public AggregateManager build() {
			AggregateManager result = new AggregateManager();
			
			if(nestedDasFactory != null)
				result.setDasFactory(nestedDasFactory);
			
			result.setAutoFlushNative(nestedAutoFlushNative);
			
			if(nestedInterceptor != null)
				result.setInterceptor(nestedInterceptor);
			
			if(nestedAssociationStrategy != null)
				result.setAssociationStrategy(nestedAssociationStrategy);
			
			if(nestedDetailStrategy != null)
				result.setDetailStrategy(nestedDetailStrategy);
			
			if(nestedPersistenceType != null)
				result.setPersistenceType(nestedPersistenceType);
			
			if(nestedTypeNarrower != null)
				result.setTypeNarrower(nestedTypeNarrower);
			
			if(nestedTypeMapper != null)
				result.setTypeMapper(nestedTypeMapper);
			
			result.setViewVersion(nestedViewVersion);
			
			result.init();
			
			return result;
		}
	}

	@Override
	public void excelExport(OutputStream outputStream, Settings settings) {
		
		// Make sure this is a denormalized query
		settings.setDenormalized(true);
		List<?> result = query(null, settings);
		
		// Currently only address one sheet, additional sheets will handle
		// dependencies
		ExcelExporter e = new ExcelExporter(outputStream, settings);

		// The first row is the column names
		e.writeRow(result.get(0));
		
		for(int i = 1; i < result.size(); i++) {
			e.writeRow(result.get(i));
		}
		// TODO: add validation support
		e.writeValidations();
		
		e.finish();
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
