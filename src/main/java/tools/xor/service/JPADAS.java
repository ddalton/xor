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
import javax.persistence.metamodel.Metamodel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.JPAType;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.util.PersistenceType;

/**
 * This class is part of the Data Access Service framework 
 * @author Dilip Dalton
 *
 */
public abstract class JPADAS extends AbstractDataAccessService {

	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	
	
	public JPADAS(TypeMapper typeMapper, String name, DASFactory dasFactory) {
		super(dasFactory);
		this.typeMapper = typeMapper;
	}
	
	public abstract EntityManagerFactory getEmf();	

	@Override
	public void define() {

		Metamodel metaModel = getEmf().getMetamodel();
		Set<EntityType<?>> classMappings = metaModel.getEntities();

		logger.info("Getting the list of JPA mapped classes");  		
		for(EntityType<?> classMapping: classMappings){ 
			logger.debug("     Adding JPA persisted class: " + classMapping.getName());
			defineTypes(classMapping);
		}		

		// Set the base types
		setBaseTypes();		

		// Define the properties for the Types 
		// This will end up defining the simple types
		defineProperties();	
		
		postProcess();			
	}

	protected void defineTypes(EntityType<?> classMapping) {
		JPAType dataType = new JPAType(classMapping);
		logger.debug("Defined data type: " + dataType.getName());
		addType(classMapping.getJavaType().getName(), dataType);
		
		for(Type type: dataType.getEmbeddableTypes()) {
			addType(type.getName(), type);
		}		
		
		defineSuperType();
	}

	protected void defineProperties() {
		for(Type type: types.values()) {
			if(JPAType.class.isAssignableFrom(type.getClass())) {
				JPAType jPAType = (JPAType) type;
				jPAType.setProperty(this);
			}
		}

		// Link the bi-directional relationship between the properties
		for(Type type: types.values()) {
			if(JPAType.class.isAssignableFrom(type.getClass())) {
				JPAType jPAType = (JPAType) type;
				jPAType.setOpposite(this);
			}			
		}		
	}	

	protected void setBaseTypes() {
		for(Type type: types.values()) {
			if(JPAType.class.isAssignableFrom(type.getClass())) {
				
				JPAType jPAType = (JPAType) type;
				
				if(jPAType.getEntityType().getPersistenceType() == javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE)
					continue;				

				List<Type> baseTypes = new ArrayList<Type>();
				Class<?> base = jPAType.getEntityType().getJavaType().getSuperclass();

				if(base != null) {
					Type baseType = types.get(base.getName());
					if(baseType != null) 
						baseTypes.add(baseType);
				}
				jPAType.setBaseType(baseTypes);
			}
		}
	}

	@Override
	public List<String> getAggregateList() {
		List<String> result = new ArrayList<String>();

		Metamodel metaModel = getEmf().getMetamodel();
		Set<EntityType<?>> classMappings = metaModel.getEntities();

		for(EntityType<?> classMapping: classMappings){ 
			defineTypes(classMapping);
			result.add(classMapping.getJavaType().getName());
		}		
		
		return result;
	}

	@Override
	public PersistenceType getAccessType() {
		return PersistenceType.JPA;
	}

}
