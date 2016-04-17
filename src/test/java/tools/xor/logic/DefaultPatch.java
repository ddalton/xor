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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Consultant;
import tools.xor.db.vo.base.ConsultantVO;
import tools.xor.db.vo.base.PersonVO;
import tools.xor.service.AggregateManager;

public class DefaultPatch extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;	

	final String NAME = "GEORGE_HADE";
	final String DISPLAY_NAME = "George Hade";
	final String DESCRIPTION = "Technician to fix HVAC issues";
	final String USER_NAME = "ghade";
	final String TYPE = "CONSTRUCTION";
	final String FIELD = "HVAC";
	
	final String NEW_TYPE = "Engineering";
	final String NEW_FIELD = "Software";
	
	// 2nd update for stale version check
	final String NEW_TYPE2 = "Medicine";
	final String NEW_FIELD2 = "Doctor";
	final String NEW_DESCRIPTION2 = "Retrained to be a pediatrician";
	
	private ConsultantVO createConsultant() {
		// create person
		Consultant consultant = new Consultant();
		consultant.setName(NAME);
		consultant.setDisplayName(DISPLAY_NAME);
		consultant.setDescription(DESCRIPTION);
		consultant.setUserName(USER_NAME);
		consultant.setField(FIELD);
		consultant.setType(TYPE);

		consultant = (Consultant) aggregateManager.create(consultant, new Settings());
		PersonVO person = new PersonVO();
		person.setId(consultant.getId());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("CONSULTANTINFO"));
		System.out.println("View object: " + settings.getView());
		List<?> toList = aggregateManager.query(person, settings);

		assert(toList.size() == 1);

		ConsultantVO result = null;
		if(ConsultantVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (ConsultantVO) toList.get(0);
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
		assert(result.getType().equals(TYPE));
		assert(result.getField() == null);
		
		return result;
	}
	
	private void updateConsultant(ConsultantVO result, String type, String field, Settings settings) {
		settings.setView(aggregateManager.getView("CONSULTANTINFO"));
		
		//TODO: might have to get version
		result.setType(type);
		result.setField(field);
		System.out.println("View object: " + settings.getView());
		aggregateManager.patch(result, settings);
		
		PersonVO person = new PersonVO();
		person.setId(result.getId());
		settings = new Settings();
		settings.setView(aggregateManager.getView("CONSULTANTINFO"));		
		List<?> toList = aggregateManager.query(person, settings);

		assert(toList.size() == 1);

		if(ConsultantVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (ConsultantVO) toList.get(0);		
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getType().equals(type));
		assert(result.getField() == null);
	}
		
	public void patchConsultant() {
		
		ConsultantVO result = createConsultant();
		updateConsultant(result, NEW_TYPE, NEW_FIELD, new Settings());
	}
	
	/**
	 * This tests stale version by making 2 updates
	 */
	public void patchConsultantVersion() {
		ConsultantVO consultant = createConsultant();
		updateConsultant(consultant, NEW_TYPE, NEW_FIELD, new Settings());
		
		Settings settings = new Settings();
		settings.setPreClear(true);
		ConsultantVO result = new ConsultantVO();
		result.setId(consultant.getId());
		result.setName(consultant.getName());
		result.setDisplayName(consultant.getDisplayName());
		result.setDescription(NEW_DESCRIPTION2);
		result.setUserName(consultant.getUserName());
		updateConsultant(result, NEW_TYPE2, NEW_FIELD2, settings);
	}
}
