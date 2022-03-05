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

package tools.xor.db.vo.common;

import java.util.HashSet;
import java.util.Set;

import tools.xor.db.vo.base.IdentityVO;
import tools.xor.db.vo.base.MetaEntityVO;

public class PropertyVO extends IdentityVO {

	private MetaEntityVO metaEntity;
	private Set<PropertyBindingVO> propertyBindings = new HashSet<PropertyBindingVO>();

	private ValueTypeVO valueType;
	private Set<ValueVO> values = new HashSet<ValueVO>();

	private int     orderIndex = -1;
	private boolean confidential;
	private boolean encrypted;

	public MetaEntityVO getMetaEntity() {
		return metaEntity;
	}

	public void setMetaEntity(MetaEntityVO value) {
		this.metaEntity = value;
	}

	protected Set<PropertyBindingVO> getPropertyBindings() {
		return propertyBindings;
	}

	protected void setPropertyBindings(Set<PropertyBindingVO> propertyBindings) {
		this.propertyBindings = propertyBindings;
	}

	public ValueTypeVO getValueType() {
		return valueType;
	}

	public void setValueType(ValueTypeVO valueType) {
		this.valueType = valueType;
	}

	public Set<ValueVO> getValues() {
		return values;
	}

	public void setValues(Set<ValueVO> values) {
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