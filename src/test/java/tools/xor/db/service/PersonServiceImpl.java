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

package tools.xor.db.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.db.base.Person;
import tools.xor.db.dao.PersonDao;


@Service("personService")
public class PersonServiceImpl implements PersonService {
	private static final Logger logger = LogManager.getLogger(new Exception()
			.getStackTrace()[0].getClassName());
	
	@Autowired private PersonDao personDao;	

	@Override
	@Transactional
	public Person createPerson(Person person) {
		logger.debug("Entering method createPerson(Person person).");

		person = personDao.saveOrUpdate(person); 

		return person;
	}

	@Override
	@Transactional
	public Person getPerson(String personId) {
		logger.debug("Entering method getPerson(String personId).");
		Person person = personDao.findById(personId);

		return person;
	}

}
