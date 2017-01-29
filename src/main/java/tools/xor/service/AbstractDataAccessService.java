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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import tools.xor.AbstractType;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ExternalType;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SimpleType;
import tools.xor.SimpleTypeFactory;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.exception.MultipleClassForPropertyException;
import tools.xor.generator.Choices;
import tools.xor.generator.Generator;
import tools.xor.service.exim.ExcelExportImport;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoRE;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.State;
import tools.xor.util.Vertex;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.DirectedSparseGraph;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryViewProperty;

public abstract class AbstractDataAccessService implements DataAccessService {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	protected TypeMapper        typeMapper;	
	protected Map<String, Type> types = new ConcurrentHashMap<String, Type>();
	protected Map<String, Type> derivedTypes = new ConcurrentHashMap<String, Type>();	
	protected DASFactory        dasFactory;
	protected Map<String, AggregateView> views = new ConcurrentHashMap<String, AggregateView>();
	protected Map<Class<?>, Map<String, Object>> narrowedClassByView = new ConcurrentHashMap<Class<?>, Map<String,Object>>();

	private volatile boolean needsUpdate;
	
	public AbstractDataAccessService(DASFactory factory) {
		this.dasFactory = factory;
	}

	protected void addDerivedType(String className, Type type) {
		addType(className, type, derivedTypes);
	}

	@Override
	public void addOpenType(OpenType type) {
		if(types.containsKey(type.getName())) {
			throw new RuntimeException("A type with the same name exists, please choose a different name for the open type: " + type.getName());
		}
		type.setProperty(this);
		addType(type.getName(), type);

		Class<?> derivedClass = typeMapper.toExternal(type.getInstanceClass());
		if(derivedClass != null) {
			ExternalType derived = typeMapper.createExternalType((EntityType)type, derivedClass);
			derivedTypes.put(derived.getName(), derived);
			derived.setProperty(this);
			setBiDirectionOnDerivedType(derived);
		}
	}

	protected void addType(String className, Type type) {
		addType(className, type, types);
	}

	protected void addType(String className, Type type, Map<String, Type> typeMap) {
		typeMap.put(className, type);

		if(EntityType.class.isAssignableFrom(type.getClass())) {
			((EntityType)type).setDAS(this);
			String entityName = ((EntityType)type).getEntityName();
			if(entityName != null && !className.equals(entityName)) {
				if(typeMap.containsKey(entityName)) {
					if(!className.equals(typeMap.get(entityName).getInstanceClass().getName()))
						throw new RuntimeException("Type " + typeMap.get(entityName).getName() + " already exists for entityName: " + entityName);
				} else
					typeMap.put(entityName, type);
			}
		}
	}
	
	@Override
	public Type getType(Class<?> clazz) {

		Type result = getType(clazz.getName());

		// create a Type object for this class
		if(result == null) {
			//result = new SimpleType(clazz);
			result = SimpleTypeFactory.getType(clazz, this);

			addType(clazz.getName(), result);
		}

		return result;
	}

	/**
	 * This is a performance intensive method. So we now define subtypes as and when they are
	 * needed.
	 * For this to work we save the DataAccessService instance along with the EntityType.
	 */
	protected void defineSuperType (){
		List<EntityType> entityTypes = new ArrayList<EntityType>();
		for(Type type: getUniqueTypes()) {
			if(!EntityType.class.isAssignableFrom(type.getClass()))
				continue;
			
			EntityType extendedType = (EntityType) type;
			entityTypes.add(extendedType);
		}
		
		for(EntityType type: entityTypes) {
			
			// Initialize supertype if applicable
			Class<?> clazz = type.getInstanceClass();
			while(clazz != Object.class) {
				Type superType = getType(clazz.getSuperclass().getName());
				if(superType != null) {
					type.setSuperType((EntityType) superType);
					break;
				}
				clazz = clazz.getSuperclass();
			}
		}
	}

