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

package tools.xor.db.vo.base;

import java.util.HashSet;
import java.util.Set;

import tools.xor.db.vo.catalog.CatalogItemVO;
import tools.xor.db.vo.common.PropertyVO;

public class MetaEntityVO extends IdentityVO {

	private Set<PropertyVO> property = new HashSet<PropertyVO>();

	public Set<PropertyVO> getProperty() {
		return this.property;
	}

	public void setProperty(Set<PropertyVO> propertys) {
		this.property = propertys;
	}

	private boolean disabled;

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	private PersonVO ownedBy;

	public PersonVO getOwnedBy() {
		return this.ownedBy;
	}

	public void setOwnedBy(PersonVO ownedBy) {
		this.ownedBy = ownedBy;
	}	
	
	private MetaEntityTypeVO metaEntityType;

	public void setMetaEntityType(MetaEntityTypeVO value) {
		this.metaEntityType = value;
	}

	public MetaEntityTypeVO getMetaEntityType() {
		return this.metaEntityType;
	}
	
	private MetaEntityStateVO state;

	public void setState(MetaEntityStateVO value) {
		this.state = value;
	}

	public MetaEntityStateVO getState() {
		return this.state;
	}	
	
	private Set<CatalogItemVO> catalogItem = new HashSet<CatalogItemVO>();

	public Set<CatalogItemVO> getCatalogItem() {
		return this.catalogItem;
	}

	public void setCatalogItem(Set<CatalogItemVO> catalogItems) {
		this.catalogItem = catalogItems;
	}		
}
