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

import java.sql.Blob;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Person extends Identity {

	private String userName;

	public void setUserName(String value) {
		this.userName = value;
	}

	public String getUserName() {
		return this.userName;
	}

	private String commonName;

	public void setCommonName(String value) {
		this.commonName = value;
	}

	public String getCommonName() {
		return this.commonName;
	}

	private String password;

	public void setPassword(String value) {
		this.password = value;
	}

	public String getPassword() {
		return this.password;
	}

	@Lob
	private java.sql.Blob photo;

	public Blob getPhoto ()
	{
		return photo;
	}

	public void setPhoto (Blob photo)
	{
		this.photo = photo;
	}

	private String email;

	public void setEmail(String value) {
		this.email = value;
	}

	public String getEmail() {
		return this.email;
	}
}