	@Override
	public Type getExternalType(Class<?> clazz) {
		Type result = derivedTypes.get(clazz.getName());

		// create a Type object for this class
		if (result == null) {
			//result = new SimpleType(clazz);
			result = SimpleTypeFactory.getType(clazz, this);
			addDerivedType(clazz.getName(), result);
		}

		return result;
	}	

	@Override
	public Type getType(String name) {
		return types.get(name);
	}

	@Override
	public Type getExternalType(String name) {
		return derivedTypes.get(name);
	}	

	@Override
	public TypeMapper getTypeMapper() {
		return typeMapper;
	}	

	@Override
	public List<Type> getTypes() {
		return new ArrayList<Type>(getUniqueTypes());
	}	

	@Override
	public void postProcess(Object newInstance, boolean autoWire) {
		if(newInstance == null)
			return;

		if(autoWire) {
			// Since we are manually creating the instance we need to autowire any dependencies
			dasFactory.injectDependencies(newInstance, null);
		}
	}

	protected void postProcess() {

		initPositionProperty();
		
		initDerived();

		initRootType();
		
//		initViews();
		
		initOrder();
	}
	
	protected void initOrder() {
		// State graph of all the entity types in topological order
		// Entity state graph is most likely a forest, so there is no root state
		StateGraph<State, Edge<State>> stateGraph = new StateGraph<State, Edge<State>>(null);
		for(Type type: getTypes()) {
			if(EntityType.class.isAssignableFrom(type.getClass())) {
				stateGraph.addVertex(new State(type, false));
			}
		}
		stateGraph.populateEdges();
		
		try {
			stateGraph.toposort();
		} catch(RuntimeException re) {
			logger.warn(GraphUtil.printGraph(stateGraph));
			throw re;
		}	
		
		stateGraph.orderTypes();
	}
	
	@Override
	public List<String> getViewNames() {
		List<String> result = new ArrayList<String>();
		
		for(AggregateView view: views.values()) {
			result.add(view.getName());
		}
		
		return result;
	}	
	
	@Override
	public AggregateView getView(String viewName) {
		return views.get(viewName);
	}	
	
	@Override
	public List<AggregateView> getViews() {
		return new ArrayList<AggregateView>(views.values());
	}
	
	@Override
	public AggregateView getView(EntityType type) {

		String viewName = AbstractType.getViewName(type);
		AggregateView result = views.get(viewName);

		if(result == null) {
			result = new AggregateView(type, viewName);
			result.setDAS(this);
			Set<String> paths = AggregatePropertyPaths.enumerate(type);
			
			result.setAttributeList(new ArrayList<String>(paths));
			
			DFAtoRE dfaRE = new DFAtoRE(type);
			result.addStateGraph(type, dfaRE.getFullStateGraph());

			views.put(viewName, result);
		}

		return result;
	}

	@Override
	public void addView(AggregateView view) {
		if(views.containsKey(view.getName())) {
			throw new RuntimeException("There is an existing view with this name: " + view.getName());
		}

		views.put(view.getName(), view);
	}
	
	@Override
	public AggregateView getBaseView(EntityType type) {

		String viewName = AbstractType.getBaseViewName(type);
		AggregateView result = views.get(viewName);

		if(result == null) {
			result = new AggregateView();
			result.setDAS(this);
			Set<String> paths = AggregatePropertyPaths.enumerateBase(type);
			
			result.setAttributeList(new ArrayList<String>(paths));
			result.setName(viewName);

			views.put(viewName, result);
		}

		return result;
	}		
	
	/**
	 * Expand view references
	 */
	private void denormalize() {
		checkViewCycles();
		
		for(AggregateView view: views.values()) {
			if(view.hasViewReference()) {
				view.expand();
			}
		}
	}
	
	@Override
	public void sync(AggregateManager am, Map<String, List<AggregateView>> avVersions) {

		for(List<AggregateView> versions: avVersions.values()) {
			AggregateView selected = versions.get(0);
			if(am.getViewVersion() < selected.getVersion()) {
				throw new IllegalStateException("Unable to find a view for the selected version: " + am.getViewVersion());
			}
			for(int i = versions.size()-1; i >= 0; i--) {
				if(versions.get(i).getVersion() <= am.getViewVersion()) {
					selected = versions.get(i);
					break;
				}
			}

			selected.setDAS(this);
			views.put(selected.getName(), selected);
		}
		denormalize();
		needsUpdate = true;
	}

