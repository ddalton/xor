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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;

import tools.xor.HibernateType;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.util.HibernateUtil;
import tools.xor.util.PersistenceType;

/**
 * This class is part of the Data Access Service framework
 * 
 * @author Dilip Dalton
 * 
 */
public abstract class HibernateDAS extends AbstractDataAccessService {

	private static final Logger logger = LogManager.getLogger(new Exception()
	.getStackTrace()[0].getClassName());
	
	public HibernateDAS(TypeMapper typeMapper, DASFactory dasFactory) {
		super(dasFactory, typeMapper);
	}
	
	public abstract SessionFactory getSessionFactory();
	
	public abstract Configuration getConfiguration();

	@Override
	public Shape createShape(String name, SchemaExtension extension) {
        Shape shape = super.createShape(name, extension);

		processShape(shape, extension, null);
		/*
		Configuration conf = getConfiguration();
		Iterator<PersistentClass> classMappings = conf.getClassMappings();

		logger.info("Getting the list of hibernate mapped classes");
		while (classMappings.hasNext()) {
			PersistentClass classMapping = (PersistentClass) classMappings
					.next();
			logger.debug("     Adding hibernate persisted class: "
					+ classMapping.getClassName());
			defineTypes(classMapping, shape);
		}

		// Set the super type
		defineSuperType(shape);

		// Set the base types
		setBaseTypes(shape);

		// Define the properties for the Types
		// This will end up defining the simple types
		defineProperties(shape);
		
		postProcess(shape, extension, shape.getUniqueTypes(), false);
*/
		return shape;
	}
	
	private void defineTypes(Shape shape, Set<String> entityNames) {
		
		Configuration conf = getConfiguration();
		Iterator<PersistentClass> providerEntities = conf.getClassMappings();
		List<PersistentClass> filteredEntities = new ArrayList<>();
		Iterator<PersistentClass> entityIterator = providerEntities;
		
		if(entityNames != null && !entityNames.isEmpty()) {
			Map<String, PersistentClass> providerEntityMap = new HashMap<>();
			while(providerEntities.hasNext()) {
				PersistentClass entityType = providerEntities.next();
				providerEntityMap.put(entityType.getEntityName(), entityType);
			}
			
			for(String entityName: entityNames) {
				if(providerEntityMap.containsKey(entityName)) {
					filteredEntities.add(providerEntityMap.get(entityName));
				}
			}
			
			entityIterator = filteredEntities.iterator();
		} 
		
		logger.info("Getting the list of hibernate mapped classes");
		while (entityIterator.hasNext()) {
			PersistentClass classMapping = (PersistentClass) entityIterator
					.next();
			logger.debug("     Adding hibernate persisted class: "
					+ classMapping.getClassName());
			defineType(classMapping, shape);
		}			
	}	
	
	@Override public void processShape(Shape shape, SchemaExtension extension, Set<String> entityNames) {
		
		defineTypes(shape, entityNames);
		
		// Set the super type
		defineSuperType(shape);

		// Set the base types
		setBaseTypes(shape);

		// Define the properties for the Types 
		// This will end up defining the simple types
		defineProperties(shape);
		
		postProcess(shape, extension, shape.getUniqueTypes(), false);		
	}	

	protected void defineType(PersistentClass classMapping, Shape shape) {
		HibernateType dataType = new HibernateType( HibernateUtil.getEntityType(getSessionFactory(), classMapping.getEntityName()),
				classMapping);
		
		logger.debug("Defined data type: " + dataType.getName());
		shape.addType(dataType.getName(), dataType);
		
		for(Type type: dataType.getEmbeddableTypes()) {
			shape.addType(type.getName(), type);
		}
	}

	protected void defineProperties(Shape shape) {
		for (Type type : shape.getUniqueTypes()) {
			if (HibernateType.class.isAssignableFrom(type.getClass())) {
				HibernateType hibernateType = (HibernateType) type;
				hibernateType.setProperty(shape);
			}
		}

		// Link the bi-directional relationship between the properties
		for (Type type : shape.getUniqueTypes()) {
			if (HibernateType.class.isAssignableFrom(type.getClass())) {
				HibernateType hibernateType = (HibernateType) type;
				hibernateType.setOpposite(shape);
			}
		}
	}

	protected void setBaseTypes(Shape shape) {
		for (Type type : shape.getUniqueTypes()) {
			if (HibernateType.class.isAssignableFrom(type.getClass())) {
				HibernateType hibernateType = (HibernateType) type;
				if(hibernateType.getHibernateType().isComponentType()) {
					continue;
				}

				List<Type> baseTypes = new ArrayList<Type>();
				PersistentClass base = ((PersistentClass)hibernateType.getHibernateClass()).getSuperclass();

				if (base != null) {
					Type baseType = shape.getType(base.getEntityName());
					if (baseType != null)
						baseTypes.add(baseType);	
				}
				hibernateType.setBaseType(baseTypes);
			}
		}
	}

	@Override
	public PersistenceType getAccessType() {
		return PersistenceType.HIBERNATE;
	}
}
