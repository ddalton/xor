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

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.base.Rate;
import tools.xor.db.base.Technician;
import tools.xor.service.AggregateManager;

public class DefaultBusinessLogic extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateService;
	
	protected void checkReadLogic() {
		Rate rate = new Rate();
		rate.setHourlyRate(new BigDecimal(70));
		
		// create person
		Technician technician = new Technician();
		technician.setName("DILIP_DALTON");
		technician.setDisplayName("Dilip Dalton");
		technician.setDescription("Software engineer in the bay area");
		technician.setUserName("daltond");
		technician.setRate(rate);
		technician = (Technician) aggregateService.create(technician, new Settings());	
		technician = (Technician) aggregateService.read(technician, new Settings());
		
		assert(technician.getId() != null);
		
		String skill = technician.getSkill();
		assert(skill != null);
		assert(skill.equals("ELECTRICIAN"));
	}
	
	protected void checkUpdateLogic() {
		Rate rate = new Rate();
		rate.setHourlyRate(new BigDecimal(70));
		
		// create person
		Technician technician = new Technician();
		technician.setName("DILIP_DALTON");
		technician.setDisplayName("Dilip Dalton");
		technician.setDescription("Software engineer in the bay area");
		technician.setUserName("daltond");
		technician.setRate(rate);
		technician = (Technician) aggregateService.create(technician, new Settings());	
		technician = (Technician) aggregateService.read(technician, new Settings());
		
		assert(technician.getId() != null);
		
		String comment = technician.getComments();
		assert(comment != null);
		assert(comment.equals("SetRate"));
	}

}
