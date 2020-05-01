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

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.ExtendedProperty;
import tools.xor.Type;
import tools.xor.db.base.Rate;
import tools.xor.db.base.Technician;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;


public class DefaultNarrowing extends AbstractDBTest {
	@Autowired
	protected AggregateManager aggregateManager;

	public void checkNarrowing() {	
		DataModel das = aggregateManager.getDataModel(); 

		Type technicianType = das.getShape().getType(Technician.class);
		ExtendedProperty rate = (ExtendedProperty) technicianType.getProperty("rate");		
		
		Type rateType = das.getShape().getType(Rate.class);
		ExtendedProperty person = (ExtendedProperty) rateType.getProperty("technician");
		
		assert(person != null);
		assert(Technician.class != person.getType().getInstanceClass());
		assert(person.isSymmetricalBiDirectionalType() == false);
		assert(rate.isSymmetricalBiDirectionalType() == false);
	}

}
