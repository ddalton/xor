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

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.Embedded;
import javax.persistence.Entity;

@Entity
public class Employee extends Person {

	LocationDetails location;

	@Embedded
	public LocationDetails getLocation() {
		return location;
	}

	public void setLocation(LocationDetails location) {
		this.location = location;
	}

	private int employeeNo;
	
	public int getEmployeeNo() {
		return employeeNo;
	}

	public void setEmployeeNo(int employeeNo) {
		this.employeeNo = employeeNo;
	}

	private Long salary;
	
	public Long getSalary() {
		return salary;
	}

	public void setSalary(Long salary) {
		this.salary = salary;
	}

	private BigDecimal largeDecimal;
	
	public BigDecimal getLargeDecimal() {
		return largeDecimal;
	}

	public void setLargeDecimal(BigDecimal largeDecimal) {
		this.largeDecimal = largeDecimal;
	}

	private BigInteger largeInteger;	
	
	public BigInteger getLargeInteger() {
		return largeInteger;
	}

	public void setLargeInteger(BigInteger largeInteger) {
		this.largeInteger = largeInteger;
	}

}
