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

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.AbstractDBTest;
import tools.xor.EntityType;
import tools.xor.MapperSide;
import tools.xor.MutableBO;
import tools.xor.Settings;
import tools.xor.TypeMapper;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.util.ObjectCreator;

public class DefaultBackPointer extends AbstractDBTest {

	public static final String A_NAME = "FIX_DEFECTS";
	public static final String B_NAME = "DEFECT 1";

	@Autowired
	protected AggregateManager aggregateService;

	// 2 objects
	private Task A;
	private Task B;


	/**
	 * Test the update of bi-directional OneToOne relationship involving 3 entities
	 * Corresponds to test case# 3-9, and 17 in OneToOneTestCombinations.docx document
	 *
	 */

	@BeforeEach
	public void setupData() {
		// create defect fixing Task
		if(A == null) {
			A = new Task();
			A.setName(A_NAME);
			A.setDisplayName("Fix defects");
			A.setDescription("Task to track the defect fixing effort");
		}

		// Create defect priority task
		if(B == null) {
			B = new Task();
			B.setName(B_NAME);
			B.setDisplayName("Defect 1");
			B.setDescription("The first defect to be filed");
		}	
	}

	@AfterEach
	public void resetAssociation() {
		A = null;
		B = null;
	}


	public void linkBackPointer (Object entity)
	{
        TypeMapper typeMapper = aggregateService.getDataModel().getTypeMapper().newInstance(MapperSide.EXTERNAL);
        ObjectCreator oc = new ObjectCreator(new Settings(), aggregateManager.getDataStore(), typeMapper);	    
		MutableBO dataObject = (MutableBO)oc.createDataObject(
			entity,
			(EntityType)oc.getType(entity.getClass()),
			null,
			null);
		oc.setShare(true);
		dataObject.createAggregate();
		dataObject.linkBackPointer();
	}

	public void oneToManyLink() {
		Set<Task> children = new HashSet<Task>();
		children.add(B);
		A.setTaskChildren(children);
		
		assert(B.getTaskParent() == null);
		linkBackPointer(A);

		// Check that B's parent is A		
		assert(A.getTaskChildren() != null && A.getTaskChildren().size() == 1);
		assert(B.getTaskParent() == A);
	}

	public void oneToOneLink() {
		A.setAuditTask(B);
		
		assert(B.getAuditedTask() == null);
		linkBackPointer(A);

		// Check that B's parent is A		
		assert(B.getAuditedTask() == A);
	}

}
