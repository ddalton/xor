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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlElement;

import org.hibernate.annotations.GenericGenerator;

@MappedSuperclass
public class Id {

	private String id;

	@XmlElement(name = "id")
	@GeneratedValue(generator = "system-uuid")
	@GenericGenerator(name = "system-uuid", strategy = "uuid")
	@Column(name = "UUID")
	@javax.persistence.Id
	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}
	
	private Long version;
	
	@Version
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}	

	private Date createdOn;

	public void setCreatedOn(Date value) {
		this.createdOn = value;
	}

	public Date getCreatedOn() {
		return this.createdOn;
	}

	private Date updatedOn;

	public void setUpdatedOn(Date value) {
		this.updatedOn = value;
	}

	public Date getUpdatedOn() {
		return this.updatedOn;
	}

	private Person createdBy;

	@ManyToOne
	public Person getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(Person createdBy) {
		this.createdBy = createdBy;
	}

	private Person updatedBy;

	@ManyToOne
	public Person getUpdatedBy() {
		return this.updatedBy;
	}

	public void setUpdatedBy(Person updatedBy) {
		this.updatedBy = updatedBy;
	}


}
