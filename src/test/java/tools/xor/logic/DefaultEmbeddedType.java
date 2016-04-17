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

import tools.xor.Settings;
import tools.xor.db.base.Employee;
import tools.xor.db.base.LocationDetails;
import tools.xor.db.base.ParkingSpot;
import tools.xor.service.AggregateManager;

public class DefaultEmbeddedType {
	@Autowired
	protected AggregateManager aggregateService;	

	public void createEmbeddedType() {

		// create person
		Employee thilfiger = new Employee();
		thilfiger.setName("TOMMY_HILFIGHER");
		thilfiger.setDisplayName("Tommy Hilfigher");
		thilfiger.setDescription("A famous fashion designer");
		thilfiger.setUserName("thilf");
		thilfiger = (Employee) aggregateService.create(thilfiger, new Settings());	
		thilfiger = (Employee) aggregateService.read(thilfiger, new Settings());			

	    // Parking spot
		ParkingSpot specialSpot = new ParkingSpot();
		specialSpot.setGarage("THILFIGER SPOT");
		specialSpot.setAssignedTo(thilfiger);
		
	    // location 
		LocationDetails location = new LocationDetails();
		location.setOfficeNumber(12);
		location.setParkingSpot(specialSpot);		
		thilfiger.setLocation(location);

		specialSpot = (ParkingSpot) aggregateService.create(specialSpot, new Settings());			
		thilfiger = (Employee) aggregateService.update(thilfiger, new Settings());	
		
        assert(Employee.class.isAssignableFrom(thilfiger.getClass()));		
	}
}
