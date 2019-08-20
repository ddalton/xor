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
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import tools.xor.AbstractProperty;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.generator.Generator;
import tools.xor.generator.LinkedChoices;
import tools.xor.generator.Lot;
import tools.xor.generator.RandomSubset;
import tools.xor.service.exim.ExcelExportImport;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryTransformer;
import tools.xor.view.View;

public abstract class AbstractDataAccessService implements DataAccessService {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	protected TypeMapper        typeMapper;
	protected DASFactory        dasFactory;

	protected Map<String, Shape> shapes; // Contains all the initialized shapes
	private ThreadLocal<Shape> overriddenShape = new ThreadLocal<Shape>(); // temporarily overridden by user

	public AbstractDataAccessService(DASFactory factory, TypeMapper typeMapper) {
		this.dasFactory = factory;
		this.typeMapper = typeMapper;
		this.shapes = new HashMap<>();
		shapes.put(DEFAULT_SHAPE, new Shape(DEFAULT_SHAPE, null, this));
	}

	@Override
	public Shape addShape(String name) {
		return this.addShape(name, null);
	}

	@Override
	public Shape getShape() {
		// Needs to be always present to allow user overrides
		if(hasOverriddenShape()) {
			return getOverriddenShape();
		}

		return shapes.get(DEFAULT_SHAPE);
	}

	@Override
	public Shape getShape(String name) {
		return shapes.get(name);
	}

	@Override
	public Shape getOwner(EntityType entityType) {
		Shape result = getShape();

		Shape parent = null;
		do {
			parent = result.getParent();
			if(result.hasType(entityType)) {
				return result;
			}
			result = parent;
		}while (result.getShapeStrategy() == Shape.ShapeStrategy.SHARED && parent != null);

		return null;
	}

	protected boolean hasOverriddenShape() {
		return overriddenShape.get() != null;
	}

	protected Shape getOverriddenShape() {
		return overriddenShape.get();
	}

	public AggregateManager getAggregateManager() {
		return this.dasFactory.getAggregateManager();
	}

	/**
	 * Override the current shape
	 * @param name for the overridden shape
	 * @param reuse true if reuse shape with same name
	 * @return the new shape that overrides the existing shape
	 */
	public Shape overrideShape(String name, boolean reuse) {
		if(shapes.containsKey(name) && !reuse) {
			throw new RuntimeException("A Shape object already exists with the name: " + name);
		}
		Shape result = getOrCreateShape(name, getShape());
		overriddenShape.set(result);

		return result;
	}

	protected void removeShapeOverride(boolean delete) {
		if(delete) {
			shapes.remove(overriddenShape.get().getName());
		}
		overriddenShape.remove();
	}

	/**
	 * Return an existing shape with the provided name or create one if not present.
	 * @param name of the shape
	 * @param parent of the shape
	 * @return the newly created shape
	 */
	public Shape getOrCreateShape (String name, Shape parent) {
		Shape shape = shapes.get(name);
		if(shape == null) {
			shape = new Shape(name, parent, this);
			shapes.put(name, shape);
		}

		return shape;
	}

	@Override
	public void removeShape(String name) {
		shapes.remove(name);
	}

	protected Shape getOrCreateShape (String name) {
		return getOrCreateShape(name, null);
	}

	@Override
	public Type getType(Shape shape, Class<?> clazz, Type type) {
		return clazz != null ? shape.getType(clazz) : type;
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
				Type superType = shape.getType(clazz.getSuperclass().getName());
				if(superType != null) {
					type.setSuperType((EntityType) superType);
					break;
				}
				clazz = clazz.getSuperclass();
			}
		}
	}

	@Override
	public TypeMapper getTypeMapper() {
		return typeMapper;
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

	protected void postProcess(Shape shape, SchemaExtension extension) {

		initPositionProperty(shape);

		// Extend the schema if applicable
		if(extension != null) {
			extension.extend(shape);
		}
		
		initExternal(shape);

		initRootType(shape);
		
//		initViews(shape);

		initOrder(shape);

		initEnd(shape);
	}

	/**
	 * Should be invoked as the final step of the Shape object construction.
	 * @param shape object being constructed
	 */
	protected void initEnd(Shape shape) {
		shape.initEnd();

		shape.setBuildFinished(true);
	}
	
	protected void initOrder(Shape shape) {
		shape.createOrderedGraph();
	}
	
	@Override
	public void sync(Map<String, List<AggregateView>> avVersions) {
		AggregateManager am = getAggregateManager();
		for(Shape shape: shapes.values()) {
			shape.sync(am, avVersions);
		}
	}

	@Override
	public Class<?> getNarrowedClass(Shape shape, Class<?> entityClass, String viewName) {
		return shape.getNarrowedClass(entityClass, viewName);
	}	
	
	@Override
	public void populateNarrowedClass(Shape shape, Class<?> superClass, TypeNarrower typeNarrower) {
		shape.populateNarrowedClass(superClass, typeNarrower);
	}

	/**
	 * This can be a performance issue depending on how many entities there are in the system
	 */
	private void initViews(Shape shape) {

		for(Type type: shape.getUniqueTypes()) {
			if(EntityType.class.isAssignableFrom(type.getClass())) {
				shape.getView((EntityType)type);
			}
		}
	}

	private void initRootType(Shape shape) {
		shape.initRootType();
	}

	private void initPositionProperty(Shape shape) {
		shape.initPositionProperty();
	}

	protected void initExternal (Shape shape) {
		shape.deriveExternal();
	}

	public QueryTransformer getQueryBuilder() {
		return new QueryTransformer();
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
				String incomingProperty = row.getCell(2) == null ? null :
					row.getCell(2).getStringCellValue();

				EntityType entityType = (EntityType)getShape().getType(entityTypeName);
				Sheet entitySheet = wb.getSheet(sheetName);
				processDomainValues(entityType, entitySheet, incomingProperty);
			}

		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	private void processDomainValues(EntityType entityType, Sheet entitySheet, String incomingProperty) throws
		ClassNotFoundException,
		NoSuchMethodException,
		IllegalAccessException,
		InvocationTargetException,
		InstantiationException
	{
		Map<String, Integer> headerMap = ExcelExportImport.getHeaderMap(entitySheet);

		Lot lot = null;
		for(Map.Entry<String, Integer> entry: headerMap.entrySet()) {

			Row row = entitySheet.getRow(1);
			Class generatorClass = Class.forName(row.getCell(entry.getValue()).getStringCellValue());
			List<String> list = new ArrayList<String>();
			for(int i = 2; i <= entitySheet.getLastRowNum(); i++) {
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

			if(gen instanceof RandomSubset) {
				gen.init(new StateGraph.ObjectGenerationVisitor(null, null, null));
			}

			ExtendedProperty property = (ExtendedProperty)entityType.getProperty(entry.getKey());

			if(incomingProperty == null || "".equals(incomingProperty.trim())) {
				incomingProperty = AbstractProperty.TYPE_GENERATOR;
			}

			// It can happen that that particular property is not present in
			// the entity type's shape
			if(property != null) {
				property.setGenerator(incomingProperty, gen);

				// Have it apply to all subtypes also
				for (EntityType subType : entityType.getSubtypes()) {
					property = (ExtendedProperty)subType.getProperty(entry.getKey());
					property.setGenerator(incomingProperty, gen);
				}
			}
		}
	}

	public Settings.SettingsBuilder settings() {
		Settings.SettingsBuilder result = new Settings.SettingsBuilder(getShape());
		return result;
	}
}
