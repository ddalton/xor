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
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.JPAType;
import tools.xor.Type;
import tools.xor.TypeMapper;

/**
 * This class is part of the Data Access Service framework 
 * @author Dilip Dalton
 *
 */
public abstract class JPADataModel extends AbstractDataModel {

	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private PersistenceUtil persistenceUtil;
	
	@Override
	public PersistenceUtil getPersistenceUtil() {
        return persistenceUtil;
    }

	@Override
    public void setPersistenceUtil(PersistenceUtil persistenceUtil) {
        this.persistenceUtil = persistenceUtil;
    }

    public JPADataModel(TypeMapper typeMapper, String name, DataModelFactory dasFactory) {
		super(dasFactory, typeMapper);
	}
	
	public abstract EntityManagerFactory getEmf();	

	@Override
	public Shape createShape(String name, SchemaExtension extension, Shape.Inheritance typeInheritance) {
        Shape shape = super.createShape(name, extension, typeInheritance);
		
		processShape(shape, extension, null);

		return shape;
	}
	
	private void defineTypes(Shape shape, Set<String> entityNames) {
		
		Metamodel metaModel = getEmf().getMetamodel();
		Set<ManagedType<?>> providerEntities = metaModel.getManagedTypes();
		List<ManagedType<?>> filteredEntities = new ArrayList<>();
		
		if(entityNames != null && !entityNames.isEmpty()) {
	        for(ManagedType<?> managedType: providerEntities) {
	            if(managedType.getPersistenceType() == PersistenceType.ENTITY) {
	                if(!entityNames.contains(((EntityType<?>)managedType).getName())) {
	                    continue;
	                }
	            }
	            filteredEntities.add(managedType);
	        }
		} else {
			filteredEntities = new ArrayList(providerEntities);
		}
		
		logger.info("Getting the list of JPA mapped classes");  		
		for(ManagedType<?> classMapping: filteredEntities){ 
			logger.debug("     Adding JPA persisted class: " + classMapping.getJavaType().getName());
			defineType(classMapping, shape);
		}	
	}
	
	@Override public void processShape(Shape shape, SchemaExtension extension, Set<String> entityNames) {
		
		defineTypes(shape, entityNames);
		
		// Set the super type
		defineParentTypes(shape);

		// Define the properties for the Types 
		// This will end up defining the simple types
		defineProperties(shape);
		
		postProcess(shape, extension, shape.getUniqueTypes(), false);		
	}

	protected void defineType(ManagedType<?> classMapping, Shape shape) {
		JPAType dataType = new JPAType(classMapping);
		logger.debug("Defined data type: " + dataType.getName());
		shape.addType(classMapping.getJavaType().getName(), dataType);
		
		for(Type type: dataType.getEmbeddableTypes()) {
			shape.addType(type.getName(), type);
		}
	}

	protected void defineProperties(Shape shape) {
		for(Type type: shape.getUniqueTypes()) {
			if(JPAType.class.isAssignableFrom(type.getClass())) {
				JPAType jPAType = (JPAType) type;
				jPAType.defineProperties(shape);
		        jPAType.initAccessType();
			}
		}

		// Link the bi-directional relationship between the properties
		for(Type type: shape.getUniqueTypes()) {
			if(JPAType.class.isAssignableFrom(type.getClass())) {
				JPAType jPAType = (JPAType) type;
				jPAType.setOpposite(shape);
			}			
		}		
	}	
}
