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

package tools.xor.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Person;
import tools.xor.service.AggregateManager;

public class DefaultPaging extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;	

	final String NAME1 = "GEORGE_HADE_1";
	final String DISPLAY_NAME1 = "George Hade 1";
	
	final String NAME2 = "GEORGE_HADE_2";
	final String DISPLAY_NAME2 = "George Hade 2";

	final String NAME3 = "GEORGE_HADE_3";
	final String DISPLAY_NAME3 = "George Hade 3";	
	
	final String DESCRIPTION = "Technician to fix HVAC issues";
	final String USER_NAME = "ghade";
	final String TYPE = "CONSTRUCTION";
	final String FIELD = "HVAC";
		
	public void sortPersonAsc() {
		
		// create person
		Person person = new Person();
		person.setName(NAME1);
		person.setDisplayName(DISPLAY_NAME1);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		// Create person 1
		aggregateManager.create(person, new Settings());

		// Create person 2
		person.setName(NAME2);
		person.setDisplayName(DISPLAY_NAME2);
		aggregateManager.create(person, new Settings());

		// Create person 3
		person.setName(NAME3);
		person.setDisplayName(DISPLAY_NAME3);
		aggregateManager.create(person, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("PERSON_ASC"));
		List<?> toList = aggregateManager.query(new Person(), settings);

		assert(toList.size() == 3);
		
		assert( ((Person)toList.get(0)).getName().equals(NAME1) );
	}
	
	public void sortPersonDesc() {
		// create person
		Person person = new Person();
		person.setName(NAME1);
		person.setDisplayName(DISPLAY_NAME1);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		// Create person 1
		aggregateManager.create(person, new Settings());

		// Create person 2
		person.setName(NAME2);
		person.setDisplayName(DISPLAY_NAME2);
		aggregateManager.create(person, new Settings());

		// Create person 3
		person.setName(NAME3);
		person.setDisplayName(DISPLAY_NAME3);
		aggregateManager.create(person, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("PERSON_DESC"));
		List<?> toList = aggregateManager.query(new Person(), settings);

		assert(toList.size() == 3);
		
		assert( ((Person)toList.get(0)).getName().equals(NAME3) );		
	}
	
	public void pagePerson() {
		// create person
		Person person = new Person();
		person.setName(NAME1);
		person.setDisplayName(DISPLAY_NAME1);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		// Create person 1
		aggregateManager.create(person, new Settings());

		// Create person 2
		person.setName(NAME2);
		person.setDisplayName(DISPLAY_NAME2);
		aggregateManager.create(person, new Settings());

		// Create person 3
		person.setName(NAME3);
		person.setDisplayName(DISPLAY_NAME3);
		aggregateManager.create(person, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("PERSON_DESC"));
		settings.setLimit(2);
		List<?> toList = aggregateManager.query(new Person(), settings);
		
		assert(toList.size() == 2);
		
		// We proceed from the nextToken, so we should return the remaining single row
		toList = aggregateManager.query(new Person(), settings);
		assert(toList.size() == 1);
	}

	public void offsetPerson() {
		// create person
		Person person = new Person();
		person.setName(NAME1);
		person.setDisplayName(DISPLAY_NAME1);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		// Create person 1
		aggregateManager.create(person, new Settings());

		// Create person 2
		person.setName(NAME2);
		person.setDisplayName(DISPLAY_NAME2);
		aggregateManager.create(person, new Settings());

		// Create person 3
		person.setName(NAME3);
		person.setDisplayName(DISPLAY_NAME3);
		aggregateManager.create(person, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("PERSON_DESC"));
		settings.setOffset(2);
		List<?> toList = aggregateManager.query(new Person(), settings);
		
		assert(toList.size() == 1);		
	}
	
	public void customOffsetPerson() {
		// create person
		Person person = new Person();
		person.setName(NAME1);
		person.setDisplayName(DISPLAY_NAME1);
		person.setDescription(DESCRIPTION);
		person.setUserName(USER_NAME);

		// Create person 1
		aggregateManager.create(person, new Settings());

		// Create person 2
		person.setName(NAME2);
		person.setDisplayName(DISPLAY_NAME2);
		aggregateManager.create(person, new Settings());

		// Create person 3
		person.setName(NAME3);
		person.setDisplayName(DISPLAY_NAME3);
		aggregateManager.create(person, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("PERSON_DESC"));
		Map<String, Object> nextToken = new HashMap<String, Object>();
		nextToken.put("name", NAME2);
		settings.setNextToken(nextToken);
		List<?> toList = aggregateManager.query(new Person(), settings);
		
		assert(toList.size() == 1);		
	}	
}
