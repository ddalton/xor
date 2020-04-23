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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.generator.Generator;
import tools.xor.generator.LinkedChoices;
import tools.xor.generator.Lot;
import tools.xor.generator.RandomSubset;
import tools.xor.service.exim.ExcelExportImport;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryTransformer;

public abstract class AbstractDataModel implements DataModel {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	protected TypeMapper          typeMapper;
	protected DataModelFactory    dasFactory;
    protected PersistenceProvider persistenceProvider;
	protected Map<String, Shape>  shapes; // Contains all the initialized shapes
	private ThreadLocal<Shape>    activeShape = new ThreadLocal<Shape>(); // currently activated shape out of many shapes. This avoids having to keep track of the name of the shape

	public AbstractDataModel(DataModelFactory factory, TypeMapper typeMapper) {
		this.dasFactory = factory;
		this.typeMapper = typeMapper;
		this.shapes = new HashMap<>();
		
		this.typeMapper.setModel(this);
	}	
	
	@Override
	public PersistenceProvider getPersistenceProvider() {
	    if(this.persistenceProvider == null) {
	        this.persistenceProvider = this.dasFactory.getDataModelBuilder().getPersistenceProvider();
	    }
	    return this.persistenceProvider;
	}
	
	@Override
	public void setPersistenceProvider(PersistenceProvider dp) {
	    this.persistenceProvider = dp;
	}
	
    @Override
    public void addShape(Shape shape) {
        if(shapes.containsKey(shape.getName())) {
            throw new RuntimeException(String.format("Shape with name %s already exists", shape.getName()));
        }
        
        shapes.put(shape.getName(), shape);
    }	
    
    @Override
    public void setActive(Shape shape) {
        activeShape.set(shape);        
    }

	@Override
	public Shape getShape() {
	    Shape shape = activeShape.get();
		if(shape == null) {
		    throw new RuntimeException("No shape currently set as active");
		}
		
		return shape;
	}

	@Override
	public Shape getShape(String name) {
	    if(StringUtils.isEmpty(name)) {
	        return getShape();
	    }
	    
		return shapes.get(name);
	}
	
    @Override public Shape createShape (String name)
    {
        return createShape(name, null);
    }	
    
    @Override public Shape createShape (String name, SchemaExtension extension)
    {
        Shape shape = new DomainShape(name, null, this);
        addShape(shape);
        this.setActive(shape);
        
        return shape;
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

	public AggregateManager getAggregateManager() {
		return this.dasFactory.getAggregateManager();
	}

	@Override
	public void removeShape(String name) {
		shapes.remove(name);
	}

	@Override
	public Type getType(Shape shape, Class<?> clazz, Type type) {
		return clazz != null ? shape.getType(clazz) : type;
	}

	/**
	 * This is a performance intensive method. So we now define subtypes as and when they are
	 * needed.
	 * For this to work we save the DataModel instance along with the EntityType.
	 *
	 * @param shape of the type being defined
	 */
	protected void defineParentTypes (Shape shape){
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
					type.setParentType((EntityType) superType);
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

	/**
	 * Post process the addition of the new types
	 * @param shape to which the types are being added
	 * @param extension any additional type processing that needs to be done
	 * @param types that are being added
	 * @param isTemporary true if the types are temporary - this affects which steps are optional
	 */
	protected void postProcess(Shape shape, SchemaExtension extension, Collection<Type> types, boolean isTemporary) {

		// Create the properties for the types
		initPositionProperty(shape, types);

		// Extend the schema if applicable
		if(extension != null) {
			extension.extend(shape);
		}

		// Initialize the root type
		initRootType(shape, types);

		// Performance hog - skip for now
//		initViews(shape);

		// Do topological sorting, to set the order between the types
		if(!isTemporary) {
			initOrder(shape);

			initEnd(shape, types);
		}
	}

	/**
	 * Should be invoked as the final step of the Shape object construction.
	 * @param shape object being constructed
	 * @param types that need to processed
	 * @param externalTypes of types that need to be processed
	 */
	protected void initEnd(Shape shape, Collection<Type> types) {
		((AbstractShape)shape).initEnd(types);

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

	private void initRootType(Shape shape, Collection<Type> types) {
		((AbstractShape)shape).initRootType(types);
	}

	private void initPositionProperty(Shape shape, Collection<Type> types) {
		((AbstractShape)shape).initPositionProperty(types);
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