	@Override
	public void refresh(TypeNarrower typeNarrower) {
		if(needsUpdate) {
			if(narrowedClassByView != null) {
				for(Class<?> clazz: narrowedClassByView.keySet())
					populateNarrowedClass(clazz, typeNarrower);
			}

			needsUpdate = false;
		}
	}	
	
	private void checkViewCycles() {
		DirectedGraph<AggregateView, Edge> dg = new DirectedSparseGraph<AggregateView, Edge>();
		
		// Add the views as state objects
		for(AggregateView view: views.values()) {
			Set<String> viewReferences = view.getViewReferences();
			for(String edge: viewReferences) {
				AggregateView start = view;
				AggregateView end = views.get(edge);
				
				// self loop
				if(start == end) {
					throw new IllegalStateException("Self-loop cycle found in view references: " + edge);
				}
				dg.addEdge(new Edge<AggregateView>(edge, start, end), start, end);
			}
		}
		
		List<List<Vertex>> cycles = dg.getCircuits();
		if(cycles.size() > 0) {
			throw new IllegalStateException("Cycle found in view references: " + GraphUtil.printCycles(cycles));
		}
			
	}

	@Override
	public Class<?> getNarrowedClass(Class<?> entityClass, String viewName) {
		Map<String, Object> narrowedByView = narrowedClassByView.get(entityClass);
		Object result = narrowedByView.get(viewName);
		
		if(logger.isDebugEnabled()) {
			logger.debug("AggregateViews#getNarrowedClass(entityClass: " + entityClass.getName()
					+ ", viewName: " + viewName);			
			for(Map.Entry<String, Object> entry: narrowedByView.entrySet()) {
				logger.debug("view: " + entry.getKey() + " narrowed class: " + ((Class<?>)entry.getValue()).getName());
			}
			if (result != null) {
				logger.debug("getNarrowedClass: " + ((Class<?>)result).getName());
			}
		}
		
		if(result == null)
			return null; // The entityClass is not applicable for this view

		if(Class.class.isAssignableFrom(result.getClass()))
			return (Class<?>) result;

		if(Set.class.isAssignableFrom(result.getClass())) {
			logger.error("Multiple sub-classes found, use a dynamic/custom TypeNarrower class to resolve this");
			return entityClass;
		}

		return null;
	}	
	
	@Override
	public void populateNarrowedClass(Class<?> superClass, TypeNarrower typeNarrower) {

		if(narrowedClassByView.get(superClass) != null) // already populated
			return;

		// Re-build in case the view has changed
		narrowedClassByView.put(superClass, new HashMap<String, Object>());

		// do the population for all views
		Map<String, Object> byViews = narrowedClassByView.get(superClass);
		nextView: for(AggregateView view: views.values()) {

			Class<?> narrowedClass = null;
			Set multipleNarrowedClass = new HashSet();
			for(String propertyPath: view.getAttributeList()) {
				String propertyName = QueryViewProperty.getRootName(propertyPath);
				Class<?> potentialNarrowedClass = typeNarrower.narrow(superClass, propertyName);
				if(potentialNarrowedClass == null)
					continue nextView; // This view property does not exist for this superClass or its sub-classes

				logger.debug("narrowedClass: " + (narrowedClass!=null ? narrowedClass.getName() : "null" )
						+ ", potentialNarrowedClass: " + potentialNarrowedClass.getName());

				if(narrowedClass == null) {
					narrowedClass = potentialNarrowedClass;
				} else {
					if(!narrowedClass.isAssignableFrom(potentialNarrowedClass))
						multipleNarrowedClass.add(potentialNarrowedClass);
					else
						narrowedClass = potentialNarrowedClass; // Update it
				}
			}

			if(multipleNarrowedClass.size() > 0) { // Might contain multiple classes or one or more MultipleClassForPropertyException objects
				multipleNarrowedClass.add(narrowedClass);

				Object commonClass = findCommonClass(multipleNarrowedClass);
				if(commonClass != null) {
					byViews.put(view.getName(), (Class<?>)commonClass);

				}
			} else
				byViews.put(view.getName(), narrowedClass);
		}
	}

