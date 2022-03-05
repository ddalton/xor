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

package tools.xor.jpa;

import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.logic.DefaultJson;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-JSON-jpa-test.xml" })
@Transactional
public class JPAJsonTest extends DefaultJson {

	@Test
	public void checkStringField() throws JSONException {
		super.checkStringField();
	}	
	
	@Test
	public void checkDateField() throws JSONException {
		super.checkDateField();
	}	
	
	@Test
	public void checkIntField() throws JSONException {
		super.checkIntField();
	}	
	
	@Test
	public void checkLongField() throws JSONException {
		super.checkLongField();
	}
	
	@Test
	public void checkEmptyLongField() throws JSONException {
		super.checkEmptyLongField();
	}
	
	@Test
	public void checkBooleanField() throws JSONException {
		super.checkBooleanField();
	}		
	
	@Test
	public void checkBigDecimalField() throws JSONException {
		super.checkBigDecimalField();
	}	
	
	@Test
	public void checkBigIntegerField() throws JSONException {
		super.checkBigIntegerField();
	}
	
	@Test
	public void checkEntityField() {
		super.checkEntityField();
	}
	
	@Test
	public void checkSetField() {
		super.checkSetField();
	}		
}
