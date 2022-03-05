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

import tools.xor.AggregateAction;
import tools.xor.annotation.XorDomain;
import tools.xor.annotation.XorExternal;
import tools.xor.annotation.XorLambda;

@Entity
public class Technician extends Person {

	private Rate rate;
	private String skill;
	private String comments;

	public String getSkill() {
		return skill;
	}

	public void setSkill(String skill) {
		this.skill = skill;
	}
	
	@XorLambda(property="skill", action={AggregateAction.READ}, capture=true)
	public static void defaultSkill(@XorDomain Technician current, @XorExternal Technician result ) {
		if(current.skill != null) { 
			result.skill = current.skill;
		} else {
			result.setSkill("ELECTRICIAN"); 
		};
	}
	
	@OneToOne(mappedBy="technician", cascade = CascadeType.ALL)
	public Rate getRate() {
		return rate;
	}

	public void setRate(Rate rate) {
		this.rate = rate;
	}
	
	@XorLambda(property="rate")
	public static void updateRate(@XorDomain Technician current, @XorExternal(path="rate") Rate rate) {
		current.rate = rate;
		current.comments = "SetRate";
	}

	public String getComments () {
		return comments;
	}

	public void setComments (String comments) {
		this.comments = comments;
	}
	
}