	private Object findCommonClass(Set result) {
		// First set all the classes to be eligble
		Map<Class<?>, Boolean> eligibleClass = new HashMap<Class<?>, Boolean>();

		for(Object obj: result) {
			if(Class.class.isAssignableFrom(obj.getClass()))
				eligibleClass.put((Class<?>) obj, Boolean.TRUE);
			else if(MultipleClassForPropertyException.class.isAssignableFrom(obj.getClass())) {
				MultipleClassForPropertyException me = (MultipleClassForPropertyException) obj;
				for(Class<?> clazz: me.getMatchedClasses())
					eligibleClass.put((Class<?>) obj, Boolean.TRUE);					
			}
		}

		// markInEligible
		for(Object obj: result)
			markInEligible(eligibleClass, (Class<?>) obj);

		Set<Class<?>> multipleCommonClasses = new HashSet<Class<?>>();
		for(Map.Entry<Class<?>, Boolean> eligibleEntry: eligibleClass.entrySet()) {
			if(eligibleEntry.getValue())
				multipleCommonClasses.add(eligibleEntry.getKey());
		}

		if(multipleCommonClasses.size() == 0)
			return null;
		else if(multipleCommonClasses.size() == 1)
			return multipleCommonClasses.iterator().next();
		else
			return multipleCommonClasses;
	}

	private void markInEligible(Map<Class<?>, Boolean> eligibleClass, Object obj) {
		next: for( Map.Entry<Class<?>, Boolean> entry : eligibleClass.entrySet()) {
			if(!entry.getValue()) // already ineligible
				continue;

			if(Class.class.isAssignableFrom(obj.getClass())) {
				if( ((Class<?>) obj).isAssignableFrom(entry.getKey()))
					continue;

			} else if(MultipleClassForPropertyException.class.isAssignableFrom(obj.getClass())) {
				MultipleClassForPropertyException me = (MultipleClassForPropertyException) obj;

				for(Class<?> clazz: me.getMatchedClasses())
					if( clazz.isAssignableFrom(entry.getKey()))
						continue next;
			}
			eligibleClass.put(entry.getKey(), Boolean.FALSE);
		}
	}

	/**
	 * This can be a performance issue depending on how many entities there are in the system
	 */
	private void initViews() {
		for(Type type: types.values()) {
			if(EntityType.class.isAssignableFrom(type.getClass())) {
				getView((EntityType) type);
			}
		}
	}
	
	private Set<Type> getUniqueTypes() {
		return new HashSet<Type>(types.values());
	}
	
	private Set<Type> getUniqueDerivedTypes() {
		return new HashSet<Type>(derivedTypes.values());
	}

