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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.AbstractDBTest;
import tools.xor.db.base.SimpleDefinition;
import tools.xor.db.base.SimpleDefinitionInfo;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-test.xml" })
@Transactional
public class JPADefinitionTest extends AbstractDBTest
{
	@PersistenceContext
	private EntityManager entityManager;

	@Test
	public void testEmbeddedId()
	{
		SimpleDefinition sd = new SimpleDefinition();
		entityManager.persist(sd);

		assertNotNull(sd.getId());

		SimpleDefinitionInfo info = new SimpleDefinitionInfo(sd.getId(), 1);
		Set<SimpleDefinitionInfo> lt = new HashSet<SimpleDefinitionInfo>();
		lt.add(info);
		sd.setDefinitionInfoList(lt);
		info.setSimpleDefinition(sd);
		entityManager.persist(sd);
		entityManager.flush();
		entityManager.clear();
		sd = entityManager.find(SimpleDefinition.class, sd.getId());

		assertTrue(sd != null);
		assertTrue(sd.getDefinitionInfoList().size() > 0);
	}

}
