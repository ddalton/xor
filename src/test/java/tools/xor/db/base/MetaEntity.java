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

package tools.xor.db.base;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import tools.xor.db.catalog.CatalogItem;
import tools.xor.db.common.Property;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class MetaEntity extends Identity {

	private Set<Property> property = new HashSet<Property>();

	@OneToMany(mappedBy = "metaEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	public Set<Property> getProperty() {
		return this.property;
	}

	public void setProperty(Set<Property> propertys) {
		this.property = propertys;
	}

	private boolean disabled;

	@Column
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	private Person ownedBy;

	@ManyToOne
	public Person getOwnedBy() {
		return this.ownedBy;
	}

	public void setOwnedBy(Person ownedBy) {
		this.ownedBy = ownedBy;
	}	
	
	private MetaEntityType metaEntityType;

	public void setMetaEntityType(MetaEntityType value) {
		this.metaEntityType = value;
	}

	@ManyToOne(optional = false)
	public MetaEntityType getMetaEntityType() {
		return this.metaEntityType;
	}
	
	private MetaEntityState state;

	public void setState(MetaEntityState value) {
		this.state = value;
	}

	@ManyToOne(optional = false)
	public MetaEntityState getState() {
		return this.state;
	}	
	
	private Set<CatalogItem> catalogItem = new HashSet<CatalogItem>();

	@OneToMany(mappedBy = "context", cascade = { CascadeType.ALL }, orphanRemoval = true)
	public Set<CatalogItem> getCatalogItem() {
		return this.catalogItem;
	}

	public void setCatalogItem(Set<CatalogItem> catalogItems) {
		this.catalogItem = catalogItems;
	}	
}
