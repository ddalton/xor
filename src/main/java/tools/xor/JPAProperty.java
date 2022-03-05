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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.PluralAttribute.CollectionType;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.Shape;

public class JPAProperty extends AbstractProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private Attribute<?, ?> attribute;
	private boolean         isPropertyMapped; // Are the JPA mappings on the getter	
	private boolean         isFieldMapped;    // Are the JPA mappings on the field
	private boolean         hasVersionAnnotation;	
	private boolean         cascaded;
	private boolean         required;

	public JPAProperty(Attribute<?, ?> attribute, Type type, JPAType parentType) {
		super(type, parentType);
		this.attribute = attribute;

		init();
	}
	
	public JPAProperty(String name, Type type, EntityType parentType) {
		super(name, type, parentType);
	}

	public JPAProperty(String name, Type type, EntityType parentType, RelationshipType relType, EntityType elementType) {
		super(name, type, parentType, relType, elementType);
	}

	@Override
	public Property refine (String name, Type type, EntityType parentType) {
		JPAProperty result = null;

		if(this.isMany()) {
			result = new JPAProperty(name, this.getType(), parentType, getRelationshipType(),
				(EntityType)type);
		} else {
			result = new JPAProperty(name, type, parentType);
		}

		return result;
	}

	@Override public boolean isIdentifier ()
	{
		return super.isIdentifier();
	}

	@Override public boolean isOpenContent ()
	{
		return super.isOpenContent();
	}

	public boolean isPropertyMapped() {
		return isPropertyMapped;
	}	

	public boolean isFieldMapped() {
		return isFieldMapped;
	}

	public boolean isVersion() {
		return hasVersionAnnotation;
	}	

	protected List<Class<? extends Annotation>> getJPAAnnotationList() {
		List<Class<? extends Annotation>> result = new ArrayList();

		// Listing common annotations on top
		result.add(Id.class);		
		result.add(OneToOne.class);
		result.add(OneToMany.class);
		result.add(ManyToMany.class);
		result.add(ManyToOne.class);
		result.add(Version.class);
		result.add(Basic.class);		
		result.add(Column.class);		
		result.add(SequenceGenerator.class);
		result.add(TableGenerator.class);
		result.add(Embedded.class);
		result.add(EmbeddedId.class);
		result.add(Transient.class);
		result.add(ElementCollection.class);
		result.add(GeneratedValue.class);
		result.add(MapKey.class);
		result.add(OrderBy.class);
		result.add(OrderColumn.class);
		result.add(PrePersist.class);
		result.add(PostPersist.class);
		result.add(PreRemove.class);
		result.add(PostRemove.class);
		result.add(PreUpdate.class);
		result.add(PostUpdate.class);
		result.add(PostLoad.class);
		result.add(JoinTable.class);
		result.add(CollectionTable.class);
		result.add(Lob.class);
		result.add(Temporal.class);
		result.add(Enumerated.class);
		result.add(JoinColumn.class);
		result.add(JoinColumns.class);

		return result;
	}

	@Override
	public void init() {
		super.init();

		// For more details refer to JPA 2.0 specification at section 2.3
		for(Class<? extends Annotation> annotationClass: getJPAAnnotationList()) {
			if(getterMethod != null && getterMethod.isAnnotationPresent(annotationClass)) {
				isPropertyMapped = true;
				break;
			}
		}	

		for(Class<? extends Annotation> annotationClass: getJPAAnnotationList()) {
			if(field != null && field.isAnnotationPresent(annotationClass)) {
				isFieldMapped = true;
				break;
			}
		}		

		if(isPropertyMapped && isFieldMapped)
			throw new IllegalStateException("Both field and getter method have JPA mappings!");
		
		// Check if it is an Identifier property
		hasIdAnnotation = isAnnotationPresent(Id.class);
		
		// Check if it is a version property
		hasVersionAnnotation = isAnnotationPresent(Version.class);

		initContainment();
	}

	@Override
	protected boolean isAnnotationPresent(Class annotationClass) {
		if(isPropertyMapped && getterMethod.isAnnotationPresent(annotationClass))
			return true;
		else if(isFieldMapped && field != null && field.isAnnotationPresent(annotationClass))
			return true;

		return false;
	}

	@Override	
	protected Annotation getAnnotation(Class annotationClass) {
		if(isPropertyMapped && getterMethod.isAnnotationPresent(annotationClass))
			return getterMethod.getAnnotation(annotationClass);
		else if(isFieldMapped && field != null && field.isAnnotationPresent(annotationClass))
			return field.getAnnotation(annotationClass);

		return null;
	}	

	protected boolean isCascaded(CascadeType[] cascadeTypes) {
		if(cascadeTypes == null)
			return false;
		
		List<CascadeType> types = Arrays.asList(cascadeTypes);
		return types.contains(CascadeType.ALL) || types.contains(CascadeType.PERSIST) || types.contains(CascadeType.MERGE);
	}
	
	private Annotation getRelationshipType(Class<? extends Annotation> annotationClass) {
		if(isPropertyMapped && getterMethod.isAnnotationPresent(annotationClass)) {
			return getterMethod.getAnnotation(annotationClass);
		} else if(isFieldMapped && field != null && field.isAnnotationPresent(annotationClass)) {
			return field.getAnnotation(annotationClass);			
		}
		
		return null;
	}

	protected void initContainment() {
		if(isOpenContent()) {
			return;
		}
		// The attribute.isAssociation method is not working, so we have to do this
		try {
			if(attribute.getPersistentAttributeType() == PersistentAttributeType.ONE_TO_ONE) {
				cascaded = isCascaded( ((OneToOne)getRelationshipType(OneToOne.class)).cascade() );
			} else if (attribute.getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY) {
				cascaded = isCascaded( ((OneToMany)getRelationshipType(OneToMany.class)).cascade() );	
			} else if (attribute.getPersistentAttributeType() == PersistentAttributeType.MANY_TO_ONE) {
				cascaded = isCascaded( ((ManyToOne)getRelationshipType(ManyToOne.class)).cascade() );			
			} else if (attribute.getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY) {
				cascaded = isCascaded( ((ManyToMany)getRelationshipType(ManyToMany.class)).cascade() );				
			}
		} catch (Exception e) {
			logger.warn("No relationship annotation found for property: " + getName() + " and type: " + getType().getName());
		}
	}

	public void init(Shape shape) {
		if(isOpenContent() && attribute != null) {
			throw new IllegalStateException("Cannot define an open property with the same name as a persistence managed property");
		}
		
		if( attribute != null && PluralAttribute.class.isAssignableFrom(attribute.getClass()) ) {
			PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attribute;
			elementType = shape.getType(pluralAttribute.getElementType().getJavaType());
			if(MapAttribute.class.isAssignableFrom(attribute.getClass())) {
				MapAttribute<?, ?, ?> mapAttribute = (MapAttribute<?, ?, ?>) attribute;
				keyType = shape.getType(mapAttribute.getKeyJavaType());
			}

			// If this is a list then wrap it for GraphQL
			setType(wrapType(getType(), elementType));
		}
	}	

	@Override
	public String getName() {
		if(!isOpenContent()) {
			return attribute.getName();
		} else {
			return this.name;
		}
	}

	@Override
	public boolean isMany() {
		if(!isOpenContent()) {
			return attribute.isCollection();
		} else {
			if(isOpenContent()) {
				return getRelationshipType() == RelationshipType.TO_MANY;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isContainment() {
		if(cascaded)
			return true;

		return super.isContainment();
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
		if(attribute != null && SingularAttribute.class.isAssignableFrom(attribute.getClass()))
			return ((SingularAttribute<?, ?>)attribute).isOptional();
		else
			return !required;
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
	public void initMappedBy(Shape shape) {
		String mappedBy = getMappedByName();

		Type type = isMany() ? getElementType() : getType();
		setMappedBy( mappedBy != null ? type.getProperty(mappedBy) : null, mappedBy);

		if(getMappedBy() != null) {
			logger.debug("Opposite of property '" + getContainingType().getName() + "." + getName() + "' is '" + mappedBy + "'");
		}
	}

	@Override
	public PersistentAttributeType getAssociationType() {
		if(!isOpenContent()) {
			return attribute.getPersistentAttributeType();
		} else {
			if(isOpenContent()) {
				if(getRelationshipType() == RelationshipType.TO_ONE) {
					return PersistentAttributeType.MANY_TO_ONE;
				} else if(getRelationshipType() == RelationshipType.TO_MANY) {
					return PersistentAttributeType.ONE_TO_MANY;
				}
			}
			return PersistentAttributeType.BASIC;
		}
	}
	
	private boolean checkCollection(CollectionType collectionType) {
		if(!isOpenContent()) {
			if (PluralAttribute.class.isAssignableFrom(attribute.getClass())) {
				PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attribute;
				if (pluralAttribute.getCollectionType() == collectionType)
					return true;
			}
		} else {
			// Currently open content collection supports only Set type
			return collectionType == CollectionType.SET;
		}
		
		return false;
	}

	@Override
	public boolean isMap() {
		if(isMany()) {
			return checkCollection(CollectionType.MAP);
		}

		return false;
	}

	@Override
	public boolean isList() {
		if(isMany()) {
			return checkCollection(CollectionType.LIST);
		}

		return false;
	}

	@Override
	public boolean isSet() {
		if(isMany()) {
			return checkCollection(CollectionType.SET);
		}

		return false;
	}

	@Override public boolean isCollectionOfReferences ()
	{
		return false;
	}
}
