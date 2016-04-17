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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

import tools.xor.annotation.XorRead;
import tools.xor.annotation.XorUpdate;
import tools.xor.annotation.XorInput;

@Entity
public class Technician extends Person {

	private Rate rate;
	private String skill;
	private String comment;

	public String getSkill() {
		return skill;
	}

	public void setSkill(String skill) {
		this.skill = skill;
	}
	
	@XorRead(property="skill")
	public String defaultSkill() {
		return skill != null ? skill : "ELECTRICIAN";
	}
	
	@OneToOne(mappedBy="technician", cascade = CascadeType.ALL)
	public Rate getRate() {
		return rate;
	}

	public void setRate(Rate rate) {
		this.rate = rate;
	}
	
	@XorUpdate(property="rate")
	public void updateRate(@XorInput Rate rate) {
		this.rate = rate;
		this.comment = "SetRate";
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	
}
