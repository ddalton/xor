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

package tools.xor.db.common;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import tools.xor.annotation.XorEntity;
import tools.xor.db.base.Identity;
import tools.xor.db.base.MetaEntity;

@Entity
@XorEntity(naturalKey = "name")
public class Property extends Identity {

	private MetaEntity metaEntity;
	private Set<PropertyBinding> propertyBindings = new HashSet<PropertyBinding>();

	private ValueType valueType;
	private Set<Value> values = new HashSet<Value>();

	private int     orderIndex = -1;
	private boolean confidential;
	private boolean encrypted;

	@ManyToOne(optional = true, fetch = FetchType.LAZY)
	public MetaEntity getMetaEntity() {
		return metaEntity;
	}

	public void setMetaEntity(MetaEntity value) {
		this.metaEntity = value;
	}

	@OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
	protected Set<PropertyBinding> getPropertyBindings() {
		return propertyBindings;
	}

	protected void setPropertyBindings(Set<PropertyBinding> propertyBindings) {
		this.propertyBindings = propertyBindings;
	}

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	public ValueType getValueType() {
		return valueType;
	}

	public void setValueType(ValueType valueType) {
		this.valueType = valueType;
	}

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "PROPERTY_ID")
	public Set<Value> getValues() {
		return values;
	}

	public void setValues(Set<Value> values) {
		this.values = values;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	public boolean isConfidential() {
		return confidential;
	}

	public void setConfidential(boolean confidential) {
		this.confidential = confidential;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}
}