	private void initRootType() {
		for (Type type : getUniqueTypes()) {
			if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
				((AbstractType)type).initRootEntityType(this);
			}
		}
		for (Type type : getUniqueDerivedTypes()) {
			if (AbstractType.class.isAssignableFrom(type.getClass())) {
				((AbstractType)type).initRootEntityType(this);
			}
		}			
	}
	
	private void initPositionProperty() {
		for (Type type : getUniqueTypes()) {
			if (AbstractType.class.isAssignableFrom(type.getClass())) {
				((AbstractType)type).initPositionProperty();
			}
		}
	}

	private boolean isOpenDomainType(ExternalType derivedType) {
		// If the domain type is open, then we cannot
		// infer the properties. For e.g., an open domain type
		// does not have a Java class and is populated dynamically
		return derivedType.getDomainType().isOpen();
	}

	protected void initDerived() {
		for(Type type: getUniqueTypes()) {
			if(SimpleType.class.isAssignableFrom(type.getClass()) || type.isOpen()) {
				continue;
			}
			Class<?> derivedClass = typeMapper.toExternal(type.getInstanceClass());
			if(derivedClass != null) {
				Type derived = typeMapper.createExternalType((EntityType) type, derivedClass);
				derivedTypes.put(derived.getName(), derived);
			}
		}

		// init the derived properties
		for (Type type : getUniqueDerivedTypes()) {
			if (ExternalType.class.isAssignableFrom(type.getClass())) {
				ExternalType derivedType = (ExternalType) type;

				if(isOpenDomainType(derivedType)) {
					continue;
				}
				derivedType.setProperty(this);
			}
		}		

		for (Type type : getUniqueDerivedTypes()) {
			if (!type.isOpen() && ExternalType.class.isAssignableFrom(type.getClass())) {
				ExternalType derivedType = (ExternalType) type;

				if(isOpenDomainType(derivedType)) {
					continue;
				}
				setBiDirectionOnDerivedType(derivedType);
			}
		}			
	}

	public void addProperty (EntityType type, Property openProperty) {
		type.addProperty(openProperty);
		
		if(derivedTypes.containsKey(type.getName())) {
			ExternalType derived = (ExternalType) derivedTypes.get(type.getName());
			if(derived == null) {
				throw new RuntimeException("Cannot find the derived type for: " + type.getName());
			}
			Property derivedProperty = derived.defineProperty(this, openProperty);
			derived.addProperty(derivedProperty);
		}
	}

	protected void setBiDirectionOnDerivedType(ExternalType derivedType) {
		derivedType.setOpposite(this);
	}

	public QueryBuilder getQueryBuilder() {
		return new QueryBuilder();
	}

	public void initGenerators(InputStream is) {
		try {
			Workbook wb = WorkbookFactory.create(is);

			Sheet domainSheet = wb.getSheet(Constants.XOR.DOMAIN_TYPE_SHEET);
			if (domainSheet == null) {
				throw new RuntimeException("The Domain types sheet is missing");
			}

			for (int i = 1; i <= domainSheet.getLastRowNum(); i++) {
				Row row = domainSheet.getRow(i);
				String entityTypeName = row.getCell(1).getStringCellValue();
				String sheetName = row.getCell(0).getStringCellValue();

				EntityType entityType = (EntityType)getType(entityTypeName);
				Sheet entitySheet = wb.getSheet(sheetName);
				processDomainValues(entityType, entitySheet);
			}

		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	private void processDomainValues(EntityType entityType, Sheet entitySheet) throws
		ClassNotFoundException,
		NoSuchMethodException,
		IllegalAccessException,
		InvocationTargetException,
		InstantiationException
	{
		Map<String, Integer> headerMap = ExcelExportImport.getHeaderMap(entitySheet);

		// Process each property
		for(Map.Entry<String, Integer> entry: headerMap.entrySet()) {
			ExtendedProperty property = (ExtendedProperty)entityType.getProperty(entry.getKey());
			if( !property.isDataType() ) {
				// Only simple types supported for domain values
				continue;
			}

			Row row = entitySheet.getRow(1);
			Class generatorClass = Class.forName(row.getCell(entry.getValue()).getStringCellValue());
			List<String> list = new ArrayList<String>();
			for(int i = 2; i < entitySheet.getLastRowNum(); i++) {
				row = entitySheet.getRow(i);
				Cell cell = row.getCell(entry.getValue());

				String value = null;
				if (cell != null) {
					try {
						if (cell.getStringCellValue() != null) {
							value = cell.getStringCellValue();
						}
					}
					catch (Exception e) {
						value = Double.toString(cell.getNumericCellValue());
					}
				}
				if(value == null) {
					break;
				}
				list.add(value);
			}

			String[] values = list.toArray(new String[list.size()]);
			SimpleType simpleType = (SimpleType)property.getType();
			Constructor cd = generatorClass.getConstructor(String[].class);
			property.setGenerator((Generator)cd.newInstance((Object)values));
		}
	}
}
