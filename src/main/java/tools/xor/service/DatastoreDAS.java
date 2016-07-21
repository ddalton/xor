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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import tools.xor.JPAType;
import tools.xor.ModelConstraint;
import tools.xor.MutableJsonProperty;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.util.JPAUtil;
import tools.xor.util.PersistenceType;

/**
 * This class is part of the Data Access Service framework
 * 
 * @author Dilip Dalton
 * 
 */
public class DatastoreDAS extends AbstractDataAccessService {

	private static final Logger logger = LogManager.getLogger(new Exception()
	.getStackTrace()[0].getClassName());
	
	private EntityManagerFactory emf;
	private List<ModelConstraint> constraints;
	
	public DatastoreDAS(TypeMapper typeMapper, String name, DASFactory dasFactory) {
		super(dasFactory);
		this.typeMapper = typeMapper;
		this.emf = JPAUtil.getEmf(name);
		
		registerConverters();
	}
	
	private void registerConverters() {
		MutableJsonProperty.registerConverter(Key.class, getKeyConverter());
	}
	
	private MutableJsonProperty.Converter getKeyConverter() {
		MutableJsonProperty.Converter c = new MutableJsonProperty.AbstractConverter() {
			
			@Override
			public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
				if(jsonObject.has(key)) {
					Object value = jsonObject.get(key);
					if(value instanceof Key) {
						return value;
					} else {
						return KeyFactory.stringToKey(jsonObject.getString(key));
					}
				}
				
				return null;
			}
			
			@Override
			public void add(JSONArray jsonArray, Object object) {
				jsonArray.put(KeyFactory.keyToString((Key)object));
			}
			
			@Override
			public void setExternal(JSONObject jsonObject, String name, Object object) throws JSONException {
				jsonObject.put(name,  KeyFactory.keyToString((Key)object));
			}
		};
		
		return c;
	}
	
	public void setConstraints(List<ModelConstraint> value) {
		this.constraints = value;
	}

	@Override
	public void define() {

		Metamodel metaModel = emf.getMetamodel();
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
				//jPAType.setProperty(this);
			}
		}

		// Link the bi-directional relationship between the properties
		for(Type type: types.values()) {
			if(JPAType.class.isAssignableFrom(type.getClass())) {
				JPAType jPAType = (JPAType) type;
				//jPAType.setOpposite(this);
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

		Metamodel metaModel = emf.getMetamodel();
		Set<EntityType<?>> classMappings = metaModel.getEntities();

		for(EntityType<?> classMapping: classMappings){ 
			defineTypes(classMapping);
			result.add(classMapping.getJavaType().getName());
		}		
		
		return result;
	}

	@Override
	public PersistenceType getAccessType() {
		return PersistenceType.DATASTORE;
	}

	@Override
	public PersistenceOrchestrator createPO(Object sessionContext, Object data) {
		return new DatastorePersistenceOrchestrator();
	}
}
