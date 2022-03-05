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

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

public class IdVO {

	private String id;

	@XmlElement(name = "id")
	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}
	
	private Long version;
	
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

	private PersonVO createdBy;

	public PersonVO getCreatedBy() {
		return this.createdBy;
	}

	public void setCreatedBy(PersonVO createdBy) {
		this.createdBy = createdBy;
	}

	private PersonVO updatedBy;

	public PersonVO getUpdatedBy() {
		return this.updatedBy;
	}

	public void setUpdatedBy(PersonVO updatedBy) {
		this.updatedBy = updatedBy;
	}


}
