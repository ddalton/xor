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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import tools.xor.db.base.Id;
import tools.xor.db.base.MetaEntity;

@Entity
public class PropertyBinding extends Id {
	
	private PropertyBindingType propertyBindingType;
	private MetaEntity artifact;
	private String artifactPropertyName;
	private Property property; // back reference

	public PropertyBinding() {
	}

	PropertyBinding(PropertyBindingType type, MetaEntity artifact, String artifactPropertyName) {
		this.propertyBindingType = type;
		this.artifact = artifact;
		this.artifactPropertyName = artifactPropertyName;
	}

	@ManyToOne(optional = false)
	public PropertyBindingType getPropertyBindingType() {
		return propertyBindingType;
	}

	public void setPropertyBindingType(PropertyBindingType propertyBindingType) {
		this.propertyBindingType = propertyBindingType;
	}

	@ManyToOne(optional = false)
	public MetaEntity getArtifact() {
		return artifact;
	}

	public void setArtifact(MetaEntity artifact) {
		this.artifact = artifact;
	}

	@Column(nullable = false)
	public String getArtifactPropertyName() {
		return artifactPropertyName;
	}

	public void setArtifactPropertyName(String artifactPropertyName) {
		this.artifactPropertyName = artifactPropertyName;
	}

	@ManyToOne(optional = false)
	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((propertyBindingType == null) ? 0 : propertyBindingType.hashCode());
		result = prime * result + ((artifact == null) ? 0 : artifact.hashCode());
		result = prime * result + ((artifactPropertyName == null) ? 0 : artifactPropertyName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj)) {
			if (!(obj instanceof PropertyBinding))
				return false;
			PropertyBinding other = (PropertyBinding) obj;
			if (propertyBindingType == null) {
				if (other.getPropertyBindingType() != null)
					return false;
			} else if (!propertyBindingType.equals(other.getPropertyBindingType()))
				return false;
			if (artifact == null) {
				if (other.getArtifact() != null)
					return false;
			} else if (artifact != other.getArtifact() && (artifact.getId() == null || !artifact.getId().equals(other.getArtifact().getId())) )
				return false;
			if (artifactPropertyName == null) {
				if (other.getArtifactPropertyName() != null)
					return false;
			} else if (!artifactPropertyName.equals(other.getArtifactPropertyName()))
				return false;
		}
		return true;
	}

}
