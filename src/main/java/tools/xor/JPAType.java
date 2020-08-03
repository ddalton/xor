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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cglib.proxy.Proxy;

import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;

public class JPAType extends AbstractType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private javax.persistence.metamodel.Type<?>  persistenceType;
	private AccessType               accessType;
	private JPAProperty              identifierProperty;
	private JPAProperty              versionProperty;	

	public JPAType(javax.persistence.metamodel.Type<?> entityType) {
		super();		
		this.persistenceType = entityType;
		
		init();
	}

	@Override
	public String getName() {
		// The name is unique in the namespace because is qualified by the package name
		return persistenceType.getJavaType().getName();
	}

	@Override
	public String getEntityName() {
		if(EntityType.class.isAssignableFrom(persistenceType.getClass()))
			return ((EntityType)persistenceType).getName();
		else
			return getName();
	}	
	
	public javax.persistence.metamodel.Type<?> getProviderType() {
	    return this.persistenceType;
	}

    protected Iterator<?> getPropertyIterator() {
        if (persistenceType.getPersistenceType() == javax.persistence.metamodel.Type.PersistenceType.ENTITY) {
            //return ((EntityType<?>) persistenceType).getAttributes().iterator();
            return ((EntityType<?>)persistenceType).getDeclaredAttributes().iterator();
        } else if (persistenceType
                .getPersistenceType() == javax.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS) {
            return ((IdentifiableType<?>) persistenceType).getAttributes().iterator();
        } else if (persistenceType
                .getPersistenceType() == javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE) {
            return ((EmbeddableType<?>) persistenceType).getAttributes().iterator();
        } else {
            throw new UnsupportedOperationException();
        }
    }

	@Override
	public List<Type> getEmbeddableTypes() {
		List<Type> result = new ArrayList<Type>();

		Iterator<?> itr = getPropertyIterator();
		while(itr.hasNext() ) {
			Attribute<?, ?> attribute = (Attribute<?, ?>) itr.next();
			if(attribute.getPersistentAttributeType() == javax.persistence.metamodel.Attribute.PersistentAttributeType.EMBEDDED) {
				JPAType type = new JPAType(((SingularAttribute<?, ?>)attribute).getType());
				result.add(type);
			}
		}

		return result; 
	}	

	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Class<?> getInstanceClass() {
		return persistenceType.getJavaType();
	}

	@Override
	public boolean isInstance(Object object) {
		return getInstanceClass().isAssignableFrom(object.getClass());
	}

	@Override
	public void defineProperties (Shape shape)
	{
	    if(!createdSuperTypeProperties()) {
	        return;
	    }

		Iterator<?> attribIter = getPropertyIterator();
		while (attribIter.hasNext()) {

			Attribute<?, ?> attribute = (Attribute<?, ?>)attribIter.next();
			logger.debug(
				"[" + getName() + "] JPA Property name: " + attribute.getName() + ", type name: "
					+ attribute.getJavaType());

			Type propertyType = getShape().getType(attribute.getJavaType());
			JPAProperty property = new JPAProperty(attribute, propertyType, this);

			if(property.getName().equals("id")) {
			    System.out.println("Type: " + getName() + ", property: " + property.getName() + ", isId: " + property.isIdentifier()) ;
			}
			
			if(isSuperTypeProperty(attribute.getName())) {
			    continue;
			}
			
            if (property.isIdentifier()) {
                identifierProperty = property;
                logger.debug("JPA Identifier attribute name: " + identifierProperty.getName());
            }
            if (property.isVersion()) {
                versionProperty = property;
                logger.debug("JPA version attribute name: " + versionProperty.getName());
            }			
            
            property.init(shape);
            shape.addProperty(property);
		}
		
		JPAType parentType = (JPAType)getParentType();
		// Set it from the superType to enable sharing of id and version property
	    if(parentType != null && parentType.getIdentifierProperty() != null) {
	        identifierProperty = (JPAProperty) parentType.getIdentifierProperty();
	    }
        if(parentType != null && parentType.getVersionProperty() != null) {
            versionProperty = (JPAProperty) parentType.getVersionProperty();
        }
	}

	public void setOpposite(Shape shape) {
		for(Property property: getProperties()) {
		    JPAProperty jpaProperty = (JPAProperty) ClassUtil.getDelegate(property);
			jpaProperty.initMappedBy(shape);
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
		return persistenceType.getPersistenceType() != javax.persistence.metamodel.Type.PersistenceType.ENTITY;
	}

	@Override
	public List<Property> getDeclaredProperties() {
		List<Property> result = new ArrayList<Property>();

		if(getEntityType().getPersistenceType() != javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE) {		
			Set<?> declaredAttributes = ((ManagedType<?>)persistenceType).getDeclaredAttributes();
			Iterator<?> attribIter = declaredAttributes.iterator();		
			while(attribIter.hasNext()) {
				Attribute<?, ?> attribute = (Attribute<?, ?>) attribIter.next();
				logger.debug("[" + getName() + "] JPA declared property name: " + attribute.getName());
				result.add(getShape().getProperty(this, attribute.getName()));
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

	public javax.persistence.metamodel.Type<?> getEntityType() {
		return this.persistenceType;
	}

	public void initAccessType() {
		javax.persistence.Access accessAnno = getInstanceClass().getAnnotation( javax.persistence.Access.class );
		if ( accessAnno != null ) {
			accessType = AccessType.valueOf(accessAnno.value().name());
		}

		if(accessType == null)
			accessType = inferAccessType();
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}	

	protected AccessType inferAccessType() {
		boolean hasAccessAnnotation = false;

		// first check if any of its properties have the Access annotation
		for(Property property: getProperties()) {
			if( ((ExtendedProperty)property).getAccessType() != null) {
				hasAccessAnnotation = true;
				break;
			}
		}

		if(!hasAccessAnnotation) {
			for(Property property: getProperties()) {
			    property = (Property) ClassUtil.getDelegate(property);
				if(((JPAProperty)property).isFieldMapped())
					return AccessType.FIELD;
				else if(((JPAProperty)property).isPropertyMapped())
					return AccessType.PROPERTY;
			}
		}

		// Default is field
		//if(isEmbedded())
			return AccessType.FIELD;
	}

	@Override
	public Property getIdentifierProperty() {
		return identifierProperty;
	}

	@Override
	public boolean isEmbedded() {
		return persistenceType.getPersistenceType() == PersistenceType.EMBEDDABLE; 
	}
	
	@Override
	public boolean isEntity() {
		return persistenceType.getPersistenceType() == PersistenceType.ENTITY; 
	}	

	@Override
	public Property getVersionProperty() {
		return versionProperty;
	}

	@Override
	public boolean supportsDynamicUpdate() {
		return false;
	}

}
