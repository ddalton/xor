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
import tools.xor.generator.Generator;
import tools.xor.generator.LinkedChoices;
import tools.xor.generator.Lot;
import tools.xor.service.exim.ExcelExportImport;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.ApplicationConfiguration;
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
	protected DASFactory        dasFactory;

	protected static final String DEFAULT_SHAPE = "_DEFAULT_";
	protected Map<String, Shape> shapes; // Contains all the initialized shapes
	
	public AbstractDataAccessService(DASFactory factory) {
		this.dasFactory = factory;
		this.shapes = new HashMap<>();
		shapes.put(DEFAULT_SHAPE, new Shape(DEFAULT_SHAPE, null, this));
	}

	@Override
	public Shape getShape() {
		return shapes.get(DEFAULT_SHAPE);
	}

	protected Shape getOrCreateShape(String name) {
		Shape shape = shapes.get(name);
		if(shape == null) {
			shape = new Shape(name, shapes.get(DEFAULT_SHAPE), this);
			shapes.put(name, shape);
		}

		return shape;
	}

	@Override
	public void addOpenType(OpenType type) {
		getShape().addOpenType(type);
	}
	
	@Override
	public Type getType(Class<?> clazz) {

		return getShape().getType(clazz);
	}

	/**
	 * This is a performance intensive method. So we now define subtypes as and when they are
	 * needed.
	 * For this to work we save the DataAccessService instance along with the EntityType.
	 *
	 * @param shape of the type being defined
	 */
	protected void defineSuperType (Shape shape){
		List<EntityType> entityTypes = new ArrayList<EntityType>();
		for(Type type: shape.getUniqueTypes()) {
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
		return getShape().getExternalType(clazz);
	}

	@Override
	public Type getType(String name) {
		return getShape().getType(name);
	}

	@Override
	public Type getExternalType(String name) {
		return getShape().getExternalType(name);
	}	

	@Override
	public TypeMapper getTypeMapper() {
		return typeMapper;
	}	

	@Override
	public List<Type> getTypes() {
		return new ArrayList<Type>(getShape().getUniqueTypes());
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

	protected void postProcess(Shape shape) {

		initPositionProperty(shape);
		
		initDerived(shape);

		initRootType(shape);
		
//		initViews(shape);

		initOrder(shape);
	}
	
	protected void initOrder(Shape shape) {
		// State graph of all the entity types in topological order
		// Entity state graph is most likely a forest, so there is no root state
		StateGraph<State, Edge<State>> stateGraph = new StateGraph<State, Edge<State>>(null, shape);
		for(Type type: getTypes()) {
			if(EntityType.class.isAssignableFrom(type.getClass())) {
				stateGraph.addVertex(new State(type, false));
			}
		}

		stateGraph.populateEdges();

		if (!ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_SKIP)
			|| !ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_SKIP)) {
			try {
				stateGraph.toposort();
			}
			catch (RuntimeException re) {
				throw re;
			}

			stateGraph.orderTypes();

			// Print out the graph if so configured
			if (ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_VISUAL)
				&& ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_VISUAL)) {
				Settings settings = new Settings();
				settings.setGraphFileName("ApplicationStateGraph_" + shape.getName() + ".png");
				stateGraph.generateVisual(settings);
			}
		}
	}
	
	@Override
	public List<String> getViewNames() {
		return getShape().getViewNames();
	}	
	
	@Override
	public AggregateView getView(String viewName) {
		return getShape().getView(viewName);
	}	
	
	@Override
	public List<AggregateView> getViews() {
		return getShape().getViews();
	}
	
	@Override
	public AggregateView getView(EntityType type) {
		return getShape().getView(type);
	}

	@Override
	public void addView(AggregateView view) {
		getShape().addView(view);
	}
	
	@Override
	public AggregateView getBaseView(EntityType type) {
		return getShape().getBaseView(type);
	}
	
	@Override
	public void sync(AggregateManager am, Map<String, List<AggregateView>> avVersions) {
		for(Shape shape: shapes.values()) {
			shape.sync(am, avVersions);
		}
	}

	@Override
	public void refresh(TypeNarrower typeNarrower) {
		getShape().refresh(typeNarrower);
	}

	@Override
	public Class<?> getNarrowedClass(Class<?> entityClass, String viewName) {
		return getShape().getNarrowedClass(entityClass, viewName);
	}	
	
	@Override
	public void populateNarrowedClass(Class<?> superClass, TypeNarrower typeNarrower) {
		getShape().populateNarrowedClass(superClass, typeNarrower);
	}

	/**
	 * This can be a performance issue depending on how many entities there are in the system
	 */
	private void initViews(Shape shape) {

		for(Type type: shape.getUniqueTypes()) {
			if(EntityType.class.isAssignableFrom(type.getClass())) {
				getView((EntityType) type);
			}
		}
	}

	private void initRootType(Shape shape) {
		shape.initRootType();
	}

	private void initPositionProperty(Shape shape) {
		shape.initPositionProperty();
	}

	protected void initDerived(Shape shape) {
		shape.initDerived();
	}

	public void addProperty (EntityType type, Property openProperty) {
		getShape().addProperty(type, openProperty);
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

		Lot lot = null;
		for(Map.Entry<String, Integer> entry: headerMap.entrySet()) {
			// Process each property
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
			Constructor cd = generatorClass.getConstructor(String[].class);

			Generator gen = (Generator)cd.newInstance((Object)values);
			if(gen instanceof LinkedChoices) {
				if(lot == null) {
					lot = new Lot(((LinkedChoices)gen).getValues().length);
				}
				((LinkedChoices)gen).setLot(lot);
			}
			property.setGenerator(gen);
		}
	}
}
