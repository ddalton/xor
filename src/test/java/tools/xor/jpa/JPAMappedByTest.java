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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.AssociationSetting;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.db.base.Department;
import tools.xor.db.common.Head;
import tools.xor.logic.DefaultMappedBy;
import tools.xor.service.DataModel;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@Transactional
public class JPAMappedByTest extends DefaultMappedBy {

	@PersistenceContext
	EntityManager entityManager;

	@Test
	public void checkOneToMany() {
		super.checkOneToMany();		
	}
	
	@Test
	public void checkOneToOne() {
		super.checkOneToOne();
	}	
	
	@Test
	public void checkManyToMany() {
		super.checkManyToMany();
	}
	
	@Test
	public void checkOneToOneEmbedded() {
		super.checkOneToOneEmbedded();
	}	
	
	@Test
	public void checkImmutable() {
		super.checkImmutable();
	}	
	
	@Test
	public void checkListIndex() {
		super.checkListIndex();
	}

	@Test
	public void saveRequired() {
		Head h = new Head();
		h.setName("Isaac Newton");

		Department d = new Department();
		d.setName("Mathematics");
		d.setHead(h);

		//entityManager.persist(d);

		DataModel das = aggregateManager.getDataModel();
		Settings settings = das.settings().base(Department.class)
			.expand(new AssociationSetting(Head.class))
			.build();

		/*
		Type deptType = das.getType(Department.class);
		Settings settings = new Settings();
		settings.setEntityType(deptType);
		settings.expand(new AssociationSetting(Head.class));
		settings.init(das.getShape());
*/
		aggregateManager.create(d, settings);
	}

	@Test
	public void deleteRequired() {
		Head h = new Head();
		h.setName("Isaac Newton");

		Department d = new Department();
		d.setName("Mathematics");
		d.setHead(h);

		DataModel das = aggregateManager.getDataModel();
		Type deptType = das.getShape().getType(Department.class);
		Settings settings = new Settings();
		settings.setPostFlush(true);
		settings.setEntityType(deptType);
		settings.expand(new AssociationSetting(Head.class));
		settings.init(das.getShape());

		d = (Department)aggregateManager.create(d, settings);

		// Delete
		aggregateManager.delete(d, settings);


	}
}
