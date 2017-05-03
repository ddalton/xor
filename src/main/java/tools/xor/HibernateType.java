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

package tools.xor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

import tools.xor.service.DataAccessService;
import tools.xor.service.HibernateDAS;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;

public class HibernateType extends AbstractType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private org.hibernate.type.Type        hibernateType;
	private Object                         hibernateClass; // Could be org.hibernate.mapping.PersistentClass or org.hibernate.mapping.Component
	private List<Type>                     baseTypes;
	private AccessType                     accessType;
	private HibernateProperty              identifierProperty;
	private HibernateProperty              versionProperty;	

	public HibernateType(org.hibernate.type.Type hibernateType, Object hibernateClass) {
		super();		
		this.hibernateType = hibernateType;
		this.hibernateClass = hibernateClass;
		accessType = ClassUtil.getHibernateAccessType(getInstanceClass());
		
		init();
	}

	@Override
	public List<Type> getEmbeddableTypes() {
		List<Type> result = new ArrayList<Type>();

		Iterator<?> itr = getPropertyIterator();
		while(itr.hasNext() ) {
			org.hibernate.mapping.Property property = (org.hibernate.mapping.Property) itr.next();
			if(property.getType().isComponentType()) {
				HibernateType type = new HibernateType(property.getType(), property.getValue());
				result.add(type);
			}
		}

		return result; 
	}

	@Override
	public String getName() {
		// The name is unique in the namespace because if is qualified by the package name
		return hibernateType.getReturnedClass().getName();
	}
	
	/**
	 * The name Hibernate uses to refer to this entity
	 * @return entity name
	 */
	@Override
	public String getEntityName() {
		return hibernateType.getName();
	}

	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Class<?> getInstanceClass() {
		return hibernateType.getReturnedClass();
	}

	protected Iterator<?> getPropertyIterator() {
		return (PersistentClass.class.isAssignableFrom(hibernateClass.getClass()) == true) ? ((PersistentClass)hibernateClass).getPropertyClosureIterator() : ((Component)hibernateClass).getPropertyIterator();
	}

	@Override
	public boolean isInstance(Object object) {
		return getInstanceClass().isAssignableFrom(object.getClass());
	}

	public void setProperty(Shape shape) {
		HibernateDAS dataAccessService = (HibernateDAS)getDAS();
		if(getProperties() == null) {
			// populate the properties for this type
			Iterator<?> propertyIterator = getPropertyIterator();
			while(propertyIterator.hasNext()) {
				org.hibernate.mapping.Property hibernateProperty = (org.hibernate.mapping.Property) propertyIterator.next();
				logger.debug("[" + getName() + "] hibernate property name: " + hibernateProperty.getName() + ", type name: " + hibernateProperty.getType().getReturnedClass());

				Type propertyType = dataAccessService.getType(hibernateProperty.getType().getReturnedClass());
				HibernateProperty property = new HibernateProperty(hibernateProperty, propertyType, this, dataAccessService.getConfiguration());
				property.init(shape);
				shape.addProperty(property);
			}		

			// Components don't have identifiers
			if(!hibernateType.isComponentType()) {
				org.hibernate.mapping.Property idProperty = ((PersistentClass)hibernateClass).getIdentifierProperty();
				if(idProperty != null) {
					logger.debug("Hibernate Identifier attribute name: " + idProperty.getName());	
					Type propertyType = dataAccessService.getType(idProperty.getType().getReturnedClass());
					identifierProperty = new HibernateProperty(idProperty, propertyType, this, dataAccessService.getConfiguration());
					shape.addProperty(identifierProperty);
				}
				
				org.hibernate.mapping.Property verProperty = ((PersistentClass)hibernateClass).getVersion();
				if(verProperty != null) {
					logger.debug("Hibernate version attribute name: " + verProperty.getName());	
					Type propertyType = dataAccessService.getType(verProperty.getType().getReturnedClass());
					versionProperty = new HibernateProperty(verProperty, propertyType, this, dataAccessService.getConfiguration());
					shape.addProperty(versionProperty);
				}				
			}
		} 		
	}

	public void setOpposite() {
		HibernateDAS dataAccessService = (HibernateDAS)getDAS();
		for(Property property: getProperties()) {
			((HibernateProperty)property).initMappedBy(dataAccessService);
		}
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isSequenced() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public List<Type> getBaseTypes() {
		return baseTypes;
	}

	public void setBaseType(List<Type> types) {
		baseTypes = types;
	}

	@Override
	public List<Property> getDeclaredProperties() {
		List<Property> result = new ArrayList<Property>();

		if(!hibernateType.isComponentType()) {
			Iterator<?> declaredPropertyIterator = ((PersistentClass)hibernateClass).getDeclaredPropertyIterator();
			while(declaredPropertyIterator.hasNext()) {
				org.hibernate.mapping.Property declaredProperty = (org.hibernate.mapping.Property) declaredPropertyIterator.next();
				logger.debug("[" + getName() + "] Hibernate declared property name: " + declaredProperty.getName());
				result.add(getDAS().getShape().getProperties(this).get(declaredProperty.getName()));
			}
		}

		return result;
	}

	@Override
	public List<?> getAliasNames() {
		return new ArrayList<String>();
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}

	public Object getHibernateClass() {
		return this.hibernateClass;
	}

	public org.hibernate.type.Type getHibernateType() {
		return this.hibernateType;
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}

	@Override
	public Property getIdentifierProperty() {
		return identifierProperty;
	}

	@Override
	public boolean isEmbedded() {
		return hibernateType.isComponentType();
	}
	
	@Override
	public boolean isEntity() {
		return hibernateType.isEntityType(); 
	}		

	@Override
	public Property getVersionProperty() {
		return versionProperty;
	}

	@Override
	public boolean supportsDynamicUpdate() {
		return ((PersistentClass) hibernateClass).useDynamicUpdate();
	}

}
