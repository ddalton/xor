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
import java.util.List;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Value;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

import tools.xor.service.DataAccessService;
import tools.xor.service.HibernateDAS;
import tools.xor.service.Shape;
import tools.xor.util.HibernateUtil;

public class HibernateProperty extends AbstractProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private org.hibernate.mapping.Property hibernateProperty;
	private Boolean                        cascaded;
	private Configuration                  configuration;
	private Boolean                        isMany;

	public HibernateProperty(org.hibernate.mapping.Property hibernateProperty, Type type, HibernateType parentType, Configuration configuration) {
		super(type, parentType);
		this.hibernateProperty = hibernateProperty;
		this.setConfiguration(configuration);

		init();		
	}	

	@Override
	public String getName() {
		return hibernateProperty.getName();
	}

	@Override
	public boolean isMany() {
		if(isMany == null) {
			isMany = hibernateProperty.getType().isCollectionType();
		}
		
		return isMany;
	}

	@Override
	public boolean isContainment ()
	{
		if (isCascaded())
			return true;

		return super.isContainment();
	}	
	
	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}	
	
	public Boolean isCascaded() {
		// initialize the cascaded attribute
		if(cascaded == null) {
			String entityName = ((HibernateType)getContainingType()).getEntityName();
			cascaded = HibernateUtil.isCascaded(getConfiguration(), entityName, hibernateProperty.getName());
		}
		
		return cascaded;
	}

	public void setCascaded(Boolean cascaded) {
		this.cascaded = cascaded;
	}	

	/**
	 * Not directly supported in Hibernate
	 */
	@Override
	public Object getDefault() {
		return null;
	}

	@Override
	public boolean isNullable() {
		return hibernateProperty.isOptional();
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}		

	@Override
	public void init(Shape shape) {
		HibernateDAS hibernateDAS = (HibernateDAS) shape.getDAS();

		if(isMany()) {
			if(hibernateProperty.getType().isCollectionType()) {
				CollectionType collType = (CollectionType) hibernateProperty.getType();
				CollectionPersister cp = HibernateUtil.getCollectionPersister(hibernateDAS.getSessionFactory(), collType);
				keyType = (cp.getIndexType() == null) ? null : shape.getType(cp.getIndexType().getReturnedClass());
				
				elementType = shape.getType(cp.getElementType().getReturnedClass());
			}
		} 
	}

	@Override
	public void initMappedBy(DataAccessService das) {
		String mappedBy = getMappedByName();

		Type type = isMany() ? getElementType() : getType();
		setMappedBy( mappedBy != null ? type.getProperty(mappedBy) : null, mappedBy);

		if(getMappedBy() != null)
			logger.debug("Opposite of property '" + getContainingType().getName() + "." + getName() + "' is '" + mappedBy + "'");
	}

	@Override
	public PersistentAttributeType getAssociationType() {
		Value value = hibernateProperty.getValue();

		if(org.hibernate.mapping.Collection.class.isAssignableFrom(value.getClass())) {
			org.hibernate.mapping.Collection collectionValue = (org.hibernate.mapping.Collection) value;
			Value element = collectionValue.getElement();

			if(ManyToOne.class.isAssignableFrom(element.getClass()))
				return PersistentAttributeType.MANY_TO_MANY;
			else
				return PersistentAttributeType.ONE_TO_MANY;
		}

		if(OneToOne.class.isAssignableFrom(value.getClass()))
			return PersistentAttributeType.ONE_TO_ONE;		

		if(ManyToOne.class.isAssignableFrom(value.getClass())) {
			if( ((ManyToOne)value).isLogicalOneToOne() )
				return PersistentAttributeType.ONE_TO_ONE;
			else
				return PersistentAttributeType.MANY_TO_ONE;
		}

		return null;		
	}

	@Override
	public boolean isMap() {
		if(isMany())
		{
			Value value = hibernateProperty.getValue();
			if(org.hibernate.mapping.Map.class.isAssignableFrom(value.getClass()))
				return true;
		}

		return false;
	}

	@Override
	public boolean isList() {
		if(isMany() && getKeyType() != null)
		{
			Value value = hibernateProperty.getValue();
			if(org.hibernate.mapping.List.class.isAssignableFrom(value.getClass()))
				return true;
		}

		return false;
	}

	@Override
	public boolean isSet() {
		if(isMany())
		{
			Value value = hibernateProperty.getValue();
			if(org.hibernate.mapping.Set.class.isAssignableFrom(value.getClass()))
				return true;
		}

		return false;
	}

	@Override public boolean isCollectionOfReferences ()
	{
		return false;
	}

}
