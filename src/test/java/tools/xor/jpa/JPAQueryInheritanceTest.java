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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.logic.DefaultQueryInheritance;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-VO-jpa-test.xml" })
@Transactional
public class JPAQueryInheritanceTest extends DefaultQueryInheritance {


	@Test
	public void queryTechnician() {
		super.queryTechnician();
	}

	@Test
	public void queryTaskSkill() {
		super.queryTaskSkill();
	}

	/*
	@Test
	public void testInheritanceOQL() {
		aggregateManager.checkPO(new Settings());

		JPAPersistenceOrchestrator po = (JPAPersistenceOrchestrator)aggregateManager.getPersistenceOrchestrator();
		EntityManager em = po.getEntityManager();

		//Query query = em.createQuery("SELECT _XOR_0.id, _XOR_0.name, _XOR_0.displayName, _XOR_0.description, _XOR_0.iconUrl, _XOR_0.detailedDescription, _XOR_1.name, _XOR_1.id, _XOR_2.id FROM tools.xor.db.pm.Task AS _XOR_1  LEFT OUTER JOIN _XOR_1.ownedBy AS _XOR_0 LEFT OUTER JOIN TREAT(_XOR_1.ownedBy AS tools.xor.db.base.Technician) _XOR_2");
		Query query = em.createQuery("SELECT _XOR_0.id, _XOR_0.name, _XOR_0.displayName, _XOR_0.description, _XOR_0.iconUrl, _XOR_0.detailedDescription, _XOR_1.name, _XOR_1.id FROM tools.xor.db.pm.Task AS _XOR_1  LEFT OUTER JOIN TREAT(_XOR_1.ownedBy AS tools.xor.db.base.Technician) _XOR_0");
		//Query query = em.createQuery("SELECT _XOR_0.id, _XOR_0.name, _XOR_0.displayName, _XOR_0.description, _XOR_0.iconUrl, _XOR_0.detailedDescription, _XOR_1.name, _XOR_1.id FROM tools.xor.db.pm.Task AS _XOR_1  LEFT OUTER JOIN _XOR_1.ownedBy AS _XOR_0 ORDER BY _XOR_1.id");

		query.getResultList();
	}
	*/
}
