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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.FunctionType;
import tools.xor.Settings;
import tools.xor.db.base.Consultant;
import tools.xor.db.vo.base.ConsultantVO;
import tools.xor.db.vo.base.PersonVO;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.view.View;

public class DefaultPatch extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}

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
		View view = aggregateManager.getView("CONSULTANTINFO");
		view = view.copy();
		view.setSplitToRoot(false);
		settings.setView(view);
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

		// Make sure to evict since we don't want patch to conflict with the exiting
		// managed instance
		Set<Object> objects = new HashSet<>();
		objects.add(consultant);
		aggregateManager.getDataStore().clear(objects);
		
		return result;
	}

	private List createConsultantBulk() {
		final int BULKSIZE = 10;

		// create person
		List consultants = new ArrayList();

		for(int i = 0; i < BULKSIZE; i++) {
			Consultant consultant = new Consultant();
			consultant.setName(NAME+i);
			consultant.setDisplayName(DISPLAY_NAME+i);
			consultant.setDescription(DESCRIPTION+i);
			consultant.setUserName(USER_NAME+i);
			consultant.setField(FIELD);
			consultant.setType(TYPE);
			consultants.add(consultant);
		}

		consultants = (List)aggregateManager.create(consultants, new Settings());
		PersonVO person = new PersonVO();
		//person.setId(((Consultant)consultants.get(0)).getId());

		// read the person object using a DataObject
		Settings settings = new Settings();
		View view = aggregateManager.getView("CONSULTANTINFO");
		view = view.copy();
		view.setSplitToRoot(false);
		settings.setView(view);
		System.out.println("View object: " + settings.getView());

		// We order by name so the first person is guaranteed to end with "0"
		settings.addFunction(FunctionType.ASC, "name");
		List<?> toList = aggregateManager.query(person, settings);

		assert(toList.size() == 10);

		ConsultantVO result = null;
		if(ConsultantVO.class.isAssignableFrom(toList.get(0).getClass()))
			result = (ConsultantVO) toList.get(0);

		assert(result != null);
		assert(result.getName().equals(NAME+0));
		assert(result.getDisplayName().equals(DISPLAY_NAME+0));
		assert(result.getDescription().equals(DESCRIPTION+0));
		assert(result.getType().equals(TYPE));
		assert(result.getField() == null);

		// Make sure to evict since we don't want patch to conflict with the exiting
		// managed instance
		Set<Object> objects = new HashSet<>();
		objects.addAll(consultants);
		aggregateManager.getDataStore().clear(objects);

		return toList;
	}
	
	private Consultant updateConsultant(ConsultantVO result, String type, String field, Settings settings) {
		settings.setView(aggregateManager.getView("CONSULTANTINFO"));
		
		//TODO: might have to get version
		result.setType(type);
		result.setField(field);
		System.out.println("View object: " + settings.getView());

		List input = new ArrayList();
		input.add(result);
		Consultant consultant = (Consultant)(aggregateManager.patch(input, null, settings)).get(0);
		
		PersonVO person = new PersonVO();
		person.setId(result.getId());
		settings = new Settings();
		View view = aggregateManager.getView("CONSULTANTINFO");
		view = view.copy();
		view.setSplitToRoot(false);
		settings.setView(view);
		List<?> toList = aggregateManager.query(person, settings);

		assert(toList.size() == 1);

		if(ConsultantVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (ConsultantVO) toList.get(0);		
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getType().equals(type));
		assert(result.getField() == null);

		return consultant;
	}

	private Consultant updateConsultantBulk(List consultantVOList, String type, String field, Settings settings) {
		settings.setView(aggregateManager.getView("CONSULTANTINFO"));

		//TODO: might have to get version
		for(int i = 0; i < consultantVOList.size(); i++) {
			ConsultantVO result = (ConsultantVO)consultantVOList.get(i);
			result.setType(type);
			result.setField(field);
		}
		Consultant consultant = (Consultant)(aggregateManager.patch(consultantVOList, null, settings)).get(0);

		PersonVO person = new PersonVO();
		settings = new Settings();
		View view = aggregateManager.getView("CONSULTANTINFO");
		view = view.copy();
		view.setSplitToRoot(false);
		settings.setView(view);
		settings.addFunction(FunctionType.ASC, "name");
		List<?> toList = aggregateManager.query(person, settings);

		assert(toList.size() == 10);

		ConsultantVO result = null;
		if(ConsultantVO.class.isAssignableFrom(toList.get(0).getClass())) {
			result = (ConsultantVO)toList.get(0);

			assert (result != null);
			assert (result.getName().equals(NAME+0));
			assert (result.getDisplayName().equals(DISPLAY_NAME+0));
			assert (result.getType().equals(type));
			assert (result.getField() == null);
		}

		return consultant;
	}
		
	public void patchConsultant() {
		
		ConsultantVO result = createConsultant();
		updateConsultant(result, NEW_TYPE, NEW_FIELD, new Settings());
	}

	public void patchConsultantBulk() {

		List result = createConsultantBulk();
		updateConsultantBulk(result, NEW_TYPE, NEW_FIELD, new Settings());
	}
	
	/**
	 * This tests stale version by making 2 updates
	 */
	public void patchConsultantVersion() {
		ConsultantVO consultant = createConsultant();
		Consultant managedConsultant = updateConsultant(consultant, NEW_TYPE, NEW_FIELD, new Settings());
		
		Settings settings = new Settings();
		settings.setPreClear(true);
		ConsultantVO result = new ConsultantVO();
		result.setId(consultant.getId());
		result.setVersion(consultant.getVersion());

		assert(consultant.getVersion() != null);

		Set<Object> objects = new HashSet<>();
		objects.add(managedConsultant);
		aggregateManager.getDataStore().clear(objects);

		result.setName(consultant.getName());
		result.setDisplayName(consultant.getDisplayName());
		result.setDescription(NEW_DESCRIPTION2);
		result.setUserName(consultant.getUserName());
		managedConsultant = updateConsultant(result, NEW_TYPE2, NEW_FIELD2, settings);

		assert(managedConsultant.getDescription().equals(NEW_DESCRIPTION2));
	}
}
