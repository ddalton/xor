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

import tools.xor.db.vo.base.IdVO;
import tools.xor.db.vo.base.MetaEntityVO;

public class PropertyBindingVO extends IdVO {
	
	private PropertyBindingTypeVO propertyBindingType;
	private MetaEntityVO artifact;
	private String artifactPropertyName;
	private PropertyVO property; // back reference

	public PropertyBindingVO() {
	}

	PropertyBindingVO(PropertyBindingTypeVO type, MetaEntityVO artifact, String artifactPropertyName) {
		this.propertyBindingType = type;
		this.artifact = artifact;
		this.artifactPropertyName = artifactPropertyName;
	}

	public PropertyBindingTypeVO getPropertyBindingType() {
		return propertyBindingType;
	}

	public void setPropertyBindingType(PropertyBindingTypeVO propertyBindingType) {
		this.propertyBindingType = propertyBindingType;
	}

	public MetaEntityVO getArtifact() {
		return artifact;
	}

	public void setArtifact(MetaEntityVO artifact) {
		this.artifact = artifact;
	}

	public String getArtifactPropertyName() {
		return artifactPropertyName;
	}

	public void setArtifactPropertyName(String artifactPropertyName) {
		this.artifactPropertyName = artifactPropertyName;
	}

	public PropertyVO getProperty() {
		return property;
	}

	public void setProperty(PropertyVO property) {
		this.property = property;
	}
}
